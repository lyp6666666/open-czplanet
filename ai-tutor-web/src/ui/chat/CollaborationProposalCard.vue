<script setup lang="ts">
import { computed } from 'vue'

import type { ChatMessageBody, CollaborationProposalStatus } from '@/api/types'

const props = defineProps<{
  body: Extract<ChatMessageBody, { type: 'collaboration_proposal' }>
  fromMe: boolean
  busy?: boolean
}>()

const emit = defineEmits<{
  (e: 'accept'): void
  (e: 'reject'): void
  (e: 'edit', payload: { proposalId: number; pricePerHour: string; trialStartAt: number; trialEndAt: number; remark?: string | null }): void
}>()

const statusText = computed(() => toStatusText(props.body.status))
const trialTimeText = computed(() => {
  const start = props.body.trialStartAt
  const end = props.body.trialEndAt
  if (typeof start === 'number' && typeof end === 'number' && Number.isFinite(start) && Number.isFinite(end)) {
    return `${formatDateTime(start)} - ${formatTime(end)}`
  }
  return props.body.classTime || '待确认'
})
const expireText = computed(() => {
  const expireAt = props.body.expireAt
  if (typeof expireAt !== 'number' || !Number.isFinite(expireAt)) return ''
  return formatDateTime(expireAt)
})

function toStatusText(s: CollaborationProposalStatus) {
  if (s === 'PENDING') return props.fromMe ? '等待对方确认' : '待你确认'
  if (s === 'ACCEPTED') return '已同意'
  if (s === 'REJECTED') return '已拒绝'
  return '状态未知'
}

function pad2(v: number) {
  return String(v).padStart(2, '0')
}

function formatDateTime(ms: number) {
  const d = new Date(ms)
  return `${d.getFullYear()}/${pad2(d.getMonth() + 1)}/${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

function formatTime(ms: number) {
  const d = new Date(ms)
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}
</script>

<template>
  <div class="cardx" :class="body.status.toLowerCase()">
    <div class="h1">试课合作</div>
    <div class="row">
      <div class="k">收费</div>
      <div class="v">{{ body.pricePerHour }}</div>
    </div>
    <div class="row">
      <div class="k">试课</div>
      <div class="v">{{ trialTimeText }}</div>
    </div>
    <div v-if="body.remark" class="row">
      <div class="k">备注</div>
      <div class="v">{{ body.remark }}</div>
    </div>
    <div class="row">
      <div class="k">状态</div>
      <div class="v">{{ statusText }}</div>
    </div>
    <div v-if="body.status === 'PENDING' && expireText" class="row">
      <div class="k">有效期</div>
      <div class="v">{{ expireText }} 前确认</div>
    </div>

    <div v-if="fromMe && body.status === 'PENDING'" class="ops">
      <button
        class="btn"
        type="button"
        :disabled="busy"
        @click="emit('edit', { proposalId: body.proposalId, pricePerHour: body.pricePerHour, trialStartAt: body.trialStartAt || Date.now(), trialEndAt: body.trialEndAt || Date.now() + 2 * 60 * 60 * 1000, remark: body.remark })"
      >
        修改提案
      </button>
    </div>

    <div v-else-if="!fromMe && body.status === 'PENDING'" class="ops">
      <button class="btn btn-primary" type="button" :disabled="busy" @click="emit('accept')">{{ busy ? '处理中...' : '同意' }}</button>
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

.accepted {
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.06);
}

.rejected {
  border-color: rgba(255, 77, 79, 0.25);
  background: rgba(255, 77, 79, 0.05);
}
</style>
