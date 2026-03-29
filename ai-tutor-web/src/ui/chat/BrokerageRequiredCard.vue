<script setup lang="ts">
import { computed } from 'vue'

import type { ChatMessageBody } from '@/api/types'

const props = defineProps<{
  body: Extract<ChatMessageBody, { type: 'brokerage_required' }>
  canPay: boolean
}>()

const emit = defineEmits<{
  (e: 'pay'): void
}>()

const amountYuan = computed(() => {
  const fen = props.body.amountFen
  if (typeof fen !== 'number' || !Number.isFinite(fen)) return ''
  return (fen / 100).toFixed(2)
})

function statusText() {
  const s = typeof props.body.status === 'string' ? props.body.status.trim().toUpperCase() : ''
  if (s === 'PENDING') return '待支付'
  if (s === 'PROOF_SUBMITTED') return '待平台确认'
  if (s === 'PAID') return '已确认支付'
  if (s === 'REJECTED') return '已拒绝'
  if (s === 'CANCELED') return '已取消'
  return s || '未知状态'
}
</script>

<template>
  <div class="cardx">
    <div class="h1">信息费</div>
    <div v-if="canPay" class="row">
      <div class="k">金额</div>
      <div class="v">{{ amountYuan ? `${amountYuan} 元` : '待定' }}</div>
    </div>
    <div class="row">
      <div class="k">状态</div>
      <div class="v">{{ canPay ? statusText() : '待教师支付信息费' }}</div>
    </div>
    <div v-if="canPay" class="ops">
      <button class="btn btn-primary" type="button" @click="emit('pay')">去支付</button>
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
  font-weight: 800;
}

.ops {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
