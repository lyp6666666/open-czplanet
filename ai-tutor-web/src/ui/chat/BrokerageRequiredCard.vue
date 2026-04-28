<script setup lang="ts">
import { computed } from 'vue'

import type { ChatMessageBody } from '@/api/types'

const props = defineProps<{
  body: Extract<ChatMessageBody, { type: 'brokerage_required' }>
  canPay: boolean
  viewerRole: 'teacher' | 'student'
}>()

const emit = defineEmits<{
  (e: 'pay'): void
  (e: 'openPolicy'): void
}>()

const amountYuan = computed(() => {
  const fen = props.body.amountFen
  if (typeof fen !== 'number' || !Number.isFinite(fen)) return ''
  return (fen / 100).toFixed(2)
})

const statusCode = computed(() => {
  return typeof props.body.status === 'string' ? props.body.status.trim().toUpperCase() : ''
})

const isPaid = computed(() => statusCode.value === 'PAID')

const showAmount = computed(() => props.canPay || isPaid.value)

const showPayButton = computed(() => props.canPay && (statusCode.value === '' || statusCode.value === 'PENDING'))

const descText = computed(() =>
  props.viewerRole === 'teacher'
    ? '支付后可继续确认详细需求与合作安排。'
    : '教师支付后，双方可继续确认详细需求与合作安排。',
)

const statusToneClass = computed(() => {
  if (isPaid.value) return 'tone-paid'
  if (statusCode.value === 'PROOF_SUBMITTED') return 'tone-review'
  return 'tone-pending'
})

function statusText() {
  const s = statusCode.value
  if (s === 'PENDING') return '待支付'
  if (s === 'PROOF_SUBMITTED') return '待平台确认'
  if (s === 'PAID') return '已支付'
  if (s === 'REJECTED') return '已拒绝'
  if (s === 'CANCELED') return '已取消'
  return s || '未知状态'
}
</script>

<template>
  <div class="cardx" :class="{ paid: isPaid }">
    <div class="head">
      <div class="title-group">
        <div class="h1">信息费</div>
        <button class="policy-link" type="button" @click="emit('openPolicy')">为什么先收费</button>
      </div>
      <div class="status-tag" :class="statusToneClass">{{ canPay || isPaid ? statusText() : '待教师支付信息费' }}</div>
    </div>
    <div class="lead">{{ descText }}</div>
    <div v-if="showAmount" class="row">
      <div class="k">金额</div>
      <div class="v">{{ amountYuan ? `${amountYuan} 元` : '待定' }}</div>
    </div>
    <div class="row">
      <div class="k">状态</div>
      <div class="v">{{ canPay || isPaid ? statusText() : '待教师支付信息费' }}</div>
    </div>
    <div v-if="showPayButton" class="ops">
      <button class="btn btn-primary" type="button" @click="emit('pay')">去支付</button>
    </div>
    <div v-else-if="isPaid" class="success-hint">平台已确认信息费支付，当前会话已进入可沟通状态。</div>
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
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.cardx.paid {
  border-color: rgba(17, 185, 129, 0.28);
  background: linear-gradient(180deg, rgba(17, 185, 129, 0.09), rgba(255, 255, 255, 0.96));
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-group {
  display: grid;
  gap: 4px;
}

.h1 {
  font-weight: 900;
}

.policy-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.lead {
  margin-top: 8px;
  color: #52636d;
  font-size: 12px;
  line-height: 1.6;
  font-weight: 700;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.tone-pending {
  background: rgba(14, 165, 233, 0.1);
  color: #0369a1;
}

.tone-review {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.tone-paid {
  background: rgba(17, 185, 129, 0.14);
  color: #047857;
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

.success-hint {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: #047857;
  font-weight: 700;
}
</style>
