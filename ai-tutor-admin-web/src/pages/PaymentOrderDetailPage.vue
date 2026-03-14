<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">付款记录详情</div>
        <div class="sub">{{ orderNo }}</div>
      </div>
      <div class="right">
        <button class="btn btn-muted" type="button" @click="back">返回</button>
        <button class="btn" type="button" :disabled="loading" @click="load">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div v-if="row" class="grid">
      <div class="card panel">
        <div class="row"><span class="k">订单号</span><span class="v mono">{{ row.orderNo }}</span></div>
        <div class="row"><span class="k">用户ID</span><span class="v">{{ row.userId }}</span></div>
        <div class="row"><span class="k">金额</span><span class="v">¥{{ (row.amount / 100).toFixed(2) }}</span></div>
        <div class="row"><span class="k">渠道</span><span class="v">{{ row.channel }}</span></div>
        <div class="row"><span class="k">状态</span><span class="v">{{ row.status }}</span></div>
        <div class="row"><span class="k">业务关联</span><span class="v">{{ row.contextType }} · {{ row.contextId }}</span></div>
      </div>

      <div class="card panel">
        <div class="row"><span class="k">交易号</span><span class="v mono">{{ row.transactionId || '-' }}</span></div>
        <div class="row"><span class="k">第三方单号</span><span class="v mono">{{ row.providerOrderNo || '-' }}</span></div>
        <div class="row"><span class="k">回调次数</span><span class="v">{{ row.notifyCount ?? 0 }}</span></div>
        <div class="row"><span class="k">验签通过</span><span class="v">{{ row.notifyVerified === 1 ? '是' : '否' }}</span></div>
        <div class="row"><span class="k">最后回调</span><span class="v">{{ timeText(row.lastNotifyTime) }}</span></div>
        <div class="row"><span class="k">支付时间</span><span class="v">{{ timeText(row.successTime) }}</span></div>
        <div class="row"><span class="k">过期时间</span><span class="v">{{ timeText(row.expireTime) }}</span></div>
      </div>

      <div class="card panel full">
        <div class="row"><span class="k">标题</span><span class="v">{{ row.subject }}</span></div>
        <div class="row"><span class="k">描述</span><span class="v">{{ row.body || '-' }}</span></div>
        <div class="row"><span class="k">客户端IP</span><span class="v mono">{{ row.clientIp || '-' }}</span></div>
        <div class="row"><span class="k">创建时间</span><span class="v">{{ timeText(row.createTime) }}</span></div>
        <div class="row"><span class="k">更新时间</span><span class="v">{{ timeText(row.updateTime) }}</span></div>
      </div>

      <div class="card panel full">
        <div class="k">支付要素 payData</div>
        <pre class="pre">{{ pretty(row.payData) }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getPaymentOrderDetail, type PaymentOrderRecord } from '@/api/paymentOrders'

const route = useRoute()
const router = useRouter()

const orderNo = computed(() => (typeof route.params.orderNo === 'string' ? route.params.orderNo.trim() : ''))
const row = ref<PaymentOrderRecord | null>(null)
const loading = ref(false)
const errorText = ref<string | null>(null)

function timeText(s?: string | null) {
  if (!s) return '-'
  return String(s).replace('T', ' ').slice(0, 19)
}

function pretty(s: string | null) {
  if (!s) return '-'
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}

async function load() {
  if (!orderNo.value) return
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    row.value = await getPaymentOrderDetail(orderNo.value)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
    row.value = null
  } finally {
    loading.value = false
  }
}

function back() {
  router.back()
}

onMounted(load)
</script>

<style scoped>
.box {
  padding: 14px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.title {
  font-weight: 800;
}

.sub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.panel {
  padding: 12px;
  display: grid;
  gap: 8px;
}

.panel.full {
  grid-column: 1 / -1;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.k {
  color: var(--muted);
  font-weight: 700;
}

.v {
  color: var(--text);
  font-weight: 800;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.pre {
  margin: 0;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(15, 23, 42, 0.03);
  overflow: auto;
  max-height: 420px;
}

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

