<script setup lang="ts">
import { computed } from 'vue'

import type { ChatMessageBody, ScheduleEventStatus } from '@/api/types'

/**
 * 授课申请卡片（聊天内）。
 *
 * 说明：本组件只负责展示与触发操作，具体的 API 调用由父组件负责。
 */
const props = defineProps<{
  body: Extract<ChatMessageBody, { type: 'lesson_request' }>
  fromMe: boolean
  busy?: boolean
}>()

const emit = defineEmits<{
  (e: 'accept'): void
  (e: 'reject'): void
}>()

const timeText = computed(() => {
  const s = new Date(props.body.startAt)
  const e = new Date(props.body.endAt)
  const pad = (n: number) => String(n).padStart(2, '0')
  const hhmm = (d: Date) => `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const date = `${s.getFullYear()}-${pad(s.getMonth() + 1)}-${pad(s.getDate())}`
  return `${date} ${hhmm(s)}-${hhmm(e)}`
})

const statusText = computed(() => toStatusText(props.body.status))

function toStatusText(s: ScheduleEventStatus) {
  if (s === 'PENDING') return props.fromMe ? '等待对方确认' : '待你确认'
  if (s === 'ACCEPTED') return '已确认'
  if (s === 'REJECTED') return '已拒绝'
  if (s === 'CANCELED') return '已取消'
  return '状态未知'
}
</script>

<template>
  <div class="cardx" :class="body.status.toLowerCase()">
    <div class="h1">授课申请</div>
    <div class="row">
      <div class="k">课程</div>
      <div class="v">{{ body.title }}</div>
    </div>
    <div class="row">
      <div class="k">时间</div>
      <div class="v">{{ timeText }}</div>
    </div>
    <div class="row">
      <div class="k">状态</div>
      <div class="v">{{ statusText }}</div>
    </div>

    <div v-if="!fromMe && body.status === 'PENDING'" class="ops">
      <button class="btn btn-primary" type="button" :disabled="busy" @click="emit('accept')">{{ busy ? '处理中...' : '接收' }}</button>
      <button class="btn" type="button" :disabled="busy" @click="emit('reject')">拒绝</button>
    </div>
  </div>
</template>

<style scoped>
.cardx {
  min-width: 260px;
  max-width: 360px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 10px 10px 12px;
  background: #fff;
}

.h1 {
  font-weight: 900;
  margin-bottom: 8px;
}

.row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  margin-top: 6px;
}

.k {
  width: 40px;
  color: var(--muted);
  flex: 0 0 auto;
}

.v {
  flex: 1 1 auto;
  word-break: break-word;
}

.ops {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.pending {
  border-color: rgba(255, 153, 0, 0.35);
  background: rgba(255, 153, 0, 0.06);
}
</style>

