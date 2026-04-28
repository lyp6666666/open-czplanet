<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { appointmentApi } from '@/api/appointment'
import { courseApi } from '@/api/course'
import { liveApi, type LiveSessionResp } from '@/api/live'
import { scheduleApi } from '@/api/schedule'
import type { CourseDetailVO, ScheduleEventVO, TutorApplicationVO, UserCardVO, UserSimpleVO } from '@/api/types'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import LessonDetailModal from '@/ui/course/LessonDetailModal.vue'
import LessonPreviewCard from '@/ui/course/LessonPreviewCard.vue'
import { normalizeAvatarUrl } from '@/utils/avatar'
import {
  buildLessonDetailModel,
  findPreviousLesson,
  findRecentLesson,
  formatLessonDateTime,
  groupLessonsForOverview,
  resolveLessonStatus,
} from '@/utils/lessonDetail'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const courseId = computed(() => Number(route.params.courseId))
const isTeacher = computed(() => auth.user?.userType === 1)

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

const detail = ref<CourseDetailVO | null>(null)
const emailHint = ref<{ show: boolean; title?: string; description?: string; actionText?: string } | null>(null)
const application = ref<TutorApplicationVO | null>(null)
const participant = ref<UserSimpleVO | null>(null)
const participantCard = ref<UserCardVO | null>(null)
const lessons = ref<ScheduleEventVO[]>([])
const liveSession = ref<LiveSessionResp | null>(null)
const avatarBroken = ref(false)
const lessonModalOpen = ref(false)
const lessonModalId = ref<number | null>(null)
const lessonPreview = ref<{ lessonId: number; top: number; left: number } | null>(null)

const scheduleOpen = ref(false)
const scheduleTitle = ref('')
const scheduleDescription = ref('')
const scheduleLessonPriceYuan = ref('')
const scheduleStartAt = ref<number>(roundToNextHalfHour(Date.now() + 2 * 60 * 60 * 1000))
const scheduleEndAt = ref<number>(scheduleStartAt.value + 60 * 60 * 1000)
const scheduleError = ref<string | null>(null)
const lessonActionError = ref<string | null>(null)
const actionLoadingByLessonId = ref<Record<number, string>>({})

const rescheduleOpen = ref(false)
const rescheduleLessonId = ref<number | null>(null)
const rescheduleTitle = ref('')
const rescheduleStartAt = ref<number>(roundToNextHalfHour(Date.now() + 24 * 60 * 60 * 1000))
const rescheduleEndAt = ref<number>(rescheduleStartAt.value + 60 * 60 * 1000)
const rescheduleRemark = ref('')
const rescheduleError = ref<string | null>(null)

const trialDecisionOpen = ref(false)
const trialDecisionResult = ref<'PASS' | 'FAIL'>('PASS')
const trialDecisionReason = ref('')
const weeklyScheduleOpen = ref(false)
const weeklyScheduleError = ref<string | null>(null)
const weeklyLessonPriceYuan = ref('')
const weeklyWeeks = ref(16)
const weeklyTitle = ref('正式每周课')
const weeklyDescription = ref('试课通过后确认的固定课表')
const weeklySlots = ref<Array<{ dayOfWeek: number; startMinute: number; endMinute: number }>>([
  { dayOfWeek: 2, startMinute: 19 * 60, endMinute: 21 * 60 },
])

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
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...opts,
  }).format(d)
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

function courseStatusText(status?: string | null) {
  const normalized = String(status || '').trim().toUpperCase()
  if (normalized === 'TRIAL_WAIT_STUDENT_DECISION') return '待学生确认是否继续'
  if (normalized === 'TRIAL_WAIT_WEEKLY_SCHEDULE') return '待提交正式课表'
  if (normalized === 'TRIAL_FAILED') return '试课失败'
  return normalized || '未知状态'
}

const participantUid = computed(() => {
  if (!detail.value) return null
  return isTeacher.value ? detail.value.studentUid : detail.value.teacherUid
})

const participantName = computed(() => {
  const realName = String(participant.value?.realName || '').trim()
  if (realName) return realName
  const name = String(participant.value?.name || '').trim()
  if (name) return name
  return participantUid.value ? `用户${participantUid.value}` : '对方'
})

const participantAvatar = computed(() => {
  if (avatarBroken.value) return ''
  return normalizeAvatarUrl(participant.value?.avatar)
})

const sortedLessons = computed(() =>
  lessons.value.slice().sort((a, b) => {
    const aTime = Number(a.startAt || 0)
    const bTime = Number(b.startAt || 0)
    return aTime - bTime
  }),
)

const currentUid = computed(() => auth.user?.id ?? null)
const latestLesson = computed(() => sortedLessons.value[sortedLessons.value.length - 1] || null)
const recentLesson = computed(() => findRecentLesson(sortedLessons.value, { live: liveSession.value, aiResultStatus: detail.value?.aiResultStatus || null }))
const previousLesson = computed(() => findPreviousLesson(sortedLessons.value, recentLesson.value?.id || null))
const lessonGroups = computed(() => groupLessonsForOverview(sortedLessons.value, { live: liveSession.value, aiResultStatus: detail.value?.aiResultStatus || null }))

const lessonStats = computed(() => ({
  total: lessons.value.length,
  pending: lessons.value.filter((item) => item.status === 'PENDING').length,
  accepted: lessons.value.filter((item) => item.status === 'ACCEPTED').length,
  canceled: lessons.value.filter((item) => item.status === 'CANCELED' || item.status === 'REJECTED').length,
}))

const completedLessonCount = computed(() =>
  lessonGroups.value.completed.filter((item) =>
    resolveLessonStatus(item, { live: latestLesson.value?.id === item.id ? liveSession.value : null, aiResultStatus: latestLesson.value?.id === item.id ? detail.value?.aiResultStatus || null : null }).key === 'COMPLETED').length,
)

const nextLessonText = computed(() => (recentLesson.value ? `${formatLessonDateTime(recentLesson.value.startAt)} 开始` : '待安排'))

const selectedLessonModel = computed(() => {
  const lesson = sortedLessons.value.find((item) => item.id === lessonModalId.value) || null
  return lesson ? lessonCardModel(lesson) : null
})

const hoveredLessonModel = computed(() => {
  const lesson = sortedLessons.value.find((item) => item.id === lessonPreview.value?.lessonId) || null
  return lesson ? lessonCardModel(lesson) : null
})

function goBack() {
  void router.push({ name: 'myCourses' })
}

function goChat() {
  if (!detail.value?.roomId) return
  void router.push({ name: 'chatRoom', params: { roomId: String(detail.value.roomId) } })
}

function goLessonAiSummary() {
  if (!detail.value?.courseId) return
  const query: Record<string, string> = {}
  if (detail.value.liveSessionId) query.sessionId = String(detail.value.liveSessionId)
  void router.push({ name: 'lessonAiSummary', params: { courseId: String(detail.value.courseId) }, query })
}

function goEmailSettings() {
  void router.push({ name: 'emailSettings' })
}

function isTrialLesson(index: number) {
  return index === 0
}

function getLessonActionBusyText(lessonId: number) {
  return actionLoadingByLessonId.value[lessonId] || ''
}

function isLessonActionBusy(lessonId: number) {
  return Boolean(getLessonActionBusyText(lessonId))
}

function setLessonActionBusy(lessonId: number, text: string | null) {
  const next = { ...actionLoadingByLessonId.value }
  if (text) next[lessonId] = text
  else delete next[lessonId]
  actionLoadingByLessonId.value = next
}

function replaceLesson(nextLesson: ScheduleEventVO) {
  lessons.value = lessons.value.map((item) => (item.id === nextLesson.id ? nextLesson : item))
}

function canCancelLesson(item: ScheduleEventVO) {
  return item.status === 'PENDING' || item.status === 'ACCEPTED' || item.status === 'RESCHEDULE_PENDING'
}

function canRescheduleLesson(item: ScheduleEventVO) {
  return item.status === 'ACCEPTED'
}

const canStudentSubmitTrialDecision = computed(() =>
  !isTeacher.value && detail.value?.status === 'TRIAL_WAIT_STUDENT_DECISION',
)

const canStudentSubmitWeeklySchedule = computed(() =>
  !isTeacher.value && detail.value?.status === 'TRIAL_WAIT_WEEKLY_SCHEDULE',
)

function canConfirmReschedule(item: ScheduleEventVO) {
  if (item.status !== 'RESCHEDULE_PENDING') return false
  if (!currentUid.value || !item.proposedBy) return false
  return currentUid.value !== item.proposedBy
}

function lessonMetaText(item: ScheduleEventVO, index: number) {
  const bits: string[] = []
  if (isTrialLesson(index)) bits.push('首节试课')
  if (item.status === 'RESCHEDULE_PENDING' && item.proposedStartAt && item.proposedEndAt) {
    bits.push(`待确认改期：${fmtDateTime(item.proposedStartAt)} - ${fmtDateTime(item.proposedEndAt, { hour: '2-digit', minute: '2-digit' })}`)
  }
  if (item.proposedBy && currentUid.value) {
    bits.push(item.proposedBy === currentUid.value ? '由你发起调课' : '等待你确认调课')
  }
  return bits.join(' · ')
}

function linkLiveSession(lesson: ScheduleEventVO) {
  return latestLesson.value?.id === lesson.id ? liveSession.value : null
}

function linkAiStatus(lesson: ScheduleEventVO) {
  return latestLesson.value?.id === lesson.id ? detail.value?.aiResultStatus || null : null
}

function lessonSummaryText(lesson: ScheduleEventVO) {
  return latestLesson.value?.id === lesson.id ? detail.value?.aiPreview || null : null
}

function lessonCardModel(lesson: ScheduleEventVO) {
  return buildLessonDetailModel(lesson, {
    live: linkLiveSession(lesson),
    aiResultStatus: linkAiStatus(lesson),
    afterClassSummary: lessonSummaryText(lesson),
  })
}

function openLessonModal(lesson: ScheduleEventVO) {
  lessonModalId.value = lesson.id
  lessonModalOpen.value = true
}

function closeLessonModal() {
  lessonModalOpen.value = false
  lessonModalId.value = null
}

function showLessonPreview(lesson: ScheduleEventVO, event: MouseEvent) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) return
  const rect = target.getBoundingClientRect()
  lessonPreview.value = {
    lessonId: lesson.id,
    top: rect.top + window.scrollY + rect.height / 2,
    left: rect.right + window.scrollX + 16,
  }
}

function hideLessonPreview() {
  lessonPreview.value = null
}

function lessonModalPrimaryLabel() {
  const model = selectedLessonModel.value
  if (!model) return null
  if (model.statusKey === 'READY_TO_START') return '去上课'
  if (model.statusKey === 'IN_PROGRESS') return '继续上课'
  if (model.statusKey === 'COMPLETED') return '查看课后总结'
  if (detail.value?.roomId) return '进入聊天'
  return null
}

function handleLessonModalPrimary() {
  const model = selectedLessonModel.value
  if (!model || !detail.value) return
  if (model.statusKey === 'READY_TO_START' || model.statusKey === 'IN_PROGRESS') {
    void router.push({ name: 'livePrepare', params: { courseId: String(detail.value.courseId) } })
    return
  }
  if (model.statusKey === 'COMPLETED') {
    goLessonAiSummary()
    return
  }
  goChat()
}

function openScheduleCreate() {
  if (!detail.value || !participantUid.value) return
  scheduleError.value = null
  lessonActionError.value = null
  scheduleTitle.value = detail.value.courseName?.trim() || `与${participantName.value}的线上课程`
  scheduleDescription.value = latestLesson.value ? '补充新增课节' : '第一节试课'
  scheduleLessonPriceYuan.value = ''
  scheduleStartAt.value = roundToNextHalfHour(Date.now() + 2 * 60 * 60 * 1000)
  scheduleEndAt.value = scheduleStartAt.value + 60 * 60 * 1000
  scheduleOpen.value = true
}

function parseLessonPriceFen() {
  const raw = scheduleLessonPriceYuan.value.trim()
  if (!raw) return undefined
  const n = Number(raw)
  if (!Number.isFinite(n) || n <= 0) return null
  return Math.round(n * 100)
}

function closeScheduleCreate() {
  if (saving.value) return
  scheduleOpen.value = false
}

function openReschedule(item: ScheduleEventVO) {
  lessonActionError.value = null
  rescheduleError.value = null
  rescheduleLessonId.value = item.id
  rescheduleTitle.value = item.title
  rescheduleStartAt.value = item.startAt
  rescheduleEndAt.value = item.endAt
  rescheduleRemark.value = item.description || '调课说明：'
  rescheduleOpen.value = true
}

function closeReschedule() {
  if (saving.value) return
  rescheduleOpen.value = false
  rescheduleLessonId.value = null
}

function minutesToTimeText(minutes: number) {
  const h = String(Math.floor(minutes / 60)).padStart(2, '0')
  const m = String(minutes % 60).padStart(2, '0')
  return `${h}:${m}`
}

function parseTimeInputMinutes(value: string) {
  const parts = value.split(':').map((part) => Number(part))
  const h = parts[0]
  const m = parts[1]
  if (!Number.isFinite(h) || !Number.isFinite(m)) return 0
  return Math.min(24 * 60, Math.max(0, Number(h) * 60 + Number(m)))
}

function applyWeeklySlotTime(index: number, key: 'startMinute' | 'endMinute', value: string) {
  const slot = weeklySlots.value[index]
  if (!slot) return
  const next = weeklySlots.value.slice()
  next[index] = { ...slot, [key]: parseTimeInputMinutes(value) }
  weeklySlots.value = next
}

function parseWeeklyPriceFen() {
  const raw = weeklyLessonPriceYuan.value.trim()
  if (!raw) return undefined
  const n = Number(raw)
  if (!Number.isFinite(n) || n <= 0) return null
  return Math.round(n * 100)
}

function addWeeklySlot() {
  weeklySlots.value = [...weeklySlots.value, { dayOfWeek: 4, startMinute: 19 * 60, endMinute: 21 * 60 }]
}

function removeWeeklySlot(index: number) {
  weeklySlots.value = weeklySlots.value.filter((_, i) => i !== index)
}

function openTrialDecision() {
  trialDecisionResult.value = 'PASS'
  trialDecisionReason.value = ''
  weeklyScheduleError.value = null
  trialDecisionOpen.value = true
}

function closeTrialDecision() {
  if (saving.value) return
  trialDecisionOpen.value = false
}

function openWeeklySchedule() {
  weeklyScheduleError.value = null
  weeklyScheduleOpen.value = true
}

function closeWeeklySchedule() {
  if (saving.value) return
  weeklyScheduleOpen.value = false
}

async function submitScheduleCreate() {
  if (!detail.value || !participantUid.value || saving.value) return
  const titleText = scheduleTitle.value.trim()
  if (!titleText) {
    scheduleError.value = '请输入课节名称'
    return
  }
  if (!(scheduleEndAt.value > scheduleStartAt.value)) {
    scheduleError.value = '结束时间必须晚于开始时间'
    return
  }
  const priceFen = parseLessonPriceFen()
  if (priceFen === null) {
    scheduleError.value = '请输入有效的单节标准课价'
    return
  }
  const lessonType = sortedLessons.value.length === 0 ? 'TRIAL' : 'NORMAL'
  saving.value = true
  scheduleError.value = null
  try {
    const created = await scheduleApi.createEvent({
      courseId: detail.value.courseId,
      lessonType,
      lessonPriceFen: priceFen,
      trialPricePercent: lessonType === 'TRIAL' ? 100 : undefined,
      title: titleText,
      participantUserId: participantUid.value,
      startAt: scheduleStartAt.value,
      endAt: scheduleEndAt.value,
      description: scheduleDescription.value.trim() || undefined,
    })
    lessons.value = [...lessons.value, created]
    scheduleOpen.value = false
    toast.show('课节已创建，已同步到聊天与我的课程。', 'success')
  } catch (e) {
    scheduleError.value = e instanceof Error ? e.message : '创建课节失败'
  } finally {
    saving.value = false
  }
}

async function completeLesson(item: ScheduleEventVO) {
  if (saving.value || item.status !== 'ACCEPTED') return
  const confirmed = window.confirm(`确认「${item.title}」已完成授课吗？平台不会生成课时费账单，请双方按约定私下结算。`)
  if (!confirmed) return
  saving.value = true
  lessonActionError.value = null
  setLessonActionBusy(item.id, '结课中...')
  try {
    await appointmentApi.complete(item.id)
    await load()
    toast.show('已结课。平台不代收课时费，请双方按约定私下结算。', 'success')
  } catch (e) {
    lessonActionError.value = e instanceof Error ? e.message : '结课失败'
  } finally {
    setLessonActionBusy(item.id, null)
    saving.value = false
  }
}

async function cancelLesson(item: ScheduleEventVO) {
  if (saving.value || !canCancelLesson(item)) return
  const confirmed = window.confirm(`确认取消课节「${item.title}」吗？取消后本节课将不能继续履约。`)
  if (!confirmed) return
  saving.value = true
  lessonActionError.value = null
  setLessonActionBusy(item.id, '取消中...')
  try {
    const canceled = await scheduleApi.cancel(item.id, '从课程详情页取消课节')
    replaceLesson(canceled)
    toast.show('课节已取消。', 'success')
  } catch (e) {
    lessonActionError.value = e instanceof Error ? e.message : '取消课节失败'
  } finally {
    setLessonActionBusy(item.id, null)
    saving.value = false
  }
}

function toLocalDateTimeIso(ms: number) {
  const d = new Date(ms)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

function applyRescheduleStartInput(value: string) {
  const nextStart = parseLocalDateTimeInputValue(value)
  const previousDuration = Math.max(30 * 60 * 1000, rescheduleEndAt.value - rescheduleStartAt.value)
  rescheduleStartAt.value = nextStart
  if (!(rescheduleEndAt.value > rescheduleStartAt.value)) {
    rescheduleEndAt.value = nextStart + previousDuration
  }
}

function applyRescheduleEndInput(value: string) {
  rescheduleEndAt.value = parseLocalDateTimeInputValue(value)
}

async function submitReschedule() {
  const lessonId = rescheduleLessonId.value
  if (!lessonId || saving.value) return
  if (!(rescheduleEndAt.value > rescheduleStartAt.value)) {
    rescheduleError.value = '改期后的结束时间必须晚于开始时间'
    return
  }
  saving.value = true
  rescheduleError.value = null
  lessonActionError.value = null
  setLessonActionBusy(lessonId, '发起调课中...')
  try {
    await appointmentApi.reschedule(lessonId, {
      proposedStartTime: toLocalDateTimeIso(rescheduleStartAt.value),
      durationMinutes: Math.max(15, Math.round((rescheduleEndAt.value - rescheduleStartAt.value) / 60000)),
      remark: rescheduleRemark.value.trim() || undefined,
    })
    await load()
    closeReschedule()
    toast.show('调课申请已发出，等待对方确认。', 'success')
  } catch (e) {
    rescheduleError.value = e instanceof Error ? e.message : '发起调课失败'
  } finally {
    setLessonActionBusy(lessonId, null)
    saving.value = false
  }
}

async function submitTrialDecision() {
  if (!detail.value || saving.value) return
  if (trialDecisionResult.value === 'FAIL' && !trialDecisionReason.value.trim()) {
    lessonActionError.value = '请选择不通过时请填写原因'
    return
  }
  saving.value = true
  lessonActionError.value = null
  try {
    await courseApi.submitTrialResult(detail.value.courseId, {
      result: trialDecisionResult.value,
      reason: trialDecisionReason.value.trim() || undefined,
    })
    trialDecisionOpen.value = false
    if (trialDecisionResult.value === 'PASS') {
      await load()
      weeklyScheduleOpen.value = true
      toast.show('试课结果已提交，请继续选择后续正式上课时间。', 'success')
    } else {
      await load()
      toast.show('已提交试课不继续结果。', 'success')
    }
  } catch (e) {
    lessonActionError.value = e instanceof Error ? e.message : '提交试课结果失败'
  } finally {
    saving.value = false
  }
}

async function submitWeeklySchedule() {
  if (!detail.value || !participantUid.value || saving.value) return
  if (!weeklySlots.value.length) {
    weeklyScheduleError.value = '请至少选择一个固定课时'
    return
  }
  const lessonPriceFen = parseWeeklyPriceFen()
  if (lessonPriceFen === null) {
    weeklyScheduleError.value = '请输入有效课时费，或留空后在线下自行约定'
    return
  }
  saving.value = true
  weeklyScheduleError.value = null
  lessonActionError.value = null
  try {
    const created = await scheduleApi.submitWeeklySchedule(detail.value.courseId, {
      participantUserId: participantUid.value,
      roomId: detail.value.roomId || undefined,
      title: weeklyTitle.value.trim() || undefined,
      description: weeklyDescription.value.trim() || undefined,
      lessonPriceFen,
      weeks: weeklyWeeks.value,
      slots: weeklySlots.value,
    })
    lessons.value = [...lessons.value, ...created]
    weeklyScheduleOpen.value = false
    await load()
    toast.show('正式上课时间已确认。平台不代收课时费，请双方按约定私下结算。', 'success')
  } catch (e) {
    weeklyScheduleError.value = e instanceof Error ? e.message : '提交正式课表失败'
  } finally {
    saving.value = false
  }
}

async function confirmReschedule(item: ScheduleEventVO) {
  if (saving.value || !canConfirmReschedule(item)) return
  const confirmed = window.confirm(`确认将「${item.title}」调整到新的时间吗？确认后会覆盖原上课时间。`)
  if (!confirmed) return
  saving.value = true
  lessonActionError.value = null
  setLessonActionBusy(item.id, '确认中...')
  try {
    await appointmentApi.confirmReschedule(item.id)
    await load()
    toast.show('调课已确认。', 'success')
  } catch (e) {
    lessonActionError.value = e instanceof Error ? e.message : '确认调课失败'
  } finally {
    setLessonActionBusy(item.id, null)
    saving.value = false
  }
}

async function load() {
  if (!(courseId.value > 0)) {
    error.value = '课程不存在'
    return
  }
  loading.value = true
  error.value = null
  lessonActionError.value = null
  try {
    try {
      emailHint.value = await userApi.emailReminderHint('COURSE_DETAIL')
    } catch {
      emailHint.value = null
    }
    const current = await courseApi.detail(courseId.value)
    detail.value = current
    const [courseLessons, appDetail] = await Promise.all([
      scheduleApi.listCourseEvents(current.courseId),
      applicationApi.detail(current.applicationId),
    ])
    lessons.value = courseLessons
    application.value = appDetail
    const uid = isTeacher.value ? current.studentUid : current.teacherUid
    const [users, card] = await Promise.all([userApi.batch([uid]), userApi.card(uid)])
    participant.value = users[0] || null
    participantCard.value = card

    const latestLesson = courseLessons
      .filter((item) => Number(item?.id || 0) > 0)
      .slice()
      .sort((a, b) => {
        const aEnd = Number(a.endAt || 0)
        const bEnd = Number(b.endAt || 0)
        if (aEnd !== bEnd) return bEnd - aEnd
        return Number(b.startAt || 0) - Number(a.startAt || 0)
      })[0]

    if (latestLesson?.id) {
      try {
        const live = await liveApi.getByCourse(current.courseId)
        liveSession.value = live
        detail.value = {
          ...detail.value,
          liveSessionId: live.sessionId,
        }
        try {
          const aiResult = await liveApi.aiResult(live.sessionId)
          detail.value = {
            ...detail.value,
            liveSessionId: live.sessionId,
            aiResultStatus: aiResult.resultStatus,
            aiPreview: aiResult.preview,
          }
        } catch {
          detail.value = {
            ...detail.value,
            liveSessionId: live.sessionId,
            aiResultStatus: detail.value?.aiResultStatus || 'PENDING',
          }
        }
      } catch {
        liveSession.value = null
        // 课程未生成课堂会话时，页面继续按基础课程信息展示。
      }
    } else {
      liveSession.value = null
    }
    if (
      route.query.weeklySchedule === '1'
      && !isTeacher.value
      && String(detail.value?.status || '').trim().toUpperCase() === 'TRIAL_WAIT_WEEKLY_SCHEDULE'
    ) {
      openWeeklySchedule()
      void router.replace({ name: 'courseDetail', params: { courseId: String(courseId.value) } })
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课程详情失败'
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
      <div>
        <div class="eyebrow">合作课程总览</div>
        <h1 class="hero-title">{{ detail?.courseName || '长期课程' }}</h1>
        <p class="hero-desc">
          {{ detail?.classTime || '还未配置固定时间' }}
          <template v-if="detail?.frequencyPerWeek"> · 每周 {{ detail.frequencyPerWeek }} 节</template>
          <template v-if="detail?.lessonPrice"> · {{ detail.lessonPrice }}</template>
        </p>
      </div>
      <div class="hero-actions">
        <button class="btn" type="button" @click="goBack">返回我的课程</button>
        <button class="btn" type="button" :disabled="!detail?.roomId" @click="goChat">进入聊天</button>
        <button class="btn btn-primary" type="button" :disabled="!detail || !participantUid" @click="openScheduleCreate">新增课节</button>
        <button v-if="canStudentSubmitTrialDecision" class="btn btn-primary" type="button" @click="openTrialDecision">提交试课结果</button>
        <button v-if="canStudentSubmitWeeklySchedule" class="btn btn-primary" type="button" @click="openWeeklySchedule">确认正式课表</button>
      </div>
    </section>

    <div v-if="loading" class="card hint">加载中...</div>
    <div v-else-if="error" class="card hint error">{{ error }}</div>
    <template v-else-if="detail">
      <section v-if="emailHint?.show" class="card email-hint-banner">
        <div>
          <div class="email-hint-title">{{ emailHint.title || '绑定邮箱，重要课程提醒将更稳妥送达' }}</div>
          <div class="email-hint-desc">
            {{ emailHint.description || '绑定主邮箱后，可接收开课提醒和课后总结；学生还可以额外设置家长邮箱，仅接收课后总结。' }}
          </div>
        </div>
        <button class="btn btn-primary" type="button" @click="goEmailSettings">{{ emailHint.actionText || '立即绑定' }}</button>
      </section>

      <section class="summary-grid">
        <div class="card metric">
          <div class="metric-label">合作状态</div>
          <div class="metric-value">{{ courseStatusText(detail.status) }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">授课形式</div>
          <div class="metric-value">{{ detail.teachingMode === 'ONLINE' ? '线上' : detail.teachingMode === 'OFFLINE' ? '线下' : '待确认' }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">全部课节</div>
          <div class="metric-value">{{ lessonStats.total }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">最近一节课</div>
          <div class="metric-value metric-small">{{ nextLessonText }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">已完课</div>
          <div class="metric-value">{{ completedLessonCount }}</div>
        </div>
      </section>

      <section class="focus-grid">
        <article class="card focus-card">
          <div class="focus-head">
            <div>
              <div class="section-title">最近的一节课</div>
              <div class="section-desc">当前最值得关注的一节课，点击可打开单节课详情弹窗。</div>
            </div>
          </div>
          <button
            v-if="recentLesson"
            class="focus-lesson"
            type="button"
            @click="openLessonModal(recentLesson)"
          >
            <div>
              <div class="focus-lesson-title">{{ lessonCardModel(recentLesson).title }}</div>
              <div class="focus-lesson-meta">{{ lessonCardModel(recentLesson).timeRangeText }}</div>
              <div class="focus-lesson-copy">{{ lessonCardModel(recentLesson).topic }}</div>
            </div>
            <span class="status-pill" :class="`tone-${lessonCardModel(recentLesson).statusTone}`">{{ lessonCardModel(recentLesson).statusLabel }}</span>
          </button>
          <div v-else class="mini-empty">暂时还没有排到最近的一节课。</div>
        </article>

        <article class="card focus-card">
          <div class="focus-head">
            <div>
              <div class="section-title">上一节课</div>
              <div class="section-desc">如果已经上过课，这里保留最近一次的回顾入口。</div>
            </div>
          </div>
          <button
            v-if="previousLesson"
            class="focus-lesson"
            type="button"
            @click="openLessonModal(previousLesson)"
          >
            <div>
              <div class="focus-lesson-title">{{ lessonCardModel(previousLesson).title }}</div>
              <div class="focus-lesson-meta">{{ lessonCardModel(previousLesson).timeRangeText }}</div>
              <div class="focus-lesson-copy">{{ lessonCardModel(previousLesson).summaryText }}</div>
            </div>
            <span class="status-pill" :class="`tone-${lessonCardModel(previousLesson).statusTone}`">{{ lessonCardModel(previousLesson).statusLabel }}</span>
          </button>
          <div v-else class="mini-empty">还没有上一节课记录，等第一节课结束后会展示在这里。</div>
        </article>
      </section>

      <section class="content-grid">
        <aside class="card side-card">
          <div class="section-title">课程关系</div>
          <div class="participant-card">
            <img v-if="participantAvatar" class="avatar" :src="participantAvatar" alt="" @error="avatarBroken = true" />
            <div v-else class="avatar fallback">{{ participantName.slice(0, 1) }}</div>
            <div class="participant-copy">
              <div class="participant-name">{{ participantName }}</div>
              <div class="participant-sub">
                {{
                  participantCard?.teacherProfile?.subject ||
                  participantCard?.jobPosting?.subjectName ||
                  participantCard?.studentProfile?.demandDescription ||
                  '资料待完善'
                }}
              </div>
            </div>
          </div>
          <div class="kv-list">
            <div class="kv-row">
              <span>申请单</span>
              <strong>#{{ detail.applicationId }}</strong>
            </div>
            <div class="kv-row">
              <span>试课周期</span>
              <strong>{{ detail.trialStartAt ? `${fmtDateTime(detail.trialStartAt)} - ${fmtDateTime(detail.trialEndAt)}` : '待开始' }}</strong>
            </div>
            <div class="kv-row">
              <span>沟通会话</span>
              <strong>{{ detail.roomId ? `#${detail.roomId}` : '未绑定' }}</strong>
            </div>
          </div>
        </aside>

        <section class="card lessons-card">
          <div class="section-head">
            <div>
              <div class="section-title">全部课节</div>
              <div class="section-desc">按“正在上 / 最近待上 / 已上过 / 后续规划”组织这段合作里的所有课节。鼠标移上去可以先看预览，点击再打开完整单节课详情弹窗。</div>
            </div>
            <button class="btn btn-primary" type="button" :disabled="!participantUid" @click="openScheduleCreate">新增课节</button>
          </div>
          <div v-if="lessonActionError" class="hint error">{{ lessonActionError }}</div>

          <div v-if="sortedLessons.length === 0" class="mini-empty">这门课还没有创建任何课节，先创建第一节试课或正式课吧。</div>
          <div v-else class="lesson-board">
            <section v-if="lessonGroups.ongoing.length" class="lesson-group">
              <div class="lesson-group-title">正在上的课</div>
              <div class="lesson-list">
                <article
                  v-for="item in lessonGroups.ongoing"
                  :key="item.id"
                  class="lesson-item"
                  @mouseenter="showLessonPreview(item, $event)"
                  @mouseleave="hideLessonPreview"
                  @click="openLessonModal(item)"
                >
                  <div class="lesson-main">
                    <div class="lesson-title-row">
                      <div class="lesson-title">{{ item.title }}</div>
                      <span v-if="item.lessonType === 'TRIAL'" class="trial-pill">试课</span>
                    </div>
                    <div class="lesson-time">{{ lessonCardModel(item).timeRangeText }}</div>
                    <div class="lesson-sub">{{ lessonCardModel(item).topic }}</div>
                    <div class="lesson-inline-actions">
                      <button class="btn" type="button" :disabled="isLessonActionBusy(item.id) || !canRescheduleLesson(item)" @click.stop="openReschedule(item)">
                        {{ getLessonActionBusyText(item.id) || '调课' }}
                      </button>
                      <button class="btn" type="button" :disabled="isLessonActionBusy(item.id) || !canConfirmReschedule(item)" @click.stop="confirmReschedule(item)">
                        {{ getLessonActionBusyText(item.id) || '确认改期' }}
                      </button>
                    </div>
                  </div>
                  <div class="lesson-side compact">
                    <span class="status-pill" :class="`tone-${lessonCardModel(item).statusTone}`">{{ lessonCardModel(item).statusLabel }}</span>
                  </div>
                </article>
              </div>
            </section>

            <section v-if="lessonGroups.nextUp.length || lessonGroups.planned.length" class="lesson-group">
              <div class="lesson-group-title">最近待上的一节课与后续规划</div>
              <div class="lesson-list">
                <article
                  v-for="item in [...lessonGroups.nextUp, ...lessonGroups.planned]"
                  :key="item.id"
                  class="lesson-item"
                  @mouseenter="showLessonPreview(item, $event)"
                  @mouseleave="hideLessonPreview"
                  @click="openLessonModal(item)"
                >
                  <div class="lesson-main">
                    <div class="lesson-title-row">
                      <div class="lesson-title">{{ item.title }}</div>
                      <span v-if="item.lessonType === 'TRIAL'" class="trial-pill">试课</span>
                    </div>
                    <div class="lesson-time">{{ lessonCardModel(item).timeRangeText }}</div>
                    <div class="lesson-sub">{{ lessonCardModel(item).topic }}</div>
                    <div v-if="lessonMetaText(item, sortedLessons.findIndex((row) => row.id === item.id))" class="lesson-meta">{{ lessonMetaText(item, sortedLessons.findIndex((row) => row.id === item.id)) }}</div>
                    <div class="lesson-inline-actions">
                      <button class="btn" type="button" :disabled="isLessonActionBusy(item.id) || !canRescheduleLesson(item)" @click.stop="openReschedule(item)">
                        {{ getLessonActionBusyText(item.id) || '调课' }}
                      </button>
                      <button class="btn" type="button" :disabled="isLessonActionBusy(item.id) || !canConfirmReschedule(item)" @click.stop="confirmReschedule(item)">
                        {{ getLessonActionBusyText(item.id) || '确认改期' }}
                      </button>
                      <button class="btn btn-danger" type="button" :disabled="isLessonActionBusy(item.id) || !canCancelLesson(item)" @click.stop="cancelLesson(item)">
                        {{ getLessonActionBusyText(item.id) || '删课' }}
                      </button>
                    </div>
                  </div>
                  <div class="lesson-side compact">
                    <span class="status-pill" :class="`tone-${lessonCardModel(item).statusTone}`">{{ lessonCardModel(item).statusLabel }}</span>
                  </div>
                </article>
              </div>
            </section>

            <section v-if="lessonGroups.completed.length" class="lesson-group">
              <div class="lesson-group-title">已上过的课</div>
              <div class="lesson-list">
                <article
                  v-for="item in lessonGroups.completed"
                  :key="item.id"
                  class="lesson-item"
                  @mouseenter="showLessonPreview(item, $event)"
                  @mouseleave="hideLessonPreview"
                  @click="openLessonModal(item)"
                >
                  <div class="lesson-main">
                    <div class="lesson-title-row">
                      <div class="lesson-title">{{ item.title }}</div>
                      <span v-if="item.lessonType === 'TRIAL'" class="trial-pill">试课</span>
                    </div>
                    <div class="lesson-time">{{ lessonCardModel(item).timeRangeText }}</div>
                    <div class="lesson-sub">{{ lessonCardModel(item).summaryText }}</div>
                    <div class="lesson-inline-actions">
                      <button class="btn" type="button" :disabled="isLessonActionBusy(item.id) || !canRescheduleLesson(item)" @click.stop="openReschedule(item)">
                        {{ getLessonActionBusyText(item.id) || '调课' }}
                      </button>
                      <button class="btn" type="button" :disabled="isLessonActionBusy(item.id) || !canConfirmReschedule(item)" @click.stop="confirmReschedule(item)">
                        {{ getLessonActionBusyText(item.id) || '确认改期' }}
                      </button>
                      <button v-if="isTeacher" class="btn" type="button" :disabled="isLessonActionBusy(item.id) || item.status !== 'ACCEPTED'" @click.stop="completeLesson(item)">
                        {{ getLessonActionBusyText(item.id) || '结课' }}
                      </button>
                      <button class="btn btn-danger" type="button" :disabled="isLessonActionBusy(item.id) || !canCancelLesson(item)" @click.stop="cancelLesson(item)">
                        {{ getLessonActionBusyText(item.id) || '删课' }}
                      </button>
                    </div>
                  </div>
                  <div class="lesson-side compact">
                    <span class="status-pill" :class="`tone-${lessonCardModel(item).statusTone}`">{{ lessonCardModel(item).statusLabel }}</span>
                  </div>
                </article>
              </div>
            </section>
          </div>
        </section>
      </section>

      <section class="card lessons-card">
        <div class="section-head">
          <div>
            <div class="section-title">课堂 AI 结果</div>
            <div class="section-desc">课程结束后，AI 会整理课后总结与报告草稿，帮助老师和家长快速回顾本节课。</div>
          </div>
          <button class="btn btn-primary" type="button" @click="goLessonAiSummary">查看课后总结</button>
        </div>
        <div class="lesson-sub">{{ detail.aiPreview || '当前暂无已生成的课后总结预览。' }}</div>
      </section>
    </template>

    <div v-if="scheduleOpen" class="mask" @click.self="closeScheduleCreate">
      <div class="modal card">
        <div class="modal-title">新增课节</div>
        <div class="modal-desc">为这门合作补充一节课。平台暂不代收课时费；若是首节试课，费用统一按 1 小时课时费由双方私下转账。</div>
        <div v-if="scheduleError" class="hint error">{{ scheduleError }}</div>
        <div class="field">
          <div class="label">课节名称</div>
          <input v-model="scheduleTitle" class="input" placeholder="例如：第 2 节｜函数强化" />
        </div>
        <div class="time-grid">
          <div class="field">
            <div class="label">开始时间</div>
            <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(scheduleStartAt)" @change="applyScheduleStartInput(($event.target as HTMLInputElement).value)" />
          </div>
          <div class="field">
            <div class="label">结束时间</div>
            <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(scheduleEndAt)" @change="applyScheduleEndInput(($event.target as HTMLInputElement).value)" />
          </div>
        </div>
        <div class="field">
          <div class="label">课节备注</div>
          <textarea v-model="scheduleDescription" class="textarea" rows="4" placeholder="填写本节课目标、教材、作业或调课说明" />
        </div>
        <div class="time-grid">
          <div class="field">
            <div class="label">参考课时费（元，可选）</div>
            <input v-model="scheduleLessonPriceYuan" class="input" inputmode="decimal" placeholder="仅用于双方参考，平台不收取" />
            <div class="field-hint">试课费用固定按 1 小时课时费计算；正式课费用也由双方私下结算。</div>
          </div>
          <div class="settlement-note">实时视频课堂和 AI 课后总结暂免费提供，本页面只记录课节安排，不生成平台课时费账单。</div>
        </div>
        <div class="modal-actions">
          <button class="btn" type="button" :disabled="saving" @click="closeScheduleCreate">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitScheduleCreate">{{ saving ? '提交中...' : '创建课节' }}</button>
        </div>
      </div>
    </div>

    <div v-if="rescheduleOpen" class="mask" @click.self="closeReschedule">
      <div class="modal card">
        <div class="modal-title">发起调课</div>
        <div class="modal-desc">调课后需要对方确认，确认前本节课会显示为“待确认调课”。</div>
        <div v-if="rescheduleError" class="hint error">{{ rescheduleError }}</div>
        <div class="field">
          <div class="label">课节名称</div>
          <input v-model="rescheduleTitle" class="input" disabled />
        </div>
        <div class="time-grid">
          <div class="field">
            <div class="label">改后开始时间</div>
            <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(rescheduleStartAt)" @change="applyRescheduleStartInput(($event.target as HTMLInputElement).value)" />
          </div>
          <div class="field">
            <div class="label">改后结束时间</div>
            <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(rescheduleEndAt)" @change="applyRescheduleEndInput(($event.target as HTMLInputElement).value)" />
          </div>
        </div>
        <div class="field">
          <div class="label">调课说明</div>
          <textarea v-model="rescheduleRemark" class="textarea" rows="4" placeholder="例如：学校活动冲突，希望顺延到周四晚上" />
        </div>
        <div class="modal-actions">
          <button class="btn" type="button" :disabled="saving" @click="closeReschedule">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitReschedule">{{ saving ? '提交中...' : '发起调课' }}</button>
        </div>
      </div>
    </div>

    <div v-if="trialDecisionOpen" class="mask" @click.self="closeTrialDecision">
      <div class="modal card">
        <div class="modal-title">提交试课结果</div>
        <div class="modal-desc">试课结束后，是否继续只能由学生确认。无论是否继续，试课费用均按 1 小时课时费由双方私下结算。</div>
        <div class="field">
          <div class="label">结果</div>
          <select v-model="trialDecisionResult" class="input">
            <option value="PASS">通过，继续上课</option>
            <option value="FAIL">不通过，结束合作</option>
          </select>
        </div>
        <div class="field">
          <div class="label">说明</div>
          <textarea v-model="trialDecisionReason" class="textarea" rows="4" placeholder="可填写试课反馈、继续原因或不通过原因" />
        </div>
        <div class="modal-actions">
          <button class="btn" type="button" :disabled="saving" @click="closeTrialDecision">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitTrialDecision">{{ saving ? '提交中...' : '提交结果' }}</button>
        </div>
      </div>
    </div>

    <div v-if="weeklyScheduleOpen" class="mask" @click.self="closeWeeklySchedule">
      <div class="modal card">
        <div class="modal-title">确认正式每周课表</div>
        <div class="modal-desc">请选择后续正式上课时间。平台暂不代收正式课费用，实时视频课堂和 AI 课后总结暂免费提供。</div>
        <div v-if="weeklyScheduleError" class="hint error">{{ weeklyScheduleError }}</div>
        <div class="field">
          <div class="label">课表标题</div>
          <input v-model="weeklyTitle" class="input" placeholder="例如：正式每周课" />
        </div>
        <div class="time-grid">
          <div class="field">
            <div class="label">参考课时费（元，可选）</div>
            <input v-model="weeklyLessonPriceYuan" class="input" inputmode="decimal" placeholder="仅用于记录，费用私下结算" />
          </div>
          <div class="field">
            <div class="label">生成周数</div>
            <input v-model.number="weeklyWeeks" class="input" type="number" min="1" max="16" />
          </div>
        </div>
        <div class="field">
          <div class="label">课表说明</div>
          <textarea v-model="weeklyDescription" class="textarea" rows="3" placeholder="例如：暂定周二/周四晚固定上课" />
        </div>
        <div class="field">
          <div class="label">固定时段</div>
          <div class="weekly-slot-list">
            <div v-for="(slot, index) in weeklySlots" :key="index" class="weekly-slot-row">
              <select v-model.number="slot.dayOfWeek" class="input">
                <option :value="1">周一</option>
                <option :value="2">周二</option>
                <option :value="3">周三</option>
                <option :value="4">周四</option>
                <option :value="5">周五</option>
                <option :value="6">周六</option>
                <option :value="7">周日</option>
              </select>
              <input class="input" type="text" inputmode="numeric" placeholder="19:00" :value="minutesToTimeText(slot.startMinute)" @change="applyWeeklySlotTime(index, 'startMinute', ($event.target as HTMLInputElement).value)" />
              <input class="input" type="text" inputmode="numeric" placeholder="21:00" :value="minutesToTimeText(slot.endMinute)" @change="applyWeeklySlotTime(index, 'endMinute', ($event.target as HTMLInputElement).value)" />
              <button class="btn" type="button" @click="removeWeeklySlot(index)">删除</button>
            </div>
          </div>
          <div class="field-hint">当前时段：{{ weeklySlots.map((slot) => `${slot.dayOfWeek}-${minutesToTimeText(slot.startMinute)}~${minutesToTimeText(slot.endMinute)}`).join('；') }}</div>
          <div class="field-hint">提交后会生成正式课节，费用由双方按约定私下结算，平台不生成课时费支付单。</div>
          <button class="btn" type="button" @click="addWeeklySlot">新增时段</button>
        </div>
        <div class="modal-actions">
          <button class="btn" type="button" :disabled="saving" @click="closeWeeklySchedule">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitWeeklySchedule">{{ saving ? '提交中...' : '提交正式课表' }}</button>
        </div>
      </div>
    </div>

    <div
      v-if="lessonPreview && hoveredLessonModel"
      class="lesson-hover-preview"
      :style="{ top: `${lessonPreview.top}px`, left: `${lessonPreview.left}px` }"
    >
      <LessonPreviewCard :model="hoveredLessonModel" />
    </div>

    <LessonDetailModal
      :open="lessonModalOpen"
      :model="selectedLessonModel"
      :cooperation-name="detail?.courseName || '单节课详情'"
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
  gap: 16px;
  padding: 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.18), transparent 34%),
    linear-gradient(135deg, #fffefb, #f5fbfb 58%, #edf8f7);
}

.eyebrow,
.metric-label,
.section-title,
.label {
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
.section-desc,
.participant-sub,
.lesson-sub,
.modal-desc,
.hint {
  color: var(--muted);
}

.hero-actions,
.summary-grid,
.focus-grid,
.content-grid,
.time-grid,
.modal-actions,
.section-head,
.kv-row,
.participant-card,
.lesson-item,
.lesson-actions {
  display: flex;
  gap: 12px;
}

.hero-actions,
.section-head,
.kv-row,
.lesson-item,
.modal-actions {
  justify-content: space-between;
  align-items: center;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.email-hint-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(0, 181, 120, 0.16);
  background: linear-gradient(135deg, rgba(240, 251, 244, 0.96), rgba(255, 248, 233, 0.96));
}

.email-hint-title {
  font-size: 16px;
  font-weight: 800;
  color: #1d4f3a;
}

.email-hint-desc {
  margin-top: 6px;
  color: #5e6f66;
  line-height: 1.6;
}

.metric {
  padding: 18px;
  border-radius: 18px;
}

.metric-value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 800;
}

.metric-small {
  font-size: 18px;
  line-height: 1.35;
}

.focus-card {
  display: grid;
  gap: 14px;
  padding: 20px;
  border-radius: 22px;
}

.focus-head {
  display: flex;
  justify-content: space-between;
}

.focus-lesson {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.86);
  cursor: pointer;
  text-align: left;
}

.focus-lesson-title {
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}

.focus-lesson-meta,
.focus-lesson-copy {
  margin-top: 6px;
  color: rgba(15, 23, 42, 0.72);
  line-height: 1.55;
}

.content-grid {
  align-items: start;
}

.side-card {
  width: 320px;
  min-width: 320px;
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 22px;
}

.lessons-card {
  flex: 1 1 auto;
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 22px;
}

.participant-card {
  align-items: center;
}

.avatar {
  width: 62px;
  height: 62px;
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

.participant-name,
.lesson-title {
  font-weight: 800;
}

.lesson-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trial-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.16);
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
}

.kv-list {
  display: grid;
  gap: 10px;
}

.lesson-list {
  display: grid;
  gap: 12px;
}

.lesson-board,
.lesson-group {
  display: grid;
  gap: 14px;
}

.lesson-group-title {
  font-size: 14px;
  font-weight: 800;
  color: #111827;
}

.lesson-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(31, 35, 41, 0.035);
  align-items: flex-start;
}

.lesson-main {
  display: grid;
  gap: 6px;
  flex: 1 1 auto;
}

.lesson-inline-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.lesson-time,
.lesson-duration {
  font-size: 13px;
  color: rgba(31, 35, 41, 0.68);
}

.lesson-meta {
  font-size: 12px;
  color: #0d7e7d;
}

.lesson-pay-meta,
.field-hint {
  font-size: 12px;
  color: rgba(31, 35, 41, 0.58);
}

.lesson-pay-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.settlement-note {
  align-self: end;
  padding: 12px 14px;
  border: 1px solid rgba(0, 190, 189, 0.18);
  border-radius: 14px;
  background: rgba(0, 190, 189, 0.08);
  color: #0d7e7d;
  font-size: 13px;
  line-height: 1.6;
}

.lesson-side {
  min-width: 240px;
  display: grid;
  justify-items: end;
  gap: 8px;
}

.lesson-side.compact {
  min-width: 148px;
  display: flex;
  justify-content: flex-end;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #0d7e7d;
  font-size: 12px;
  font-weight: 700;
}

.tone-slate {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.tone-amber {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.tone-sky {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.tone-emerald {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.tone-rose {
  background: rgba(244, 63, 94, 0.12);
  color: #be123c;
}

.mini-empty {
  padding: 32px 20px;
  border-radius: 18px;
  background: rgba(31, 35, 41, 0.03);
  text-align: center;
  color: var(--muted);
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
  width: min(720px, 100%);
  padding: 20px;
  border-radius: 24px;
  display: grid;
  gap: 14px;
}

.modal-title {
  font-size: 18px;
  font-weight: 800;
}

.field {
  display: grid;
  gap: 8px;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.input,
.textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  outline: none;
}

.input {
  height: 44px;
  padding: 0 14px;
}

.textarea {
  min-height: 120px;
  padding: 12px 14px;
  resize: vertical;
}

.btn-link {
  text-decoration: none;
}

.lesson-actions-secondary {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.btn-danger {
  border-color: rgba(190, 24, 93, 0.22);
  color: #be123c;
}

.hint.error {
  color: #be123c;
}

.lesson-hover-preview {
  position: absolute;
  z-index: 30;
  transform: translateY(-50%);
  pointer-events: none;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .focus-grid {
    grid-template-columns: 1fr;
  }

  .content-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .side-card {
    width: auto;
    min-width: 0;
  }
}

@media (max-width: 720px) {
  .hero,
  .email-hint-banner,
  .hero-actions,
  .lesson-item,
  .modal-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-grid,
  .time-grid {
    grid-template-columns: 1fr;
  }

  .focus-lesson {
    flex-direction: column;
  }

  .lesson-side {
    width: 100%;
    justify-items: start;
  }

  .lesson-hover-preview {
    display: none;
  }
}
</style>
