<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { assetsApi } from '@/api/assets'
import { chatApi } from '@/api/chat'
import type { CollaborationProposalPayload } from '@/api/chat'
import { courseApi } from '@/api/course'
import { liveApi, type LiveReminderItemResp, type LiveSessionResp } from '@/api/live'
import { scheduleApi } from '@/api/schedule'
import type { CourseItemVO, ScheduleEventStatus, ScheduleEventVO, TutorApplicationVO, UserCardVO, UserSimpleVO } from '@/api/types'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import LessonDetailModal from '@/ui/course/LessonDetailModal.vue'
import CollaborationProposalModal from '@/ui/chat/CollaborationProposalModal.vue'
import { normalizeAvatarUrl } from '@/utils/avatar'
import { buildLessonDetailModel, findPreviousLesson, findRecentLesson } from '@/utils/lessonDetail'

type CourseStageKey =
  | 'WAIT_PAY'
  | 'COMMUNICATING'
  | 'TRIALING'
  | 'TRIAL_CONFIRMING'
  | 'TRIAL_WAIT_WEEKLY_SCHEDULE'
  | 'TRIAL_FAILED'
  | 'TEACHING'
  | 'FINISHED'
  | 'REFUND_REVIEW'
  | 'REFUNDED'
  | 'TRIAL_REFUND_REVIEW'
  | 'UNKNOWN'

type StageMeta = {
  key: CourseStageKey
  label: string
  tone: 'slate' | 'amber' | 'sky' | 'violet' | 'emerald' | 'rose'
  description: string
  actionLabel: string
}

type CourseViewModel = {
  raw: CourseItemVO
  courseId: number
  applicationId: number
  roomId: number | null
  teacherUid: number
  studentUid: number
  participantUid: number
  participantName: string
  participantRoleLabel: string
  participantSubtitle: string
  avatar: string
  stage: StageMeta
  headline: string
  summary: string
  latestLesson: ScheduleEventVO | null
  lessonList: ScheduleEventVO[]
  lessonTimeText: string
  lessonStatusText: string
  lessonStateTone: 'slate' | 'amber' | 'sky' | 'violet' | 'emerald' | 'rose'
  currentStateNote: string
  currentActionHint: string
  trialCountdown: string
  countdownText: string
  live: LiveSessionResp | null
  reminder: LiveReminderItemResp | null
  aiResultStatus: string | null
  aiPreview: string | null
  canEnterClassroom: boolean
  canEndClassroom: boolean
  endBlockedReason: string
  showAbnormalAttendanceConfirm: boolean
  abnormalAttendanceHint: string
  afterClassStatusLabel: string
  afterClassStatusTone: string
  afterClassHint: string
  payDeadlineAt: string | null
  payExpired: boolean
  archiveReason: string | null
  latestRefund: CourseItemVO['latestRefund']
  latestProposal: CourseItemVO['latestProposal']
}

type ProcessStepState = 'done' | 'current' | 'upcoming' | 'alert'

type CourseProcessStep = {
  key: string
  label: string
  caption: string
  state: ProcessStepState
}

type CourseActionKey =
  | 'PAY'
  | 'CHAT'
  | 'START_COLLABORATION'
  | 'ENTER_CLASSROOM'
  | 'END_CLASSROOM'
  | 'TRIAL_PASS'
  | 'TRIAL_FAIL'
  | 'VIEW_SUMMARY'
  | 'RETRY_SUMMARY'
  | 'VIEW_TIMELINE'
  | 'VIEW_DETAIL'
  | 'SCHEDULE'
  | 'APPLY_INFO_REFUND'
  | 'EDIT_DEMAND'

type CourseActionModel = {
  key: CourseActionKey
  label: string
  tone: 'primary' | 'secondary' | 'danger'
  disabled?: boolean
}

type CourseJourneyModel = {
  badge: string
  title: string
  description: string
  nextStepLabel: string
  nextStepHint: string
  tips: string[]
  steps: CourseProcessStep[]
  primaryAction: CourseActionModel | null
  secondaryAction: CourseActionModel | null
  tertiaryAction: CourseActionModel | null
}

type StageContextModel = {
  owner: string
  normalFlow: string
  branchHint: string
  branchTone: 'neutral' | 'warn' | 'danger' | 'success'
}

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const isTeacher = computed(() => auth.user?.userType === 1)
const role = computed<'TEACHER' | 'STUDENT'>(() => (isTeacher.value ? 'TEACHER' : 'STUDENT'))

const loading = ref(false)
const error = ref<string | null>(null)
const emailHint = ref<{ show: boolean; title?: string; description?: string; actionText?: string } | null>(null)
const list = ref<CourseItemVO[]>([])
const userMap = ref<Record<number, UserSimpleVO>>({})
const userCardMap = ref<Record<number, UserCardVO | null>>({})
const applicationMap = ref<Record<number, TutorApplicationVO | null>>({})
const lessonListMap = ref<Record<number, ScheduleEventVO[]>>({})
const lessonMap = ref<Record<number, ScheduleEventVO | null>>({})
const liveMap = ref<Record<number, LiveSessionResp | null>>({})
const reminderMap = ref<Record<number, LiveReminderItemResp | null>>({})
const aiResultMap = ref<Record<number, { resultStatus?: string | null; preview?: string | null; sessionId?: number | null }>>({})
const avatarBroken = ref<Record<number, boolean>>({})

const stageFilter = ref<'ALL' | 'ACTIVE' | CourseStageKey>('ALL')
const search = ref('')
const selectedCourseId = ref<number | null>(null)
const actionBusyCourseId = ref<number | null>(null)

const scheduleOpen = ref(false)
const scheduleBusy = ref(false)
const scheduleError = ref<string | null>(null)
const scheduleCourseId = ref<number | null>(null)
const scheduleTitle = ref('')
const scheduleDescription = ref('')
const scheduleStartAt = ref(roundToNextHalfHour(Date.now()))
const scheduleEndAt = ref(scheduleStartAt.value + 60 * 60 * 1000)
const scheduleReminderMinutes = ref(30)

const modalOpen = ref(false)
const modalCourseId = ref<number | null>(null)
const modalReason = ref('')
const modalFiles = ref<File[]>([])
const modalVideoUrl = ref('')
const modalVideoDurationSeconds = ref<number | null>(60)
const modalBusy = ref(false)
const modalErr = ref<string | null>(null)

const infoRefundOpen = ref(false)
const infoRefundCourse = ref<CourseViewModel | null>(null)
const infoRefundReason = ref('')
const infoRefundBusy = ref(false)
const infoRefundErr = ref<string | null>(null)

const proposalBusyId = ref<number | null>(null)
const proposalErr = ref<string | null>(null)
const proposalEditOpen = ref(false)
const proposalEditCourse = ref<CourseViewModel | null>(null)
const proposalEditBusy = ref(false)
const proposalEditErr = ref<string | null>(null)
const proposalEditInitial = ref<{ pricePerHour: string; trialStartAt: number; trialEndAt: number; remark?: string | null } | null>(null)

const liveTimelineOpen = ref(false)
const liveTimelineBusy = ref(false)
const liveTimelineError = ref<string | null>(null)
const liveTimelineItems = ref<Array<{ eventType: string; eventSource: string; occurredAt: string }>>([])
const liveTimelineCourseId = ref<number | null>(null)

const endModalOpen = ref(false)
const endModalBusy = ref(false)
const endModalCountdown = ref(3)
const endModalErr = ref<string | null>(null)
const endModalCourseId = ref<number | null>(null)
const lessonModalOpen = ref(false)
const lessonModalCourseId = ref<number | null>(null)
const lessonModalLessonId = ref<number | null>(null)
let endModalTimer: number | null = null

function roundToNextHalfHour(nowMs: number) {
  const d = new Date(nowMs)
  d.setSeconds(0, 0)
  const m = d.getMinutes()
  const next = m <= 0 ? 0 : m <= 30 ? 30 : 60
  d.setMinutes(next)
  if (next === 60) d.setHours(d.getHours() + 1, 0, 0, 0)
  return d.getTime()
}

function fmtDateTime(value: string | number | Date | null | undefined, opts?: Intl.DateTimeFormatOptions) {
  if (value == null) return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...opts,
  }).format(d)
}

function formatDuration(startAt: number, endAt: number) {
  const minutes = Math.max(0, Math.round((endAt - startAt) / 60000))
  if (!minutes) return '未设置时长'
  if (minutes % 60 === 0) return `${minutes / 60} 小时`
  if (minutes > 60) return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分`
  return `${minutes} 分钟`
}

function formatCountdown(deltaMs: number) {
  if (deltaMs <= 0) return '已到预约开始时间'
  const totalMinutes = Math.ceil(deltaMs / 60000)
  if (totalMinutes < 60) return `距开课 ${totalMinutes} 分钟`
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (!minutes) return `距开课 ${hours} 小时`
  return `距开课 ${hours} 小时 ${minutes} 分钟`
}

function displayNameOf(user: UserSimpleVO | null | undefined, uid: number) {
  const realName = String(user?.realName || '').trim()
  if (realName) return realName
  const name = String(user?.name || '').trim()
  if (name) return name
  return `用户${uid}`
}

function avatarOf(uid: number, raw?: string | null) {
  if (avatarBroken.value[uid]) return ''
  return normalizeAvatarUrl(raw)
}

function markAvatarBroken(uid: number) {
  avatarBroken.value = { ...avatarBroken.value, [uid]: true }
}

function trialExpired(trialEndAt?: string | null) {
  if (!trialEndAt) return false
  const t = Date.parse(trialEndAt)
  return Number.isFinite(t) ? Date.now() > t : false
}

function moneyFenText(amount?: number | null) {
  if (amount == null) return ''
  return `${(amount / 100).toFixed(amount % 100 === 0 ? 0 : 2)} 元`
}

function currentUserAcceptedCopy(item: CourseViewModel) {
  const application = applicationMap.value[item.applicationId]
  return application?.senderUid === auth.user?.id ? '对方已经通过你的沟通申请啦' : '你已经通过对方的沟通申请啦'
}

function waitPayDescription(item: CourseViewModel) {
  const prefix = currentUserAcceptedCopy(item)
  if (isTeacher.value) {
    return `${prefix}，请支付信息费以开始聊天，在平台校对更细的细节并推进后续的流程。`
  }
  return `${prefix}，请等待教师支付信息费以开启后续的流程。`
}

function waitPayTip() {
  if (isTeacher.value) {
    return '如若沟通不合适，发起申请退款平台承诺在8小时内全额退还，平均退款时长23分钟。'
  }
  return '如果教师在48小时内没有支付信息费用，该项目进入归档状态（流程停止）。'
}

function parseMaybeDateMs(value?: string | number | Date | null) {
  if (value == null) return NaN
  const ms = value instanceof Date ? value.getTime() : typeof value === 'number' ? value : Date.parse(value)
  return Number.isFinite(ms) ? ms : NaN
}

function proposalFromMe(item: CourseViewModel) {
  return item.latestProposal?.fromUid === auth.user?.id
}

function proposalNeedsMe(item: CourseViewModel) {
  return item.latestProposal?.toUid === auth.user?.id
}

function proposalStatusText(status?: string | null, fromMe = false) {
  const s = String(status || '').trim().toUpperCase()
  if (s === 'PENDING') return fromMe ? '等待对方确认' : '待你确认'
  if (s === 'ACCEPTED') return '已同意'
  if (s === 'REJECTED') return '已拒绝'
  return '状态同步中'
}

function refundStatusText(status?: string | null) {
  const s = String(status || '').trim().toUpperCase()
  if (s === 'PENDING' || s === 'REVIEWING' || s === 'REFUND_REVIEW') return '退费审批中'
  if (s === 'APPROVED' || s === 'REFUNDED' || s === 'SUCCESS') return '审批通过，已退费'
  if (s === 'REJECTED' || s === 'FAILED') return '审批不通过'
  return s || '状态同步中'
}

function refundTypeText(type?: string | null) {
  const t = String(type || '').trim().toUpperCase()
  if (t === 'BROKERAGE' || t === 'INFO_FEE') return '信息费退费'
  if (t === 'TRIAL' || t === 'TRIAL_REFUND') return '试课后信息费处理'
  return '退费申请'
}

function isTrialRefundType(type?: string | null) {
  return String(type || '').trim().toUpperCase().includes('TRIAL')
}

function trialFeePolicyText() {
  return '按 1 小时课时费私下结算'
}

function refundSectionTitle(refund?: CourseItemVO['latestRefund']) {
  return isTrialRefundType(refund?.type) ? '信息费处理' : '退费信息'
}

function refundSectionDesc(refund?: CourseItemVO['latestRefund']) {
  if (isTrialRefundType(refund?.type)) {
    return '这里处理的是平台信息费，不是试课课时费。试课费仍按 1 小时课时费由双方私下结算，平台不代收也不生成课时费账单。'
  }
  return '退费申请会同步影响聊天页和我的课程页。审核中不再展示继续推进合作的动作；审核完成后这里展示最终结果和原因。'
}

function isStudentInfoFeeRefundFlow(item: CourseViewModel) {
  if (isTeacher.value || !item.latestRefund) return false
  if (isTrialRefundType(item.latestRefund.type)) return false
  return item.stage.key === 'REFUND_REVIEW' || item.stage.key === 'REFUNDED'
}

function linkedDemandId(item: CourseViewModel) {
  const application = applicationMap.value[item.applicationId]
  if (application?.contextType === 'DEMAND' && application.contextId > 0) return application.contextId
  return null
}

function proposalTimeText(item: CourseViewModel) {
  const proposal = item.latestProposal
  if (!proposal) return '待确认'
  const start = parseMaybeDateMs(proposal.trialStartAt)
  const end = parseMaybeDateMs(proposal.trialEndAt)
  if (Number.isFinite(start) && Number.isFinite(end)) {
    return `${fmtDateTime(start)} - ${fmtDateTime(end, { hour: '2-digit', minute: '2-digit' })}`
  }
  return proposal.classTime || '待确认'
}

function courseTags(item: CourseViewModel) {
  const tags: Array<{ text: string; tone?: 'primary' | 'warn' | 'danger' | 'violet' | 'success' }> = [
    { text: buildJourneyModel(item).badge, tone: 'primary' },
  ]
  if (item.payExpired) tags.push({ text: '已归档：教师超时未支付信息费', tone: 'danger' })
  if (item.stage.key === 'REFUND_REVIEW') tags.push({ text: isStudentInfoFeeRefundFlow(item) ? '需求已重新开放' : '退费审批中', tone: isStudentInfoFeeRefundFlow(item) ? 'success' : 'warn' })
  if (item.latestProposal?.status === 'PENDING') tags.push({ text: proposalFromMe(item) ? '等待对方确认合作' : '合作待你确认', tone: 'warn' })
  if (item.stage.key === 'TRIAL_FAILED') tags.push({ text: '试课未通过', tone: 'danger' })
  if (item.stage.key === 'TRIAL_REFUND_REVIEW') tags.push({ text: '信息费处理中', tone: 'warn' })
  if (item.stage.key === 'REFUNDED') tags.push({ text: isStudentInfoFeeRefundFlow(item) ? '需求已重新开放' : '已退费', tone: 'success' })
  if (item.countdownText && item.canEnterClassroom) tags.push({ text: item.countdownText })
  if (item.showAbnormalAttendanceConfirm) tags.push({ text: '异常待处理', tone: 'warn' })
  else if (item.aiResultStatus === 'FAILED') tags.push({ text: '课后生成失败', tone: 'danger' })
  return tags
}

function displayStageLabel(item: CourseViewModel) {
  if (isStudentInfoFeeRefundFlow(item)) return '沟通已结束'
  return item.stage.label
}

function resolveCourseStage(rawStatus: string, trialEndAt?: string | null): StageMeta {
  const status = String(rawStatus || '').trim().toUpperCase()
  if (status === 'APPLYING') return { key: 'UNKNOWN', label: '申请中', tone: 'slate', description: '等待对方确认申请，确认后才会进入我的课程。', actionLabel: '查看详情' }
  if (status === 'WAIT_PAY') return { key: 'WAIT_PAY', label: '待支付信息费', tone: 'amber', description: '支付信息费后才会进入正式沟通。', actionLabel: '去支付' }
  if (status === 'COMMUNICATING') return { key: 'COMMUNICATING', label: '沟通中', tone: 'sky', description: '双方已进入聊天沟通，可继续了解需求或发起合作。', actionLabel: '进入聊天' }
  if (status === 'TRIALING' && trialExpired(trialEndAt)) return { key: 'TRIAL_CONFIRMING', label: '试课待确认', tone: 'violet', description: '试课已到结束时间，等待学生确认结果。', actionLabel: '查看试课' }
  if (status === 'TRIALING') return { key: 'TRIALING', label: '试课阶段', tone: 'violet', description: '合作已建立，试课日程会直接展示在双方课表与课程页中。', actionLabel: '查看试课' }
  if (status === 'TRIAL_WAIT_STUDENT_DECISION') return { key: 'TRIAL_CONFIRMING', label: '待学生确认试课结果', tone: 'violet', description: '试课已结束，需要学生确认是否继续合作。', actionLabel: '查看试课' }
  if (status === 'TRIAL_WAIT_WEEKLY_SCHEDULE') return { key: 'TRIAL_WAIT_WEEKLY_SCHEDULE', label: '正式课待确认', tone: 'violet', description: '试课已通过，等待确认后续正式课。', actionLabel: '确认课表' }
  if (status === 'TRIAL_FAILED') return { key: 'TRIAL_FAILED', label: '试课未通过', tone: 'rose', description: '本次试课未继续，课程合作将在售后处理后结束。', actionLabel: '查看详情' }
  if (status === 'TEACHING') return { key: 'TEACHING', label: '正式上课中', tone: 'emerald', description: '试课通过，已进入长期正式上课阶段。', actionLabel: '安排正式课' }
  if (status === 'FINISHED') return { key: 'FINISHED', label: '已结课', tone: 'slate', description: '课程合作已结束，可回看历史记录。', actionLabel: '查看记录' }
  if (status === 'REFUND_REVIEW') return { key: 'REFUND_REVIEW', label: '信息费退费审批中', tone: 'rose', description: '信息费退费申请已提交，等待平台审核。', actionLabel: '查看详情' }
  if (status === 'REFUNDED') return { key: 'REFUNDED', label: '已退费', tone: 'slate', description: '信息费退费完成，本次合作已结束。', actionLabel: '查看详情' }
  if (status === 'TRIAL_REFUND_REVIEW') return { key: 'TRIAL_REFUND_REVIEW', label: '试课不通过处理中', tone: 'rose', description: '试课不通过申请已提交，等待平台审核处理。', actionLabel: '查看申请' }
  return { key: 'UNKNOWN', label: '状态同步中', tone: 'slate', description: '当前合作状态正在同步，请稍后刷新或查看详情。', actionLabel: '查看详情' }
}

function lessonStatusText(status: ScheduleEventStatus) {
  if (status === 'PENDING') return '约课待确认'
  if (status === 'ACCEPTED') return '已预约'
  if (status === 'RESCHEDULE_PENDING') return '待确认改期'
  if (status === 'REJECTED') return '老师已拒绝'
  if (status === 'CANCELED') return '已取消'
  if (status === 'COMPLETED') return '已结束'
  return '状态未知'
}

function hasLiveStarted(live: LiveSessionResp | null) {
  if (!live) return false
  const status = String(live.status || '').trim().toUpperCase()
  return status === 'IN_PROGRESS' || status === 'ENDED' || !!live.actualStartAt || !!live.actualEndAt
}

function hasLiveEnded(live: LiveSessionResp | null) {
  if (!live) return false
  return String(live.status || '').trim().toUpperCase() === 'ENDED' || !!live.actualEndAt
}

function hasRealtimeAttendanceEvidence(live: LiveSessionResp | null) {
  if (!live?.sessionId) return false
  if (hasLiveStarted(live)) return true
  return !!live.peerJoined || !!live.peerOnline
}

function hasCompletedLessonSignals(lesson: ScheduleEventVO | null, live: LiveSessionResp | null, aiResultStatus?: string | null) {
  if (lesson?.status === 'COMPLETED') return true
  if (hasRealtimeAttendanceEvidence(live)) return true
  const normalizedAi = String(aiResultStatus || '').trim().toUpperCase()
  return normalizedAi === 'READY'
}

function isLessonEnterable(lesson: ScheduleEventVO | null) {
  if (!lesson?.id) return false
  if (lesson.status !== 'ACCEPTED' && lesson.status !== 'COMPLETED') return false
  return Date.now() < Number(lesson.endAt || 0)
}

function isAbnormalPendingConfirm(lesson: ScheduleEventVO | null, live: LiveSessionResp | null, aiResultStatus?: string | null) {
  if (!lesson || lesson.status !== 'ACCEPTED') return false
  if (Number(lesson.endAt || 0) > Date.now()) return false
  return !hasCompletedLessonSignals(lesson, live, aiResultStatus)
}

function lessonTone(lesson: ScheduleEventVO | null, live: LiveSessionResp | null): CourseViewModel['lessonStateTone'] {
  if (!lesson) return 'slate'
  if (isAbnormalPendingConfirm(lesson, live, null)) return 'amber'
  if (hasLiveEnded(live)) return 'emerald'
  if (hasRealtimeAttendanceEvidence(live)) return 'emerald'
  if (lesson.status === 'PENDING' || lesson.status === 'RESCHEDULE_PENDING') return 'amber'
  if (lesson.status === 'REJECTED' || lesson.status === 'CANCELED') return 'rose'
  if (lesson.status === 'COMPLETED') return 'slate'
  if (lesson.status === 'ACCEPTED') return 'sky'
  return 'slate'
}

function lessonRuntimeStatusText(lesson: ScheduleEventVO | null, live: LiveSessionResp | null, aiResultStatus?: string | null) {
  if (!lesson) return '待生成课节'
  if (isAbnormalPendingConfirm(lesson, live, aiResultStatus)) return '异常待处理'
  if (hasLiveEnded(live) || lesson.status === 'COMPLETED' || String(aiResultStatus || '').trim().toUpperCase() === 'READY') return '课堂已结束'
  if (hasLiveStarted(live)) return '课堂进行中'
  if (lesson.status === 'ACCEPTED') return Number(lesson.startAt || 0) > Date.now() ? '已预约' : '可进入课堂'
  return lessonStatusText(lesson.status)
}

function lessonRuntimeHint(lesson: ScheduleEventVO | null, live: LiveSessionResp | null, aiResultStatus?: string | null, stageKey?: CourseStageKey) {
  if (!lesson) return '当前合作还没有同步出具体课节。'
  const endedByRuntime = hasLiveEnded(live) || lesson.status === 'COMPLETED' || String(aiResultStatus || '').trim().toUpperCase() === 'READY'
  if (endedByRuntime) {
    if (stageKey === 'TRIAL_CONFIRMING') return '试课课堂已结束，等待学生确认是否继续合作。'
    return '课堂已结束，可查看课堂详情与课后总结。'
  }
  if (isAbnormalPendingConfirm(lesson, live, aiResultStatus)) {
    return '已过预约结束时间，未检测到双方成功进入实时课堂，本节课进入异常待处理。'
  }
  if (hasLiveStarted(live)) return '课堂正在进行中；任一方点击结束课程后，本节课才会进入课堂结束状态。'
  if (lesson.status === 'ACCEPTED') {
    if (Number(lesson.startAt || 0) > Date.now()) return '预约时间只用于提醒，双方也可以提前进入课堂准备。'
    return '已到预约开始时间，可以进入课堂；真实上课状态以进入实时课堂和结束课程为准。'
  }
  return lessonStatusText(lesson.status)
}

function buildParticipantSubtitle(card: UserCardVO | null, isTeacherParticipant: boolean) {
  if (isTeacherParticipant) {
    const profile = card?.teacherProfile
    const parts = [profile?.subject, profile?.education, profile?.experienceYears != null ? `${profile.experienceYears} 年经验` : ''].filter(Boolean)
    return parts.length > 0 ? parts.join(' · ') : '老师信息待完善'
  }
  const student = card?.studentProfile
  const job = card?.jobPosting
  const parts = [student?.childAge != null ? `${student.childAge} 岁` : '', job?.subjectName || '', job?.city || ''].filter(Boolean)
  return parts.length > 0 ? parts.join(' · ') : '学生信息待完善'
}

function sortLessons(events: ScheduleEventVO[]) {
  return events.slice().sort((a, b) => {
    const aScore = a.status === 'ACCEPTED' && a.endAt >= Date.now() ? 0 : a.status === 'PENDING' || a.status === 'RESCHEDULE_PENDING' ? 1 : 2
    const bScore = b.status === 'ACCEPTED' && b.endAt >= Date.now() ? 0 : b.status === 'PENDING' || b.status === 'RESCHEDULE_PENDING' ? 1 : 2
    if (aScore !== bScore) return aScore - bScore
    return a.startAt - b.startAt
  })
}

function afterClassMeta(aiResultStatus: string | null | undefined, lesson: ScheduleEventVO | null, live: LiveSessionResp | null) {
  const normalized = String(aiResultStatus || '').trim().toUpperCase()
  if (isAbnormalPendingConfirm(lesson, live, aiResultStatus)) {
    return {
      label: '未走正常课后流程',
      tone: 'tone-amber',
      hint: '本节课尚未确认是否属于未正常开课，暂不进入课后总结生成流程。',
    }
  }
  if (normalized === 'READY') {
    return {
      label: '课后总结已生成',
      tone: 'tone-emerald',
      hint: '课程结束后生成完成，可直接查看课后总结。',
    }
  }
  if (normalized === 'FAILED') {
    return {
      label: '课后总结生成失败',
      tone: 'tone-rose',
      hint: '生成失败时前端需明确提示，并提供重试生成入口。',
    }
  }
  if (lesson && (lesson.status === 'COMPLETED' || String(live?.status || '').trim().toUpperCase() === 'ENDED')) {
    return {
      label: '课后总结生成中',
      tone: 'tone-sky',
      hint: '课程结束后自动开始生成，生成完成后通过站内消息或聊天系统消息提醒。',
    }
  }
  return {
    label: '等待课程结束',
    tone: 'tone-slate',
    hint: '课程结束后，系统才会开始生成课后总结。',
  }
}

function buildTrialCountdown(trialEndAt?: string | null) {
  if (!trialEndAt) return ''
  const endMs = Date.parse(trialEndAt)
  if (!Number.isFinite(endMs)) return ''
  const diff = endMs - Date.now()
  if (diff <= 0) return '试课周期已结束，等待双方确认'
  const days = Math.ceil(diff / (24 * 60 * 60 * 1000))
  return `试课期剩余 ${days} 天`
}

function buildCourseView(item: CourseItemVO): CourseViewModel {
  const participantUid = isTeacher.value ? item.studentUid : item.teacherUid
  const participant = userMap.value[participantUid] || null
  const participantCard = userCardMap.value[participantUid] || null
  const participantName = displayNameOf(participant, participantUid)
  const stage = resolveCourseStage(item.status, item.trialEndAt)
  const lessonList = sortLessons(lessonListMap.value[item.courseId] || [])
  const latestLesson = lessonList[0] || null
  const live = liveMap.value[item.courseId] || null
  const reminder = reminderMap.value[item.courseId] || null
  const aiResult = aiResultMap.value[item.courseId] || null
  const countdownText = latestLesson?.startAt && Number(latestLesson.endAt || 0) > Date.now()
    ? formatCountdown(Number(latestLesson.startAt) - Date.now())
    : ''
  const canEnterClassroom = isLessonEnterable(latestLesson)
  const canEndClassroom = !!live?.sessionId && hasLiveStarted(live) && String(live.status || '').trim().toUpperCase() !== 'ENDED'
  const showAbnormalAttendanceConfirm = !!latestLesson
    && latestLesson.status === 'ACCEPTED'
    && Number(latestLesson.endAt || 0) <= Date.now()
    && !hasCompletedLessonSignals(latestLesson, live, aiResult?.resultStatus || item.aiResultStatus)
  const abnormalAttendanceHint = showAbnormalAttendanceConfirm
    ? '到预约结束时间前课程保持未开始；结束后若整节课从未建立双人实时视频，则进入“待确认未上课/异常待处理”，不直接进入课后总结。'
    : ''
  const afterClass = afterClassMeta(aiResult?.resultStatus || item.aiResultStatus, latestLesson, live)
  const endBlockedReason = !canEndClassroom && live?.sessionId && latestLesson && !hasCompletedLessonSignals(latestLesson, live, aiResult?.resultStatus || item.aiResultStatus)
    ? '未检测到双方成功建立实时视频，暂不可结束课程'
    : ''

  const runtimeLessonStatus = lessonRuntimeStatusText(latestLesson, live, aiResult?.resultStatus || item.aiResultStatus)
  const runtimeLessonHint = lessonRuntimeHint(latestLesson, live, aiResult?.resultStatus || item.aiResultStatus, stage.key)

  let currentStateNote = stage.description
  let currentActionHint = latestLesson
    ? `${runtimeLessonStatus} · ${fmtDateTime(latestLesson.startAt)} - ${fmtDateTime(latestLesson.endAt, { hour: '2-digit', minute: '2-digit' })}`
    : stage.description

  if (canEnterClassroom) {
    currentStateNote = latestLesson && Number(latestLesson.startAt) > Date.now() ? '当前状态：未开始，但现在可以进入课堂' : '当前状态：课程可进入'
    currentActionHint = runtimeLessonHint
  } else if (showAbnormalAttendanceConfirm) {
    currentStateNote = '当前状态：待确认未上课'
    currentActionHint = runtimeLessonHint
  } else if (canEndClassroom) {
    currentStateNote = '当前状态：课程进行中，可结束课程'
    currentActionHint = runtimeLessonHint
  } else if (latestLesson) {
    currentStateNote = runtimeLessonStatus
    currentActionHint = runtimeLessonHint
  }

  return {
    raw: item,
    courseId: item.courseId,
    applicationId: item.applicationId,
    roomId: item.roomId ?? applicationMap.value[item.applicationId]?.roomId ?? null,
    teacherUid: item.teacherUid,
    studentUid: item.studentUid,
    participantUid,
    participantName,
    participantRoleLabel: participant?.userType === 1 ? '授课老师' : '学生',
    participantSubtitle: buildParticipantSubtitle(participantCard, participant?.userType === 1),
    avatar: avatarOf(participantUid, participant?.avatar),
    stage,
    headline:
      stage.key === 'TRIALING' || stage.key === 'TRIAL_CONFIRMING' || stage.key === 'TRIAL_WAIT_WEEKLY_SCHEDULE'
        ? `与${participantName}的试课合作`
        : stage.key === 'TEACHING'
          ? `与${participantName}的正式课程合作`
          : `与${participantName}的课程合作`,
    summary: `${stage.label}${latestLesson ? ` · 下一节 ${fmtDateTime(latestLesson.startAt)}` : ''}`,
    latestLesson,
    lessonList,
    lessonTimeText: latestLesson ? `${fmtDateTime(latestLesson.startAt)} - ${fmtDateTime(latestLesson.endAt, { hour: '2-digit', minute: '2-digit' })}` : '当前还没有安排具体课节',
    lessonStatusText: runtimeLessonStatus,
    lessonStateTone: lessonTone(latestLesson, live),
    currentStateNote,
    currentActionHint,
    trialCountdown: buildTrialCountdown(item.trialEndAt),
    countdownText,
    live,
    reminder,
    aiResultStatus: aiResult?.resultStatus || item.aiResultStatus || null,
    aiPreview: aiResult?.preview || item.aiPreview || null,
    canEnterClassroom,
    canEndClassroom,
    endBlockedReason,
    showAbnormalAttendanceConfirm,
    abnormalAttendanceHint,
    afterClassStatusLabel: afterClass.label,
    afterClassStatusTone: afterClass.tone,
    afterClassHint: afterClass.hint,
    payDeadlineAt: item.payDeadlineAt || null,
    payExpired: !!item.payExpired,
    archiveReason: item.archiveReason || null,
    latestRefund: item.latestRefund || null,
    latestProposal: item.latestProposal || null,
  }
}

function processState(current: number, target: number, options?: { alert?: boolean }): ProcessStepState {
  if (options?.alert) return 'alert'
  if (target < current) return 'done'
  if (target === current) return 'current'
  return 'upcoming'
}

function processIndexOf(item: CourseViewModel) {
  if (item.payExpired) return 4
  if (item.stage.key === 'WAIT_PAY') return 0
  if (item.stage.key === 'COMMUNICATING' || item.stage.key === 'REFUND_REVIEW') return 1
  if (['TRIALING', 'TRIAL_CONFIRMING', 'TRIAL_FAILED', 'TRIAL_REFUND_REVIEW'].includes(item.stage.key)) return 2
  if (item.stage.key === 'TRIAL_WAIT_WEEKLY_SCHEDULE' || item.stage.key === 'TEACHING') return 3
  if (item.stage.key === 'FINISHED' || item.stage.key === 'REFUNDED') return 4
  return 1
}

function isBranchAlert(item: CourseViewModel, target: number) {
  const key = item.stage.key
  if (target === 1) return key === 'REFUND_REVIEW'
  if (target === 2) return key === 'TRIAL_FAILED' || key === 'TRIAL_REFUND_REVIEW' || item.showAbnormalAttendanceConfirm
  if (target === 4) return key === 'REFUNDED' || item.payExpired
  return false
}

function buildProcessSteps(item: CourseViewModel): CourseProcessStep[] {
  const currentIndex = processIndexOf(item)

  return [
    {
      key: 'pay',
      label: '支付开通',
      caption: '支付信息费后正式进入合作',
      state: processState(currentIndex, 0),
    },
    {
      key: 'chat',
      label: '沟通确认',
      caption: '聊天确认需求与是否试课',
      state: processState(currentIndex, 1, { alert: isBranchAlert(item, 1) }),
    },
    {
      key: 'trial',
      label: '试课阶段',
      caption: '安排试课并完成结果确认',
      state: processState(currentIndex, 2, { alert: isBranchAlert(item, 2) }),
    },
    {
      key: 'schedule',
      label: '正式课',
      caption: '试课通过后正式上课阶段',
      state: processState(currentIndex, 3),
    },
    {
      key: 'teaching',
      label: '结束合作',
      caption: '正式上课并查看课后总结',
      state: processState(currentIndex, 4, { alert: isBranchAlert(item, 4) }),
    },
  ]
}

function buildStageContext(item: CourseViewModel): StageContextModel {
  const teacherSide = isTeacher.value
  if (item.payExpired) {
    return {
      owner: '流程已归档',
      normalFlow: '教师 48 小时内未支付信息费，本次合作已停止。',
      branchHint: item.archiveReason || '已归档：教师超时未支付信息费',
      branchTone: 'danger',
    }
  }
  if (item.stage.key === 'WAIT_PAY') {
    return {
      owner: teacherSide ? '当前需要你完成支付' : '等待教师支付',
      normalFlow: '支付成功后进入「沟通确认」。',
      branchHint: teacherSide
        ? '如若沟通不合适，后续可申请退款；平台承诺 8 小时内全额退还，平均退款时长 23 分钟。'
        : '如果教师在 48 小时内没有支付信息费用，该项目将进入归档状态。',
      branchTone: 'neutral',
    }
  }
  if (item.stage.key === 'COMMUNICATING') {
    return {
      owner: '双方共同确认需求',
      normalFlow: '确认试课安排并被接受后，进入「试课阶段」。',
      branchHint: teacherSide ? '教师可申请退信息费；提交后合作暂停，进入平台审核。' : '如果教师申请退信息费，本阶段会显示退费审批进度。',
      branchTone: teacherSide ? 'warn' : 'neutral',
    }
  }
  if (item.stage.key === 'REFUND_REVIEW') {
    if (isStudentInfoFeeRefundFlow(item)) {
      return {
        owner: '本次沟通已结束',
        normalFlow: '你的需求已重新开放，其他老师可以继续看到并发起沟通。',
        branchHint: '建议先检查需求描述、预算和可上课时间，修改后更容易匹配合适老师。',
        branchTone: 'success',
      }
    }
    return {
      owner: '平台审核',
      normalFlow: '审核通过后进入「结束合作 · 已退费」。',
      branchHint: item.latestRefund?.adminNote ? `审核备注：${item.latestRefund.adminNote}` : '审核不通过时应回到「沟通确认」，并展示平台给出的原因。',
      branchTone: 'danger',
    }
  }
  if (item.stage.key === 'TRIALING') {
    return {
      owner: item.canEnterClassroom ? '双方可进入课堂' : '等待试课时间',
      normalFlow: '试课结束后进入「试课阶段 · 待确认结果」。',
      branchHint: '取消试课会回到沟通确认；试课异常或退费会在本阶段内显示分支结果。',
      branchTone: item.showAbnormalAttendanceConfirm ? 'warn' : 'neutral',
    }
  }
  if (item.stage.key === 'TRIAL_CONFIRMING') {
    return {
      owner: teacherSide ? '等待学生确认' : '当前需要你确认试课结果',
      normalFlow: teacherSide ? '学生确认试课合适后，会继续选择后续正式上课时间。' : '学生确认试课合适后，进入「正式课」。',
      branchHint: teacherSide ? '若学生确认试课不合适，合作会结束，平台退还 60% 信息费。' : '学生确认不合适时，仍在试课阶段内展示试课未通过和相关退费信息。',
      branchTone: 'warn',
    }
  }
  if (item.stage.key === 'TRIAL_FAILED') {
    return {
      owner: '试课阶段已结束',
      normalFlow: '本次合作不会继续进入正式课。',
      branchHint: item.latestRefund?.reason ? `原因：${item.latestRefund.reason}` : '这里展示的是试课阶段的分支结果，可查看失败原因、课堂记录或退费说明。',
      branchTone: 'danger',
    }
  }
  if (item.stage.key === 'TRIAL_REFUND_REVIEW') {
    return {
      owner: '平台审核',
      normalFlow: '审核通过后进入「结束合作 · 已退费」。',
      branchHint: item.latestRefund?.adminNote ? `审核备注：${item.latestRefund.adminNote}` : '审核不通过时应展示不通过原因，并给出可继续处理方式。',
      branchTone: 'danger',
    }
  }
  if (item.stage.key === 'TRIAL_WAIT_WEEKLY_SCHEDULE') {
    return {
      owner: teacherSide ? '等待学生确认正式课' : '当前需要确认正式课安排',
      normalFlow: '正式课安排确认后，进入正式上课阶段。',
      branchHint: '如果超过确认期限，系统会把结果收敛为试课未通过。',
      branchTone: 'warn',
    }
  }
  if (item.stage.key === 'TEACHING') {
    return {
      owner: item.canEnterClassroom ? '双方可进入正式课课堂' : teacherSide && !item.latestLesson ? '教师可发送下一节预约' : '双方查看正式课进度',
      normalFlow: '正式课完成后查看课堂详情和课后总结，最终进入「结束合作」。',
      branchHint: item.showAbnormalAttendanceConfirm ? '当前课节未检测到双方成功建立实时视频，需要处理异常后再进入课后总结。' : '课后总结生成失败时，可在本页重试生成。',
      branchTone: item.showAbnormalAttendanceConfirm ? 'warn' : 'neutral',
    }
  }
  if (item.stage.key === 'REFUNDED') {
    if (isStudentInfoFeeRefundFlow(item)) {
      return {
        owner: '本次沟通已结束',
        normalFlow: '平台已重新发布你的需求，继续帮你寻找更合适的老师。',
        branchHint: '你可以修改需求后再发布，让新的老师更快理解孩子情况和上课目标。',
        branchTone: 'success',
      }
    }
    return {
      owner: '流程已结束',
      normalFlow: '费用已退回，本次合作不再继续推进。',
      branchHint: '可查看退费详情、聊天记录和课程记录。',
      branchTone: 'success',
    }
  }
  if (item.stage.key === 'FINISHED') {
    return {
      owner: '流程已结束',
      normalFlow: '合作已结课，可回看历史课程与课后总结。',
      branchHint: '结束后不再安排新课节，历史信息保留可查。',
      branchTone: 'success',
    }
  }
  return {
    owner: '等待下一步',
    normalFlow: item.currentActionHint,
    branchHint: '当前状态仍可进入详情查看完整信息。',
    branchTone: 'neutral',
  }
}

function shouldShowCurrentLessonSection(item: CourseViewModel) {
  return !!item.latestLesson || ['TRIALING', 'TRIAL_CONFIRMING', 'TRIAL_WAIT_WEEKLY_SCHEDULE', 'TEACHING'].includes(item.stage.key)
}

function shouldShowLessonListSection(item: CourseViewModel) {
  return item.lessonList.length > 1 || ['TRIAL_WAIT_WEEKLY_SCHEDULE', 'TEACHING', 'FINISHED'].includes(item.stage.key)
}

function shouldShowAfterClassSection(item: CourseViewModel) {
  return !!item.live?.sessionId || !!item.aiPreview || ['TEACHING', 'FINISHED'].includes(item.stage.key) || item.aiResultStatus === 'FAILED'
}

function shouldShowClassCostPolicy(item: CourseViewModel) {
  return ['TRIAL_WAIT_WEEKLY_SCHEDULE', 'TEACHING'].includes(item.stage.key)
}

function buildJourneyModel(item: CourseViewModel): CourseJourneyModel {
  const fallbackSecondary: CourseActionModel = item.roomId
    ? { key: 'CHAT', label: '进入聊天', tone: 'secondary' }
    : { key: 'VIEW_DETAIL', label: '查看课程详情', tone: 'secondary' }

  if (item.payExpired) {
    return {
      badge: '结束合作 · 已归档',
      title: '已归档：教师超时未支付信息费',
      description: '教师在 48 小时内未支付信息费，本次合作流程已停止。',
      nextStepLabel: '当前结果',
      nextStepHint: '本次合作不会继续推进，可查看详情或返回继续寻找其他合作。',
      tips: ['申请通过后需要教师支付信息费才会开启聊天和后续流程。'],
      steps: buildProcessSteps(item),
      primaryAction: { key: 'VIEW_DETAIL', label: '查看详情', tone: 'primary' },
      secondaryAction: null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'WAIT_PAY') {
    return {
      badge: '当前第 1 步',
      title: isTeacher.value ? '请支付信息费以开启聊天' : '等待教师支付信息费',
      description: waitPayDescription(item),
      nextStepLabel: '下一步',
      nextStepHint: isTeacher.value ? '支付成功后，系统会解锁聊天并进入沟通确认。' : '教师支付成功后，双方会进入沟通确认。',
      tips: [waitPayTip()],
      steps: buildProcessSteps(item),
      primaryAction: isTeacher.value ? { key: 'PAY', label: '支付信息费', tone: 'primary' } : { key: 'VIEW_DETAIL', label: '查看详情', tone: 'primary' },
      secondaryAction: null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'COMMUNICATING') {
    return {
      badge: '当前第 2 步',
      title: '确认需求与是否试课',
      description: '双方在聊天中确认学生需求、合作方式，以及是否需要安排试课。',
      nextStepLabel: '下一步',
      nextStepHint: item.latestProposal?.status === 'PENDING' ? '当前已有待确认的合作/试课安排，请处理后推进。' : '确认好后选择发起合作；双方接受试课安排后会进入试课阶段。',
      tips: ['请双方确认上课具体需求、孩子情况、试课时间。', '请勿在聊天中泄露任何联系方式，否则平台可能无法保障信息费退还。'],
      steps: buildProcessSteps(item),
      primaryAction: item.latestProposal?.status === 'PENDING'
        ? { key: 'CHAT', label: '查看待确认提案', tone: 'primary', disabled: !item.roomId }
        : { key: 'START_COLLABORATION', label: '发起合作', tone: 'primary', disabled: !item.roomId },
      secondaryAction: { key: 'CHAT', label: '进入聊天', tone: 'secondary', disabled: !item.roomId },
      tertiaryAction: isTeacher.value ? { key: 'APPLY_INFO_REFUND', label: '申请退信息费', tone: 'danger', disabled: !item.roomId } : null,
    }
  }

  if (item.stage.key === 'REFUND_REVIEW') {
    if (isStudentInfoFeeRefundFlow(item)) {
      return {
        badge: '沟通确认 · 已结束',
        title: '本次沟通已结束，需求已重新开放',
        description: '老师已申请退还信息费，本次需求沟通结束。平台已重新发布你的需求，继续帮你匹配更合适的老师。',
        nextStepLabel: '下一步',
        nextStepHint: '可以先修改需求描述、预算或可上课时间，再继续等待新的老师沟通。',
        tips: ['退费审核属于教师与平台之间的处理，不影响你继续找老师。', '需求重新开放后，其他老师可以重新看到并联系你。'],
        steps: buildProcessSteps(item),
        primaryAction: { key: 'EDIT_DEMAND', label: '修改需求', tone: 'primary' },
        secondaryAction: item.roomId ? { key: 'CHAT', label: '查看聊天记录', tone: 'secondary' } : null,
        tertiaryAction: null,
      }
    }
    return {
      badge: '沟通确认 · 退费审批中',
      title: '已申请退信息费',
      description: '退费申请已提交，当前合作暂停推进，等待平台审核。',
      nextStepLabel: '平台审核',
      nextStepHint: '审核通过后会显示已退费；审核不通过后会回到沟通确认，并应展示不通过原因。',
      tips: ['当前不再展示继续试课或正式课动作。', '如需查看申请原因和审核进度，可先进入课程详情或聊天记录。'],
      steps: buildProcessSteps(item),
      primaryAction: { key: 'VIEW_DETAIL', label: '查看退费进度', tone: 'primary' },
      secondaryAction: item.roomId ? { key: 'CHAT', label: '查看聊天记录', tone: 'secondary' } : null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'TRIALING') {
    const primary = item.canEnterClassroom
      ? ({ key: 'ENTER_CLASSROOM', label: '进入课堂', tone: 'primary' } satisfies CourseActionModel)
      : ({ key: 'VIEW_DETAIL', label: '查看试课信息', tone: 'primary' } satisfies CourseActionModel)
    const tips = item.canEnterClassroom
      ? ['进入课堂只代表进入等待态，不会立刻开始计课。', '在预约结束前都可以进入课堂等待对方。']
      : [item.currentActionHint]

    return {
      badge: '当前第 3 步',
      title: item.canEnterClassroom ? '试课时间已到，可以直接进入课堂' : '等待试课开始',
      description: item.currentStateNote,
      nextStepLabel: '下一步',
      nextStepHint: '试课结束后，学生需要确认“试课合适”或“试课不合适”。',
      tips,
      steps: buildProcessSteps(item),
      primaryAction: primary,
      secondaryAction: item.roomId ? { key: 'CHAT', label: '进入聊天', tone: 'secondary' } : null,
      tertiaryAction: item.latestLesson ? { key: 'VIEW_TIMELINE', label: '课堂详情', tone: 'secondary', disabled: !item.live } : null,
    }
  }

  if (item.stage.key === 'TRIAL_CONFIRMING') {
    if (isTeacher.value) {
      return {
        badge: '当前第 3 步',
        title: '等待学生确认试课结果',
        description: '试课已结束，等待学生在 24 小时内确认是否继续合作。',
        nextStepLabel: '下一步',
        nextStepHint: '学生确认合适后，会继续选择后续正式上课时间；若不合适，本次合作结束并退还 60% 信息费。',
        tips: ['你可以通过微信或站内聊天继续沟通孩子情况、试课反馈和后续安排，帮助学生更快做出确认。', item.afterClassHint],
        steps: buildProcessSteps(item),
        primaryAction: item.roomId ? { key: 'CHAT', label: '进入聊天沟通', tone: 'primary' } : { key: 'VIEW_DETAIL', label: '查看课程详情', tone: 'primary' },
        secondaryAction: null,
        tertiaryAction: null,
      }
    }
    return {
      badge: '当前第 3 步',
      title: '需要确认试课结果',
      description: '试课已经结束，请尽快确认是否继续合作，避免流程卡住。',
      nextStepLabel: '下一步',
      nextStepHint: '确认合适后进入正式课确认；不合适则发起终止处理。',
      tips: ['只有学生侧可以确认试课结果。', '如果试课不合适，本次合作会结束；试课费用仍按 1 小时课时费由双方私下结算。', item.afterClassHint],
      steps: buildProcessSteps(item),
      primaryAction: canConfirmTrialPass(item.raw) ? { key: 'TRIAL_PASS', label: '试课合适', tone: 'primary' } : fallbackSecondary,
      secondaryAction: canSubmitTrialFail(item.raw) ? { key: 'TRIAL_FAIL', label: '试课不合适', tone: 'danger' } : null,
      tertiaryAction: item.roomId ? { key: 'CHAT', label: '进入聊天', tone: 'secondary' } : null,
    }
  }

  if (item.stage.key === 'TRIAL_WAIT_WEEKLY_SCHEDULE') {
    return {
      badge: '当前第 4 步',
      title: '试课已通过，进入正式课',
      description: '接下来确认正式课安排，后续课程会进入正式上课阶段。',
      nextStepLabel: '下一步',
      nextStepHint: '学生选择后续正式上课时间后，每节课都会在这里展示，并进入课后总结闭环。',
      tips: ['当前阶段需要学生确认后续正式上课时间。', '平台暂不收取线上授课抽成，正式课费用由双方自行协商并私下结算。'],
      steps: buildProcessSteps(item),
      primaryAction: item.roomId ? { key: 'CHAT', label: '进入聊天确认课表', tone: 'primary' } : { key: 'VIEW_DETAIL', label: '查看课程详情', tone: 'primary' },
      secondaryAction: null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'TEACHING') {
    const primary = item.canEnterClassroom
      ? ({ key: 'ENTER_CLASSROOM', label: '进入课堂', tone: 'primary' } satisfies CourseActionModel)
      : ({ key: 'VIEW_DETAIL', label: item.latestLesson ? '查看本节安排' : '查看课程详情', tone: 'primary' } satisfies CourseActionModel)

    return {
      badge: '当前第 4 步',
      title: item.canEnterClassroom ? '正式课可以开始了' : item.latestLesson ? '正式课进行中' : '安排下一节正式课',
      description: item.currentStateNote,
      nextStepLabel: '结束合作',
      nextStepHint: item.afterClassStatusLabel === '课后总结已生成' ? '课后总结已生成，可回看本节课并继续后续安排。' : '正式上课结束后，系统会进入课后总结生成流程。',
      tips: [item.currentActionHint, item.afterClassHint, '平台免费提供实时视频课堂与 AI 课后总结，正式课费用由双方自行协商结算。'],
      steps: buildProcessSteps(item),
      primaryAction: primary,
      secondaryAction: item.canEndClassroom ? { key: 'END_CLASSROOM', label: '结束课程', tone: 'secondary' } : item.roomId ? { key: 'CHAT', label: '进入聊天', tone: 'secondary' } : null,
      tertiaryAction: item.afterClassStatusLabel === '课后总结已生成'
        ? { key: 'VIEW_SUMMARY', label: '查看课后总结', tone: 'secondary' }
        : item.aiResultStatus === 'FAILED'
          ? { key: 'RETRY_SUMMARY', label: '重试课后总结', tone: 'secondary' }
          : isTeacher.value && !item.latestLesson
            ? { key: 'SCHEDULE', label: '发送预约', tone: 'secondary' }
            : null,
    }
  }

  if (item.stage.key === 'TRIAL_FAILED') {
    return {
      badge: '试课阶段 · 分支结果',
      title: '试课未通过',
      description: '本次试课没有进入正式课，合作已停止继续推进。',
      nextStepLabel: '当前结果',
      nextStepHint: '可查看试课记录、失败原因或信息费处理说明。',
      tips: ['这是试课阶段下的分支结果，不新增主流程步骤。', '试课不合适也需要按 1 小时课时费由双方私下结算，平台不代收。'],
      steps: buildProcessSteps(item),
      primaryAction: { key: 'VIEW_DETAIL', label: '查看详情', tone: 'primary' },
      secondaryAction: item.live ? { key: 'VIEW_TIMELINE', label: '课堂详情', tone: 'secondary' } : null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'TRIAL_REFUND_REVIEW') {
    return {
      badge: '试课阶段 · 退费审批中',
      title: '试课后信息费处理中',
      description: '试课后发起的信息费处理已提交，当前合作暂停推进，等待平台审核。',
      nextStepLabel: '平台审核',
      nextStepHint: '审核通过后会显示已退费；审核不通过后应展示不通过原因和可继续处理方式。',
      tips: ['这是试课阶段下的信息费处理分支，不新增主流程步骤。', '试课课时费不在平台收取，仍按 1 小时课时费由双方私下结算。'],
      steps: buildProcessSteps(item),
      primaryAction: { key: 'VIEW_DETAIL', label: '查看退费进度', tone: 'primary' },
      secondaryAction: item.live ? { key: 'VIEW_TIMELINE', label: '课堂详情', tone: 'secondary' } : null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'REFUNDED') {
    if (isStudentInfoFeeRefundFlow(item)) {
      return {
        badge: '结束合作 · 需求已重新开放',
        title: '本次沟通已结束，继续为你匹配老师',
        description: '老师已申请退还信息费，本次需求沟通结束。你的需求已重新开放给其他老师可见，平台会继续帮你寻找更合适的老师。',
        nextStepLabel: '建议操作',
        nextStepHint: '如果这次沟通暴露出预算、时间或孩子情况需要补充，可以先修改需求后再发布。',
        tips: ['退费是否通过与学生侧无关，你无需处理任何退费审批。', '把孩子情况和期望目标写清楚，新的老师会更快判断是否合适。'],
        steps: buildProcessSteps(item),
        primaryAction: { key: 'EDIT_DEMAND', label: '修改需求', tone: 'primary' },
        secondaryAction: item.roomId ? { key: 'CHAT', label: '查看聊天记录', tone: 'secondary' } : null,
        tertiaryAction: null,
      }
    }
    return {
      badge: '结束合作 · 已退费',
      title: '费用已退回，合作结束',
      description: '平台已完成退费处理，本次合作不再继续推进。',
      nextStepLabel: '当前结果',
      nextStepHint: '可查看课程详情、聊天记录或退费说明。',
      tips: ['退费是合作流程里的终止分支，会收敛到“结束合作”。'],
      steps: buildProcessSteps(item),
      primaryAction: { key: 'VIEW_DETAIL', label: '查看详情', tone: 'primary' },
      secondaryAction: item.roomId ? { key: 'CHAT', label: '查看聊天记录', tone: 'secondary' } : null,
      tertiaryAction: null,
    }
  }

  if (item.stage.key === 'FINISHED') {
    return {
      badge: '流程已完成',
      title: '合作已结束',
      description: '课程合作已结束，可回看历史课程与课后总结。',
      nextStepLabel: '下一步',
      nextStepHint: '如需回顾本次合作，优先查看课后总结与课堂详情。',
      tips: [item.afterClassHint],
      steps: buildProcessSteps(item),
      primaryAction: item.afterClassStatusLabel === '课后总结已生成' ? { key: 'VIEW_SUMMARY', label: '查看课后总结', tone: 'primary' } : { key: 'VIEW_DETAIL', label: '查看课程详情', tone: 'primary' },
      secondaryAction: item.live ? { key: 'VIEW_TIMELINE', label: '课堂详情', tone: 'secondary' } : null,
      tertiaryAction: null,
    }
  }

  return {
    badge: '当前状态',
    title: item.stage.label,
    description: item.stage.description,
    nextStepLabel: '下一步',
    nextStepHint: item.currentActionHint,
    tips: [item.afterClassHint],
    steps: buildProcessSteps(item),
    primaryAction: fallbackSecondary,
    secondaryAction: null,
    tertiaryAction: null,
  }
}

function lessonOverviewStats(item: CourseViewModel) {
  const total = item.lessonList.length
  const upcoming = item.lessonList.filter((lesson) => lesson.status === 'ACCEPTED' && Number(lesson.endAt || 0) > Date.now()).length
  const finished = item.lessonList.filter((lesson) => lessonRuntimeStatusText(lesson, item.live, item.aiResultStatus) === '课堂已结束').length
  const abnormal = item.lessonList.filter((lesson) => isAbnormalPendingConfirm(lesson, item.live, item.aiResultStatus)).length
  const trialCount = item.lessonList.filter((lesson) => lesson.lessonType === 'TRIAL').length
  const normalCount = Math.max(0, total - trialCount)
  return { total, upcoming, finished, abnormal, trialCount, normalCount }
}

function recentLessonFor(item: CourseViewModel) {
  return findRecentLesson(item.lessonList, { live: item.live, aiResultStatus: item.aiResultStatus })
}

function previousLessonFor(item: CourseViewModel) {
  return findPreviousLesson(item.lessonList, recentLessonFor(item)?.id || null)
}

function lessonDetailModelFor(item: CourseViewModel, lesson: ScheduleEventVO | null) {
  if (!lesson) return null
  return buildLessonDetailModel(lesson, {
    live: item.latestLesson?.id === lesson.id ? item.live : null,
    aiResultStatus: item.latestLesson?.id === lesson.id ? item.aiResultStatus : null,
    afterClassSummary: item.latestLesson?.id === lesson.id ? item.aiPreview : null,
  })
}

const selectedLessonModalModel = computed(() => {
  const course = courseViews.value.find((item) => item.courseId === lessonModalCourseId.value) || null
  if (!course) return null
  const lesson = course.lessonList.find((item) => item.id === lessonModalLessonId.value) || null
  return lesson ? lessonDetailModelFor(course, lesson) : null
})

function openLessonModal(item: CourseViewModel, lesson: ScheduleEventVO | null) {
  if (!lesson) return
  lessonModalCourseId.value = item.courseId
  lessonModalLessonId.value = lesson.id
  lessonModalOpen.value = true
}

function closeLessonModal() {
  lessonModalOpen.value = false
  lessonModalCourseId.value = null
  lessonModalLessonId.value = null
}

function lessonModalPrimaryLabel() {
  const course = courseViews.value.find((item) => item.courseId === lessonModalCourseId.value) || null
  const model = selectedLessonModalModel.value
  if (!course || !model) return null
  if (model.statusKey === 'READY_TO_START') return '去上课'
  if (model.statusKey === 'IN_PROGRESS') return '继续上课'
  if (model.statusKey === 'COMPLETED') return '查看课后总结'
  if (course.roomId) return '进入聊天'
  return '查看全部课节'
}

function handleLessonModalPrimary() {
  const course = courseViews.value.find((item) => item.courseId === lessonModalCourseId.value) || null
  const model = selectedLessonModalModel.value
  if (!course || !model) return
  if (model.statusKey === 'READY_TO_START' || model.statusKey === 'IN_PROGRESS') {
    goLivePrepare(course.courseId)
    return
  }
  if (model.statusKey === 'COMPLETED') {
    goLessonAiSummary(course)
    return
  }
  if (course.roomId) {
    goChat(course.roomId)
    return
  }
  goCourseDetail(course.courseId)
}

function hasUpcomingLesson(item: CourseViewModel) {
  return item.lessonList.some((lesson) => (
    lesson.status === 'ACCEPTED'
    && Number(lesson.endAt || 0) > Date.now()
    && lessonRuntimeStatusText(lesson, item.live, item.aiResultStatus) !== '课堂已结束'
    && !isAbnormalPendingConfirm(lesson, item.live, item.aiResultStatus)
  ))
}

const courseViews = computed(() => list.value
  .filter((item) => String(item.status || '').trim().toUpperCase() !== 'APPLYING')
  .map((item) => buildCourseView(item)))
const selectedJourney = computed(() => (selectedCourse.value ? buildJourneyModel(selectedCourse.value) : null))
const selectedStageContext = computed(() => (selectedCourse.value ? buildStageContext(selectedCourse.value) : null))

const filteredCourses = computed(() => {
  const kw = search.value.trim().toLowerCase()
  const activeKeys = new Set<CourseStageKey>(['COMMUNICATING', 'TRIALING', 'TRIAL_CONFIRMING', 'TRIAL_WAIT_WEEKLY_SCHEDULE', 'TEACHING'])
  return courseViews.value.filter((item) => {
    if (stageFilter.value === 'ACTIVE' && !activeKeys.has(item.stage.key)) return false
    if (stageFilter.value !== 'ALL' && stageFilter.value !== 'ACTIVE' && item.stage.key !== stageFilter.value) return false
    if (!kw) return true
    const text = [item.headline, item.participantName, item.participantSubtitle, item.currentStateNote, item.afterClassStatusLabel].join(' ').toLowerCase()
    return text.includes(kw)
  })
})

const selectedCourse = computed(() => {
  const pool = filteredCourses.value.length > 0 ? filteredCourses.value : courseViews.value
  if (!pool.length) return null
  return pool.find((item) => item.courseId === selectedCourseId.value) || pool[0]
})

const overview = computed(() => {
  const all = courseViews.value
  return {
    total: all.length,
    trialing: all.filter((item) => ['TRIALING', 'TRIAL_CONFIRMING', 'TRIAL_WAIT_WEEKLY_SCHEDULE'].includes(item.stage.key)).length,
    upcoming: all.filter((item) => hasUpcomingLesson(item)).length,
  }
})

const reminderBanner = computed(() => {
  const joinable = courseViews.value.find((item) => item.canEnterClassroom)
  if (joinable) return joinable
  return courseViews.value.find((item) => item.countdownText.includes('距开课')) || null
})

function statusToneClass(tone: string) {
  return `tone-${tone}`
}

function selectCourse(courseId: number) {
  selectedCourseId.value = courseId
}

function canOpenSchedule(item: CourseViewModel | null) {
  return !!item && item.stage.key === 'TEACHING'
}

function canConfirmTrialPass(it: CourseItemVO) {
  if (isTeacher.value) return false
  const status = String(it.status || '').trim().toUpperCase()
  return status === 'TRIAL_WAIT_STUDENT_DECISION' || (status === 'TRIALING' && trialExpired(it.trialEndAt))
}

function canSubmitTrialFail(it: CourseItemVO) {
  if (isTeacher.value) return false
  const s = String(it.status || '').trim().toUpperCase()
  return s === 'TRIAL_WAIT_STUDENT_DECISION' || s === 'TRIAL_WAIT_WEEKLY_SCHEDULE' || (s === 'TRIALING' && trialExpired(it.trialEndAt))
}

function goChat(roomId?: number | null, options?: { startCollaboration?: boolean; otherUid?: number | null }) {
  if (!roomId) return
  void router.push({
    name: 'chatRoom',
    params: { roomId: String(roomId) },
    query: {
      ...(options?.otherUid ? { otherUid: String(options.otherUid) } : {}),
      ...(options?.startCollaboration ? { action: 'collaboration' } : {}),
    },
  })
}

function startCollaborationFromCourse(item: CourseViewModel) {
  goChat(item.roomId, { startCollaboration: true, otherUid: item.participantUid })
}

function goCourseDetail(courseId: number) {
  void router.push({ name: 'courseDetail', params: { courseId: String(courseId) } })
}

function goEditDemand(item: CourseViewModel | null) {
  if (!item) return
  const demandId = linkedDemandId(item)
  if (demandId) {
    void router.push({ name: 'studentEditJob', params: { id: String(demandId) } })
    return
  }
  void router.push({ name: 'studentMineJobs' })
}

function goLivePrepare(courseId?: number | null) {
  if (!courseId) {
    toast.show('当前课程还没有可进入的课节。', 'info')
    return
  }
  void router.push({ name: 'livePrepare', params: { courseId: String(courseId) } })
}

function goLessonAiSummary(item: CourseViewModel | null) {
  if (!item) return
  const query: Record<string, string> = {}
  if (item.live?.sessionId) query.sessionId = String(item.live.sessionId)
  void router.push({ name: 'lessonAiSummary', params: { courseId: String(item.courseId) }, query })
}

async function openApplicationFlow(item: CourseViewModel) {
  actionBusyCourseId.value = item.courseId
  try {
    const result = await applicationApi.enterChat(item.applicationId)
    if (result.paymentRequired) {
      if (result.orderId) {
        await router.push({
          name: 'cashierPay',
          query: {
            contextType: 'BROKERAGE_ORDER',
            contextId: String(result.orderId),
            applicationId: String(item.applicationId),
          },
        })
        return
      }
      toast.show('当前申请尚未生成支付订单，请稍后重试。', 'info')
      return
    }
    if (result.waitingForTeacherPayment) {
      toast.show('当前还在等待教师支付信息费。', 'info')
      return
    }
    if (result.roomId) {
      goChat(result.roomId)
      return
    }
    toast.show('当前申请暂时无法进入聊天。', 'info')
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败', 'error')
  } finally {
    actionBusyCourseId.value = null
  }
}

function openSchedule(item: CourseViewModel) {
  if (!canOpenSchedule(item)) {
    toast.show('当前阶段暂不支持直接约课。', 'info')
    return
  }
  scheduleOpen.value = true
  scheduleBusy.value = false
  scheduleError.value = null
  scheduleCourseId.value = item.courseId
  scheduleTitle.value = `${item.headline}｜线上课`
  scheduleDescription.value = '正式课程安排'
  scheduleStartAt.value = roundToNextHalfHour(Date.now() + 2 * 60 * 60 * 1000)
  scheduleEndAt.value = scheduleStartAt.value + 60 * 60 * 1000
}

function openInfoRefund(item: CourseViewModel) {
  if (!item.roomId) {
    toast.show('当前合作还没有可申请退费的聊天。', 'info')
    return
  }
  infoRefundCourse.value = item
  infoRefundReason.value = ''
  infoRefundErr.value = null
  infoRefundOpen.value = true
}

function closeInfoRefund() {
  if (infoRefundBusy.value) return
  infoRefundOpen.value = false
  infoRefundCourse.value = null
  infoRefundErr.value = null
}

async function submitInfoRefund() {
  const item = infoRefundCourse.value
  if (!item?.roomId || infoRefundBusy.value) return
  const reason = infoRefundReason.value.trim()
  if (!reason) {
    infoRefundErr.value = '请填写申请退费原因，方便平台审核。'
    return
  }
  infoRefundBusy.value = true
  infoRefundErr.value = null
  try {
    await chatApi.requestBrokerageRefund(item.roomId, reason)
    toast.show('退费申请已提交，等待平台审核。', 'success')
    infoRefundOpen.value = false
    infoRefundCourse.value = null
    await load()
  } catch (e) {
    infoRefundErr.value = e instanceof Error ? e.message : '申请退费失败'
  } finally {
    infoRefundBusy.value = false
  }
}

async function respondLatestProposal(item: CourseViewModel, action: 'ACCEPT' | 'REJECT') {
  const proposalId = item.latestProposal?.id
  if (!proposalId || proposalBusyId.value) return
  proposalBusyId.value = proposalId
  proposalErr.value = null
  try {
    await chatApi.respondCollaborationProposal(proposalId, action)
    toast.show(action === 'ACCEPT' ? '已同意试课合作，流程将进入试课阶段。' : '已拒绝本次试课合作。', 'success')
    await load()
  } catch (e) {
    proposalErr.value = e instanceof Error ? e.message : '处理合作提案失败'
  } finally {
    proposalBusyId.value = null
  }
}

function openProposalEdit(item: CourseViewModel) {
  const proposal = item.latestProposal
  if (!proposal || !item.roomId) return
  const start = parseMaybeDateMs(proposal.trialStartAt)
  const end = parseMaybeDateMs(proposal.trialEndAt)
  proposalEditCourse.value = item
  proposalEditInitial.value = {
    pricePerHour: proposal.pricePerHour || item.raw.lessonPrice || '',
    trialStartAt: Number.isFinite(start) ? start : roundToNextHalfHour(Date.now()),
    trialEndAt: Number.isFinite(end) ? end : roundToNextHalfHour(Date.now()) + 2 * 60 * 60 * 1000,
    remark: proposal.remark || '',
  }
  proposalEditErr.value = null
  proposalEditOpen.value = true
}

function closeProposalEdit() {
  if (proposalEditBusy.value) return
  proposalEditOpen.value = false
  proposalEditCourse.value = null
  proposalEditInitial.value = null
  proposalEditErr.value = null
}

async function submitProposalEdit(payload: Omit<CollaborationProposalPayload, 'roomId'>) {
  const item = proposalEditCourse.value
  const proposalId = item?.latestProposal?.id
  if (!item?.roomId || !proposalId || proposalEditBusy.value) return
  proposalEditBusy.value = true
  proposalEditErr.value = null
  try {
    await chatApi.updateCollaborationProposal(proposalId, {
      roomId: item.roomId,
      ...payload,
      clientRequestId: `course-proposal-edit-${proposalId}-${Date.now()}`,
    })
    toast.show('已修改试课合作提案，等待对方确认。', 'success')
    proposalEditOpen.value = false
    proposalEditCourse.value = null
    proposalEditInitial.value = null
    await load()
  } catch (e) {
    proposalEditErr.value = e instanceof Error ? e.message : '修改合作提案失败'
  } finally {
    proposalEditBusy.value = false
  }
}

function runCourseAction(action: CourseActionModel | null, item: CourseViewModel | null) {
  if (!action || !item || action.disabled) return
  if (action.key === 'PAY') {
    void openApplicationFlow(item)
    return
  }
  if (action.key === 'CHAT') {
    goChat(item.roomId)
    return
  }
  if (action.key === 'START_COLLABORATION') {
    startCollaborationFromCourse(item)
    return
  }
  if (action.key === 'ENTER_CLASSROOM') {
    goLivePrepare(item.courseId)
    return
  }
  if (action.key === 'END_CLASSROOM') {
    openEndModal(item)
    return
  }
  if (action.key === 'TRIAL_PASS') {
    void submitTrialPass(item.courseId)
    return
  }
  if (action.key === 'TRIAL_FAIL') {
    openTrialRefund(item.courseId)
    return
  }
  if (action.key === 'VIEW_SUMMARY') {
    goLessonAiSummary(item)
    return
  }
  if (action.key === 'RETRY_SUMMARY') {
    void retryAiResult(item)
    return
  }
  if (action.key === 'VIEW_TIMELINE') {
    void openLiveTimeline(item.courseId)
    return
  }
  if (action.key === 'SCHEDULE') {
    openSchedule(item)
    return
  }
  if (action.key === 'APPLY_INFO_REFUND') {
    openInfoRefund(item)
    return
  }
  if (action.key === 'EDIT_DEMAND') {
    goEditDemand(item)
    return
  }
  goCourseDetail(item.courseId)
}

function closeSchedule() {
  if (scheduleBusy.value) return
  scheduleOpen.value = false
}

function toLocalDateTimeInputValue(ms: number) {
  const d = new Date(ms)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function parseLocalDateTimeInputValue(value: string) {
  const ms = Date.parse(value)
  return Number.isFinite(ms) ? ms : Date.now()
}

function applyScheduleStartInput(value: string) {
  const nextStart = parseLocalDateTimeInputValue(value)
  const previousDuration = Math.max(30 * 60 * 1000, scheduleEndAt.value - scheduleStartAt.value)
  scheduleStartAt.value = nextStart
  if (!(scheduleEndAt.value > scheduleStartAt.value)) {
    scheduleEndAt.value = nextStart + previousDuration
  }
}

function applyScheduleEndInput(value: string) {
  scheduleEndAt.value = parseLocalDateTimeInputValue(value)
}

async function submitScheduleCreate() {
  if (scheduleBusy.value) return
  scheduleError.value = null
  const item = courseViews.value.find((it) => it.courseId === scheduleCourseId.value) || null
  if (!item) {
    scheduleError.value = '课程信息不存在'
    return
  }
  if (!(scheduleEndAt.value > scheduleStartAt.value)) {
    scheduleError.value = '结束时间必须晚于开始时间'
    return
  }
  scheduleBusy.value = true
  try {
    const created = await scheduleApi.createEvent({
      courseId: item.courseId,
      title: scheduleTitle.value.trim() || `${item.headline}｜线上课`,
      participantUserId: item.participantUid,
      startAt: scheduleStartAt.value,
      endAt: scheduleEndAt.value,
      description: `${scheduleDescription.value.trim() || '正式课程安排'}\n提醒：提前 ${scheduleReminderMinutes.value} 分钟`,
    })
    lessonListMap.value = {
      ...lessonListMap.value,
      [item.courseId]: sortLessons([created, ...(lessonListMap.value[item.courseId] || [])]),
    }
    lessonMap.value = { ...lessonMap.value, [item.courseId]: created }
    scheduleOpen.value = false
    toast.show('约课申请已发送，聊天窗口会同步展示课节信息。', 'success')
  } catch (e) {
    scheduleError.value = e instanceof Error ? e.message : '发起约课失败'
  } finally {
    scheduleBusy.value = false
  }
}

function openTrialRefund(courseId: number) {
  modalCourseId.value = courseId
  modalReason.value = ''
  modalFiles.value = []
  modalVideoUrl.value = ''
  modalVideoDurationSeconds.value = 60
  modalErr.value = null
  modalOpen.value = true
}

function closeTrialRefund() {
  if (modalBusy.value) return
  modalOpen.value = false
}

function onPickFiles(e: Event) {
  const input = e.target as HTMLInputElement
  modalFiles.value = input.files ? Array.from(input.files) : []
}

async function submitTrialRefund() {
  if (modalBusy.value || !modalCourseId.value) return
  const item = courseViews.value.find((it) => it.courseId === modalCourseId.value) || null
  if (!item) {
    modalErr.value = '课程信息不存在'
    return
  }
  const reason = modalReason.value.trim()
  if (!reason) {
    modalErr.value = '请填写试课不通过说明'
    return
  }
  modalBusy.value = true
  modalErr.value = null
  try {
    if (String(item.raw.teachingMode || '').trim().toUpperCase() === 'OFFLINE') {
      if (!modalFiles.value.length) {
        modalErr.value = '请至少上传 1 张证据图片'
        return
      }
      if (!modalVideoUrl.value.trim()) {
        modalErr.value = '请上传并填写微信聊天录屏 URL'
        return
      }
      const duration = Number(modalVideoDurationSeconds.value)
      if (!Number.isFinite(duration) || duration <= 0 || duration > 60) {
        modalErr.value = '录屏时长需控制在 1-60 秒内'
        return
      }
      const urls: string[] = []
      for (const file of modalFiles.value) {
        const uploaded = await assetsApi.uploadImage(file, 'trial_refund')
        if (uploaded?.url) urls.push(uploaded.url)
      }
      await courseApi.applyTrialRefund(modalCourseId.value, {
        reason,
        evidenceImageUrls: urls,
        evidenceVideoUrl: modalVideoUrl.value.trim(),
        evidenceVideoDurationSeconds: Math.round(duration),
      })
    } else {
      await courseApi.submitTrialResult(modalCourseId.value, { result: 'FAIL', reason })
    }
    modalOpen.value = false
    toast.show('已提交试课不合适处理。', 'success')
    await load()
  } catch (e) {
    modalErr.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    modalBusy.value = false
  }
}

async function submitTrialPass(courseId: number) {
  if (actionBusyCourseId.value != null) return
  actionBusyCourseId.value = courseId
  try {
    await courseApi.submitTrialResult(courseId, { result: 'PASS' })
    toast.show('已确认试课合适，请继续选择后续正式上课时间。', 'success')
    await router.push({ name: 'courseDetail', params: { courseId: String(courseId) }, query: { weeklySchedule: '1' } })
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '提交失败', 'error')
  } finally {
    actionBusyCourseId.value = null
  }
}

async function openLiveTimeline(courseId: number) {
  const item = courseViews.value.find((it) => it.courseId === courseId) || null
  const sessionId = item?.live?.sessionId
  liveTimelineCourseId.value = courseId
  liveTimelineOpen.value = true
  liveTimelineBusy.value = true
  liveTimelineError.value = null
  liveTimelineItems.value = []
  if (!sessionId) {
    liveTimelineBusy.value = false
    liveTimelineError.value = '当前课程尚未生成课堂时间线'
    return
  }
  try {
    liveTimelineItems.value = await liveApi.timeline(sessionId)
  } catch (e) {
    liveTimelineError.value = e instanceof Error ? e.message : '加载课堂详情失败'
  } finally {
    liveTimelineBusy.value = false
  }
}

function closeLiveTimeline() {
  liveTimelineOpen.value = false
}

function timelineEventLabel(eventType: string) {
  const normalized = String(eventType || '').trim().toUpperCase()
  if (normalized === 'SESSION_CREATED') return '课堂已创建'
  if (normalized === 'SESSION_SYNCED') return '课堂信息已同步'
  if (normalized === 'JOIN_TOKEN_ISSUED') return '用户进入准备中'
  if (normalized === 'PARTICIPANT_JOINED') return '成员进入课堂'
  if (normalized === 'PARTICIPANT_LEFT') return '成员离开课堂'
  if (normalized === 'CLASS_ENDED' || normalized === 'ROOM_FINISHED') return '课堂已结束'
  if (normalized === 'DEVICE_REPORTED') return '设备检测已上报'
  return normalized || '状态更新'
}

function openEndModal(item: CourseViewModel | null) {
  if (!item?.live?.sessionId) return
  endModalCourseId.value = item.courseId
  endModalOpen.value = true
  endModalBusy.value = false
  endModalErr.value = null
  endModalCountdown.value = 3
  if (endModalTimer != null) window.clearInterval(endModalTimer)
  endModalTimer = window.setInterval(() => {
    if (endModalCountdown.value <= 1) {
      endModalCountdown.value = 0
      if (endModalTimer != null) {
        window.clearInterval(endModalTimer)
        endModalTimer = null
      }
      return
    }
    endModalCountdown.value -= 1
  }, 1000)
}

function closeEndModal() {
  if (endModalBusy.value) return
  endModalOpen.value = false
  endModalErr.value = null
  endModalCourseId.value = null
  endModalCountdown.value = 3
  if (endModalTimer != null) {
    window.clearInterval(endModalTimer)
    endModalTimer = null
  }
}

async function confirmEndLesson() {
  const item = courseViews.value.find((it) => it.courseId === endModalCourseId.value) || null
  if (!item?.live?.sessionId || endModalBusy.value || endModalCountdown.value > 0) return
  endModalBusy.value = true
  try {
    await liveApi.end(item.live.sessionId, { reason: 'MANUAL_END', confirm: true })
    toast.show('课堂已结束，系统开始生成课后总结。', 'success')
    closeEndModal()
    await load()
  } catch (e) {
    endModalErr.value = e instanceof Error ? e.message : '结束课程失败'
  } finally {
    endModalBusy.value = false
  }
}

async function retryAiResult(item: CourseViewModel | null) {
  if (!item?.live?.sessionId) return
  try {
    const result = await liveApi.retryAiResult(item.live.sessionId)
    aiResultMap.value = {
      ...aiResultMap.value,
      [item.courseId]: {
        resultStatus: result.resultStatus,
        preview: result.preview,
        sessionId: item.live.sessionId,
      },
    }
    toast.show('已发起课后总结重试，请稍后刷新查看。', 'success')
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '重试失败', 'error')
  }
}

async function loadUsersAndCards(items: CourseItemVO[]) {
  const ids = Array.from(new Set(items.flatMap((item) => [item.teacherUid, item.studentUid]).filter((uid) => uid > 0)))
  if (!ids.length) return
  try {
    const users = await userApi.batch(ids)
    userMap.value = Object.fromEntries(users.map((item) => [item.id, item]))
  } catch {
    userMap.value = {}
  }
  const cards = await Promise.all(
    ids.map(async (uid) => {
      try {
        return [uid, await userApi.card(uid)] as const
      } catch {
        return [uid, null] as const
      }
    }),
  )
  userCardMap.value = Object.fromEntries(cards)
}

async function loadApplications(items: CourseItemVO[]) {
  const rows = await Promise.all(
    items.map(async (item) => {
      try {
        return [item.applicationId, await applicationApi.detail(item.applicationId)] as const
      } catch {
        return [item.applicationId, null] as const
      }
    }),
  )
  applicationMap.value = Object.fromEntries(rows)
}

async function loadLessons(items: CourseItemVO[]) {
  const startAt = Date.now() - 15 * 24 * 60 * 60 * 1000
  const endAt = Date.now() + 45 * 24 * 60 * 60 * 1000
  try {
    const events = await scheduleApi.listEvents({ startAt, endAt, includePending: true })
    const nextMap: Record<number, ScheduleEventVO | null> = {}
    const listMap: Record<number, ScheduleEventVO[]> = {}
    items.forEach((item) => {
      const otherUid = isTeacher.value ? item.studentUid : item.teacherUid
      const candidates = sortLessons(
        events.filter((event) => {
          if (event.courseId && event.courseId === item.courseId) return true
          const participantId = event.participant?.id
          if (participantId && participantId === otherUid) return true
          if (item.roomId && event.chatRoomId && event.chatRoomId === item.roomId) return true
          return false
        }),
      )
      listMap[item.courseId] = candidates
      nextMap[item.courseId] = candidates[0] || null
    })
    lessonListMap.value = listMap
    lessonMap.value = nextMap
  } catch {
    lessonListMap.value = {}
    lessonMap.value = {}
  }
}

async function loadLive(items: CourseItemVO[]) {
  const liveEntries = await Promise.all(
    items.map(async (item) => {
      if (!(item.courseId > 0)) return [item.courseId, null] as const
      try {
        return [item.courseId, await liveApi.getByCourse(item.courseId)] as const
      } catch {
        return [item.courseId, null] as const
      }
    }),
  )
  liveMap.value = Object.fromEntries(liveEntries)

  const aiEntries = await Promise.all(
    items.map(async (item) => {
      const live = liveMap.value[item.courseId]
      if (!live?.sessionId) return [item.courseId, { resultStatus: item.aiResultStatus || null, preview: item.aiPreview || null, sessionId: null }] as const
      try {
        const ai = await liveApi.aiResult(live.sessionId)
        return [item.courseId, { resultStatus: ai.resultStatus, preview: ai.preview, sessionId: live.sessionId }] as const
      } catch {
        return [item.courseId, { resultStatus: item.aiResultStatus || null, preview: item.aiPreview || null, sessionId: live.sessionId }] as const
      }
    }),
  )
  aiResultMap.value = Object.fromEntries(aiEntries)

  try {
    const reminders = await liveApi.reminders()
    const mapped: Record<number, LiveReminderItemResp | null> = {}
    items.forEach((item) => {
      mapped[item.courseId] = reminders.find((entry) => entry.courseId === item.courseId) || null
    })
    reminderMap.value = mapped
  } catch {
    reminderMap.value = {}
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const rows = await courseApi.myCourses({ page: 1, size: 50, role: role.value })
    list.value = rows
    await Promise.all([loadUsersAndCards(rows), loadApplications(rows)])
    await loadLessons(rows)
    await loadLive(rows)
    if (!selectedCourseId.value && rows[0]) selectedCourseId.value = rows[0].courseId
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadEmailHint() {
  try {
    emailHint.value = await userApi.emailReminderHint('COURSE_LIST')
  } catch {
    emailHint.value = null
  }
}

function goEmailSettings() {
  void router.push({ name: 'emailSettings' })
}

onMounted(() => {
  void load()
  void loadEmailHint()
})

onUnmounted(() => {
  if (endModalTimer != null) {
    window.clearInterval(endModalTimer)
    endModalTimer = null
  }
})
</script>

<template>
  <div class="page-shell">
    <section v-if="reminderBanner" class="banner card">
      <div>
        <div class="banner-title">{{ reminderBanner.headline }}</div>
        <div class="banner-desc">{{ reminderBanner.currentActionHint }}</div>
      </div>
      <div class="banner-actions">
        <button v-if="reminderBanner.roomId" class="btn" type="button" @click="goChat(reminderBanner.roomId)">进入聊天</button>
        <button v-if="reminderBanner.canEnterClassroom" class="btn btn-primary" type="button" @click="goLivePrepare(reminderBanner.courseId)">进入课堂</button>
      </div>
    </section>

    <section v-if="emailHint?.show" class="email-course-banner card">
      <div>
        <div class="banner-title">{{ emailHint.title || '开课提醒将通过邮箱发送' }}</div>
        <div class="banner-desc">{{ emailHint.description || '绑定主邮箱后，可按站内提醒时间同步收到邮件提醒。' }}</div>
      </div>
      <button class="btn btn-primary" type="button" @click="goEmailSettings">{{ emailHint.actionText || '去绑定邮箱' }}</button>
    </section>

    <div v-if="error" class="hint error">{{ error }}</div>

    <section v-if="!loading && filteredCourses.length === 0" class="empty card">
      <div class="empty-title">暂无符合条件的合作</div>
      <div class="empty-desc">合作建立后，会在这里看到对应的课程阶段、当前说明、课程入口与课后结果。</div>
    </section>

    <section v-else class="workspace-grid">
      <aside class="cooperation-panel card">
        <div class="side-head">
          <div class="section-title strong">我的合作</div>
          <div class="side-desc">先看合作，再点进某个合作查看全部课程。</div>
        </div>
        <div class="metric-strip">
          <div class="metric-mini">
            <strong>{{ overview.total }}</strong>
            <span>全部合作</span>
          </div>
          <div class="metric-mini">
            <strong>{{ overview.trialing }}</strong>
            <span>试课阶段</span>
          </div>
          <div class="metric-mini">
            <strong>{{ overview.upcoming }}</strong>
            <span>有待上课程</span>
          </div>
        </div>
        <div class="course-column">
          <article
            v-for="item in filteredCourses"
            :key="item.courseId"
            class="course-card"
            :class="{ active: selectedCourse?.courseId === item.courseId }"
            @click="selectCourse(item.courseId)"
          >
            <div class="course-top">
              <div class="course-person">
                <img v-if="item.avatar" class="avatar" :src="item.avatar" alt="" @error="markAvatarBroken(item.participantUid)" />
                <div v-else class="avatar fallback">{{ item.participantName.slice(0, 1) }}</div>
                <div class="course-person-copy">
                  <div class="person-label">{{ item.participantRoleLabel }}</div>
                  <div class="person-name">{{ item.participantName }}</div>
                </div>
              </div>
              <span class="status-pill" :class="statusToneClass(item.stage.tone)">{{ displayStageLabel(item) }}</span>
            </div>

            <div class="course-headline">{{ item.headline }}</div>
            <div class="course-summary">{{ buildJourneyModel(item).title }}</div>
            <div class="course-meta">{{ buildJourneyModel(item).nextStepLabel }}：{{ buildJourneyModel(item).nextStepHint }}</div>
            <div v-if="item.trialCountdown" class="trial-hint">{{ item.trialCountdown }}</div>
            <div class="cooperation-tags">
              <span
                v-for="tag in courseTags(item)"
                :key="tag.text"
                class="mini-chip"
                :class="tag.tone"
              >
                {{ tag.text }}
              </span>
            </div>
          </article>
        </div>
      </aside>

      <main v-if="selectedCourse" class="detail-panel card">
        <div class="detail-head">
          <div>
            <div class="detail-title">{{ selectedCourse.headline }}</div>
            <div class="detail-summary">{{ selectedJourney?.description }}</div>
          </div>
          <span class="status-pill" :class="statusToneClass(selectedCourse.stage.tone)">{{ displayStageLabel(selectedCourse) }}</span>
        </div>

        <section v-if="selectedJourney" class="journey-panel">
          <div class="journey-header">
            <div>
              <div class="journey-badge">{{ selectedJourney.badge }}</div>
              <div class="journey-title">{{ selectedJourney.title }}</div>
              <div class="journey-desc">{{ selectedJourney.description }}</div>
            </div>
            <div class="journey-next">
              <div class="summary-label">{{ selectedJourney.nextStepLabel }}</div>
              <div class="journey-next-text">{{ selectedJourney.nextStepHint }}</div>
            </div>
          </div>

          <div v-if="selectedStageContext" class="stage-context-grid">
            <div class="stage-context-card">
              <div class="summary-label">当前责任方</div>
              <div class="stage-context-text">{{ selectedStageContext.owner }}</div>
            </div>
            <div class="stage-context-card">
              <div class="summary-label">正常流转</div>
              <div class="stage-context-text">{{ selectedStageContext.normalFlow }}</div>
            </div>
            <div class="stage-context-card" :class="`is-${selectedStageContext.branchTone}`">
              <div class="summary-label">分支提示</div>
              <div class="stage-context-text">{{ selectedStageContext.branchHint }}</div>
            </div>
          </div>

          <div class="journey-steps">
            <article
              v-for="step in selectedJourney.steps"
              :key="step.key"
              class="journey-step"
              :class="`is-${step.state}`"
            >
              <div class="journey-step-dot"></div>
              <div class="journey-step-label">{{ step.label }}</div>
              <div class="journey-step-caption">{{ step.caption }}</div>
            </article>
          </div>

          <div v-if="selectedJourney.tips.length" class="journey-tips">
            <div v-for="tip in selectedJourney.tips" :key="tip" class="journey-tip">{{ tip }}</div>
          </div>

          <div class="journey-actions">
            <button
              v-if="selectedJourney.primaryAction"
              class="btn btn-primary"
              type="button"
              :disabled="selectedJourney.primaryAction.disabled || actionBusyCourseId === selectedCourse.courseId"
              @click="runCourseAction(selectedJourney.primaryAction, selectedCourse)"
            >
              {{ actionBusyCourseId === selectedCourse.courseId && ['PAY', 'TRIAL_PASS'].includes(selectedJourney.primaryAction.key) ? '处理中...' : selectedJourney.primaryAction.label }}
            </button>
            <button
              v-if="selectedJourney.secondaryAction"
              class="btn"
              :class="{ 'btn-danger': selectedJourney.secondaryAction.tone === 'danger' }"
              type="button"
              :disabled="selectedJourney.secondaryAction.disabled"
              @click="runCourseAction(selectedJourney.secondaryAction, selectedCourse)"
            >
              {{ selectedJourney.secondaryAction.label }}
            </button>
            <button
              v-if="selectedJourney.tertiaryAction"
              class="btn"
              type="button"
              :disabled="selectedJourney.tertiaryAction.disabled"
              @click="runCourseAction(selectedJourney.tertiaryAction, selectedCourse)"
            >
              {{ selectedJourney.tertiaryAction.label }}
            </button>
          </div>
        </section>

        <section v-if="shouldShowClassCostPolicy(selectedCourse)" class="detail-section">
          <div class="class-policy-card">
            <div>
              <div class="section-title strong">正式课费用说明</div>
              <div class="policy-title">平台暂不收取线上授课抽成</div>
              <div class="policy-desc">
                现阶段实时视频课堂和 AI 课后总结免费提供。正式课费用请双方自行协商并线下结算；合作确认后，双方可在聊天中查看对方联系方式。
              </div>
            </div>
            <button v-if="selectedCourse.roomId" class="btn" type="button" @click="goChat(selectedCourse.roomId)">进入聊天确认</button>
          </div>
        </section>

        <section v-if="selectedCourse.payExpired" class="detail-section">
          <div class="process-info-card danger">
            <div>
              <div class="section-title strong">归档说明</div>
              <div class="info-title">已归档：教师超时未支付信息费</div>
              <div class="info-desc">
                教师在 48 小时内没有完成信息费支付，本次合作流程已停止，不会继续开启聊天、试课或正式课。
              </div>
            </div>
            <span class="status-pill tone-rose">{{ selectedCourse.archiveReason || '教师超时未支付信息费' }}</span>
          </div>
        </section>

        <section v-if="selectedCourse.latestProposal?.status === 'PENDING'" class="detail-section">
          <div class="process-info-card warn">
            <div class="info-card-head">
              <div>
                <div class="section-title strong">待确认试课合作</div>
                <div class="info-title">{{ proposalFromMe(selectedCourse) ? '已发起，等待对方确认' : '对方发来了试课合作安排' }}</div>
                <div class="info-desc">该提案仍属于「沟通确认」阶段，确认后会进入「试课阶段」。拒绝或修改后，双方继续在沟通确认内重新校对。</div>
              </div>
              <span class="status-pill tone-amber">{{ proposalStatusText(selectedCourse.latestProposal.status, proposalFromMe(selectedCourse)) }}</span>
            </div>
            <div class="info-grid">
              <div class="info-kv">
                <span>试课时间</span>
                <strong>{{ proposalTimeText(selectedCourse) }}</strong>
              </div>
              <div class="info-kv">
                <span>试课费用</span>
                <strong>{{ trialFeePolicyText() }}</strong>
              </div>
              <div class="info-kv">
                <span>频次</span>
                <strong>{{ selectedCourse.latestProposal.frequencyPerWeek ? `每周 ${selectedCourse.latestProposal.frequencyPerWeek} 次` : '待确认' }}</strong>
              </div>
              <div class="info-kv">
                <span>有效期</span>
                <strong>{{ selectedCourse.latestProposal.expireAt ? `${fmtDateTime(selectedCourse.latestProposal.expireAt)} 前` : '待确认' }}</strong>
              </div>
            </div>
            <div v-if="selectedCourse.latestProposal.remark" class="info-note">{{ selectedCourse.latestProposal.remark }}</div>
            <div class="info-note">平台暂不代收试课或正式课费用；试课无论是否继续，均按 1 小时课时费由双方私下转账。</div>
            <div v-if="proposalErr" class="m-error">{{ proposalErr }}</div>
            <div class="info-actions">
              <template v-if="proposalNeedsMe(selectedCourse)">
                <button class="btn btn-primary" type="button" :disabled="proposalBusyId === selectedCourse.latestProposal.id" @click="respondLatestProposal(selectedCourse, 'ACCEPT')">
                  {{ proposalBusyId === selectedCourse.latestProposal.id ? '处理中...' : '同意安排' }}
                </button>
                <button class="btn btn-danger" type="button" :disabled="proposalBusyId === selectedCourse.latestProposal.id" @click="respondLatestProposal(selectedCourse, 'REJECT')">拒绝</button>
              </template>
              <button v-if="proposalFromMe(selectedCourse)" class="btn" type="button" :disabled="proposalBusyId === selectedCourse.latestProposal.id" @click="openProposalEdit(selectedCourse)">修改提案</button>
              <button v-if="selectedCourse.roomId" class="btn" type="button" @click="goChat(selectedCourse.roomId)">进入聊天</button>
            </div>
          </div>
        </section>

        <section v-if="selectedCourse.latestRefund && !isStudentInfoFeeRefundFlow(selectedCourse)" class="detail-section">
          <div class="process-info-card" :class="selectedCourse.stage.key === 'REFUNDED' ? 'success' : 'danger'">
            <div class="info-card-head">
              <div>
                <div class="section-title strong">{{ refundSectionTitle(selectedCourse.latestRefund) }}</div>
                <div class="info-title">{{ refundTypeText(selectedCourse.latestRefund.type) }} · {{ refundStatusText(selectedCourse.latestRefund.status) }}</div>
                <div class="info-desc">{{ refundSectionDesc(selectedCourse.latestRefund) }}</div>
              </div>
              <span class="status-pill" :class="selectedCourse.stage.key === 'REFUNDED' ? 'tone-emerald' : 'tone-rose'">{{ refundStatusText(selectedCourse.latestRefund.status) }}</span>
            </div>
            <div class="info-grid">
              <div class="info-kv">
                <span>申请时间</span>
                <strong>{{ selectedCourse.latestRefund.createTime ? fmtDateTime(selectedCourse.latestRefund.createTime) : '待同步' }}</strong>
              </div>
              <div class="info-kv">
                <span>退费比例</span>
                <strong>{{ selectedCourse.latestRefund.refundPercent != null ? `${selectedCourse.latestRefund.refundPercent}%` : '按平台规则' }}</strong>
              </div>
              <div class="info-kv">
                <span>退费金额</span>
                <strong>{{ moneyFenText(selectedCourse.latestRefund.refundAmountFen) || '待审核后确认' }}</strong>
              </div>
              <div class="info-kv">
                <span>审核时间</span>
                <strong>{{ selectedCourse.latestRefund.decidedAt ? fmtDateTime(selectedCourse.latestRefund.decidedAt) : '审核中' }}</strong>
              </div>
            </div>
            <div v-if="selectedCourse.latestRefund.reason" class="info-note">申请原因：{{ selectedCourse.latestRefund.reason }}</div>
            <div v-if="selectedCourse.latestRefund.adminNote" class="info-note">审核说明：{{ selectedCourse.latestRefund.adminNote }}</div>
          </div>
        </section>

        <section v-if="shouldShowCurrentLessonSection(selectedCourse)" class="detail-section">
          <div class="section-title strong">当前课节</div>
          <div class="section-subtitle">当前阶段需要关注的课节、进入规则和限制原因。</div>
          <div class="current-lesson-card">
            <div class="lesson-main">
              <div>
                <div class="lesson-title-row">
                  <div class="lesson-time-main">{{ selectedCourse.lessonTimeText }}</div>
                  <span class="status-pill sm" :class="statusToneClass(selectedCourse.lessonStateTone)">{{ selectedCourse.lessonStatusText }}</span>
                </div>
                <div class="lesson-state-line">
                  <span v-if="selectedCourse.countdownText" class="countdown-text">{{ selectedCourse.countdownText }}</span>
                  <span>{{ selectedCourse.currentStateNote }}</span>
                </div>
              </div>
              <div class="lesson-type-pill">{{ selectedCourse.latestLesson?.lessonType === 'TRIAL' ? '试课' : '课程' }}</div>
            </div>

            <div class="rule-hint" :class="selectedCourse.showAbnormalAttendanceConfirm ? 'warn' : 'success'">{{ selectedCourse.currentActionHint }}</div>
            <div v-if="selectedCourse.showAbnormalAttendanceConfirm" class="rule-hint warn">{{ selectedCourse.abnormalAttendanceHint }}</div>
            <div v-if="!selectedCourse.canEndClassroom && selectedCourse.endBlockedReason" class="inline-hint">{{ selectedCourse.endBlockedReason }}</div>
          </div>
        </section>

        <section v-if="shouldShowLessonListSection(selectedCourse)" class="detail-section">
          <div class="section-title strong">合作课节</div>
          <div class="section-subtitle">这里先展示最近的一节课和上一节课；需要完整查看这个合作里的所有课节时，再进入合作课程总览页。</div>
          <div class="lesson-summary-grid">
            <button
              v-if="recentLessonFor(selectedCourse)"
              class="lesson-summary-card"
              type="button"
              @click="openLessonModal(selectedCourse, recentLessonFor(selectedCourse))"
            >
              <div class="summary-card-label">最近的一节课</div>
              <div class="summary-card-title">{{ lessonDetailModelFor(selectedCourse, recentLessonFor(selectedCourse))?.title }}</div>
              <div class="summary-card-meta">{{ lessonDetailModelFor(selectedCourse, recentLessonFor(selectedCourse))?.timeRangeText }}</div>
              <div class="summary-card-copy">{{ lessonDetailModelFor(selectedCourse, recentLessonFor(selectedCourse))?.topic }}</div>
              <span class="status-pill sm" :class="statusToneClass(lessonDetailModelFor(selectedCourse, recentLessonFor(selectedCourse))?.statusTone || 'slate')">
                {{ lessonDetailModelFor(selectedCourse, recentLessonFor(selectedCourse))?.statusLabel }}
              </span>
            </button>
            <div v-else class="lesson-summary-empty">还没有排到最近的一节课。</div>

            <button
              v-if="previousLessonFor(selectedCourse)"
              class="lesson-summary-card"
              type="button"
              @click="openLessonModal(selectedCourse, previousLessonFor(selectedCourse))"
            >
              <div class="summary-card-label">上一节课</div>
              <div class="summary-card-title">{{ lessonDetailModelFor(selectedCourse, previousLessonFor(selectedCourse))?.title }}</div>
              <div class="summary-card-meta">{{ lessonDetailModelFor(selectedCourse, previousLessonFor(selectedCourse))?.timeRangeText }}</div>
              <div class="summary-card-copy">{{ lessonDetailModelFor(selectedCourse, previousLessonFor(selectedCourse))?.summaryText }}</div>
              <span class="status-pill sm" :class="statusToneClass(lessonDetailModelFor(selectedCourse, previousLessonFor(selectedCourse))?.statusTone || 'slate')">
                {{ lessonDetailModelFor(selectedCourse, previousLessonFor(selectedCourse))?.statusLabel }}
              </span>
            </button>
            <div v-else class="lesson-summary-empty">上一节课会在第一节课结束后显示。</div>
          </div>

          <div class="lesson-overview-card">
            <div class="overview-stat">
              <strong>{{ lessonOverviewStats(selectedCourse).total }}</strong>
              <span>全部课节</span>
            </div>
            <div class="overview-stat">
              <strong>{{ lessonOverviewStats(selectedCourse).upcoming }}</strong>
              <span>待上课程</span>
            </div>
            <div class="overview-stat">
              <strong>{{ lessonOverviewStats(selectedCourse).finished }}</strong>
              <span>已结束</span>
            </div>
            <div class="overview-stat" :class="{ warn: lessonOverviewStats(selectedCourse).abnormal > 0 }">
              <strong>{{ lessonOverviewStats(selectedCourse).abnormal }}</strong>
              <span>异常待处理</span>
            </div>
            <div class="overview-copy">
              <div class="overview-title">查看完整合作课程总览</div>
              <div class="overview-desc">进入后会看到这个合作的全部课节列表、后续规划时间，并支持 hover 预览和单节课详情弹窗。</div>
            </div>
            <button class="btn" type="button" @click="goCourseDetail(selectedCourse.courseId)">查看全部课节</button>
          </div>
        </section>

        <section v-if="shouldShowAfterClassSection(selectedCourse)" class="detail-section">
          <div class="section-title strong">课后结果</div>
          <div class="result-card">
            <div class="lesson-top">
              <span class="status-pill sm" :class="selectedCourse.afterClassStatusTone">{{ selectedCourse.afterClassStatusLabel }}</span>
            </div>
            <div class="lesson-desc">{{ selectedCourse.aiPreview || selectedCourse.afterClassHint }}</div>
            <div class="lesson-actions">
              <button class="btn" type="button" :disabled="selectedCourse.afterClassStatusLabel !== '课后总结已生成'" @click="goLessonAiSummary(selectedCourse)">查看课后总结</button>
              <button v-if="selectedCourse.aiResultStatus === 'FAILED'" class="btn" type="button" @click="retryAiResult(selectedCourse)">重试生成</button>
              <button class="btn" type="button" :disabled="!selectedCourse.live" @click="openLiveTimeline(selectedCourse.courseId)">课堂详情</button>
            </div>
          </div>
        </section>
      </main>
    </section>

    <div v-if="scheduleOpen" class="mask" @click.self="closeSchedule">
      <div class="modal card schedule-modal">
        <div class="m-title">预约课程</div>
        <div class="m-desc">像预约会议一样选择时间，发送后会在聊天中同步课程卡片，并等待对方确认。</div>
        <div v-if="scheduleError" class="m-error">{{ scheduleError }}</div>
        <div class="field-grid">
          <div class="field">
            <div class="lab">课程名称</div>
            <input v-model="scheduleTitle" class="ipt" placeholder="例如：初二数学｜函数强化" />
          </div>
          <div class="field">
            <div class="lab">提醒设置</div>
            <select v-model="scheduleReminderMinutes" class="ipt">
              <option :value="10">提前 10 分钟提醒</option>
              <option :value="30">提前 30 分钟提醒</option>
              <option :value="60">提前 1 小时提醒</option>
            </select>
          </div>
        </div>
        <div class="field-grid">
          <div class="field">
            <div class="lab">开始时间</div>
            <input class="ipt" type="datetime-local" :value="toLocalDateTimeInputValue(scheduleStartAt)" @change="applyScheduleStartInput(($event.target as HTMLInputElement).value)" />
          </div>
          <div class="field">
            <div class="lab">结束时间</div>
            <input class="ipt" type="datetime-local" :value="toLocalDateTimeInputValue(scheduleEndAt)" @change="applyScheduleEndInput(($event.target as HTMLInputElement).value)" />
          </div>
        </div>
        <div class="field">
          <div class="lab">课程备注</div>
          <textarea v-model="scheduleDescription" class="txt" rows="4" placeholder="填写本节课想讲的内容、教材、作业或会议说明" />
        </div>
        <div class="booking-summary">
          <div class="booking-title">预约摘要</div>
          <div class="booking-row"><span>时间</span><span>{{ fmtDateTime(scheduleStartAt) }} - {{ fmtDateTime(scheduleEndAt, { hour: '2-digit', minute: '2-digit' }) }}</span></div>
          <div class="booking-row"><span>时长</span><span>{{ formatDuration(scheduleStartAt, scheduleEndAt) }}</span></div>
          <div class="booking-row"><span>提醒</span><span>上课前 {{ scheduleReminderMinutes }} 分钟</span></div>
        </div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="scheduleBusy" @click="closeSchedule">取消</button>
          <button class="btn btn-primary" type="button" :disabled="scheduleBusy" @click="submitScheduleCreate">{{ scheduleBusy ? '发送中...' : '发送预约' }}</button>
        </div>
      </div>
    </div>

    <div v-if="modalOpen" class="mask" @click.self="closeTrialRefund">
      <div class="modal card">
        <div class="m-title">试课不合适</div>
        <div class="m-desc">确认不继续后，本次合作会结束并关闭聊天。试课费用仍按 1 小时课时费由双方私下结算；平台不代收试课或正式课费用。</div>
        <div class="m-form">
          <textarea v-model="modalReason" class="txt" rows="4" placeholder="请填写试课不通过说明"></textarea>
          <template v-if="selectedCourse?.raw.teachingMode === 'OFFLINE'">
            <input class="file" type="file" accept="image/*" multiple @change="onPickFiles" />
            <div class="m-hint">已选择 {{ modalFiles.length }} 张</div>
            <input v-model="modalVideoUrl" class="input" placeholder="微信录屏 URL（1 分钟内）" />
            <input v-model.number="modalVideoDurationSeconds" class="input" type="number" min="1" max="60" placeholder="录屏时长（秒）" />
          </template>
        </div>
        <div v-if="modalErr" class="m-error">{{ modalErr }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="modalBusy" @click="closeTrialRefund">取消</button>
          <button class="btn btn-danger" type="button" :disabled="modalBusy" @click="submitTrialRefund">{{ modalBusy ? '提交中...' : '提交申请' }}</button>
        </div>
      </div>
    </div>

    <div v-if="infoRefundOpen" class="mask" @click.self="closeInfoRefund">
      <div class="modal card refund-modal">
        <div class="m-title">申请退信息费</div>
        <div class="m-desc">
          如申请退费之后聊天功能将立刻全部关闭，并确保聊天过程没有泄露任何联系方式，否则信息费将不予退回，确认之后信息费将在8小时内原路退回。
        </div>
        <div class="refund-warning">
          提交后本合作会停留在「沟通确认 · 退费审批中」，聊天页也会同步系统消息。
        </div>
        <div class="field">
          <div class="lab">退费原因</div>
          <textarea v-model="infoRefundReason" class="txt" rows="4" placeholder="请说明为什么当前沟通不合适，便于平台审核" />
        </div>
        <div v-if="infoRefundErr" class="m-error">{{ infoRefundErr }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="infoRefundBusy" @click="closeInfoRefund">取消</button>
          <button class="btn btn-danger" type="button" :disabled="infoRefundBusy" @click="submitInfoRefund">{{ infoRefundBusy ? '提交中...' : '确认申请退费' }}</button>
        </div>
      </div>
    </div>

    <CollaborationProposalModal
      :open="proposalEditOpen"
      :busy="proposalEditBusy"
      :error="proposalEditErr"
      title="修改试课合作"
      submit-text="发送修改"
      :other-uid="proposalEditCourse?.participantUid || null"
      :initial="proposalEditInitial"
      @close="closeProposalEdit"
      @submit="submitProposalEdit"
    />

    <div v-if="liveTimelineOpen" class="mask" @click.self="closeLiveTimeline">
      <div class="modal card timeline-modal">
        <div class="m-title">课堂详情</div>
        <div class="m-desc">课程 #{{ liveTimelineCourseId }} 的实时课堂状态时间线</div>
        <div v-if="liveTimelineError" class="m-error">{{ liveTimelineError }}</div>
        <div v-else-if="liveTimelineBusy" class="timeline-empty">加载中...</div>
        <div v-else-if="liveTimelineItems.length === 0" class="timeline-empty">暂未产生课堂事件</div>
        <div v-else class="timeline-list">
          <div v-for="item in liveTimelineItems" :key="`${item.eventType}-${item.occurredAt}`" class="timeline-item">
            <div class="timeline-dot" />
            <div class="timeline-content">
              <div class="timeline-title">{{ timelineEventLabel(item.eventType) }}</div>
              <div class="timeline-meta">{{ item.eventSource }} · {{ item.occurredAt }}</div>
            </div>
          </div>
        </div>
        <div class="m-ops">
          <button class="btn" type="button" @click="closeLiveTimeline">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="endModalOpen" class="mask" @click.self="closeEndModal">
      <div class="modal card end-modal">
        <div class="m-title">结束课堂确认</div>
        <div class="m-desc">双方都可以结束课堂，但必须满足“本节课曾成功建立过一次双人实时视频”的条件。为避免误触，确认按钮需等待 3 秒后才可点击。</div>
        <div class="end-callout">请确认本节课已完成。点击确认后将开始生成课后总结。</div>
        <div v-if="endModalErr" class="m-error">{{ endModalErr }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="endModalBusy" @click="closeEndModal">取消</button>
          <button class="btn btn-primary" type="button" :disabled="endModalBusy || endModalCountdown > 0" @click="confirmEndLesson">
            {{ endModalBusy ? '处理中...' : endModalCountdown > 0 ? `确认结束（${endModalCountdown}s）` : '确认结束' }}
          </button>
        </div>
      </div>
    </div>

    <LessonDetailModal
      :open="lessonModalOpen"
      :model="selectedLessonModalModel"
      :cooperation-name="courseViews.find((item) => item.courseId === lessonModalCourseId)?.headline || '单节课详情'"
      :primary-label="lessonModalPrimaryLabel()"
      :primary-disabled="!lessonModalPrimaryLabel()"
      @close="closeLessonModal"
      @primary="handleLessonModalPrimary"
    />
  </div>
</template>

<style scoped>
.page-shell {
  display: grid;
  gap: 16px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.18), transparent 32%),
    linear-gradient(140deg, #fffefc, #f6fbfb 55%, #eef7f7);
}

.eyebrow,
.person-label,
.section-title,
.lab,
.booking-title,
.summary-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(31, 35, 41, 0.52);
}

.hero-title {
  margin: 8px 0 0;
  font-size: 32px;
  line-height: 1.05;
}

.hero-desc,
.banner-desc,
.side-desc,
.course-summary,
.course-meta,
.detail-summary,
.summary-text,
.section-subtitle,
.lesson-row-sub,
.flow-note,
.lesson-desc,
.m-desc,
.timeline-meta,
.timeline-empty,
.empty-desc,
.hint,
.inline-hint {
  color: var(--muted);
}

.hero-actions,
.banner-actions,
.toolbar-left,
.lesson-actions,
.detail-actions,
.card-actions,
.m-ops {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.banner,
.email-course-banner,
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
}

.toolbar {
  align-items: flex-start;
}

.banner {
  border-color: rgba(0, 190, 189, 0.22);
  background: linear-gradient(120deg, rgba(0, 190, 189, 0.08), rgba(255, 255, 255, 0.95));
}

.email-course-banner {
  border-color: rgba(0, 181, 120, 0.2);
  background: linear-gradient(120deg, rgba(0, 181, 120, 0.1), rgba(255, 248, 232, 0.9));
}

.banner-title {
  font-size: 18px;
  font-weight: 800;
}

.chip {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
}

.chip.active {
  border-color: rgba(0, 190, 189, 0.32);
  background: rgba(0, 190, 189, 0.12);
  color: #0d7e7d;
}

.search {
  width: 320px;
  height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
  outline: none;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.cooperation-panel,
.detail-panel {
  padding: 20px;
  border-radius: 28px;
}

.cooperation-panel {
  display: grid;
  gap: 16px;
}

.side-head,
.course-column,
.detail-panel,
.detail-section,
.course-card,
.journey-panel,
.current-lesson-card,
.result-card,
.lesson-list-card,
.lesson-overview-card,
.class-policy-card {
  display: grid;
  gap: 12px;
}

.course-column {
  gap: 10px;
  padding-top: 6px;
  border-top: 1px solid rgba(45, 98, 242, 0.08);
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  padding: 10px 8px;
  border: 1px solid rgba(45, 98, 242, 0.08);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 251, 255, 0.92));
}

.metric-mini {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 4px 10px;
  border-right: 1px solid rgba(45, 98, 242, 0.08);
}

.metric-mini:last-child {
  border-right: 0;
}

.metric-mini strong {
  font-size: 24px;
  line-height: 1;
}

.metric-mini span,
.trial-hint,
.summary-label,
.section-subtitle,
.person-label,
.m-hint,
.lesson-row-sub,
.inline-hint {
  font-size: 13px;
}

.course-card {
  position: relative;
  gap: 10px;
  padding: 16px;
  overflow: hidden;
  border: 1px solid rgba(80, 112, 195, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.course-card:hover,
.course-card.active {
  border-color: rgba(0, 190, 189, 0.28);
  background:
    linear-gradient(90deg, rgba(0, 190, 189, 0.08), transparent 46%),
    rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(17, 43, 98, 0.08);
  transform: translateY(-1px);
}

.course-card.active::before {
  position: absolute;
  inset: 14px auto 14px 0;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #20c7c2, #2d62f2);
  content: "";
}

.course-person,
.detail-head,
.lesson-main,
.lesson-title-row,
.lesson-top,
.lesson-row-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-top,
.detail-head,
.lesson-main,
.lesson-top {
  justify-content: space-between;
}

.course-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  object-fit: cover;
  background: linear-gradient(135deg, rgba(0, 190, 189, 0.18), rgba(31, 35, 41, 0.08));
}

.fallback {
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #0d7e7d;
}

.course-person {
  min-width: 0;
}

.course-person-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.person-name {
  font-size: 15px;
  font-weight: 800;
  line-height: 1.25;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.course-headline,
.detail-title {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.16;
}

.course-headline {
  margin-top: 4px;
  font-size: 16px;
  line-height: 1.35;
}

.course-summary {
  font-size: 13px;
  line-height: 1.5;
  color: #3f5590;
}

.course-meta {
  display: -webkit-box;
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  flex: none;
  max-width: 100%;
  white-space: nowrap;
}

.course-top > .status-pill {
  justify-self: end;
  height: 28px;
  padding: 0 11px;
  font-size: 11px;
}

.status-pill.sm {
  height: 28px;
}

.tone-slate {
  background: rgba(31, 35, 41, 0.08);
  color: rgba(31, 35, 41, 0.82);
}

.tone-amber {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.tone-sky {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.tone-violet {
  background: rgba(124, 58, 237, 0.12);
  color: #6d28d9;
}

.tone-emerald {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.tone-rose {
  background: rgba(244, 63, 94, 0.12);
  color: #be123c;
}

.trial-hint {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(124, 58, 237, 0.08);
}

.cooperation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
}

.mini-chip,
.lesson-type-pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(31, 35, 41, 0.06);
  color: rgba(31, 35, 41, 0.82);
  font-size: 11px;
  font-weight: 700;
}

.mini-chip.primary,
.lesson-type-pill {
  background: rgba(0, 190, 189, 0.12);
  color: #0d7e7d;
}

.mini-chip.warn {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.mini-chip.danger {
  background: rgba(244, 63, 94, 0.12);
  color: #be123c;
}

.mini-chip.success {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.mini-chip.violet {
  background: rgba(124, 58, 237, 0.12);
  color: #6d28d9;
}

.summary-callout,
.journey-panel,
.current-lesson-card,
.lesson-list-card,
.result-card,
.flow-note,
.booking-summary {
  padding: 18px;
  border-radius: 22px;
  background: rgba(31, 35, 41, 0.035);
}

.summary-callout {
  border: 1px solid rgba(14, 165, 233, 0.12);
}

.journey-panel {
  gap: 18px;
  border: 1px solid rgba(45, 98, 242, 0.1);
  background:
    radial-gradient(circle at top right, rgba(45, 98, 242, 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
}

.journey-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.journey-badge {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(45, 98, 242, 0.08);
  color: #2d62f2;
  font-size: 12px;
  font-weight: 800;
}

.journey-title {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 900;
  color: var(--text);
}

.journey-desc,
.journey-next-text,
.journey-step-caption,
.journey-tip {
  color: var(--muted);
}

.journey-desc {
  margin-top: 8px;
  max-width: 720px;
  line-height: 1.7;
}

.journey-next {
  min-width: 240px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(45, 98, 242, 0.08);
}

.journey-next-text {
  margin-top: 6px;
  line-height: 1.7;
  font-weight: 600;
}

.stage-context-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stage-context-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(45, 98, 242, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
}

.stage-context-card.is-warn {
  border-color: rgba(245, 158, 11, 0.22);
  background: rgba(245, 158, 11, 0.08);
}

.stage-context-card.is-danger {
  border-color: rgba(244, 63, 94, 0.2);
  background: rgba(244, 63, 94, 0.07);
}

.stage-context-card.is-success {
  border-color: rgba(16, 185, 129, 0.2);
  background: rgba(16, 185, 129, 0.07);
}

.stage-context-text {
  margin-top: 6px;
  color: var(--text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.55;
}

.journey-steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.journey-step {
  position: relative;
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
}

.journey-step-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.5);
}

.journey-step-label {
  font-size: 14px;
  font-weight: 800;
  color: var(--text);
}

.journey-step-caption {
  font-size: 12px;
  line-height: 1.5;
}

.journey-step.is-done {
  border-color: rgba(16, 185, 129, 0.18);
  background: rgba(16, 185, 129, 0.06);
}

.journey-step.is-done .journey-step-dot {
  background: #10b981;
}

.journey-step.is-current {
  border-color: rgba(45, 98, 242, 0.24);
  box-shadow: 0 16px 36px rgba(45, 98, 242, 0.08);
}

.journey-step.is-current .journey-step-dot {
  background: #2d62f2;
}

.journey-step.is-alert {
  border-color: rgba(245, 158, 11, 0.28);
  background: rgba(245, 158, 11, 0.08);
}

.journey-step.is-alert .journey-step-dot {
  background: #f59e0b;
}

.journey-tips {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.journey-tip {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(31, 35, 41, 0.035);
  line-height: 1.6;
}

.journey-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.process-info-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(45, 98, 242, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
}

.process-info-card.warn {
  border-color: rgba(245, 158, 11, 0.22);
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.95), rgba(255, 255, 255, 0.92));
}

.process-info-card.danger {
  border-color: rgba(244, 63, 94, 0.2);
  background: linear-gradient(180deg, rgba(255, 241, 242, 0.95), rgba(255, 255, 255, 0.92));
}

.process-info-card.success {
  border-color: rgba(16, 185, 129, 0.2);
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.95), rgba(255, 255, 255, 0.92));
}

.class-policy-card {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  padding: 18px;
  border: 1px solid rgba(16, 185, 129, 0.18);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(236, 253, 245, 0.9), rgba(255, 255, 255, 0.95));
}

.policy-title {
  margin-top: 6px;
  color: var(--text);
  font-size: 18px;
  font-weight: 900;
}

.policy-desc {
  margin-top: 6px;
  color: #47608f;
  line-height: 1.7;
}

.info-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.info-title {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 900;
  line-height: 1.25;
}

.info-desc,
.info-note {
  color: var(--muted);
  line-height: 1.7;
}

.info-desc {
  margin-top: 6px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.info-kv {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(45, 98, 242, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.76);
}

.info-kv span {
  color: var(--muted);
  font-size: 12px;
}

.info-kv strong {
  min-width: 0;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.info-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-text,
.flow-note {
  line-height: 1.7;
}

.lesson-time-main {
  font-size: 34px;
  font-weight: 800;
}

.lesson-state-line {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 18px;
  color: var(--muted);
}

.countdown-text {
  color: var(--primary);
  font-weight: 700;
}

.rule-hint {
  padding: 12px 14px;
  border-radius: 14px;
  line-height: 1.6;
}

.rule-hint.success {
  background: rgba(16, 185, 129, 0.1);
  color: #166534;
}

.rule-hint.warn {
  background: rgba(245, 158, 11, 0.12);
  color: #9a6700;
}

.lesson-list-card {
  gap: 10px;
}

.lesson-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.lesson-summary-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(45, 98, 242, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  cursor: pointer;
  text-align: left;
}

.lesson-summary-empty {
  display: grid;
  place-items: center;
  min-height: 128px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(31, 35, 41, 0.035);
  color: var(--muted);
  text-align: center;
}

.summary-card-label {
  font-size: 12px;
  color: var(--muted);
}

.summary-card-title {
  color: var(--text);
  font-size: 16px;
  font-weight: 900;
}

.summary-card-meta,
.summary-card-copy {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.55;
}

.lesson-overview-card {
  grid-template-columns: repeat(4, minmax(0, 110px)) minmax(240px, 1fr) auto;
  align-items: center;
  padding: 16px;
  border: 1px solid rgba(45, 98, 242, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
}

.overview-stat {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.035);
}

.overview-stat.warn {
  background: rgba(245, 158, 11, 0.12);
  color: #9a6700;
}

.overview-stat strong {
  color: var(--text);
  font-size: 22px;
  line-height: 1;
}

.overview-stat span,
.overview-desc {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.overview-title {
  color: var(--text);
  font-weight: 900;
}

.overview-desc {
  margin-top: 4px;
}

.lesson-row {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.98);
}

.empty,
.hint.error {
  padding: 22px;
  text-align: center;
}

.empty-title {
  font-size: 18px;
  font-weight: 800;
}

.btn-danger {
  border-color: rgba(244, 63, 94, 0.3);
  background: rgba(244, 63, 94, 0.1);
  color: #be123c;
}

.mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(6px);
}

.modal {
  width: min(620px, 100%);
  padding: 20px;
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
}

.timeline-modal {
  width: min(680px, 100%);
}

.schedule-modal {
  width: min(760px, 100%);
}

.end-modal {
  width: min(560px, 100%);
}

.refund-modal {
  width: min(600px, 100%);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field,
.m-form {
  display: grid;
  gap: 8px;
}

.ipt,
.input,
.txt,
.file {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px 14px;
  background: #fff;
}

.booking-row {
  justify-content: space-between;
}

.m-title {
  font-size: 22px;
  font-weight: 800;
}

.m-error {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(244, 63, 94, 0.12);
  color: #be123c;
}

.timeline-list {
  display: grid;
  gap: 14px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 999px;
  background: var(--primary);
}

.timeline-title {
  font-weight: 700;
}

.end-callout {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(245, 158, 11, 0.12);
  color: #9a6700;
  line-height: 1.6;
}

.refund-warning {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(244, 63, 94, 0.1);
  color: #be123c;
  line-height: 1.65;
}

@media (max-width: 1100px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .journey-header,
  .journey-steps,
  .stage-context-grid,
  .info-card-head,
  .info-grid,
  .lesson-summary-grid,
  .lesson-overview-card,
  .class-policy-card,
  .detail-head,
  .lesson-main,
  .lesson-top {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .status-pill {
    height: 30px;
  }
}
</style>
