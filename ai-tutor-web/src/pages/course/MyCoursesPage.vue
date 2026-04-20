<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
  | 'APPLYING'
  | 'APPLY_REJECTED'
  | 'WAIT_PAY'
  | 'PAY_FAILED'
  | 'PAY_EXPIRED'
  | 'COMMUNICATING'
  | 'COMMUNICATION_CLOSED'
  | 'COLLABORATING'
  | 'COLLAB_REJECTED'
  | 'TRIALING'
  | 'TRIAL_CONFIRMING'
  | 'TRIAL_FAILED'
  | 'TEACHING'
  | 'PAUSED'
  | 'TERMINATING'
  | 'FINISHED'
  | 'REFUND_REVIEW'
  | 'REFUNDED'
  | 'TRIAL_REFUND_REVIEW'
  | 'UNKNOWN'

type CourseViewModel = {
  raw: CourseItemVO
  courseId: number
  applicationId: number
  application: TutorApplicationVO | null
  roomId: number | null
  teacherUid: number
  studentUid: number
  stage: ReturnType<typeof resolveCourseStage>
  participantUid: number
  participant: UserSimpleVO | null
  participantCard: UserCardVO | null
  participantName: string
  participantRoleLabel: string
  participantSubtitle: string
  avatar: string
  headline: string
  summary: string
  stageHint: string
  progressStep: number
  progressText: string
  nextActionLabel: string
  nextActionTone: 'primary' | 'neutral'
  lessonCourseId: number | null
  latestLesson: ScheduleEventVO | null
  lessonState: ReturnType<typeof resolveLessonState>
  lessonStatusText: string
  lessonTimeText: string
  lessonSubtitle: string
  live: LiveSessionResp | null
  reminder: LiveReminderItemResp | null
  isOnlineCourse: boolean
  liveBadge: string
  liveHint: string
  trialCountdown: string
}

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const isTeacher = computed(() => auth.user?.userType === 1)
const role = computed<'TEACHER' | 'STUDENT'>(() => (isTeacher.value ? 'TEACHER' : 'STUDENT'))
const loading = ref(false)
const error = ref<string | null>(null)
const list = ref<CourseItemVO[]>([])
const liveMap = ref<Record<number, LiveSessionResp>>({})
const lessonMap = ref<Record<number, ScheduleEventVO | null>>({})
const reminderMap = ref<Record<number, LiveReminderItemResp>>({})
const applicationMap = ref<Record<number, TutorApplicationVO | null>>({})
const userMap = ref<Record<number, UserSimpleVO>>({})
const userCardMap = ref<Record<number, UserCardVO | null>>({})
const avatarBroken = ref<Record<number, boolean>>({})
const actionBusyCourseId = ref<number | null>(null)

const stageFilter = ref<'ALL' | 'ACTIVE' | CourseStageKey>('ALL')
const search = ref('')
const selectedCourseId = ref<number | null>(null)

const scheduleOpen = ref(false)
const scheduleBusy = ref(false)
const scheduleError = ref<string | null>(null)
const scheduleCourseId = ref<number | null>(null)
const scheduleTitle = ref('')
const scheduleDescription = ref('')
const scheduleStartAt = ref<number>(roundToNextHalfHour(Date.now()))
const scheduleEndAt = ref<number>(scheduleStartAt.value + 60 * 60 * 1000)
const scheduleReminderMinutes = ref<number>(30)

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

const progressSteps = ['申请', '信息费', '沟通', '合作', '试课', '正式上课', '结课']

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

function fmtDateOnly(value: string | number | Date | null | undefined) {
  return fmtDateTime(value, { year: 'numeric', month: 'numeric', day: 'numeric' })
}

function formatDuration(startAt: number, endAt: number) {
  const minutes = Math.max(0, Math.round((endAt - startAt) / 60000))
  if (!minutes) return '未设置时长'
  if (minutes % 60 === 0) return `${minutes / 60} 小时`
  if (minutes > 60) return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分`
  return `${minutes} 分钟`
}

function trialExpired(trialEndAt?: string | null): boolean {
  if (!trialEndAt) return false
  const t = Date.parse(trialEndAt)
  return Number.isFinite(t) ? Date.now() > t : false
}

function resolveCourseStage(rawStatus: string, trialEndAt?: string | null) {
  const status = String(rawStatus || '').trim().toUpperCase()
  if (status === 'APPLYING') {
    return {
      key: 'APPLYING' as const,
      label: '发起申请中',
      tone: 'slate',
      description: '已提交申请，等待老师或平台处理。',
      actionLabel: '查看申请',
      phaseIndex: 1,
      sectionLabel: '申请阶段',
    }
  }
  if (status === 'WAIT_PAY') {
    return {
      key: 'WAIT_PAY' as const,
      label: '待支付信息费',
      tone: 'amber',
      description: '支付信息费后才会进入正式沟通。',
      actionLabel: '去支付',
      phaseIndex: 2,
      sectionLabel: '支付阶段',
    }
  }
  if (status === 'COMMUNICATING') {
    return {
      key: 'COMMUNICATING' as const,
      label: '沟通中',
      tone: 'sky',
      description: '双方已进入聊天沟通，可继续了解需求或发起合作。',
      actionLabel: '进入聊天',
      phaseIndex: 3,
      sectionLabel: '沟通阶段',
    }
  }
  if (status === 'TRIALING') {
    if (trialExpired(trialEndAt)) {
      return {
        key: 'TRIAL_CONFIRMING' as const,
        label: '试课待确认',
        tone: 'violet',
        description: '试课周期已到，等待双方确认是否进入正式上课。',
        actionLabel: '查看试课',
        phaseIndex: 5,
        sectionLabel: '试课阶段',
      }
    }
    return {
      key: 'TRIALING' as const,
      label: '试课中',
      tone: 'violet',
      description: '合作已建立，当前处于一周试课观察期。',
      actionLabel: '预约试课',
      phaseIndex: 5,
      sectionLabel: '试课阶段',
    }
  }
  if (status === 'TEACHING') {
    return {
      key: 'TEACHING' as const,
      label: '正式上课中',
      tone: 'emerald',
      description: '试课通过，已进入长期正式上课阶段。',
      actionLabel: '安排上课',
      phaseIndex: 6,
      sectionLabel: '上课阶段',
    }
  }
  if (status === 'FINISHED') {
    return {
      key: 'FINISHED' as const,
      label: '已结课',
      tone: 'slate',
      description: '课程合作已结束，支持回看历史记录。',
      actionLabel: '查看记录',
      phaseIndex: 7,
      sectionLabel: '结课阶段',
    }
  }
  if (status === 'REFUND_REVIEW') {
    return {
      key: 'REFUND_REVIEW' as const,
      label: '信息费退费审批中',
      tone: 'rose',
      description: '信息费退费申请已提交，等待平台审核。',
      actionLabel: '查看详情',
      phaseIndex: 3,
      sectionLabel: '售后阶段',
    }
  }
  if (status === 'REFUNDED') {
    return {
      key: 'REFUNDED' as const,
      label: '已退费',
      tone: 'slate',
      description: '信息费退费完成，本次合作已结束。',
      actionLabel: '查看详情',
      phaseIndex: 3,
      sectionLabel: '售后阶段',
    }
  }
  if (status === 'TRIAL_REFUND_REVIEW') {
    return {
      key: 'TRIAL_REFUND_REVIEW' as const,
      label: '试课不通过处理中',
      tone: 'rose',
      description: '试课不通过申请已提交，等待平台审核处理。',
      actionLabel: '查看申请',
      phaseIndex: 5,
      sectionLabel: '试课阶段',
    }
  }
  return {
    key: 'UNKNOWN' as const,
    label: status || '未知状态',
    tone: 'slate',
    description: '当前课程状态尚未完成映射，可继续查看课程详情。',
    actionLabel: '查看详情',
    phaseIndex: 1,
    sectionLabel: '课程状态',
  }
}

function lessonStatusText(status: ScheduleEventStatus) {
  if (status === 'PENDING') return '约课待确认'
  if (status === 'ACCEPTED') return '已预约'
  if (status === 'REJECTED') return '老师已拒绝'
  if (status === 'CANCELED') return '已取消'
  return '状态未知'
}

function resolveLessonState(lesson: ScheduleEventVO | null, live: LiveSessionResp | null) {
  if (!lesson) {
    return {
      key: 'NOT_STARTED' as const,
      label: '待约课',
      tone: 'slate',
      description: '还没有安排具体课节。',
    }
  }
  const now = Date.now()
  const startAt = Number(lesson.startAt || 0)
  const endAt = Number(lesson.endAt || 0)

  if (live?.status === 'IN_PROGRESS') {
    return { key: 'IN_PROGRESS' as const, label: '上课中', tone: 'emerald', description: '课堂已开始。' }
  }
  if (live?.joinableNow) {
    return { key: 'JOINABLE' as const, label: '即将开始', tone: 'amber', description: '课堂入口已开放，可进入课前准备。' }
  }
  if (lesson.status === 'PENDING') {
    return { key: 'PENDING' as const, label: '约课待确认', tone: 'amber', description: '等待对方确认这一节课。' }
  }
  if (lesson.status === 'REJECTED') {
    return { key: 'REJECTED' as const, label: '老师已拒绝', tone: 'rose', description: '本次课节未确认，需要重新约课。' }
  }
  if (lesson.status === 'CANCELED') {
    return { key: 'CANCELED' as const, label: '已取消', tone: 'rose', description: '本次课节已被取消。' }
  }
  if (lesson.status === 'ACCEPTED' && endAt < now) {
    return { key: 'FINISHED' as const, label: '已完成', tone: 'slate', description: '最近一节课已经结束。' }
  }
  if (lesson.status === 'ACCEPTED' && startAt > now) {
    const delta = startAt - now
    if (delta <= 30 * 60 * 1000) {
      return { key: 'UPCOMING' as const, label: '即将开始', tone: 'amber', description: '距离开课时间较近。' }
    }
    return { key: 'ACCEPTED' as const, label: '已预约', tone: 'sky', description: '课节已确认，等待上课。' }
  }
  return { key: 'ACCEPTED' as const, label: lessonStatusText(lesson.status), tone: 'sky', description: '已安排具体课节。' }
}

function buildCourseTitle(item: CourseItemVO, participantName: string, stage: ReturnType<typeof resolveCourseStage>) {
  if (stage.key === 'TRIALING' || stage.key === 'TRIAL_CONFIRMING') return `与${participantName}的试课安排`
  if (stage.key === 'TEACHING') return `与${participantName}的正式课程`
  if (stage.key === 'COMMUNICATING') return `与${participantName}的课程沟通`
  if (stage.key === 'WAIT_PAY') return `与${participantName}的课程申请`
  if (stage.key === 'FINISHED') return `与${participantName}的课程记录`
  return `课程 #${item.courseId}`
}

function buildParticipantSubtitle(card: UserCardVO | null, isTeacherParticipant: boolean) {
  if (isTeacherParticipant) {
    const profile = card?.teacherProfile
    const parts = [profile?.subject, profile?.education, profile?.experienceYears != null ? `${profile.experienceYears} 年经验` : ''].filter(Boolean)
    return parts.length > 0 ? parts.join(' · ') : '老师信息待完善'
  }
  const student = card?.studentProfile
  const job = card?.jobPosting
  const parts = [
    student?.childAge != null ? `${student.childAge} 岁` : '',
    job?.subjectName || '',
    job?.city || '',
  ].filter(Boolean)
  return parts.length > 0 ? parts.join(' · ') : '学生信息待完善'
}

function buildCourseSummary(stage: ReturnType<typeof resolveCourseStage>, participantName: string, lesson: ScheduleEventVO | null, live: LiveSessionResp | null) {
  if (lesson) {
    return `${stage.label} · 下一节 ${fmtDateTime(lesson.startAt)} · ${lessonStatusText(lesson.status)}`
  }
  if (live?.scheduledStartAt) {
    return `${stage.label} · 课堂计划 ${fmtDateTime(live.scheduledStartAt)}`
  }
  return `${participantName} · ${stage.description}`
}

function buildStageHint(stage: ReturnType<typeof resolveCourseStage>, lessonState: ReturnType<typeof resolveLessonState>, trialEndAt?: string | null) {
  if ((stage.key === 'TRIALING' || stage.key === 'TRIAL_CONFIRMING') && trialEndAt) {
    return `试课截止：${fmtDateOnly(trialEndAt)}`
  }
  if (stage.key === 'WAIT_PAY') return '支付信息费后即可进入沟通'
  if (stage.key === 'COMMUNICATING') return '可以继续聊天，也可以发起合作'
  if (stage.key === 'TEACHING' && lessonState.key === 'NOT_STARTED') return '建议尽快安排下一节课'
  return lessonState.description
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

function buildLessonSubtitle(lesson: ScheduleEventVO | null, live: LiveSessionResp | null) {
  if (!lesson && !live) return '试课或正式上课后，会在这里看到具体课节。'
  if (live?.joinableNow) return '课堂已开放，双方可进入课前准备。'
  if (lesson) return `${formatDuration(lesson.startAt, lesson.endAt)} · ${lessonStatusText(lesson.status)}`
  return '课堂信息已生成'
}

function buildLiveBadge(live: LiveSessionResp | null, reminder: LiveReminderItemResp | null) {
  if (live?.status === 'IN_PROGRESS') return '课堂进行中'
  if (live?.joinableNow) return '课堂已开放'
  if (reminder?.joinableNow) return '可进入课堂'
  if (live?.scheduledStartAt) return '线上课堂'
  return ''
}

function buildLiveHint(live: LiveSessionResp | null, reminder: LiveReminderItemResp | null) {
  if (live?.status === 'IN_PROGRESS') return '当前可以直接进入课堂'
  if (live?.scheduledStartAt) return `预计开始：${fmtDateTime(live.scheduledStartAt)}`
  if (reminder?.scheduledStartAt) return `最近提醒：${fmtDateTime(reminder.scheduledStartAt)}`
  return '未生成课堂'
}

function buildCourseView(item: CourseItemVO): CourseViewModel {
  const participantUid = isTeacher.value ? item.studentUid : item.teacherUid
  const participant = userMap.value[participantUid] || null
  const participantCard = userCardMap.value[participantUid] || null
  const application = applicationMap.value[item.applicationId] || null
  const participantName = displayNameOf(participant, participantUid)
  const stage = resolveCourseStage(item.status, item.trialEndAt)
  const live = liveMap.value[item.courseId] || null
  const lesson = lessonMap.value[item.courseId] || null
  const reminder = reminderMap.value[item.courseId] || null
  const lessonState = resolveLessonState(lesson, live)
  const isTeacherParticipant = participant?.userType === 1

  return {
    raw: item,
    courseId: item.courseId,
    applicationId: item.applicationId,
    application,
    roomId: application?.roomId ?? item.roomId ?? null,
    teacherUid: item.teacherUid,
    studentUid: item.studentUid,
    stage,
    participantUid,
    participant,
    participantCard,
    participantName,
    participantRoleLabel: isTeacherParticipant ? '授课老师' : '学生',
    participantSubtitle: buildParticipantSubtitle(participantCard, isTeacherParticipant),
    avatar: avatarOf(participantUid, participant?.avatar),
    headline: buildCourseTitle(item, participantName, stage),
    summary: buildCourseSummary(stage, participantName, lesson, live),
    stageHint: buildStageHint(stage, lessonState, item.trialEndAt),
    progressStep: stage.phaseIndex,
    progressText: `${stage.sectionLabel} · 第 ${Math.min(stage.phaseIndex, progressSteps.length)} / ${progressSteps.length} 步`,
    nextActionLabel: stage.actionLabel,
    nextActionTone: stage.key === 'WAIT_PAY' || stage.key === 'TRIALING' || stage.key === 'TEACHING' ? 'primary' : 'neutral',
    lessonCourseId: lesson?.id ?? null,
    latestLesson: lesson,
    lessonState,
    lessonStatusText: lessonState.label,
    lessonTimeText: lesson ? `${fmtDateTime(lesson.startAt)} - ${fmtDateTime(lesson.endAt, { hour: '2-digit', minute: '2-digit' })}` : '还没有安排具体课节',
    lessonSubtitle: buildLessonSubtitle(lesson, live),
    live,
    reminder,
    isOnlineCourse: !!live || !!lesson,
    liveBadge: buildLiveBadge(live, reminder),
    liveHint: buildLiveHint(live, reminder),
    trialCountdown: buildTrialCountdown(item.trialEndAt),
  }
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

function activeStageFilterKeys() {
  return new Set<CourseStageKey>(['COMMUNICATING', 'COLLABORATING', 'TRIALING', 'TRIAL_CONFIRMING', 'TEACHING', 'PAUSED'])
}

const courseViews = computed(() => list.value.map((item) => buildCourseView(item)))

const filteredCourses = computed(() => {
  const kw = search.value.trim().toLowerCase()
  const activeKeys = activeStageFilterKeys()
  return courseViews.value.filter((item) => {
    if (stageFilter.value === 'ACTIVE' && !activeKeys.has(item.stage.key)) return false
    if (stageFilter.value !== 'ALL' && stageFilter.value !== 'ACTIVE' && item.stage.key !== stageFilter.value) return false
    if (!kw) return true
    const text = [item.headline, item.participantName, item.participantSubtitle, item.summary, item.lessonSubtitle].join(' ').toLowerCase()
    return text.includes(kw)
  })
})

const selectedCourse = computed(() => {
  const pool = filteredCourses.value.length > 0 ? filteredCourses.value : courseViews.value
  if (pool.length === 0) return null
  const picked = pool.find((item) => item.courseId === selectedCourseId.value)
  return picked || pool[0]
})

const overview = computed(() => {
  const all = courseViews.value
  const active = all.filter((item) => ['COMMUNICATING', 'TRIALING', 'TRIAL_CONFIRMING', 'TEACHING'].includes(item.stage.key)).length
  const waitingPay = all.filter((item) => item.stage.key === 'WAIT_PAY').length
  const trialing = all.filter((item) => item.stage.key === 'TRIALING' || item.stage.key === 'TRIAL_CONFIRMING').length
  const upcoming = all.filter((item) => ['ACCEPTED', 'UPCOMING', 'JOINABLE', 'IN_PROGRESS'].includes(item.lessonState.key)).length
  return { total: all.length, active, waitingPay, trialing, upcoming }
})

const upcomingLessons = computed(() =>
  courseViews.value
    .filter((item) => item.latestLesson || item.live?.scheduledStartAt || item.reminder?.scheduledStartAt)
    .slice()
    .sort((a, b) => {
      const aTime = a.latestLesson?.startAt || Date.parse(a.live?.scheduledStartAt || a.reminder?.scheduledStartAt || '') || Number.MAX_SAFE_INTEGER
      const bTime = b.latestLesson?.startAt || Date.parse(b.live?.scheduledStartAt || b.reminder?.scheduledStartAt || '') || Number.MAX_SAFE_INTEGER
      return aTime - bTime
    })
    .slice(0, 5),
)

const reminderBanner = computed(() => {
  const joinable = courseViews.value.find((item) => item.live?.joinableNow || item.reminder?.joinableNow)
  if (joinable) return joinable
  return upcomingLessons.value.find((item) => {
    const startAt = item.latestLesson?.startAt || Date.parse(item.live?.scheduledStartAt || item.reminder?.scheduledStartAt || '')
    return Number.isFinite(startAt) && startAt - Date.now() <= 30 * 60 * 1000
  }) || null
})

function statusToneClass(tone: string) {
  return `tone-${tone}`
}

function canOpenSchedule(item: CourseViewModel | null) {
  if (!item) return false
  return item.stage.key === 'TRIALING' || item.stage.key === 'TRIAL_CONFIRMING' || item.stage.key === 'TEACHING'
}

function canApplyTrialRefund(it: CourseItemVO) {
  if (!isTeacher.value) return false
  const s = String(it.status || '').trim().toUpperCase()
  if (s !== 'TRIALING') return false
  if (trialExpired(it.trialEndAt)) return false
  return true
}

function goChat(roomId?: number | null) {
  if (!roomId) return
  void router.push({ name: 'chatRoom', params: { roomId: String(roomId) } })
}

function goLivePrepare(lessonCourseId?: number | null) {
  if (!lessonCourseId) {
    toast.show('当前课程还没有可进入的已确认课节。', 'info')
    return
  }
  void router.push({ name: 'livePrepare', params: { courseId: String(lessonCourseId) } })
}

function goSchedule() {
  void router.push({ name: 'schedule' })
}

function goCourseDetail(courseId: number) {
  void router.push({ name: 'courseDetail', params: { courseId: String(courseId) } })
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

function onPrimaryAction(item: CourseViewModel) {
  if (item.stage.key === 'WAIT_PAY' || item.stage.key === 'COMMUNICATING') {
    void openApplicationFlow(item)
    return
  }
  if (item.stage.key === 'TRIALING' || item.stage.key === 'TRIAL_CONFIRMING' || item.stage.key === 'TEACHING') {
    openSchedule(item)
    return
  }
  if (item.roomId) {
    goChat(item.roomId)
    return
  }
  toast.show('当前课程暂未生成沟通会话。', 'info')
}

function selectCourse(courseId: number) {
  selectedCourseId.value = courseId
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
  scheduleDescription.value = item.stage.key === 'TRIALING' || item.stage.key === 'TRIAL_CONFIRMING' ? '试课安排' : '正式课程安排'
  scheduleStartAt.value = roundToNextHalfHour(Date.now() + 2 * 60 * 60 * 1000)
  scheduleEndAt.value = scheduleStartAt.value + 60 * 60 * 1000
  scheduleReminderMinutes.value = 30
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
  const titleText = scheduleTitle.value.trim()
  if (!titleText) {
    scheduleError.value = '请输入课程名称'
    return
  }
  if (!(scheduleEndAt.value > scheduleStartAt.value)) {
    scheduleError.value = '结束时间必须晚于开始时间'
    return
  }
  if (scheduleStartAt.value < Date.now() - 60_000) {
    scheduleError.value = '请选择当前时间之后的课程'
    return
  }

  scheduleBusy.value = true
  try {
    const created = await scheduleApi.createEvent({
      courseId: item.courseId,
      title: titleText,
      participantUserId: item.participantUid,
      startAt: scheduleStartAt.value,
      endAt: scheduleEndAt.value,
      description: `${scheduleDescription.value.trim() || '课程安排'}\n提醒：提前 ${scheduleReminderMinutes.value} 分钟`,
    })
    lessonMap.value = { ...lessonMap.value, [item.courseId]: created }
    scheduleOpen.value = false
    toast.show('约课申请已发送，聊天窗口会同步展示课节信息。', 'success', 3000)
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
  const files = input.files ? Array.from(input.files) : []
  modalFiles.value = files
}

async function submitTrialRefund() {
  if (modalBusy.value) return
  if (!modalCourseId.value) return
  const reason = modalReason.value.trim()
  if (!reason) {
    modalErr.value = '请填写试课不通过说明'
    return
  }
  if (modalFiles.value.length < 1) {
    modalErr.value = '请至少上传 1 张证据图片'
    return
  }
  const videoUrl = modalVideoUrl.value.trim()
  if (!videoUrl) {
    modalErr.value = '请上传并填写微信聊天录屏 URL'
    return
  }
  const duration = Number(modalVideoDurationSeconds.value)
  if (!Number.isFinite(duration) || duration <= 0 || duration > 60) {
    modalErr.value = '录屏时长需控制在 1-60 秒内'
    return
  }

  modalBusy.value = true
  modalErr.value = null
  try {
    const urls: string[] = []
    for (const file of modalFiles.value) {
      const uploaded = await assetsApi.uploadImage(file, 'trial_refund')
      if (uploaded?.url) urls.push(uploaded.url)
    }
    if (urls.length < 1) {
      modalErr.value = '图片上传失败，请稍后重试'
      return
    }
    await courseApi.applyTrialRefund(modalCourseId.value, {
      reason,
      evidenceImageUrls: urls,
      evidenceVideoUrl: videoUrl,
      evidenceVideoDurationSeconds: Math.round(duration),
    })
    modalOpen.value = false
    await load()
  } catch (e) {
    modalErr.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    modalBusy.value = false
  }
}

async function openLiveTimeline(courseId: number) {
  const lessonCourseId = lessonMap.value[courseId]?.id
  if (!lessonCourseId) {
    liveTimelineCourseId.value = courseId
    liveTimelineOpen.value = true
    liveTimelineBusy.value = false
    liveTimelineError.value = '当前课程还没有已确认并同步课堂的课节'
    liveTimelineItems.value = []
    return
  }
  liveTimelineCourseId.value = courseId
  liveTimelineOpen.value = true
  liveTimelineBusy.value = true
  liveTimelineError.value = null
  liveTimelineItems.value = []
  try {
    const live = await liveApi.getByCourse(lessonCourseId)
    if (!live.sessionId) {
      liveTimelineError.value = '当前课程尚未生成课堂时间线'
      return
    }
    liveTimelineItems.value = await liveApi.timeline(live.sessionId)
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

async function loadUsersAndCards(items: CourseItemVO[]) {
  const ids = Array.from(
    new Set(
      items.flatMap((item) => [item.teacherUid, item.studentUid]).filter((uid) => uid > 0),
    ),
  )
  if (ids.length === 0) return
  try {
    const users = await userApi.batch(ids)
    userMap.value = Object.fromEntries(users.map((item) => [item.id, item]))
  } catch {
    userMap.value = {}
  }
  const cards = await Promise.all(
    ids.map(async (uid) => {
      try {
        const card = await userApi.card(uid)
        return [uid, card] as const
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
        const detail = await applicationApi.detail(item.applicationId)
        return [item.applicationId, detail] as const
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
    items.forEach((item) => {
      const otherUid = isTeacher.value ? item.studentUid : item.teacherUid
      const candidates = events
        .filter((event) => {
          const participantId = event.participant?.id
          if (participantId && participantId === otherUid) return true
          if (item.roomId && event.chatRoomId && event.chatRoomId === item.roomId) return true
          return false
        })
        .sort((a, b) => {
          const aScore = a.status === 'ACCEPTED' && a.endAt >= Date.now() ? 0 : a.status === 'PENDING' ? 1 : 2
          const bScore = b.status === 'ACCEPTED' && b.endAt >= Date.now() ? 0 : b.status === 'PENDING' ? 1 : 2
          if (aScore !== bScore) return aScore - bScore
          return a.startAt - b.startAt
        })
      nextMap[item.courseId] = candidates[0] || null
    })
    lessonMap.value = nextMap
  } catch {
    lessonMap.value = {}
  }
}

async function loadLive(items: CourseItemVO[]) {
  const liveEntries = await Promise.all(
    items.map(async (it) => {
      const lessonCourseId = lessonMap.value[it.courseId]?.id
      if (!lessonCourseId) return null
      try {
        const live = await liveApi.getByCourse(lessonCourseId)
        return [it.courseId, live] as const
      } catch {
        return null
      }
    }),
  )
  liveMap.value = Object.fromEntries(liveEntries.filter(Boolean) as Array<readonly [number, LiveSessionResp]>)
  try {
    const reminders = await liveApi.reminders()
    const mapped: Record<number, LiveReminderItemResp> = {}
    items.forEach((item) => {
      const lessonCourseId = lessonMap.value[item.courseId]?.id
      if (!lessonCourseId) return
      const reminder = reminders.find((entry) => entry.courseId === lessonCourseId)
      if (reminder) mapped[item.courseId] = reminder
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
    await Promise.all([loadUsersAndCards(rows), loadApplications(rows), loadLessons(rows)])
    await loadLive(rows)
    const firstRow = rows[0]
    if (!selectedCourseId.value && firstRow) {
      selectedCourseId.value = firstRow.courseId
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page-shell">
    <section class="hero card">
      <div class="hero-copy">
        <div class="eyebrow">长期课程工作台</div>
        <h1 class="hero-title">我的课程</h1>
        <p class="hero-desc">把长期课程关系、单节课预约、聊天沟通、课堂入口和提醒放在一个页面里处理。</p>
      </div>
      <div class="hero-actions">
        <button class="btn btn-primary" type="button" :disabled="!selectedCourse || !canOpenSchedule(selectedCourse)" @click="selectedCourse && openSchedule(selectedCourse)">
          发起约课
        </button>
        <button class="btn" type="button" :disabled="loading" @click="load">{{ loading ? '刷新中...' : '刷新列表' }}</button>
      </div>
    </section>

    <section v-if="reminderBanner" class="banner card">
      <div class="banner-main">
        <div class="banner-title">{{ reminderBanner.headline }}</div>
        <div class="banner-desc">
          <template v-if="reminderBanner.latestLesson">
            {{ reminderBanner.lessonStatusText }} · {{ reminderBanner.lessonTimeText }}
          </template>
          <template v-else>
            {{ reminderBanner.liveHint }}
          </template>
        </div>
      </div>
      <div class="banner-actions">
        <button v-if="reminderBanner.roomId" class="btn" type="button" @click="goChat(reminderBanner.roomId)">进入聊天</button>
        <button v-if="reminderBanner.live" class="btn btn-primary" type="button" @click="goLivePrepare(reminderBanner.lessonCourseId)">进入课堂</button>
      </div>
    </section>

    <section class="metrics">
      <div class="metric card">
        <div class="metric-value">{{ overview.total }}</div>
        <div class="metric-label">全部长期课程</div>
      </div>
      <div class="metric card">
        <div class="metric-value">{{ overview.active }}</div>
        <div class="metric-label">进行中关系</div>
      </div>
      <div class="metric card">
        <div class="metric-value">{{ overview.waitingPay }}</div>
        <div class="metric-label">待支付信息费</div>
      </div>
      <div class="metric card">
        <div class="metric-value">{{ overview.trialing }}</div>
        <div class="metric-label">试课阶段</div>
      </div>
      <div class="metric card">
        <div class="metric-value">{{ overview.upcoming }}</div>
        <div class="metric-label">已有具体课节</div>
      </div>
    </section>

    <div class="toolbar card">
      <div class="toolbar-left">
        <button class="chip" :class="{ active: stageFilter === 'ALL' }" type="button" @click="stageFilter = 'ALL'">全部</button>
        <button class="chip" :class="{ active: stageFilter === 'ACTIVE' }" type="button" @click="stageFilter = 'ACTIVE'">进行中</button>
        <button class="chip" :class="{ active: stageFilter === 'WAIT_PAY' }" type="button" @click="stageFilter = 'WAIT_PAY'">待支付</button>
        <button class="chip" :class="{ active: stageFilter === 'COMMUNICATING' }" type="button" @click="stageFilter = 'COMMUNICATING'">沟通中</button>
        <button class="chip" :class="{ active: stageFilter === 'TRIALING' }" type="button" @click="stageFilter = 'TRIALING'">试课中</button>
        <button class="chip" :class="{ active: stageFilter === 'TEACHING' }" type="button" @click="stageFilter = 'TEACHING'">正式上课中</button>
        <button class="chip" :class="{ active: stageFilter === 'FINISHED' }" type="button" @click="stageFilter = 'FINISHED'">已结课</button>
      </div>
      <div class="toolbar-right">
        <input v-model="search" class="search" type="search" placeholder="搜索老师、学生或课程状态" />
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <section v-if="!loading && filteredCourses.length === 0" class="empty card">
      <div class="empty-title">暂无符合条件的课程</div>
      <div class="empty-desc">长期课程关系建立后，会在这里展示状态进展、沟通入口和后续上课安排。</div>
    </section>

    <section v-else class="content-grid">
      <div class="course-column">
        <article
          v-for="item in filteredCourses"
          :key="item.courseId"
          class="course-card card"
          :class="{ active: selectedCourse?.courseId === item.courseId }"
          @click="selectCourse(item.courseId)"
        >
          <div class="course-top">
            <div class="course-person">
              <img v-if="item.avatar" class="avatar" :src="item.avatar" alt="" @error="markAvatarBroken(item.participantUid)" />
              <div v-else class="avatar fallback">{{ item.participantName.slice(0, 1) }}</div>
              <div class="person-copy">
                <div class="person-label">{{ item.participantRoleLabel }}</div>
                <div class="person-name">{{ item.participantName }}</div>
                <div class="person-subtitle">{{ item.participantSubtitle }}</div>
              </div>
            </div>
            <span class="status-pill" :class="statusToneClass(item.stage.tone)">{{ item.stage.label }}</span>
          </div>

          <div class="course-headline">{{ item.headline }}</div>
          <div class="course-summary">{{ item.summary }}</div>

          <div class="progress-row">
            <div class="progress-meta">{{ item.progressText }}</div>
            <div class="progress-dots">
              <span v-for="idx in progressSteps.length" :key="idx" class="progress-dot" :class="{ filled: idx <= item.progressStep }" />
            </div>
          </div>

          <div class="info-grid">
            <div class="info-block">
              <div class="info-label">长期状态</div>
              <div class="info-value">{{ item.stageHint }}</div>
            </div>
            <div class="info-block">
              <div class="info-label">最近课节</div>
              <div class="info-value">{{ item.lessonStatusText }}</div>
              <div class="info-sub">{{ item.lessonTimeText }}</div>
            </div>
          </div>

          <div v-if="item.trialCountdown" class="trial-hint">{{ item.trialCountdown }}</div>

          <div class="card-actions">
            <button class="btn" type="button" :disabled="!item.roomId" @click.stop="goChat(item.roomId)">进入聊天</button>
            <button class="btn" type="button" :disabled="!canOpenSchedule(item)" @click.stop="openSchedule(item)">约课</button>
            <button class="btn" type="button" :disabled="!item.live" @click.stop="goLivePrepare(item.lessonCourseId)">课堂</button>
            <button class="btn" type="button" @click.stop="goCourseDetail(item.courseId)">详情</button>
            <button class="btn" type="button" :disabled="actionBusyCourseId === item.courseId" @click.stop="onPrimaryAction(item)">
              {{ actionBusyCourseId === item.courseId ? '处理中...' : item.nextActionLabel }}
            </button>
          </div>
        </article>
      </div>

      <aside v-if="selectedCourse" class="detail-panel card">
        <div class="detail-head">
          <div>
            <div class="detail-eyebrow">课程详情</div>
            <div class="detail-title">{{ selectedCourse.headline }}</div>
            <div class="detail-summary">{{ selectedCourse.summary }}</div>
          </div>
          <span class="status-pill" :class="statusToneClass(selectedCourse.stage.tone)">{{ selectedCourse.stage.label }}</span>
        </div>

        <div class="detail-section">
          <div class="section-title">对方信息</div>
          <div class="participant-card">
            <img
              v-if="selectedCourse.avatar"
              class="participant-avatar"
              :src="selectedCourse.avatar"
              alt=""
              @error="markAvatarBroken(selectedCourse.participantUid)"
            />
            <div v-else class="participant-avatar fallback">{{ selectedCourse.participantName.slice(0, 1) }}</div>
            <div class="participant-copy">
              <div class="participant-name">{{ selectedCourse.participantName }}</div>
              <div class="participant-role">{{ selectedCourse.participantRoleLabel }}</div>
              <div class="participant-sub">{{ selectedCourse.participantSubtitle }}</div>
              <div class="participant-desc">
                {{
                  selectedCourse.participantCard?.teacherProfile?.introduction ||
                  selectedCourse.participantCard?.studentProfile?.demandDescription ||
                  '资料补充后可在这里展示老师简介或学生需求说明。'
                }}
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">长期课程进度</div>
          <div class="phase-line">
            <div v-for="(step, idx) in progressSteps" :key="step" class="phase-item">
              <div class="phase-index" :class="{ active: idx + 1 <= selectedCourse.progressStep }">{{ idx + 1 }}</div>
              <div class="phase-name">{{ step }}</div>
            </div>
          </div>
          <div class="phase-note">{{ selectedCourse.stage.description }}</div>
        </div>

        <div class="detail-section">
          <div class="section-title">单节课安排</div>
          <div class="lesson-card">
            <div class="lesson-top">
              <span class="status-pill sm" :class="statusToneClass(selectedCourse.lessonState.tone)">{{ selectedCourse.lessonStatusText }}</span>
              <span class="lesson-time">{{ selectedCourse.lessonTimeText }}</span>
            </div>
            <div class="lesson-desc">{{ selectedCourse.lessonSubtitle }}</div>
            <div v-if="selectedCourse.liveBadge" class="live-box">
              <div class="live-badge">{{ selectedCourse.liveBadge }}</div>
              <div class="live-hint">{{ selectedCourse.liveHint }}</div>
            </div>
            <div class="lesson-actions">
              <button class="btn btn-primary" type="button" :disabled="!canOpenSchedule(selectedCourse)" @click="openSchedule(selectedCourse)">
                {{ selectedCourse.stage.key === 'TRIALING' || selectedCourse.stage.key === 'TRIAL_CONFIRMING' ? '发起试课预约' : '发起约课' }}
              </button>
              <button class="btn" type="button" :disabled="!selectedCourse.roomId" @click="goChat(selectedCourse.roomId)">聊天联动</button>
              <button class="btn" type="button" :disabled="!selectedCourse.live" @click="goLivePrepare(selectedCourse.lessonCourseId)">进入课堂</button>
              <button class="btn" type="button" :disabled="!selectedCourse.live" @click="openLiveTimeline(selectedCourse.courseId)">课堂详情</button>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">近期安排</div>
          <div v-if="upcomingLessons.length === 0" class="mini-empty">暂无已确认课节，可先发起约课。</div>
          <div v-else class="agenda-list">
            <button
              v-for="item in upcomingLessons"
              :key="item.courseId"
              class="agenda-item"
              type="button"
              @click="selectCourse(item.courseId)"
            >
              <div class="agenda-time">
                {{
                  item.latestLesson
                    ? fmtDateTime(item.latestLesson.startAt)
                    : fmtDateTime(item.live?.scheduledStartAt || item.reminder?.scheduledStartAt || '')
                }}
              </div>
              <div class="agenda-title">{{ item.headline }}</div>
              <div class="agenda-sub">{{ item.lessonStatusText }} · {{ item.participantName }}</div>
            </button>
          </div>
        </div>

        <div class="detail-actions">
          <button class="btn" type="button" :disabled="!selectedCourse.roomId" @click="goChat(selectedCourse.roomId)">进入聊天</button>
          <button class="btn" type="button" @click="goCourseDetail(selectedCourse.courseId)">课程详情</button>
          <button class="btn" type="button" @click="goSchedule">查看日程表</button>
          <button
            v-if="canApplyTrialRefund(selectedCourse.raw)"
            class="btn btn-danger"
            type="button"
            @click="openTrialRefund(selectedCourse.courseId)"
          >
            试课不通过
          </button>
        </div>
      </aside>
    </section>

    <div v-if="scheduleOpen" class="mask" @click.self="closeSchedule">
      <div class="modal card schedule-modal">
        <div class="modal-header">
          <div>
            <div class="m-title">预约课程</div>
            <div class="m-desc">像飞书预约会议一样选择时间，发送后会在聊天中同步课程卡片，并等待对方确认。</div>
          </div>
        </div>
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
            <input
              class="ipt"
              type="datetime-local"
              :value="toLocalDateTimeInputValue(scheduleStartAt)"
              @change="applyScheduleStartInput(($event.target as HTMLInputElement).value)"
            />
          </div>
          <div class="field">
            <div class="lab">结束时间</div>
            <input
              class="ipt"
              type="datetime-local"
              :value="toLocalDateTimeInputValue(scheduleEndAt)"
              @change="applyScheduleEndInput(($event.target as HTMLInputElement).value)"
            />
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
          <div class="booking-row"><span>同步</span><span>发送到聊天窗口，并等待对方确认</span></div>
        </div>

        <div class="m-ops">
          <button class="btn" type="button" :disabled="scheduleBusy" @click="closeSchedule">取消</button>
          <button class="btn btn-primary" type="button" :disabled="scheduleBusy" @click="submitScheduleCreate">
            {{ scheduleBusy ? '发送中...' : '发送预约' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="modalOpen" class="mask" @click.self="closeTrialRefund">
      <div class="modal card">
        <div class="m-title">试课不通过</div>
        <div class="m-desc">线下试课不通过需提交微信聊天记录滚动并删除拉黑的录屏，管理员审核通过后退还 80% 信息费并删除录屏。</div>
        <div class="m-form">
          <textarea v-model="modalReason" class="txt" rows="4" placeholder="请填写试课不通过说明"></textarea>
          <input class="file" type="file" accept="image/*" multiple @change="onPickFiles" />
          <div class="m-hint">已选择 {{ modalFiles.length }} 张</div>
          <input v-model="modalVideoUrl" class="input" placeholder="微信录屏 URL（1 分钟内）" />
          <input v-model.number="modalVideoDurationSeconds" class="input" type="number" min="1" max="60" placeholder="录屏时长（秒）" />
        </div>
        <div v-if="modalErr" class="m-error">{{ modalErr }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="modalBusy" @click="closeTrialRefund">取消</button>
          <button class="btn btn-danger" type="button" :disabled="modalBusy" @click="submitTrialRefund">
            {{ modalBusy ? '提交中...' : '提交申请' }}
          </button>
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
  gap: 16px;
  padding: 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.18), transparent 32%),
    linear-gradient(140deg, #fffefc, #f6fbfb 55%, #eef7f7);
}

.eyebrow,
.detail-eyebrow,
.person-label,
.info-label,
.section-title,
.metric-label,
.lab,
.booking-title {
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

.hero-desc {
  margin: 10px 0 0;
  max-width: 640px;
  color: var(--muted);
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.banner {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 18px;
  border-color: rgba(0, 190, 189, 0.22);
  background: linear-gradient(120deg, rgba(0, 190, 189, 0.08), rgba(255, 255, 255, 0.95));
}

.banner-title {
  font-size: 18px;
  font-weight: 800;
}

.banner-desc {
  margin-top: 6px;
  color: var(--muted);
}

.banner-actions {
  display: flex;
  gap: 10px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  padding: 18px;
  border-radius: 18px;
}

.metric-value {
  font-size: 28px;
  font-weight: 800;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
}

.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  width: 280px;
  height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
  outline: none;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 16px;
  align-items: start;
}

.course-column {
  display: grid;
  gap: 12px;
}

.course-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.course-card:hover,
.course-card.active {
  transform: translateY(-1px);
  border-color: rgba(0, 190, 189, 0.26);
  box-shadow: 0 16px 32px rgba(0, 190, 189, 0.1);
}

.course-top,
.lesson-top,
.detail-head,
.progress-row,
.booking-row,
.m-ops,
.card-actions,
.detail-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.course-person,
.participant-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar,
.participant-avatar {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  object-fit: cover;
  background: linear-gradient(135deg, rgba(0, 190, 189, 0.18), rgba(31, 35, 41, 0.08));
}

.participant-avatar {
  width: 64px;
  height: 64px;
}

.fallback {
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #0d7e7d;
}

.person-name,
.participant-name {
  font-size: 16px;
  font-weight: 800;
}

.person-subtitle,
.course-summary,
.detail-summary,
.participant-sub,
.participant-desc,
.phase-note,
.lesson-desc,
.live-hint,
.m-desc,
.timeline-meta,
.timeline-empty,
.empty-desc,
.hint {
  color: var(--muted);
}

.course-headline,
.detail-title {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.18;
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
  height: 26px;
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

.progress-meta,
.info-sub,
.trial-hint,
.participant-role,
.agenda-sub,
.m-hint {
  font-size: 12px;
  color: rgba(31, 35, 41, 0.58);
}

.progress-dots {
  display: flex;
  gap: 6px;
}

.progress-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: rgba(31, 35, 41, 0.12);
}

.progress-dot.filled {
  background: var(--primary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-block {
  padding: 12px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.03);
}

.info-value {
  margin-top: 6px;
  font-weight: 700;
}

.trial-hint {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(124, 58, 237, 0.08);
}

.card-actions,
.lesson-actions,
.detail-actions,
.banner-actions {
  flex-wrap: wrap;
}

.detail-panel {
  position: sticky;
  top: calc(var(--app-topbar-height) + 18px);
  display: grid;
  gap: 18px;
  padding: 20px;
  border-radius: 22px;
}

.detail-section {
  display: grid;
  gap: 12px;
}

.participant-card,
.lesson-card,
.booking-summary {
  padding: 16px;
  border-radius: 18px;
  background: rgba(31, 35, 41, 0.035);
}

.phase-line {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.phase-item {
  display: grid;
  gap: 8px;
  justify-items: center;
  text-align: center;
}

.phase-index {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(31, 35, 41, 0.08);
  font-weight: 700;
}

.phase-index.active {
  background: var(--primary);
  color: #fff;
}

.phase-name,
.agenda-time,
.live-badge {
  font-weight: 700;
}

.live-box {
  margin-top: 12px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(0, 190, 189, 0.1);
}

.agenda-list {
  display: grid;
  gap: 10px;
}

.agenda-item {
  text-align: left;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
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

.m-title {
  font-size: 18px;
  font-weight: 800;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.field,
.m-form {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.ipt,
.txt {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.95);
  outline: none;
}

.ipt {
  height: 44px;
  padding: 0 14px;
}

.txt {
  padding: 12px 14px;
  resize: vertical;
  min-height: 110px;
}

.booking-summary {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.booking-row span:last-child {
  color: rgba(31, 35, 41, 0.82);
  font-weight: 700;
}

.file {
  width: 100%;
}

.m-error {
  margin-top: 12px;
  color: #be123c;
}

.timeline-list {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
  align-items: start;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
  margin-top: 5px;
}

.timeline-content {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(15, 118, 110, 0.06);
}

.timeline-title {
  font-weight: 700;
}

@media (max-width: 1100px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .detail-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .hero,
  .banner,
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions,
  .banner-actions,
  .toolbar-right {
    width: 100%;
  }

  .search {
    width: 100%;
  }

  .metrics,
  .field-grid,
  .info-grid,
  .phase-line {
    grid-template-columns: 1fr;
  }

  .course-top,
  .lesson-top,
  .detail-head,
  .progress-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .modal,
  .schedule-modal,
  .timeline-modal {
    width: 100%;
    padding: 16px;
  }
}
</style>
