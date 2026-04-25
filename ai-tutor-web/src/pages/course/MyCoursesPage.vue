<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { assetsApi } from '@/api/assets'
import { courseApi } from '@/api/course'
import { liveApi, type LiveReminderItemResp, type LiveSessionResp } from '@/api/live'
import { scheduleApi } from '@/api/schedule'
import type { CourseItemVO, ScheduleEventStatus, ScheduleEventVO, TutorApplicationVO, UserCardVO, UserSimpleVO } from '@/api/types'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { normalizeAvatarUrl } from '@/utils/avatar'

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

function resolveCourseStage(rawStatus: string, trialEndAt?: string | null): StageMeta {
  const status = String(rawStatus || '').trim().toUpperCase()
  if (status === 'WAIT_PAY') return { key: 'WAIT_PAY', label: '待支付信息费', tone: 'amber', description: '支付信息费后才会进入正式沟通。', actionLabel: '去支付' }
  if (status === 'COMMUNICATING') return { key: 'COMMUNICATING', label: '沟通中', tone: 'sky', description: '双方已进入聊天沟通，可继续了解需求或发起合作。', actionLabel: '进入聊天' }
  if (status === 'TRIALING' && trialExpired(trialEndAt)) return { key: 'TRIAL_CONFIRMING', label: '试课待确认', tone: 'violet', description: '试课已到结束时间，等待学生确认结果。', actionLabel: '查看试课' }
  if (status === 'TRIALING') return { key: 'TRIALING', label: '试课阶段', tone: 'violet', description: '合作已建立，试课日程会直接展示在双方课表与课程页中。', actionLabel: '查看试课' }
  if (status === 'TRIAL_WAIT_STUDENT_DECISION') return { key: 'TRIAL_CONFIRMING', label: '待学生确认试课结果', tone: 'violet', description: '试课已结束，需要学生确认是否继续合作。', actionLabel: '查看试课' }
  if (status === 'TRIAL_WAIT_WEEKLY_SCHEDULE') return { key: 'TRIAL_WAIT_WEEKLY_SCHEDULE', label: '待确认正式课时间', tone: 'violet', description: '试课已通过，等待学生支付并确认后续上课时间。', actionLabel: '确认课表' }
  if (status === 'TRIAL_FAILED') return { key: 'TRIAL_FAILED', label: '试课未通过', tone: 'rose', description: '本次试课未继续，课程合作将在售后处理后结束。', actionLabel: '查看详情' }
  if (status === 'TEACHING') return { key: 'TEACHING', label: '正式上课中', tone: 'emerald', description: '试课通过，已进入长期正式上课阶段。', actionLabel: '安排正式课' }
  if (status === 'FINISHED') return { key: 'FINISHED', label: '已结课', tone: 'slate', description: '课程合作已结束，可回看历史记录。', actionLabel: '查看记录' }
  if (status === 'REFUND_REVIEW') return { key: 'REFUND_REVIEW', label: '信息费退费审批中', tone: 'rose', description: '信息费退费申请已提交，等待平台审核。', actionLabel: '查看详情' }
  if (status === 'REFUNDED') return { key: 'REFUNDED', label: '已退费', tone: 'slate', description: '信息费退费完成，本次合作已结束。', actionLabel: '查看详情' }
  if (status === 'TRIAL_REFUND_REVIEW') return { key: 'TRIAL_REFUND_REVIEW', label: '试课不通过处理中', tone: 'rose', description: '试课不通过申请已提交，等待平台审核处理。', actionLabel: '查看申请' }
  return { key: 'UNKNOWN', label: status || '未知状态', tone: 'slate', description: '当前课程状态尚未完成映射，可继续查看详情。', actionLabel: '查看详情' }
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
  return status === 'IN_PROGRESS' || !!live.actualStartAt
}

function isLessonEnterable(lesson: ScheduleEventVO | null) {
  if (!lesson?.id) return false
  if (lesson.status !== 'ACCEPTED' && lesson.status !== 'COMPLETED') return false
  return Date.now() < Number(lesson.endAt || 0)
}

function isAbnormalPendingConfirm(lesson: ScheduleEventVO | null, live: LiveSessionResp | null) {
  if (!lesson || lesson.status !== 'ACCEPTED') return false
  return Number(lesson.endAt || 0) <= Date.now() && !hasLiveStarted(live)
}

function lessonTone(lesson: ScheduleEventVO | null, live: LiveSessionResp | null): CourseViewModel['lessonStateTone'] {
  if (!lesson) return 'slate'
  if (isAbnormalPendingConfirm(lesson, live)) return 'amber'
  if (hasLiveStarted(live)) return 'emerald'
  if (lesson.status === 'PENDING' || lesson.status === 'RESCHEDULE_PENDING') return 'amber'
  if (lesson.status === 'REJECTED' || lesson.status === 'CANCELED') return 'rose'
  if (lesson.status === 'COMPLETED') return 'slate'
  if (lesson.status === 'ACCEPTED') return 'sky'
  return 'slate'
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
  if (isAbnormalPendingConfirm(lesson, live)) {
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
  const countdownText = latestLesson?.startAt ? formatCountdown(Number(latestLesson.startAt) - Date.now()) : ''
  const canEnterClassroom = isLessonEnterable(latestLesson)
  const canEndClassroom = !!live?.sessionId && hasLiveStarted(live) && String(live.status || '').trim().toUpperCase() !== 'ENDED'
  const showAbnormalAttendanceConfirm = isAbnormalPendingConfirm(latestLesson, live)
  const abnormalAttendanceHint = showAbnormalAttendanceConfirm
    ? '到预约结束时间前课程保持未开始；结束后若整节课从未建立双人实时视频，则进入“待确认未上课/异常待处理”，不直接进入课后总结。'
    : ''
  const afterClass = afterClassMeta(aiResult?.resultStatus || item.aiResultStatus, latestLesson, live)

  let currentStateNote = stage.description
  let currentActionHint = latestLesson
    ? `${lessonStatusText(latestLesson.status)} · ${fmtDateTime(latestLesson.startAt)} - ${fmtDateTime(latestLesson.endAt, { hour: '2-digit', minute: '2-digit' })}`
    : stage.description

  if (canEnterClassroom) {
    currentStateNote = latestLesson && Number(latestLesson.startAt) > Date.now() ? '当前状态：未开始，但现在可以进入课堂' : '当前状态：课程可进入'
    currentActionHint = `课程在预约结束前都可以进入；${countdownText || '当前'}也能进入课堂等待对方。`
  } else if (showAbnormalAttendanceConfirm) {
    currentStateNote = '当前状态：待确认未上课'
    currentActionHint = '对方未到场 / 本节未正常开始，等待双方或客服确认。'
  } else if (canEndClassroom) {
    currentStateNote = '当前状态：课程进行中，可结束课程'
    currentActionHint = '本节课曾成功建立过双人实时视频，已满足结束课程条件。'
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
    lessonStatusText: showAbnormalAttendanceConfirm ? '待确认未上课' : latestLesson ? lessonStatusText(latestLesson.status) : '待生成课节',
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
    endBlockedReason: canEndClassroom ? '' : '未检测到双方成功建立实时视频，暂不可结束课程',
    showAbnormalAttendanceConfirm,
    abnormalAttendanceHint,
    afterClassStatusLabel: afterClass.label,
    afterClassStatusTone: afterClass.tone,
    afterClassHint: afterClass.hint,
  }
}

const courseViews = computed(() => list.value.map((item) => buildCourseView(item)))

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
    upcoming: all.filter((item) => !!item.latestLesson && item.latestLesson.status === 'ACCEPTED').length,
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
  return String(it.status || '').trim().toUpperCase() === 'TRIAL_WAIT_STUDENT_DECISION'
}

function canSubmitTrialFail(it: CourseItemVO) {
  if (isTeacher.value) return false
  const s = String(it.status || '').trim().toUpperCase()
  return s === 'TRIAL_WAIT_STUDENT_DECISION' || s === 'TRIAL_WAIT_WEEKLY_SCHEDULE'
}

function goChat(roomId?: number | null) {
  if (!roomId) return
  void router.push({ name: 'chatRoom', params: { roomId: String(roomId) } })
}

function goCourseDetail(courseId: number) {
  void router.push({ name: 'courseDetail', params: { courseId: String(courseId) } })
}

function goSchedule() {
  void router.push({ name: 'schedule' })
}

function goLivePrepare(lessonCourseId?: number | null) {
  if (!lessonCourseId) {
    toast.show('当前课程还没有可进入的课节。', 'info')
    return
  }
  void router.push({ name: 'livePrepare', params: { courseId: String(lessonCourseId) } })
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
    toast.show('已确认试课合适，请继续提交正式每周课表。', 'success')
    await load()
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
      const lessonCourseId = lessonMap.value[item.courseId]?.id
      if (!lessonCourseId) return [item.courseId, null] as const
      try {
        return [item.courseId, await liveApi.getByCourse(lessonCourseId)] as const
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
      const lessonCourseId = lessonMap.value[item.courseId]?.id
      mapped[item.courseId] = reminders.find((entry) => entry.courseId === lessonCourseId) || null
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
        <button v-if="reminderBanner.canEnterClassroom" class="btn btn-primary" type="button" @click="goLivePrepare(reminderBanner.latestLesson?.id)">进入课堂</button>
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
                <div>
                  <div class="person-label">{{ item.participantRoleLabel }}</div>
                  <div class="person-name">{{ item.participantName }}</div>
                </div>
              </div>
              <span class="status-pill" :class="statusToneClass(item.stage.tone)">{{ item.stage.label }}</span>
            </div>
            <div class="course-headline">{{ item.headline }}</div>
            <div class="course-summary">{{ item.currentStateNote }}</div>
            <div class="course-meta">{{ item.currentActionHint }}</div>
            <div v-if="item.trialCountdown" class="trial-hint">{{ item.trialCountdown }}</div>
            <div class="cooperation-tags">
              <span v-if="item.latestLesson" class="mini-chip">{{ item.lessonStatusText }}</span>
              <span v-if="item.countdownText && item.canEnterClassroom" class="mini-chip primary">{{ item.countdownText }}</span>
              <span v-if="item.showAbnormalAttendanceConfirm" class="mini-chip warn">待确认未上课</span>
              <span v-if="item.aiResultStatus === 'FAILED'" class="mini-chip danger">课后生成失败</span>
            </div>
          </article>
        </div>
      </aside>

      <main v-if="selectedCourse" class="detail-panel card">
        <div class="detail-head">
          <div>
            <div class="detail-title">{{ selectedCourse.headline }}</div>
            <div class="detail-summary">合作说明：试课通过后由学生确认是否通过；确认通过后进入支付与后续上课时间确认。</div>
          </div>
          <span class="status-pill" :class="statusToneClass(selectedCourse.stage.tone)">{{ selectedCourse.stage.label }}</span>
        </div>

        <div class="summary-callout">
          <div class="summary-label">当前说明</div>
          <div class="summary-text">本节课是一个会议事件。进入课堂只代表进入等待态，不立即产生上课数据；只要本节课曾成功建立过一次双人实时视频，就满足“可结束课程”条件。</div>
        </div>

        <section class="detail-section">
          <div class="section-title strong">当前课程</div>
          <div class="section-subtitle">把当前状态、按钮规则和限制原因收在一张卡片里。</div>
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

            <div class="rule-hint success">{{ selectedCourse.currentActionHint }}</div>
            <div v-if="selectedCourse.showAbnormalAttendanceConfirm" class="rule-hint warn">{{ selectedCourse.abnormalAttendanceHint }}</div>

            <div class="card-actions">
              <button class="btn btn-primary" type="button" :disabled="!selectedCourse.canEnterClassroom" @click="goLivePrepare(selectedCourse.latestLesson?.id)">进入课堂</button>
              <button class="btn" type="button" @click="goCourseDetail(selectedCourse.courseId)">查看课程详情</button>
              <button class="btn" type="button" :disabled="!selectedCourse.canEndClassroom" @click="openEndModal(selectedCourse)">结束课程</button>
              <span v-if="!selectedCourse.canEndClassroom" class="inline-hint">{{ selectedCourse.endBlockedReason }}</span>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title strong">合作内全部课程</div>
          <div class="section-subtitle">同一个合作下展示已预约、上课中、已结束与异常待处理的全部课节。</div>
          <div class="lesson-list-card">
            <div v-for="event in selectedCourse.lessonList" :key="event.id" class="lesson-row">
              <div class="lesson-row-main">
                <span class="mini-chip" :class="event.lessonType === 'TRIAL' ? 'violet' : 'primary'">{{ event.lessonType === 'TRIAL' ? '试课' : '正式课' }}</span>
                <strong>{{ fmtDateTime(event.startAt) }} - {{ fmtDateTime(event.endAt, { hour: '2-digit', minute: '2-digit' }) }}</strong>
                <span class="status-pill sm" :class="isAbnormalPendingConfirm(event, selectedCourse.live) ? 'tone-amber' : statusToneClass(lessonTone(event, selectedCourse.live))">
                  {{ isAbnormalPendingConfirm(event, selectedCourse.live) ? '待确认未上课' : lessonStatusText(event.status) }}
                </span>
              </div>
              <div class="lesson-row-sub">
                <span v-if="isLessonEnterable(event)">在预约结束前均可进入课堂</span>
                <span v-else-if="isAbnormalPendingConfirm(event, selectedCourse.live)">对方未到场 / 本节未正常开始，等待双方或客服确认</span>
                <span v-else>{{ formatDuration(event.startAt, event.endAt) }}</span>
              </div>
            </div>
            <div v-if="selectedCourse.lessonList.length === 0" class="mini-empty">当前合作还没有同步出具体课节。</div>
          </div>
        </section>

        <section class="detail-section">
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

        <section class="detail-section">
          <div class="section-title strong">流程说明</div>
          <div class="flow-note">
            当前阶段：{{ selectedCourse.stage.description }}<br />
            业务规则：课程在预约结束前允许进入；超过结束时间后禁止新进入，但已经在课堂中的用户可继续停留；若整节课未建立双人实时视频，则进入“待确认未上课/异常待处理”。
          </div>
        </section>

        <div class="detail-actions">
          <button class="btn" type="button" :disabled="!selectedCourse.roomId" @click="goChat(selectedCourse.roomId)">进入聊天</button>
          <button class="btn" type="button" :disabled="!canOpenSchedule(selectedCourse)" @click="selectedCourse && openSchedule(selectedCourse)">安排正式课</button>
          <button class="btn" type="button" @click="goSchedule">查看日程表</button>
          <button v-if="canConfirmTrialPass(selectedCourse.raw)" class="btn btn-primary" type="button" :disabled="actionBusyCourseId === selectedCourse.courseId" @click="submitTrialPass(selectedCourse.courseId)">试课合适</button>
          <button v-if="canSubmitTrialFail(selectedCourse.raw)" class="btn btn-danger" type="button" @click="openTrialRefund(selectedCourse.courseId)">试课不合适</button>
          <button v-if="selectedCourse.stage.key === 'WAIT_PAY'" class="btn" type="button" :disabled="actionBusyCourseId === selectedCourse.courseId" @click="openApplicationFlow(selectedCourse)">
            {{ actionBusyCourseId === selectedCourse.courseId ? '处理中...' : '去支付' }}
          </button>
        </div>
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
        <div class="m-desc">线上试课不合适会结束课程并关闭聊天；如符合规则，教师可后续申请退还 80% 信息费。</div>
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
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.cooperation-panel,
.detail-panel {
  padding: 22px;
  border-radius: 28px;
}

.side-head,
.course-column,
.detail-panel,
.detail-section,
.course-card,
.current-lesson-card,
.result-card,
.lesson-list-card {
  display: grid;
  gap: 12px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-mini {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
}

.metric-mini strong {
  font-size: 24px;
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
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.course-card:hover,
.course-card.active {
  border-color: rgba(0, 190, 189, 0.28);
  box-shadow: 0 18px 40px rgba(0, 190, 189, 0.1);
  transform: translateY(-1px);
}

.course-top,
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

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  object-fit: cover;
  background: linear-gradient(135deg, rgba(0, 190, 189, 0.18), rgba(31, 35, 41, 0.08));
}

.fallback {
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #0d7e7d;
}

.person-name {
  font-size: 16px;
  font-weight: 800;
}

.course-headline,
.detail-title {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.16;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
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
  gap: 8px;
}

.mini-chip,
.lesson-type-pill {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(31, 35, 41, 0.06);
  color: rgba(31, 35, 41, 0.82);
  font-size: 12px;
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

.mini-chip.violet {
  background: rgba(124, 58, 237, 0.12);
  color: #6d28d9;
}

.summary-callout,
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

@media (max-width: 1100px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search {
    width: 100%;
  }

  .course-top,
  .detail-head,
  .lesson-main,
  .lesson-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
