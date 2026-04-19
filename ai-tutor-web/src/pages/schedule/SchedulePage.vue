<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { liveApi, type LiveSessionResp } from '@/api/live'
import { scheduleApi } from '@/api/schedule'
import type { ScheduleEventVO, UserSimpleVO } from '@/api/types'

type ViewMode = 'month' | 'week' | 'day'

const view = ref<ViewMode>('month')
const cursorDate = ref(new Date())
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const events = ref<ScheduleEventVO[]>([])
const liveMap = ref<Record<number, LiveSessionResp>>({})

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
        try {
          const live = await liveApi.getByCourse(it.id)
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

function openLivePrepare(eventId: number) {
  void router.push({ name: 'livePrepare', params: { courseId: String(eventId) } })
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
    <div class="head card">
      <div class="left">
        <button class="btn" type="button" @click="onToday">今天</button>
        <button class="btn" type="button" @click="onPrev">‹</button>
        <button class="btn" type="button" @click="onNext">›</button>
        <div class="title">{{ formatDate(cursorDate) }}</div>
      </div>
      <div class="right">
        <div class="seg">
          <button class="seg-btn" :class="{ on: view === 'day' }" type="button" @click="view = 'day'">日</button>
          <button class="seg-btn" :class="{ on: view === 'week' }" type="button" @click="view = 'week'">周</button>
          <button class="seg-btn" :class="{ on: view === 'month' }" type="button" @click="view = 'month'">月</button>
        </div>
        <button class="btn btn-primary" type="button" @click="openCreate()">创建日程</button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="view === 'month'" class="card cal">
      <div class="week-head">
        <div class="w">一</div>
        <div class="w">二</div>
        <div class="w">三</div>
        <div class="w">四</div>
        <div class="w">五</div>
        <div class="w">六</div>
        <div class="w">日</div>
      </div>
      <div class="grid">
        <div
          v-for="d in monthDays"
          :key="d.date.toISOString()"
          class="cell"
          :class="{ dim: !d.inMonth }"
          @click="cursorDate = d.date; view = 'day'"
        >
          <div class="day-num">{{ d.date.getDate() }}</div>
          <div class="items">
            <div v-for="ev in eventsOnDay(d.date).slice(0, 3)" :key="ev.id" class="item" :title="ev.title">
              <span class="t">{{ formatHm(ev.startAt) }}</span>
              <span class="n">{{ ev.title }}</span>
              <span v-if="liveStatusLabel(ev.id)" class="live-flag">{{ liveStatusLabel(ev.id) }}</span>
            </div>
            <div v-if="eventsOnDay(d.date).length > 3" class="more">+{{ eventsOnDay(d.date).length - 3 }}</div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card cal">
      <div class="time-head">
        <div class="time-col" />
        <div class="days" :style="{ gridTemplateColumns: view === 'week' ? 'repeat(7, 1fr)' : '1fr' }">
          <div v-for="d in (view === 'week' ? weekDays : [startOfDay(cursorDate)])" :key="d.toISOString()" class="day">
            <div class="day-title">{{ d.getMonth() + 1 }}/{{ d.getDate() }}</div>
          </div>
        </div>
      </div>
      <div class="time-body">
        <div class="time-col">
          <div v-for="h in DAY_END_HOUR - DAY_START_HOUR" :key="h" class="hour">{{ String(h + DAY_START_HOUR).padStart(2, '0') }}:00</div>
        </div>
        <div class="days" :style="{ gridTemplateColumns: view === 'week' ? 'repeat(7, 1fr)' : '1fr' }">
          <div
            v-for="d in (view === 'week' ? weekDays : [startOfDay(cursorDate)])"
            :key="d.toISOString()"
            class="day-col"
            @dblclick="openCreate(d)"
          >
            <div class="slots">
              <div v-for="h in DAY_END_HOUR - DAY_START_HOUR" :key="h" class="slot" />
            </div>
            <div class="events">
              <div
                v-for="p in layoutDayEvents(events, d)"
                :key="p.event.id"
                class="event"
                :class="p.event.status.toLowerCase()"
                :style="{ top: `${p.top}px`, height: `${p.height}px`, left: `${p.leftPct}%`, width: `${p.widthPct}%` }"
                @click.stop="liveMap[p.event.id] ? openLivePrepare(p.event.id) : void 0"
              >
                <div class="et">{{ p.event.title }}</div>
                <div class="es">{{ formatHm(p.event.startAt) }}-{{ formatHm(p.event.endAt) }}</div>
                <div v-if="liveStatusLabel(p.event.id)" class="elive">{{ liveStatusLabel(p.event.id) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="createOpen" class="mask" @click.self="createOpen = false">
      <div class="modal card">
        <div class="m-title">创建日程</div>
        <div v-if="createError" class="m-error">{{ createError }}</div>

        <div class="field">
          <div class="lab">课程名称</div>
          <input v-model="title" class="ipt" placeholder="例如：初二数学｜一次函数强化" />
        </div>

        <div class="field">
          <div class="lab">授课对象</div>
          <div class="pick">
            <div v-if="participant" class="picked">
              <span class="pname">{{ participant.name }}</span>
              <button class="btn small" type="button" @click="participant = null">更换</button>
            </div>
            <div v-else class="picker">
              <div class="row">
                <input v-model="contactSearch" class="ipt" placeholder="搜索昵称/手机号" />
                <button class="btn small" type="button" :disabled="contactLoading" @click="searchContacts">搜索</button>
              </div>
              <div v-if="contactError" class="m-error">{{ contactError }}</div>
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
                <div v-if="!contactLoading && contacts.length === 0" class="empty">暂无联系人</div>
              </div>
            </div>
          </div>
        </div>

        <div class="field">
          <div class="lab">开始时间</div>
          <input
            class="ipt"
            type="datetime-local"
            :value="new Date(startAt).toISOString().slice(0, 16)"
            @change="startAt = new Date(($event.target as HTMLInputElement).value).getTime()"
          />
        </div>

        <div class="field">
          <div class="lab">结束时间</div>
          <input
            class="ipt"
            type="datetime-local"
            :value="new Date(endAt).toISOString().slice(0, 16)"
            @change="endAt = new Date(($event.target as HTMLInputElement).value).getTime()"
          />
        </div>

        <div class="field">
          <div class="lab">备注</div>
          <textarea v-model="description" class="txt" rows="3" placeholder="可选" />
        </div>

        <div class="m-ops">
          <button class="btn" type="button" :disabled="createBusy" @click="createOpen = false">取消</button>
          <button class="btn btn-primary" type="button" :disabled="createBusy" @click="submitCreate">
            {{ createBusy ? '创建中...' : '创建并发送申请' }}
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
  gap: 12px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.title {
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.seg {
  display: inline-flex;
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

.seg-btn {
  border: none;
  background: #fff;
  padding: 8px 10px;
  cursor: pointer;
}

.seg-btn.on {
  background: rgba(0, 190, 189, 0.12);
  color: var(--primary);
  font-weight: 700;
}

.cal {
  padding: 12px;
}

.week-head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  color: var(--muted);
  font-weight: 600;
  margin-bottom: 8px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.cell {
  border: 1px solid var(--border);
  border-radius: 12px;
  min-height: 104px;
  padding: 8px;
  cursor: pointer;
  background: #fff;
}

.cell.dim {
  opacity: 0.55;
}

.day-num {
  font-weight: 700;
  font-size: 12px;
  color: var(--muted);
}

.items {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item {
  display: flex;
  gap: 6px;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item .t {
  color: var(--muted);
  flex: 0 0 auto;
}

.item .n {
  overflow: hidden;
  text-overflow: ellipsis;
}

.more {
  font-size: 12px;
  color: var(--muted);
}

.live-flag {
  margin-left: auto;
  flex: 0 0 auto;
  color: #0f766e;
  font-weight: 700;
}

.time-head {
  display: grid;
  grid-template-columns: 60px 1fr;
  align-items: end;
  border-bottom: 1px solid var(--border);
  padding-bottom: 8px;
}

.time-body {
  display: grid;
  grid-template-columns: 60px 1fr;
  margin-top: 8px;
}

.time-col {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.hour {
  height: 72px;
  font-size: 12px;
  color: var(--muted);
}

.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day {
  text-align: center;
  font-weight: 700;
}

.day-col {
  position: relative;
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  height: 1296px;
}

.slots {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
}

.slot {
  height: 72px;
  border-top: 1px dashed rgba(0, 0, 0, 0.06);
}

.events {
  position: absolute;
  inset: 0;
}

.event {
  position: absolute;
  padding: 6px;
  border-radius: 10px;
  box-sizing: border-box;
  overflow: hidden;
  background: rgba(0, 190, 189, 0.15);
  border: 1px solid rgba(0, 190, 189, 0.35);
}

.event.pending {
  background: rgba(255, 153, 0, 0.14);
  border-color: rgba(255, 153, 0, 0.35);
}

.event.canceled,
.event.rejected {
  opacity: 0.5;
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
  color: var(--muted);
}

.elive {
  margin-top: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #0f766e;
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
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
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
  border: 1px solid var(--border);
  border-radius: 10px;
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

@media (max-width: 900px) {
  .days {
    grid-template-columns: 1fr;
  }
}
</style>
