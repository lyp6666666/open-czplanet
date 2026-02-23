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
}>()

const statusText = computed(() => toStatusText(props.body.status))

function toStatusText(s: CollaborationProposalStatus) {
  if (s === 'PENDING') return props.fromMe ? '等待对方确认' : '待你确认'
  if (s === 'ACCEPTED') return '已同意'
  if (s === 'REJECTED') return '已拒绝'
  return '状态未知'
}
</script>

<template>
  <div class="cardx" :class="body.status.toLowerCase()">
    <div class="h1">家教合作</div>
    <div class="row">
      <div class="k">收费</div>
      <div class="v">{{ body.pricePerHour }}</div>
    </div>
    <div class="row">
      <div class="k">时间</div>
      <div class="v">{{ body.classTime }}</div>
    </div>
    <div class="row">
      <div class="k">频次</div>
      <div class="v">每周 {{ body.frequencyPerWeek }} 次</div>
    </div>
    <div class="row">
      <div class="k">状态</div>
      <div class="v">{{ statusText }}</div>
    </div>

    <div v-if="!fromMe && body.status === 'PENDING'" class="ops">
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
