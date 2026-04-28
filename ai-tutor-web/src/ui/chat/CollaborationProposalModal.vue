<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import type { ScheduleAvailabilityVO, ScheduleBusyBlockVO } from '@/api/schedule'
import { scheduleApi } from '@/api/schedule'

const TRIAL_FEE_TEXT = '试课费用为 1 小时课时费，请双方私下转账'

const props = defineProps<{
  open: boolean
  busy?: boolean
  error?: string | null
  title?: string
  submitText?: string
  otherUid?: number | null
  initial?: { pricePerHour: string; trialStartAt: number; trialEndAt: number; remark?: string | null } | null
}>()

const emit = defineEmits<{
  close: []
  submit: [{ pricePerHour: string; trialStartAt: number; trialEndAt: number; remark?: string }]
}>()

const trialStartAt = ref<number>(roundToNextHalfHour(Date.now()))
const trialEndAt = ref<number>(trialStartAt.value + 2 * 60 * 60 * 1000)
const remark = ref('')
const selectedDate = ref(toDateInputValue(trialStartAt.value))
const availability = ref<ScheduleAvailabilityVO | null>(null)
const availabilityLoading = ref(false)
const availabilityError = ref<string | null>(null)

watch(
  () => [props.open, props.initial] as const,
  ([open]) => {
    if (!open) return
    const init = props.initial
    if (init) {
      trialStartAt.value = init.trialStartAt
      trialEndAt.value = init.trialEndAt
      selectedDate.value = toDateInputValue(init.trialStartAt)
      remark.value = init.remark || ''
      void loadAvailability()
      return
    }
    const start = roundToNextHalfHour(Date.now())
    trialStartAt.value = start
    trialEndAt.value = start + 2 * 60 * 60 * 1000
    selectedDate.value = toDateInputValue(start)
    remark.value = ''
    void loadAvailability()
  },
  { immediate: true },
)

watch(
  () => props.otherUid,
  () => {
    if (props.open) void loadAvailability()
  },
)

const canSubmit = computed(() => {
  if (props.busy) return false
  return trialEndAt.value > trialStartAt.value && trialStartAt.value >= Date.now() - 60_000 && conflicts.value.length <= 0
})

const conflicts = computed(() => {
  const out: string[] = []
  if (!(trialEndAt.value > trialStartAt.value)) out.push('结束时间必须晚于开始时间')
  if (trialStartAt.value < Date.now() - 60_000) out.push('试课时间不能早于当前时间')
  const data = availability.value
  if (!data) return out
  if (hasConflict(data.myBusyBlocks, trialStartAt.value, trialEndAt.value)) out.push('与我的日程冲突')
  if (hasConflict(data.otherBusyBlocks, trialStartAt.value, trialEndAt.value)) out.push('与对方日程冲突')
  return out
})

function pad2(v: number) {
  return String(v).padStart(2, '0')
}

function roundToNextHalfHour(nowMs: number) {
  const d = new Date(nowMs)
  d.setSeconds(0, 0)
  const m = d.getMinutes()
  const next = m <= 0 ? 0 : m <= 30 ? 30 : 60
  d.setMinutes(next)
  if (next === 60) d.setHours(d.getHours() + 1, 0, 0, 0)
  return d.getTime()
}

function toLocalDateTimeInputValue(ms: number) {
  const d = new Date(ms)
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

function toDateInputValue(ms: number) {
  const d = new Date(ms)
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function parseDateInputValue(value: string) {
  const parts = value.split('-')
  const y = Number(parts[0] ?? '')
  const m = Number(parts[1] ?? '')
  const d = Number(parts[2] ?? '')
  if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return Date.now()
  return new Date(y, m - 1, d, 0, 0, 0, 0).getTime()
}

function parseLocalDateTimeInputValue(value: string) {
  const parsed = Date.parse(value)
  return Number.isFinite(parsed) ? parsed : Date.now()
}

function formatTime(ms: number) {
  const d = new Date(ms)
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

function applyStart(value: string) {
  const nextStart = parseLocalDateTimeInputValue(value)
  const duration = Math.max(30 * 60 * 1000, trialEndAt.value - trialStartAt.value)
  trialStartAt.value = nextStart
  if (!(trialEndAt.value > trialStartAt.value)) {
    trialEndAt.value = nextStart + duration
  }
}

function applyEnd(value: string) {
  trialEndAt.value = parseLocalDateTimeInputValue(value)
}

function applyDate(value: string) {
  selectedDate.value = value
  const day = new Date(parseDateInputValue(value))
  const start = new Date(trialStartAt.value)
  start.setFullYear(day.getFullYear(), day.getMonth(), day.getDate())
  trialStartAt.value = start.getTime()
  trialEndAt.value = trialStartAt.value + 2 * 60 * 60 * 1000
  void loadAvailability()
}

async function loadAvailability() {
  availability.value = null
  availabilityError.value = null
  if (!props.open || !props.otherUid) return
  availabilityLoading.value = true
  try {
    availability.value = await scheduleApi.dayAvailability({
      otherUid: props.otherUid,
      dateAt: parseDateInputValue(selectedDate.value),
    })
  } catch (e) {
    availabilityError.value = e instanceof Error ? e.message : '读取双方日程失败'
  } finally {
    availabilityLoading.value = false
  }
}

function pickTime(hour: number) {
  const base = new Date(parseDateInputValue(selectedDate.value))
  base.setHours(hour, 0, 0, 0)
  trialStartAt.value = base.getTime()
  trialEndAt.value = trialStartAt.value + 2 * 60 * 60 * 1000
}

function hasConflict(blocks: ScheduleBusyBlockVO[] | null | undefined, start: number, end: number) {
  return (blocks || []).some((block) => block.startAt < end && block.endAt > start)
}

function blockStyle(block: ScheduleBusyBlockVO) {
  const start = new Date(block.startAt)
  const end = new Date(block.endAt)
  const top = ((start.getHours() * 60 + start.getMinutes()) / (24 * 60)) * 100
  const height = Math.max(3, ((end.getTime() - start.getTime()) / 60_000 / (24 * 60)) * 100)
  return { top: `${top}%`, height: `${height}%` }
}

function selectedStyle() {
  const start = new Date(trialStartAt.value)
  const height = ((trialEndAt.value - trialStartAt.value) / 60_000 / (24 * 60)) * 100
  const top = ((start.getHours() * 60 + start.getMinutes()) / (24 * 60)) * 100
  return { top: `${top}%`, height: `${Math.max(3, height)}%` }
}

const hours = Array.from({ length: 15 }, (_, i) => i + 8)

function close() {
  if (props.busy) return
  emit('close')
}

function submit() {
  if (!canSubmit.value) return
  emit('submit', {
    pricePerHour: TRIAL_FEE_TEXT,
    trialStartAt: trialStartAt.value,
    trialEndAt: trialEndAt.value,
    remark: remark.value.trim() || undefined,
  })
}
</script>

<template>
  <div v-if="open" class="mask" @click.self="close">
    <div class="modal card">
        <div class="m-head">
          <div class="head-copy">
            <div class="title">{{ title || '发起合作' }}</div>
          <div class="subtitle">先确认试课时间。平台暂不代收课时费，试课费用按 1 小时课时费由双方私下结算。</div>
          </div>
          <button class="icon-btn" type="button" :disabled="busy" @click="close">×</button>
      </div>

      <div class="modal-body">
        <div class="content-grid">
          <section class="panel form-panel">
            <div class="panel-title">试课信息</div>
            <div class="form">
              <div class="fee-policy">
                <div class="fee-policy-title">试课费用</div>
                <div class="fee-policy-main">统一按 1 小时课时费计算</div>
                <div class="fee-policy-desc">实际试课一般可安排 2 小时；平台现阶段不直接收取试课或正式课费用，请双方确认后私下转账。</div>
              </div>
              <label class="field">
                <div class="k">试课日期（北京时间）</div>
                <input class="input" type="date" :value="selectedDate" :disabled="busy" @input="applyDate(($event.target as HTMLInputElement).value)" />
              </label>
              <div class="datetime-row">
                <label class="field">
                  <div class="k">试课开始时间</div>
                  <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(trialStartAt)" :disabled="busy" @input="applyStart(($event.target as HTMLInputElement).value)" />
                </label>
                <label class="field">
                  <div class="k">试课结束时间</div>
                  <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(trialEndAt)" :disabled="busy" @input="applyEnd(($event.target as HTMLInputElement).value)" />
                </label>
              </div>
              <div class="summary-card">
                <div class="summary-row">
                  <span>费用规则</span>
                  <strong>按 1 小时课时费私下结算</strong>
                </div>
                <div class="summary-row">
                  <span>当前选择</span>
                  <strong>{{ formatTime(trialStartAt) }} - {{ formatTime(trialEndAt) }}</strong>
                </div>
                <div class="summary-row">
                  <span>状态</span>
                  <strong :class="conflicts.length ? 'danger-text' : 'ok-text'">{{ conflicts.length ? '存在冲突' : '可发起试课' }}</strong>
                </div>
              </div>
              <label class="field">
                <div class="k">备注</div>
                <textarea v-model="remark" class="textarea" placeholder="例如：先试讲一次，课后再由学生确认后续固定时间" :disabled="busy" />
              </label>
            </div>
          </section>

          <section class="panel calendar-panel">
            <div class="panel-head">
              <div>
                <div class="panel-title">双方当天日程</div>
                <div class="calendar-hint">左侧点击空白时段可快速生成试课块，灰色表示已占用。</div>
              </div>
              <div class="date-chip">{{ selectedDate.split('-').join(' / ') }}</div>
            </div>

            <div v-if="availabilityLoading" class="hint">读取双方日程中...</div>
            <div v-else-if="availabilityError" class="hint error">{{ availabilityError }}</div>
            <div v-else class="dual-calendar">
              <div class="calendar-col mine-col">
                <div class="col-title">我的日程</div>
                <div class="day-grid">
                  <button v-for="h in hours" :key="`mine-${h}`" class="hour-line" type="button" :style="{ top: `${(h / 24) * 100}%` }" @click="pickTime(h)">
                    {{ pad2(h) }}:00
                  </button>
                  <div v-for="block in availability?.myBusyBlocks || []" :key="`m-${block.eventId}`" class="busy-block" :style="blockStyle(block)">
                    {{ block.title || '已占用' }}
                  </div>
                  <div class="selected-block" :class="{ invalid: conflicts.length > 0 }" :style="selectedStyle()">试课 {{ formatTime(trialStartAt) }} - {{ formatTime(trialEndAt) }}</div>
                </div>
              </div>
              <div class="calendar-col">
                <div class="col-title">对方日程</div>
                <div class="day-grid muted-grid">
                  <div v-for="h in hours" :key="`peer-${h}`" class="hour-readonly" :style="{ top: `${(h / 24) * 100}%` }">{{ pad2(h) }}:00</div>
                  <div v-for="block in availability?.otherBusyBlocks || []" :key="`o-${block.eventId}`" class="busy-block peer" :style="blockStyle(block)">
                    {{ block.title || '已占用' }}
                  </div>
                </div>
              </div>
            </div>

            <div v-if="conflicts.length" class="hint error">{{ conflicts.join('，') }}</div>
          </section>
        </div>
      </div>

      <div class="footer">
        <div class="footer-status">
          <div v-if="busy" class="hint">提交中...</div>
          <div v-else-if="error" class="hint error">{{ error }}</div>
          <div v-else class="hint">确认无冲突后即可发送合作提案。</div>
        </div>

        <div class="ops">
          <button class="btn" type="button" :disabled="busy" @click="close">取消</button>
          <button class="btn btn-primary" type="button" :disabled="!canSubmit" @click="submit">{{ submitText || '发送提案' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(circle at top, rgba(15, 118, 110, 0.12), transparent 32%),
    rgba(15, 23, 42, 0.42);
  display: grid;
  place-items: center;
  padding: 20px;
  z-index: 60;
  backdrop-filter: blur(8px);
}

.modal {
  width: min(1100px, 100%);
  max-height: calc(100vh - 40px);
  padding: 22px;
  border-radius: 24px;
  display: grid;
  gap: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 252, 0.96));
  box-shadow:
    0 28px 80px rgba(15, 23, 42, 0.18),
    0 4px 20px rgba(15, 23, 42, 0.08);
}

.m-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.head-copy {
  display: grid;
  gap: 6px;
}

.title {
  font-weight: 900;
  font-size: 28px;
  line-height: 1.05;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
}

.icon-btn {
  width: 46px;
  height: 46px;
  border-radius: 15px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.modal-body {
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(420px, 1.1fr);
  gap: 18px;
  align-items: start;
}

.panel {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.form-panel {
  position: sticky;
  top: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  font-size: 14px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.01em;
}

.date-chip {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(15, 118, 110, 0.08);
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
}

.field {
  display: grid;
  gap: 7px;
}

.form {
  display: grid;
  gap: 14px;
}

.fee-policy {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(15, 118, 110, 0.16);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(240, 253, 250, 0.95), rgba(255, 255, 255, 0.96));
}

.fee-policy-title {
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.fee-policy-main {
  color: #0f172a;
  font-size: 17px;
  font-weight: 900;
  line-height: 1.35;
}

.fee-policy-desc {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.datetime-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.summary-card {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.07), rgba(14, 165, 233, 0.05));
  border: 1px solid rgba(15, 118, 110, 0.12);
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.summary-row span {
  color: #64748b;
}

.summary-row strong {
  color: #0f172a;
}

.ok-text {
  color: #0f766e !important;
}

.danger-text {
  color: #b91c1c !important;
}

.k {
  font-size: 12px;
  color: #475569;
  font-weight: 700;
}

.input {
  height: 46px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  padding: 0 14px;
  outline: none;
  background: rgba(255, 255, 255, 0.96);
  font-size: 15px;
  color: #0f172a;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.textarea {
  min-height: 124px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  padding: 12px 14px;
  outline: none;
  background: rgba(255, 255, 255, 0.96);
  resize: vertical;
  font-size: 14px;
  line-height: 1.6;
  color: #0f172a;
}

.calendar-hint {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.dual-calendar {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
}

.calendar-col + .calendar-col {
  border-left: 1px solid rgba(148, 163, 184, 0.16);
}

.mine-col {
  background: linear-gradient(180deg, rgba(240, 253, 250, 0.92), rgba(255, 255, 255, 0.96));
}

.col-title {
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 900;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  color: #0f172a;
  background: rgba(248, 250, 252, 0.96);
}

.day-grid {
  position: relative;
  height: 480px;
  background: linear-gradient(to bottom, transparent calc(100% / 24 - 1px), rgba(148, 163, 184, 0.16) calc(100% / 24));
  background-size: 100% calc(100% / 24);
}

.muted-grid {
  background-color: rgba(248, 250, 252, 0.72);
}

.hour-line,
.hour-readonly {
  position: absolute;
  left: 0;
  right: 0;
  height: 20px;
  transform: translateY(-10px);
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  text-align: left;
  padding-left: 12px;
}

.hour-line {
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}

.hour-line:hover {
  color: #0f766e;
  background: linear-gradient(90deg, rgba(15, 118, 110, 0.08), transparent 45%);
}

.busy-block,
.selected-block {
  position: absolute;
  left: 64px;
  right: 10px;
  border-radius: 14px;
  padding: 8px 10px;
  font-size: 11px;
  overflow: hidden;
  line-height: 1.4;
}

.busy-block {
  background: rgba(148, 163, 184, 0.22);
  color: #475569;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.busy-block.peer {
  background: rgba(203, 213, 225, 0.42);
}

.selected-block {
  border: 1px solid rgba(15, 118, 110, 0.5);
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.16), rgba(20, 184, 166, 0.12));
  color: #0f766e;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.14);
}

.selected-block.invalid {
  border-color: rgba(220, 38, 38, 0.72);
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.12), rgba(248, 113, 113, 0.08));
  color: #b91c1c;
  box-shadow: none;
}

.input:focus,
.textarea:focus {
  border-color: rgba(15, 118, 110, 0.52);
  box-shadow: 0 0 0 4px rgba(20, 184, 166, 0.12);
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 4px;
}

.footer-status {
  min-width: 0;
}

.ops {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex: 0 0 auto;
}

.hint {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.hint.error {
  color: #b91c1c;
}

@media (max-width: 980px) {
  .modal {
    width: min(760px, 100%);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .form-panel {
    position: static;
  }
}

@media (max-width: 720px) {
  .mask {
    padding: 10px;
  }

  .modal {
    max-height: calc(100vh - 20px);
    padding: 16px;
    border-radius: 20px;
  }

  .title {
    font-size: 24px;
  }

  .datetime-row,
  .dual-calendar,
  .footer {
    grid-template-columns: 1fr;
    display: grid;
  }

  .footer {
    gap: 12px;
  }

  .ops {
    width: 100%;
  }

  .ops .btn {
    flex: 1 1 0;
  }

  .day-grid {
    height: 360px;
  }
}
</style>
