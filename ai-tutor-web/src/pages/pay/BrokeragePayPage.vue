<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useToastStore } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const orderId = computed(() => {
  const raw = route.query.orderId
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

const applicationId = computed(() => {
  const raw = route.query.applicationId
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

function toCashier() {
  if (!orderId.value) {
    toast.show('缺少支付订单', 'error')
    void router.back()
    return
  }
  void router.replace({
    name: 'cashierPay',
    query: {
      contextType: 'BROKERAGE_ORDER',
      contextId: String(orderId.value),
      ...(applicationId.value ? { applicationId: String(applicationId.value) } : {}),
    },
  })
}

onMounted(() => {
  toCashier()
})
</script>

<template>
  <div class="page">
    <div class="wrap">
      <div class="card panel">
        <div class="title">正在进入支付页</div>
        <div class="sub">若未自动跳转，请点击下方按钮继续。</div>
        <button class="btn btn-primary" type="button" @click="toCashier">继续支付</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100%;
  display: grid;
  place-items: center;
  padding: 24px;
}

.wrap {
  width: min(420px, 100%);
}

.panel {
  padding: 22px;
  display: grid;
  gap: 12px;
  text-align: center;
}

.title {
  font-size: 20px;
  font-weight: 900;
}

.sub {
  color: var(--muted);
  font-size: 13px;
}
</style>
