<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { liveApi, type LiveSessionResp } from '@/api/live'
import { scheduleApi } from '@/api/schedule'
import type { ScheduleEventVO, UserSimpleVO } from '@/api/types'
import LessonDetailModal from '@/ui/course/LessonDetailModal.vue'
import LessonPreviewCard from '@/ui/course/LessonPreviewCard.vue'
import { buildLessonDetailModel } from '@/utils/lessonDetail'

type ViewMode = 'month' | 'week' | 'day'

const view = ref<ViewMode>('week')
const cursorDate = ref(new Date())
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const events = ref<ScheduleEventVO[]>([])
const liveMap = ref<Record<number, LiveSessionResp>>({})
const detailOpen = ref(false)
const detailBusy = ref(false)
const detailError = ref<string | null>(null)
const detailTitle = ref('')
const detailTimeline = ref<Array<{ eventType: string; eventSource: string; occurredAt: string }>>([])
const hoverPreview = ref<{ eventId: number; top: number; left: number } | null>(null)
const lessonModalEventId = ref<number | null>(null)
const lessonModalOpen = ref(false)

const createOpen = ref(false)
const createBusy = ref(false)
const createError = ref<string | null>(null)

const title = ref('')
const description = ref('')
const participant = ref<UserSimpleVO | null>(null)
const startAt = ref<number>(roundToNextHalfHour(Date.now()))
const endAt = ref<number>(startAt.value + 60 * 60 * 1000)

const contactLoading = ref(false)
const contactError = ref<string | null>(null)
const contacts = ref<UserSimpleVO[]>([])
const contactSearch = ref('')

function roundToNextHalfHour(nowMs: number) {
  const d = new Date(nowMs)
  d.setSeconds(0, 0)
  const m = d.getMinutes()
  const next = m <= 0 ? 0 : m <= 30 ? 30 : 60
  d.setMinutes(next)
  if (next === 60) d.setHours(d.getHours() + 1, 0, 0, 0)
  return d.getTime()
}

function startOfDay(d: Date) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

function addDays(d: Date, delta: number) {
  const x = new Date(d)
  x.setDate(x.getDate() + delta)
  return x
}

function startOfMonth(d: Date) {
  const x = new Date(d)
  x.setDate(1)
  x.setHours(0, 0, 0, 0)
  return x
}

function addMonths(d: Date, delta: number) {
  const x = new Date(d)
  x.setMonth(x.getMonth() + delta, 1)
  x.setHours(0, 0, 0, 0)
  return x
}

function startOfWeekMonday(d: Date) {
  const x = startOfDay(d)
  const day = x.getDay() // 0=Sun ... 6=Sat
  const diff = day === 0 ? -6 : 1 - day
  return addDays(x, diff)
}

function formatDate(d: Date) {
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function formatMonthLabel(d: Date) {
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
}

function isSameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

function formatHm(ms: number) {
  const d = new Date(ms)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

const range = computed(() => {
  const d = cursorDate.value
  if (view.value === 'month') {
    const start = startOfMonth(d)
    const end = addMonths(start, 1)
    return { startAt: start.getTime(), endAt: end.getTime() }
  }
  if (view.value === 'week') {
    const start = startOfWeekMonday(d)
    const end = addDays(start, 7)
    return { startAt: start.getTime(), endAt: end.getTime() }
  }
  const start = startOfDay(d)
  const end = addDays(start, 1)
  return { startAt: start.getTime(), endAt: end.getTime() }
})

async function loadEvents() {
  loading.value = true
  error.value = null
  try {
    events.value = await scheduleApi.listEvents({ startAt: range.value.startAt, endAt: range.value.endAt, includePending: true })
    const liveEntries = await Promise.all(
      events.value.map(async (it) => {
        const courseId = Number(it.courseId || 0)
        if (!(courseId > 0)) return null
        try {
          const live = await liveApi.getByCourse(courseId)
          return [it.id, live] as const
        } catch {
          return null
        }
      }),
    )
    liveMap.value = Object.fromEntries(liveEntries.filter(Boolean) as Array<readonly [number, LiveSessionResp]>)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    events.value = []
    liveMap.value = {}
  } finally {
    loading.value = false
  }
}

function liveStatusLabel(eventId: number) {
  const live = liveMap.value[eventId]
  if (!live) return ''
  if (live.status === 'IN_PROGRESS') return '进行中'
  if (live.joinableNow) return '可入会'
  return ''
}

function lessonModel(event: ScheduleEventVO) {
  return buildLessonDetailModel(event, {
    live: liveMap.value[event.id] || null,
  })
}

const hoveredEventModel = computed(() => {
  const event = events.value.find((item) => item.id === hoverPreview.value?.eventId) || null
  return event ? lessonModel(event) : null
})

const selectedEventModel = computed(() => {
  const event = events.value.find((item) => item.id === lessonModalEventId.value) || null
  return event ? lessonModel(event) : null
})

function showHoverPreview(event: ScheduleEventVO, mouseEvent: MouseEvent) {
  const target = mouseEvent.currentTarget as HTMLElement | null
  if (!target) return
  const rect = target.getBoundingClientRect()
  hoverPreview.value = {
    eventId: event.id,
    top: rect.top + window.scrollY + rect.height / 2,
    left: rect.right + window.scrollX + 16,
  }
}

function hideHoverPreview() {
  hoverPreview.value = null
}

function openLessonModal(event: ScheduleEventVO) {
  lessonModalEventId.value = event.id
  lessonModalOpen.value = true
}

function closeLessonModal() {
  lessonModalOpen.value = false
  lessonModalEventId.value = null
}

function lessonModalPrimaryLabel() {
  const event = events.value.find((item) => item.id === lessonModalEventId.value) || null
  const model = selectedEventModel.value
  if (!event || !model) return null
  if (model.statusKey === 'READY_TO_START' || model.statusKey === 'IN_PROGRESS') return '去上课'
  if (event.courseId) return '打开课程总览'
  return null
}

function handleLessonModalPrimary() {
  const event = events.value.find((item) => item.id === lessonModalEventId.value) || null
  const model = selectedEventModel.value
  if (!event || !model) return
  if (model.statusKey === 'READY_TO_START' || model.statusKey === 'IN_PROGRESS') {
    openLivePrepare(event.courseId)
    return
  }
  if (event.courseId) {
    void router.push({ name: 'courseDetail', params: { courseId: String(event.courseId) } })
  }
}

function openLivePrepare(courseId?: number | null) {
  if (!(courseId && courseId > 0)) return
  void router.push({ name: 'livePrepare', params: { courseId: String(courseId) } })
}

async function openEventDetail(event: ScheduleEventVO) {
  detailOpen.value = true
  detailBusy.value = true
  detailError.value = null
  detailTitle.value = event.title
  detailTimeline.value = []
  try {
    if (!(event.courseId && event.courseId > 0)) {
      detailError.value = '当前课节还未关联课程'
      return
    }
    const live = liveMap.value[event.id] || (await liveApi.getByCourse(event.courseId))
    if (!live.sessionId) {
      detailError.value = '当前课程尚未生成课堂详情'
      return
    }
    detailTimeline.value = await liveApi.timeline(live.sessionId)
  } catch (e) {
    detailError.value = e instanceof Error ? e.message : '加载课堂详情失败'
  } finally {
    detailBusy.value = false
  }
}

function closeEventDetail() {
  detailOpen.value = false
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

function onPrev() {
  if (view.value === 'month') cursorDate.value = addMonths(cursorDate.value, -1)
  else if (view.value === 'week') cursorDate.value = addDays(cursorDate.value, -7)
  else cursorDate.value = addDays(cursorDate.value, -1)
}

function onNext() {
  if (view.value === 'month') cursorDate.value = addMonths(cursorDate.value, 1)
  else if (view.value === 'week') cursorDate.value = addDays(cursorDate.value, 7)
  else cursorDate.value = addDays(cursorDate.value, 1)
}

function onToday() {
  cursorDate.value = new Date()
}

function openCreate(prefillDay?: Date) {
  createError.value = null
  title.value = ''
  description.value = ''
  participant.value = null
  const base = prefillDay ? prefillDay.getTime() : Date.now()
  startAt.value = roundToNextHalfHour(base)
  endAt.value = startAt.value + 60 * 60 * 1000
  createOpen.value = true
  void loadRecentContacts()
}

async function loadRecentContacts() {
  contactLoading.value = true
  contactError.value = null
  try {
    contacts.value = await scheduleApi.listRecentContacts(50)
  } catch (e) {
    contactError.value = e instanceof Error ? e.message : '加载联系人失败'
    contacts.value = []
  } finally {
    contactLoading.value = false
  }
}

async function searchContacts() {
  const q = contactSearch.value.trim()
  if (!q) {
    await loadRecentContacts()
    return
  }
  contactLoading.value = true
  contactError.value = null
  try {
    contacts.value = await scheduleApi.searchContacts(q, 50)
  } catch (e) {
    contactError.value = e instanceof Error ? e.message : '搜索失败'
    contacts.value = []
  } finally {
    contactLoading.value = false
  }
}

async function submitCreate() {
  if (createBusy.value) return
  createError.value = null
  const t = title.value.trim()
  if (!t) {
    createError.value = '请输入课程名称'
    return
  }
  if (!participant.value) {
    createError.value = '请选择授课对象'
    return
  }
  if (!(endAt.value > startAt.value)) {
    createError.value = '结束时间必须晚于开始时间'
    return
  }

  createBusy.value = true
  try {
    await scheduleApi.createEvent({
      title: t,
      participantUserId: participant.value.id,
      startAt: startAt.value,
      endAt: endAt.value,
      description: description.value.trim() || undefined,
    })
    createOpen.value = false
    await loadEvents()
  } catch (e) {
    createError.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    createBusy.value = false
  }
}

watch([view, cursorDate], () => void loadEvents())

onMounted(() => void loadEvents())

const monthDays = computed(() => {
  const start = startOfMonth(cursorDate.value)
  const firstWeekDay = start.getDay() // 0 Sunday
  const pad = firstWeekDay === 0 ? 6 : firstWeekDay - 1
  const days: Array<{ date: Date; inMonth: boolean }> = []
  for (let i = 0; i < pad; i++) {
    days.push({ date: addDays(start, -(pad - i)), inMonth: false })
  }
  const nextMonth = addMonths(start, 1)
  for (let d = new Date(start); d < nextMonth; d = addDays(d, 1)) {
    days.push({ date: new Date(d), inMonth: true })
  }
  while (days.length % 7 !== 0) {
    const last = days[days.length - 1]!.date
    days.push({ date: addDays(last, 1), inMonth: false })
  }
  return days
})

function eventsOnDay(day: Date) {
  const s = startOfDay(day).getTime()
  const e = addDays(startOfDay(day), 1).getTime()
  return events.value
    .filter((it) => it.startAt < e && it.endAt > s)
    .slice()
    .sort((a, b) => a.startAt - b.startAt)
}

const weekDays = computed(() => {
  const start = startOfWeekMonday(cursorDate.value)
  return Array.from({ length: 7 }, (_, i) => addDays(start, i))
})

const selectedDayEvents = computed(() => eventsOnDay(cursorDate.value))

const selectedDayAcceptedCount = computed(() => selectedDayEvents.value.filter((it) => it.status === 'ACCEPTED').length)

const selectedDayPendingCount = computed(() => selectedDayEvents.value.filter((it) => it.status === 'PENDING').length)

const selectedDayMinutes = computed(() =>
  selectedDayEvents.value.reduce((sum, it) => sum + Math.max(0, Math.round((it.endAt - it.startAt) / 60000)), 0),
)

const selectedDayHoursText = computed(() => {
  const hours = selectedDayMinutes.value / 60
  if (!hours) return '暂无安排'
  if (Number.isInteger(hours)) return `${hours}h`
  return `${hours.toFixed(1)}h`
})

const currentRangeLabel = computed(() => {
  if (view.value === 'month') return formatMonthLabel(cursorDate.value)
  if (view.value === 'day') return `${formatDate(cursorDate.value)} · 今日安排`
  const start = weekDays.value[0]!
  const end = weekDays.value[weekDays.value.length - 1]!
  return `${formatDate(start)} - ${formatDate(end)}`
})

type PlacedEvent = {
  event: ScheduleEventVO
  top: number
  height: number
  leftPct: number
  widthPct: number
}

const DAY_START_HOUR = 6
const DAY_END_HOUR = 24
const PX_PER_MIN = 1.2

function layoutDayEvents(list: ScheduleEventVO[], day: Date): PlacedEvent[] {
  // 日历重叠布局：按“区间相交”分组，组内用贪心分配列，并按列数均分宽度。
  const startMs = startOfDay(day).getTime()
  const endMs = addDays(startOfDay(day), 1).getTime()
  const dayEvents = list
    .filter((e) => e.startAt >= startMs && e.endAt <= endMs)
    .slice()
    .sort((a, b) => a.startAt - b.startAt || a.endAt - b.endAt)

  const groups: ScheduleEventVO[][] = []
  let cur: ScheduleEventVO[] = []
  let curEnd = 0
  for (const ev of dayEvents) {
    if (cur.length === 0) {
      cur = [ev]
      curEnd = ev.endAt
      continue
    }
    if (ev.startAt < curEnd) {
      cur.push(ev)
      curEnd = Math.max(curEnd, ev.endAt)
    } else {
      groups.push(cur)
      cur = [ev]
      curEnd = ev.endAt
    }
  }
  if (cur.length) groups.push(cur)

  const placed: PlacedEvent[] = []
  for (const g of groups) {
    const colsEnd: number[] = []
    const colIndex = new Map<number, number>()
    for (const ev of g) {
      let col = colsEnd.findIndex((end) => end <= ev.startAt)
      if (col === -1) {
        col = colsEnd.length
        colsEnd.push(ev.endAt)
      } else {
        colsEnd[col] = ev.endAt
      }
      colIndex.set(ev.id, col)
    }
    const colCount = Math.max(colsEnd.length, 1)
    for (const ev of g) {
      const col = colIndex.get(ev.id) ?? 0
      const startMin = (new Date(ev.startAt).getHours() * 60 + new Date(ev.startAt).getMinutes()) - DAY_START_HOUR * 60
      const durMin = Math.max(15, Math.round((ev.endAt - ev.startAt) / 60000))
      const top = Math.max(0, startMin) * PX_PER_MIN
      const height = durMin * PX_PER_MIN
      const widthPct = 100 / colCount
      const leftPct = col * widthPct
      placed.push({ event: ev, top, height, leftPct, widthPct })
    }
  }
  return placed
}
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">
          Schedule Studio
        </p>
        <h1 class="hero-title">
          让排课更像在看一张真正可用的日程板
        </h1>
        <p class="hero-desc">
          左侧快速浏览整月节奏，右侧专注当天或本周的详细安排，减少无关信息，让课程排布和课堂状态一眼看清。
        </p>
      </div>
      <button
        class="btn btn-primary hero-action"
        type="button"
        @click="openCreate()"
      >
        创建日程
      </button>
    </section>

    <div
      v-if="error"
      class="hint error"
    >
      {{ error }}
    </div>

    <section class="board">
      <aside class="sidebar card">
        <div class="sidebar-head">
          <div>
            <div class="panel-kicker">
              月视图
            </div>
            <div class="panel-title">
              {{ formatMonthLabel(cursorDate) }}
            </div>
          </div>
          <div class="panel-nav">
            <button
              class="icon-btn"
              type="button"
              @click="cursorDate = addMonths(cursorDate, -1)"
            >
              ‹
            </button>
            <button
              class="icon-btn"
              type="button"
              @click="cursorDate = addMonths(cursorDate, 1)"
            >
              ›
            </button>
          </div>
        </div>

        <div class="mini-weekdays">
          <span
            v-for="label in ['一', '二', '三', '四', '五', '六', '日']"
            :key="label"
          >{{ label }}</span>
        </div>

        <div class="mini-grid">
          <button
            v-for="d in monthDays"
            :key="d.date.toISOString()"
            class="mini-day"
            :class="{
              out: !d.inMonth,
              selected: isSameDay(d.date, cursorDate),
              today: isSameDay(d.date, new Date()),
              busy: eventsOnDay(d.date).length > 0,
            }"
            type="button"
            @click="
              cursorDate = d.date;
              if (view === 'month') view = 'day'
            "
          >
            <span>{{ d.date.getDate() }}</span>
            <i v-if="eventsOnDay(d.date).length > 0" />
          </button>
        </div>

        <div class="sidebar-summary">
          <div class="summary-head">
            <div>
              <div class="panel-kicker">
                当前选中
              </div>
              <div class="summary-date">
                {{ formatDate(cursorDate) }}
              </div>
            </div>
            <button
              class="btn btn-muted summary-today"
              type="button"
              @click="onToday"
            >
              今天
            </button>
          </div>

          <div class="stats">
            <div class="stat-card">
              <span class="stat-label">总安排</span>
              <strong>{{ selectedDayEvents.length }}</strong>
            </div>
            <div class="stat-card">
              <span class="stat-label">已确认</span>
              <strong>{{ selectedDayAcceptedCount }}</strong>
            </div>
            <div class="stat-card">
              <span class="stat-label">待响应</span>
              <strong>{{ selectedDayPendingCount }}</strong>
            </div>
            <div class="stat-card">
              <span class="stat-label">占用时长</span>
              <strong>{{ selectedDayHoursText }}</strong>
            </div>
          </div>

          <div class="agenda-list">
            <div
              v-if="selectedDayEvents.length === 0"
              class="agenda-empty"
            >
              这一天暂时没有课程安排，可以直接在右侧空白区域双击创建。
            </div>
            <button
              v-for="ev in selectedDayEvents.slice(0, 4)"
              :key="ev.id"
              class="agenda-item"
              type="button"
              @click="liveMap[ev.id] ? openEventDetail(ev) : void 0"
            >
              <span class="agenda-time">{{ formatHm(ev.startAt) }}</span>
              <span class="agenda-main">
                <strong>{{ ev.title }}</strong>
                <em>{{ ev.status === 'PENDING' ? '待确认' : '已安排' }}</em>
              </span>
              <span
                v-if="liveStatusLabel(ev.id)"
                class="agenda-badge"
              >{{ liveStatusLabel(ev.id) }}</span>
            </button>
          </div>
        </div>
      </aside>

      <main class="main card">
        <div class="main-top">
          <div class="toolbar-left">
            <button
              class="btn btn-muted"
              type="button"
              @click="onToday"
            >
              今天
            </button>
            <div class="panel-nav">
              <button
                class="icon-btn"
                type="button"
                @click="onPrev"
              >
                ‹
              </button>
              <button
                class="icon-btn"
                type="button"
                @click="onNext"
              >
                ›
              </button>
            </div>
            <div class="range-copy">
              <div class="panel-kicker">
                当前视图
              </div>
              <div class="range-title">
                {{ currentRangeLabel }}
              </div>
            </div>
          </div>

          <div class="toolbar-right">
            <div class="seg">
              <button
                class="seg-btn"
                :class="{ on: view === 'day' }"
                type="button"
                @click="view = 'day'"
              >
                日
              </button>
              <button
                class="seg-btn"
                :class="{ on: view === 'week' }"
                type="button"
                @click="view = 'week'"
              >
                周
              </button>
              <button
                class="seg-btn"
                :class="{ on: view === 'month' }"
                type="button"
                @click="view = 'month'"
              >
                月
              </button>
            </div>
            <button
              class="btn btn-primary desktop-create"
              type="button"
              @click="openCreate()"
            >
              新建
            </button>
          </div>
        </div>

        <div
          v-if="view === 'month'"
          class="month-shell"
        >
          <div class="month-week-head">
            <div
              v-for="label in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']"
              :key="label"
              class="w"
            >
              {{ label }}
            </div>
          </div>
          <div class="month-grid">
            <div
              v-for="d in monthDays"
              :key="d.date.toISOString()"
              class="month-cell"
              :class="{ dim: !d.inMonth, active: isSameDay(d.date, cursorDate) }"
              @click="
                cursorDate = d.date;
                view = 'day'
              "
            >
              <div class="month-cell-head">
                <span class="month-day-num">{{ d.date.getDate() }}</span>
                <span
                  v-if="eventsOnDay(d.date).length"
                  class="month-count"
                >{{ eventsOnDay(d.date).length }}项</span>
              </div>
              <div class="month-items">
                <div
                  v-for="ev in eventsOnDay(d.date).slice(0, 3)"
                  :key="ev.id"
                  class="month-item"
                  :title="`${ev.title}｜${formatHm(ev.startAt)} - ${formatHm(ev.endAt)}`"
                >
                  <span class="month-item-time">{{ formatHm(ev.startAt) }}</span>
                  <span class="month-item-name">{{ ev.title }}</span>
                </div>
                <div
                  v-if="eventsOnDay(d.date).length > 3"
                  class="month-more"
                >
                  +{{ eventsOnDay(d.date).length - 3 }} more
                </div>
              </div>
            </div>
          </div>
        </div>

        <div
          v-else
          class="schedule-shell"
        >
          <div class="time-head">
            <div class="time-col axis-label">
              GMT+8
            </div>
            <div
              class="days"
              :style="{ gridTemplateColumns: view === 'week' ? 'repeat(7, 1fr)' : '1fr' }"
            >
              <div
                v-for="d in (view === 'week' ? weekDays : [startOfDay(cursorDate)])"
                :key="d.toISOString()"
                class="day"
                :class="{ active: isSameDay(d, cursorDate) }"
              >
                <div class="day-week">
                  {{ ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()] }}
                </div>
                <div class="day-title">
                  {{ d.getMonth() + 1 }}月{{ d.getDate() }}日
                </div>
              </div>
            </div>
          </div>

          <div class="time-body">
            <div class="time-col">
              <div
                v-for="h in DAY_END_HOUR - DAY_START_HOUR"
                :key="h"
                class="hour"
              >
                {{ String(h + DAY_START_HOUR).padStart(2, '0') }}:00
              </div>
            </div>
            <div
              class="days"
              :style="{ gridTemplateColumns: view === 'week' ? 'repeat(7, 1fr)' : '1fr' }"
            >
              <div
                v-for="d in (view === 'week' ? weekDays : [startOfDay(cursorDate)])"
                :key="d.toISOString()"
                class="day-col"
                :class="{ active: isSameDay(d, cursorDate) }"
                @dblclick="openCreate(d)"
              >
                <div class="slots">
                  <div
                    v-for="h in DAY_END_HOUR - DAY_START_HOUR"
                    :key="h"
                    class="slot"
                  />
                </div>
                <div class="events">
                  <div
                    v-for="p in layoutDayEvents(events, d)"
                    :key="p.event.id"
                    class="event"
                    :class="p.event.status.toLowerCase()"
                    :style="{ top: `${p.top}px`, height: `${p.height}px`, left: `${p.leftPct}%`, width: `${p.widthPct}%` }"
                    @mouseenter="showHoverPreview(p.event, $event)"
                    @mouseleave="hideHoverPreview"
                    @click.stop="openLessonModal(p.event)"
                  >
                    <div class="event-accent" />
                    <div class="et">
                      {{ p.event.title }}
                    </div>
                    <div class="es">
                      {{ formatHm(p.event.startAt) }} - {{ formatHm(p.event.endAt) }}
                    </div>
                    <div
                      v-if="liveStatusLabel(p.event.id)"
                      class="elive"
                    >
                      {{ liveStatusLabel(p.event.id) }}
                    </div>
                    <button
                      v-if="liveMap[p.event.id]"
                      class="event-link"
                      type="button"
                      @click.stop="openLivePrepare(p.event.courseId)"
                    >
                      去上课
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </section>

    <div
      v-if="createOpen"
      class="mask"
      @click.self="createOpen = false"
    >
      <div class="modal card">
        <div class="m-title">
          创建日程
        </div>
        <div
          v-if="createError"
          class="m-error"
        >
          {{ createError }}
        </div>

        <div class="field">
          <div class="lab">
            课程名称
          </div>
          <input
            v-model="title"
            class="ipt"
            placeholder="例如：初二数学｜一次函数强化"
          >
        </div>

        <div class="field">
          <div class="lab">
            授课对象
          </div>
          <div class="pick">
            <div
              v-if="participant"
              class="picked"
            >
              <span class="pname">{{ participant.name }}</span>
              <button
                class="btn small"
                type="button"
                @click="participant = null"
              >
                更换
              </button>
            </div>
            <div
              v-else
              class="picker"
            >
              <div class="row">
                <input
                  v-model="contactSearch"
                  class="ipt"
                  placeholder="搜索昵称/手机号"
                >
                <button
                  class="btn small"
                  type="button"
                  :disabled="contactLoading"
                  @click="searchContacts"
                >
                  搜索
                </button>
              </div>
              <div
                v-if="contactError"
                class="m-error"
              >
                {{ contactError }}
              </div>
              <div class="list">
                <button
                  v-for="u in contacts"
                  :key="u.id"
                  class="contact"
                  type="button"
                  @click="participant = u"
                >
                  <span class="cname">{{ u.name }}</span>
                  <span class="ctag">{{ u.userType === 1 ? '教师' : '家长' }}</span>
                </button>
                <div
                  v-if="!contactLoading && contacts.length === 0"
                  class="empty"
                >
                  暂无联系人
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="field">
          <div class="lab">
            开始时间
          </div>
          <input
            class="ipt"
            type="datetime-local"
            :value="new Date(startAt).toISOString().slice(0, 16)"
            @change="startAt = new Date(($event.target as HTMLInputElement).value).getTime()"
          >
        </div>

        <div class="field">
          <div class="lab">
            结束时间
          </div>
          <input
            class="ipt"
            type="datetime-local"
            :value="new Date(endAt).toISOString().slice(0, 16)"
            @change="endAt = new Date(($event.target as HTMLInputElement).value).getTime()"
          >
        </div>

        <div class="field">
          <div class="lab">
            备注
          </div>
          <textarea
            v-model="description"
            class="txt"
            rows="3"
            placeholder="可选"
          />
        </div>

        <div class="m-ops">
          <button
            class="btn"
            type="button"
            :disabled="createBusy"
            @click="createOpen = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="createBusy"
            @click="submitCreate"
          >
            {{ createBusy ? '创建中...' : '创建并发送申请' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="hoverPreview && hoveredEventModel"
      class="lesson-hover-preview"
      :style="{ top: `${hoverPreview.top}px`, left: `${hoverPreview.left}px` }"
    >
      <LessonPreviewCard :model="hoveredEventModel" />
    </div>

    <LessonDetailModal
      :open="lessonModalOpen"
      :model="selectedEventModel"
      cooperation-name="单节课详情"
      :primary-label="lessonModalPrimaryLabel()"
      :primary-disabled="!lessonModalPrimaryLabel()"
      @close="closeLessonModal"
      @primary="handleLessonModalPrimary"
    />

    <div
      v-if="detailOpen"
      class="mask"
      @click.self="closeEventDetail"
    >
      <div class="modal card detail-modal">
        <div class="m-title">
          课堂详情
        </div>
        <div class="detail-subtitle">
          {{ detailTitle }}
        </div>
        <div
          v-if="detailError"
          class="m-error"
        >
          {{ detailError }}
        </div>
        <div
          v-else-if="detailBusy"
          class="detail-empty"
        >
          加载中...
        </div>
        <div
          v-else-if="detailTimeline.length === 0"
          class="detail-empty"
        >
          暂未产生课堂事件
        </div>
        <div
          v-else
          class="detail-list"
        >
          <div
            v-for="item in detailTimeline"
            :key="`${item.eventType}-${item.occurredAt}`"
            class="detail-item"
          >
            <div class="detail-dot" />
            <div class="detail-content">
              <div class="detail-title">
                {{ timelineEventLabel(item.eventType) }}
              </div>
              <div class="detail-meta">
                {{ item.eventSource }} · {{ item.occurredAt }}
              </div>
            </div>
          </div>
        </div>
        <div class="m-ops">
          <button
            class="btn"
            type="button"
            @click="closeEventDetail"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 12px 4px 24px;
}

.hero {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 30px;
  border-radius: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(0, 190, 189, 0.18), transparent 34%),
    radial-gradient(circle at 85% 25%, rgba(20, 97, 255, 0.12), transparent 24%),
    linear-gradient(135deg, #fefefe 0%, #f6fbff 50%, #f7f8fc 100%);
  border: 1px solid rgba(31, 35, 41, 0.08);
  box-shadow: 0 18px 48px rgba(28, 51, 84, 0.08);
}

.hero::after {
  content: '';
  position: absolute;
  inset: auto -8% -55% auto;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(17, 87, 255, 0.12), transparent 66%);
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 760px;
}

.eyebrow,
.panel-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #6c7381;
}

.hero-title {
  margin: 6px 0 4px;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.06;
  letter-spacing: -0.03em;
  font-weight: 800;
  color: #18202d;
}

.hero-desc {
  margin: 0;
  max-width: 100%;
  color: #5d6675;
  font-size: 14px;
  line-height: 1.45;
  white-space: nowrap;
}

.hero-action {
  position: relative;
  z-index: 1;
  align-self: flex-start;
  min-width: 128px;
  height: 44px;
  border-radius: 14px;
  box-shadow: 0 16px 30px rgba(0, 190, 189, 0.24);
}

.board {
  display: grid;
  grid-template-columns: minmax(280px, 328px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.sidebar,
.main {
  border-radius: 26px;
  box-shadow: 0 16px 40px rgba(21, 35, 54, 0.08);
}

.sidebar {
  padding: 22px 18px 18px;
  background:
    linear-gradient(180deg, rgba(247, 250, 255, 0.95), rgba(255, 255, 255, 0.98)),
    #fff;
}

.main {
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(249, 251, 255, 0.98), rgba(255, 255, 255, 1) 26%),
    #fff;
  min-width: 0;
}

.sidebar-head,
.summary-head,
.main-top,
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title,
.range-title,
.summary-date {
  margin-top: 4px;
  font-weight: 800;
  color: #1c2431;
}

.panel-title {
  font-size: 28px;
  letter-spacing: -0.04em;
}

.summary-date,
.range-title {
  font-size: 22px;
  letter-spacing: -0.03em;
}

.panel-nav {
  display: inline-flex;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(31, 35, 41, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #3e4858;
  cursor: pointer;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.icon-btn:hover {
  background: #fff;
  border-color: rgba(20, 97, 255, 0.18);
}

.mini-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin: 18px 0 10px;
  padding: 0 2px;
  color: #8a92a1;
  font-size: 12px;
  text-align: center;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.mini-day {
  position: relative;
  border: none;
  min-height: 42px;
  border-radius: 14px;
  background: transparent;
  color: #2c3442;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.mini-day:hover {
  transform: translateY(-1px);
  background: rgba(0, 190, 189, 0.08);
}

.mini-day.out {
  color: #adb4c0;
}

.mini-day.today {
  box-shadow: inset 0 0 0 1px rgba(20, 97, 255, 0.25);
}

.mini-day.selected {
  background: linear-gradient(135deg, #1e67ff, #49a1ff);
  color: #fff;
  box-shadow: 0 12px 22px rgba(30, 103, 255, 0.26);
}

.mini-day i {
  position: absolute;
  left: 50%;
  bottom: 6px;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: currentColor;
  transform: translateX(-50%);
  opacity: 0.52;
}

.mini-day.selected i {
  opacity: 1;
}

.sidebar-summary {
  margin-top: 20px;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(247, 249, 252, 0.95), rgba(255, 255, 255, 0.9));
  border: 1px solid rgba(31, 35, 41, 0.08);
}

.summary-today {
  flex: 0 0 auto;
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.stat-card {
  padding: 12px 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(31, 35, 41, 0.08);
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  letter-spacing: -0.04em;
  color: #1c2431;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #7c8492;
}

.agenda-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.agenda-item,
.agenda-empty {
  width: 100%;
  padding: 12px 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(31, 35, 41, 0.08);
}

.agenda-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  text-align: left;
}

.agenda-time {
  font-weight: 700;
  color: #1457ff;
}

.agenda-main {
  display: grid;
  gap: 4px;
}

.agenda-main strong {
  font-size: 14px;
  color: #1f2633;
}

.agenda-main em {
  font-style: normal;
  font-size: 12px;
  color: #7c8492;
}

.agenda-badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #008b8b;
  font-size: 12px;
  font-weight: 700;
}

.agenda-empty {
  color: #7c8492;
  font-size: 13px;
  line-height: 1.7;
}

.main-top {
  padding: 4px 4px 18px;
  border-bottom: 1px solid rgba(31, 35, 41, 0.08);
  margin-bottom: 18px;
}

.toolbar-left,
.toolbar-right {
  gap: 14px;
}

.range-copy {
  min-width: 0;
}

.seg {
  display: inline-flex;
  padding: 4px;
  border-radius: 16px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(246, 248, 251, 0.96);
}

.seg-btn {
  border: none;
  min-width: 56px;
  height: 38px;
  background: transparent;
  padding: 0 14px;
  border-radius: 12px;
  cursor: pointer;
  color: #657080;
  font-weight: 700;
}

.seg-btn.on {
  background: linear-gradient(135deg, rgba(30, 103, 255, 0.14), rgba(73, 161, 255, 0.18));
  color: #1457ff;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.desktop-create {
  min-width: 88px;
}

.schedule-shell {
  min-width: 0;
}

.month-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.month-week-head {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  padding: 0 6px;
  color: #8891a0;
  font-size: 13px;
  font-weight: 700;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.month-cell {
  min-width: 0;
  min-height: 158px;
  padding: 12px;
  border-radius: 20px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 249, 252, 0.88));
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.month-cell:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 24px rgba(21, 35, 54, 0.08);
}

.month-cell.dim {
  opacity: 0.56;
}

.month-cell.active {
  border-color: rgba(20, 97, 255, 0.18);
  box-shadow: 0 18px 30px rgba(20, 97, 255, 0.12);
}

.month-cell-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.month-day-num {
  font-weight: 800;
  color: #1f2633;
}

.month-count {
  color: #7c8492;
  font-size: 12px;
}

.month-items {
  min-width: 0;
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.month-item {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-width: 0;
  font-size: 12px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(20, 97, 255, 0.06);
  overflow: hidden;
}

.month-item-time {
  color: #1457ff;
  font-weight: 700;
  flex: 0 0 auto;
}

.month-item-name {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.month-more {
  font-size: 12px;
  color: #7c8492;
  padding: 0 4px;
}

.time-head {
  display: grid;
  grid-template-columns: 72px 1fr;
  align-items: end;
  border-bottom: 1px solid rgba(31, 35, 41, 0.08);
  padding-bottom: 12px;
}

.axis-label {
  color: #8a92a1;
  font-size: 13px;
  font-weight: 700;
}

.time-body {
  display: grid;
  grid-template-columns: 72px 1fr;
  margin-top: 10px;
  max-height: min(72vh, 980px);
  overflow: auto;
  padding-right: 6px;
  scrollbar-gutter: stable;
}

.time-col {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.hour {
  height: 72px;
  font-size: 12px;
  color: #8a92a1;
}

.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.day {
  text-align: center;
  padding: 10px 10px 6px;
  border-radius: 18px;
  transition: background 0.18s ease;
}

.day.active {
  background: rgba(20, 97, 255, 0.06);
}

.day-week {
  font-size: 12px;
  color: #8a92a1;
}

.day-title {
  margin-top: 6px;
  font-weight: 800;
  color: #1c2431;
}

.day-col {
  position: relative;
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 253, 0.92));
  height: 1296px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.day-col.active {
  border-color: rgba(20, 97, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(20, 97, 255, 0.06);
}

.slots {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
}

.slot {
  height: 72px;
  border-top: 1px solid rgba(31, 35, 41, 0.06);
}

.events {
  position: absolute;
  inset: 0;
}

.event {
  position: absolute;
  padding: 10px 10px 8px 14px;
  border-radius: 16px;
  box-sizing: border-box;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(15, 198, 196, 0.18), rgba(15, 198, 196, 0.08));
  border: 1px solid rgba(15, 198, 196, 0.26);
  box-shadow: 0 8px 18px rgba(0, 190, 189, 0.16);
}

.event.pending {
  background: linear-gradient(180deg, rgba(255, 171, 51, 0.22), rgba(255, 171, 51, 0.09));
  border-color: rgba(255, 171, 51, 0.34);
  box-shadow: 0 8px 18px rgba(255, 171, 51, 0.18);
}

.event.canceled,
.event.rejected {
  opacity: 0.5;
}

.event-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #00a8a6;
}

.event.pending .event-accent {
  background: #ff9d1f;
}

.et {
  font-weight: 700;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.es {
  font-size: 12px;
  color: #617085;
}

.elive {
  margin-top: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #007b79;
}

.event-link {
  margin-top: 4px;
  border: none;
  background: transparent;
  padding: 0;
  color: #1457ff;
  font-size: 11px;
  cursor: pointer;
}

.lesson-hover-preview {
  position: absolute;
  z-index: 40;
  transform: translateY(-50%);
  pointer-events: none;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 50;
}

.modal {
  width: min(520px, 100%);
  padding: 14px;
  border-radius: 24px;
}

.detail-modal {
  width: min(640px, 100%);
}

.detail-subtitle {
  color: rgba(0, 0, 0, 0.6);
  font-size: 13px;
}

.m-title {
  font-weight: 900;
  font-size: 16px;
  margin-bottom: 10px;
}

.m-error {
  color: var(--danger);
  font-size: 12px;
  margin: 6px 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.lab {
  font-size: 12px;
  color: var(--muted);
}

.ipt,
.txt {
  width: 100%;
  border: 1px solid rgba(31, 35, 41, 0.12);
  border-radius: 14px;
  padding: 10px;
  background: #fff;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

.detail-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.detail-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
}

.detail-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
  margin-top: 5px;
}

.detail-content {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(15, 118, 110, 0.06);
}

.detail-title {
  font-weight: 700;
}

.detail-meta,
.detail-empty {
  color: rgba(0, 0, 0, 0.6);
  font-size: 12px;
}

.btn.small {
  padding: 6px 8px;
  font-size: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 240px;
  overflow: auto;
  padding-right: 4px;
}

.contact {
  border: 1px solid rgba(31, 35, 41, 0.12);
  border-radius: 14px;
  padding: 10px;
  background: #fff;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ctag {
  font-size: 12px;
  color: var(--muted);
}

.empty {
  font-size: 12px;
  color: var(--muted);
  padding: 8px 0;
}

@media (max-width: 1180px) {
  .board {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: 2;
  }

  .main {
    order: 1;
  }
}

@media (max-width: 900px) {
  .page {
    padding-inline: 0;
  }

  .hero,
  .sidebar,
  .main {
    border-radius: 22px;
  }

  .hero {
    padding: 22px 20px;
    flex-direction: column;
  }

  .hero-desc {
    white-space: normal;
  }

  .main-top,
  .toolbar-left,
  .toolbar-right,
  .summary-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .days {
    grid-template-columns: 1fr;
  }

  .time-head,
  .time-body {
    grid-template-columns: 56px 1fr;
  }

  .month-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats {
    grid-template-columns: 1fr 1fr;
  }

  .mini-grid {
    gap: 6px;
  }

  .mini-day {
    min-height: 38px;
    border-radius: 12px;
    font-size: 14px;
  }

  .agenda-item {
    grid-template-columns: 1fr;
  }

  .lesson-hover-preview {
    display: none;
  }
}
</style>
