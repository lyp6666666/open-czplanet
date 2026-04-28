import type { LiveSessionResp } from '@/api/live'
import type { ScheduleEventVO } from '@/api/types'

export type LessonStatusKey =
  | 'NOT_STARTED'
  | 'READY_TO_START'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'PENDING_CONFIRM'
  | 'CANCELED'
  | 'ABNORMAL'

export type LessonTone = 'slate' | 'amber' | 'sky' | 'emerald' | 'rose'

export type LessonDetailModel = {
  lessonId: number
  title: string
  topic: string
  description: string
  statusKey: LessonStatusKey
  statusLabel: string
  statusTone: LessonTone
  timeRangeText: string
  durationText: string
  startLabel: string
  endLabel: string
  lessonTypeLabel: string
  summaryTitle: string
  summaryText: string
  helperText: string
}

function parseDateMs(value?: string | number | Date | null) {
  if (value == null) return NaN
  if (value instanceof Date) return value.getTime()
  if (typeof value === 'number') return value
  return Date.parse(value)
}

export function formatLessonDateTime(value?: string | number | Date | null, opts?: Intl.DateTimeFormatOptions) {
  const ms = parseDateMs(value)
  if (!Number.isFinite(ms)) return '待确认'
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...opts,
  }).format(ms)
}

export function formatLessonDuration(startAt?: number | null, endAt?: number | null) {
  if (!startAt || !endAt || endAt <= startAt) return '时长待确认'
  const minutes = Math.max(0, Math.round((endAt - startAt) / 60000))
  if (minutes < 60) return `${minutes} 分钟`
  if (minutes % 60 === 0) return `${minutes / 60} 小时`
  return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟`
}

export function hasLessonStarted(live?: LiveSessionResp | null) {
  if (!live) return false
  const status = String(live.status || '').trim().toUpperCase()
  return status === 'IN_PROGRESS' || status === 'ENDED' || !!live.actualStartAt || !!live.actualEndAt
}

export function hasLessonEnded(live?: LiveSessionResp | null) {
  if (!live) return false
  const status = String(live.status || '').trim().toUpperCase()
  return status === 'ENDED' || !!live.actualEndAt
}

export function resolveLessonStatus(
  lesson: ScheduleEventVO,
  options?: {
    live?: LiveSessionResp | null
    aiResultStatus?: string | null
    nowMs?: number
  },
) {
  const nowMs = options?.nowMs ?? Date.now()
  const live = options?.live ?? null
  const aiResultStatus = String(options?.aiResultStatus || '').trim().toUpperCase()
  const lessonStatus = String(lesson.status || '').trim().toUpperCase()

  if (lessonStatus === 'CANCELED' || lessonStatus === 'REJECTED') {
    return { key: 'CANCELED' as const, label: lessonStatus === 'CANCELED' ? '已取消' : '已拒绝', tone: 'rose' as const }
  }
  if (lessonStatus === 'PENDING' || lessonStatus === 'RESCHEDULE_PENDING') {
    return { key: 'PENDING_CONFIRM' as const, label: lessonStatus === 'PENDING' ? '待确认' : '待确认改期', tone: 'amber' as const }
  }
  if (hasLessonEnded(live) || lessonStatus === 'COMPLETED' || aiResultStatus === 'READY') {
    return { key: 'COMPLETED' as const, label: '已完课', tone: 'emerald' as const }
  }
  if (hasLessonStarted(live)) {
    return { key: 'IN_PROGRESS' as const, label: '上课中', tone: 'sky' as const }
  }
  if (lessonStatus === 'ACCEPTED' && nowMs >= Number(lesson.endAt || 0)) {
    return { key: 'ABNORMAL' as const, label: '待确认未上课', tone: 'amber' as const }
  }
  if (lessonStatus === 'ACCEPTED' && nowMs >= Number(lesson.startAt || 0)) {
    return { key: 'READY_TO_START' as const, label: '已到预约时间，请上课', tone: 'amber' as const }
  }
  return { key: 'NOT_STARTED' as const, label: '未开始', tone: 'slate' as const }
}

export function buildLessonDetailModel(
  lesson: ScheduleEventVO,
  options?: {
    live?: LiveSessionResp | null
    aiResultStatus?: string | null
    afterClassSummary?: string | null
    nowMs?: number
  },
): LessonDetailModel {
  const resolved = resolveLessonStatus(lesson, options)
  const startAt = Number(lesson.startAt || 0)
  const endAt = Number(lesson.endAt || 0)
  const description = String(lesson.description || '').trim()
  const summary = String(options?.afterClassSummary || '').trim()
  const lessonTypeLabel = String(lesson.lessonType || '').trim().toUpperCase() === 'TRIAL' ? '试课' : '正式课'

  let helperText = '点击后查看这节课的完整安排与状态。'
  if (resolved.key === 'NOT_STARTED') helperText = '还没到开始时间，可以先确认安排与学习目标。'
  if (resolved.key === 'READY_TO_START') helperText = '已经到预约时间，可以直接去上课。'
  if (resolved.key === 'IN_PROGRESS') helperText = '这节课正在进行中，可继续回到课堂。'
  if (resolved.key === 'COMPLETED') helperText = '课程已经结束，可回看课后结果。'
  if (resolved.key === 'ABNORMAL') helperText = '预约时间已过，但还未确认是否正常开课，需要人工确认。'
  if (resolved.key === 'PENDING_CONFIRM') helperText = '这节课还在等待对方确认，时间和安排仍可能调整。'
  if (resolved.key === 'CANCELED') helperText = '这节课已取消或被拒绝，不再继续履约。'

  let summaryTitle = '课后总结'
  let summaryText = '这节课结束后，会在这里沉淀课后总结。'
  if (resolved.key === 'COMPLETED' && summary) {
    summaryText = summary
  } else if (resolved.key === 'COMPLETED') {
    summaryText = '这节课已经完成，课后总结正在整理中或暂未生成。'
  } else if (resolved.key === 'ABNORMAL') {
    summaryTitle = '状态说明'
    summaryText = '本节课未进入正常课后流程，需先确认是否属于未正常开课。'
  }

  return {
    lessonId: lesson.id,
    title: lesson.title || '未命名课节',
    topic: description || lesson.title || '本节课学习内容待补充',
    description: description || '当前还没有补充这节课的详细说明。',
    statusKey: resolved.key,
    statusLabel: resolved.label,
    statusTone: resolved.tone,
    timeRangeText: `${formatLessonDateTime(startAt)} - ${formatLessonDateTime(endAt, { hour: '2-digit', minute: '2-digit' })}`,
    durationText: formatLessonDuration(startAt, endAt),
    startLabel: formatLessonDateTime(startAt),
    endLabel: formatLessonDateTime(endAt),
    lessonTypeLabel,
    summaryTitle,
    summaryText,
    helperText,
  }
}

export function sortLessonsByTime(lessons: ScheduleEventVO[]) {
  return lessons.slice().sort((a, b) => Number(a.startAt || 0) - Number(b.startAt || 0))
}

export function findRecentLesson(lessons: ScheduleEventVO[], options?: { live?: LiveSessionResp | null; aiResultStatus?: string | null; nowMs?: number }) {
  const sorted = sortLessonsByTime(lessons)
  const nowMs = options?.nowMs ?? Date.now()
  const active = sorted.find((lesson) => {
    const status = resolveLessonStatus(lesson, options)
    return status.key === 'IN_PROGRESS' || status.key === 'READY_TO_START'
  })
  if (active) return active
  const nextLesson = sorted.find((lesson) => Number(lesson.endAt || 0) >= nowMs && String(lesson.status || '').trim().toUpperCase() !== 'CANCELED')
  if (nextLesson) return nextLesson
  return sorted[sorted.length - 1] || null
}

export function findPreviousLesson(lessons: ScheduleEventVO[], currentLessonId?: number | null) {
  const sorted = sortLessonsByTime(lessons)
  if (!sorted.length) return null
  if (!currentLessonId) return sorted.length > 1 ? sorted[sorted.length - 2] || null : null
  const currentIndex = sorted.findIndex((lesson) => lesson.id === currentLessonId)
  if (currentIndex <= 0) return null
  return sorted[currentIndex - 1] || null
}

export function groupLessonsForOverview(
  lessons: ScheduleEventVO[],
  options?: {
    live?: LiveSessionResp | null
    aiResultStatus?: string | null
    nowMs?: number
  },
) {
  const nowMs = options?.nowMs ?? Date.now()
  const sorted = sortLessonsByTime(lessons)
  const ongoing: ScheduleEventVO[] = []
  const nextUp: ScheduleEventVO[] = []
  const completed: ScheduleEventVO[] = []
  const planned: ScheduleEventVO[] = []

  sorted.forEach((lesson) => {
    const resolved = resolveLessonStatus(lesson, { ...options, nowMs })
    if (resolved.key === 'IN_PROGRESS') {
      ongoing.push(lesson)
      return
    }
    if (resolved.key === 'READY_TO_START') {
      nextUp.push(lesson)
      return
    }
    if (resolved.key === 'COMPLETED' || resolved.key === 'ABNORMAL' || resolved.key === 'CANCELED') {
      completed.push(lesson)
      return
    }
    planned.push(lesson)
  })

  return {
    ongoing,
    nextUp,
    completed: completed.slice().reverse(),
    planned,
  }
}
