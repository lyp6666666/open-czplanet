<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { brokerageApi, type BrokerageOrderStatus, type BrokeragePayMethod, type BrokerageOrderVO } from '@/api/brokerage'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => {
  const raw = route.query.orderId
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

const loading = ref(false)
const error = ref<string | null>(null)
const order = ref<BrokerageOrderVO | null>(null)

const payMethod = ref<BrokeragePayMethod>('WECHAT')
const proofUrl = ref('')
const proofNote = ref('')
const submitBusy = ref(false)
const submitError = ref<string | null>(null)

const wechatQrUrl = computed(() => {
  const s = typeof import.meta.env.VITE_BROKERAGE_WECHAT_QR_URL === 'string' ? import.meta.env.VITE_BROKERAGE_WECHAT_QR_URL.trim() : ''
  return s || ''
})
const alipayQrUrl = computed(() => {
  const s = typeof import.meta.env.VITE_BROKERAGE_ALIPAY_QR_URL === 'string' ? import.meta.env.VITE_BROKERAGE_ALIPAY_QR_URL.trim() : ''
  return s || ''
})

const amountYuan = computed(() => {
  const fen = order.value?.amountFen
  if (typeof fen !== 'number' || !Number.isFinite(fen)) return ''
  return (fen / 100).toFixed(2)
})

function statusText(s: BrokerageOrderStatus | null | undefined) {
  if (s === 'PENDING') return '待支付'
  if (s === 'PROOF_SUBMITTED') return '待平台确认'
  if (s === 'PAID') return '已确认支付'
  if (s === 'REJECTED') return '已拒绝'
  if (s === 'CANCELED') return '已取消'
  return '未知状态'
}

const qrUrl = computed(() => {
  if (payMethod.value === 'ALIPAY') return alipayQrUrl.value
  return wechatQrUrl.value
})

async function load() {
  if (!orderId.value) {
    error.value = '缺少订单号'
    order.value = null
    return
  }
  loading.value = true
  error.value = null
  try {
    order.value = await brokerageApi.getOrder(orderId.value)
    const pm = typeof order.value?.payMethod === 'string' ? order.value.payMethod.trim().toUpperCase() : ''
    if (pm === 'ALIPAY') payMethod.value = 'ALIPAY'
    if (pm === 'WECHAT') payMethod.value = 'WECHAT'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    order.value = null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!orderId.value) return
  if (submitBusy.value) return
  submitBusy.value = true
  submitError.value = null
  try {
    const next = await brokerageApi.submitProof(orderId.value, {
      payMethod: payMethod.value,
      proofUrl: proofUrl.value.trim() || null,
      proofNote: proofNote.value.trim() || null,
    })
    order.value = next
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    submitBusy.value = false
  }
}

function back() {
  router.back()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <button class="btn back" type="button" @click="back">返回</button>
      <div class="name">中介费支付</div>
      <div />
    </div>

    <div v-if="loading" class="hint">加载中...</div>
    <div v-else-if="error" class="hint error">{{ error }}</div>

    <template v-else-if="order">
      <div class="card panel">
        <div class="row"><span class="k">订单号</span><span class="v">{{ order.id }}</span></div>
        <div class="row"><span class="k">金额</span><span class="v">{{ amountYuan }} 元</span></div>
        <div class="row"><span class="k">状态</span><span class="v">{{ statusText(order.status) }}</span></div>
      </div>

      <div class="card panel">
        <div class="tabs">
          <button class="tab" type="button" :class="{ active: payMethod === 'WECHAT' }" @click="payMethod = 'WECHAT'">微信</button>
          <button class="tab" type="button" :class="{ active: payMethod === 'ALIPAY' }" @click="payMethod = 'ALIPAY'">支付宝</button>
        </div>

        <div class="qr">
          <img v-if="qrUrl" class="qr-img" :src="qrUrl" alt="" />
          <div v-else class="qr-empty">未配置收款码</div>
        </div>

        <div class="hint muted">完成支付后可在此提交凭证，平台确认后自动解锁联系方式。</div>
      </div>

      <div class="card panel">
        <div class="rowx">
          <div class="k">凭证链接</div>
          <input v-model="proofUrl" class="input" placeholder="可选：粘贴支付截图链接" />
        </div>
        <div class="rowx">
          <div class="k">备注</div>
          <input v-model="proofNote" class="input" placeholder="可选：填写备注信息" />
        </div>
        <button class="btn btn-primary" type="button" :disabled="submitBusy || order.status !== 'PENDING'" @click="submit">
          {{ submitBusy ? '提交中...' : order.status === 'PENDING' ? '我已完成支付' : '已提交/已确认' }}
        </button>
        <div v-if="submitError" class="hint error">{{ submitError }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
  height: 100%;
}

.head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: 10px 12px;
}

.name {
  text-align: center;
  font-weight: 900;
}

.panel {
  padding: 12px;
  display: grid;
  gap: 10px;
}

.row {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 10px;
  font-size: 13px;
}

.k {
  color: var(--muted);
}

.v {
  font-weight: 700;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.tab {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 12px;
  padding: 8px 10px;
  cursor: pointer;
  font-weight: 800;
}

.tab.active {
  border-color: rgba(0, 190, 189, 0.45);
  background: rgba(0, 190, 189, 0.08);
}

.qr {
  display: grid;
  place-items: center;
  padding: 10px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 14px;
  border: 1px solid var(--border);
}

.qr-img {
  width: min(280px, 100%);
  height: auto;
  border-radius: 12px;
}

.qr-empty {
  padding: 40px 10px;
  color: var(--muted);
  font-weight: 700;
}

.rowx {
  display: grid;
  gap: 6px;
}

.input {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 12px;
  outline: none;
}

.hint {
  font-size: 12px;
  color: var(--text);
}

.hint.muted {
  color: var(--muted);
}

.hint.error {
  color: #ff4d4f;
  font-weight: 700;
}
</style>

