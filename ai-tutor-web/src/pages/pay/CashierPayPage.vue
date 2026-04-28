<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { getPaymentOrderStatus, prepay, type PayChannel } from '@/api/payment'
import { useToastStore } from '@/stores/toast'

type UiState = 'loading' | 'ready' | 'paid' | 'expired' | 'failed'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const contextType = computed(() => (typeof route.query.contextType === 'string' ? route.query.contextType.trim() : ''))
const contextId = computed(() => {
  const raw = route.query.contextId
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})
const applicationId = computed(() => {
  const raw = route.query.applicationId
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})
const otherUid = computed(() => {
  const raw = route.query.otherUid
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

const channel = ref<PayChannel>('WECHAT')
const state = ref<UiState>('loading')
const error = ref<string | null>(null)

const orderNo = ref<string>('')
const amountFen = ref<number>(0)
const qrCodeUrl = ref<string>('')
const codeUrl = ref<string>('')
const expireTime = ref<string>('')

const nowMs = ref(Date.now())
let tickTimer: number | null = null
let pollTimer: number | null = null

const amountYuan = computed(() => (amountFen.value ? (amountFen.value / 100).toFixed(2) : ''))
const expireMs = computed(() => {
  if (!expireTime.value) return null
  const ms = Date.parse(expireTime.value)
  return Number.isFinite(ms) ? ms : null
})
const leftSeconds = computed(() => {
  const ex = expireMs.value
  if (ex == null) return null
  return Math.max(0, Math.floor((ex - nowMs.value) / 1000))
})

function wait(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

function buildOpenMessage(type: string) {
  return {
    type,
    contextType: contextType.value,
    contextId: contextId.value,
    applicationId: applicationId.value,
    orderNo: orderNo.value,
    channel: channel.value,
  }
}

function notifyOpenerPaid() {
  if (!window.opener) return
  try {
    window.opener.postMessage(buildOpenMessage('PAY_SUCCESS'), window.location.origin)
  } catch {
    //
  }
}

async function redirectAfterPaid() {
  if (!applicationId.value) return
  toast.show('支付成功，正在进入聊天…', 'success', 2200)
  for (let i = 0; i < 8; i += 1) {
    try {
      const res = await applicationApi.enterChat(applicationId.value)
      if (res.roomId) {
        await router.replace({
          name: 'chatRoom',
          params: { roomId: String(res.roomId) },
          query: { otherUid: otherUid.value ? String(otherUid.value) : undefined },
        })
        return
      }
    } catch {
      //
    }
    if (i < 7) {
      await wait(900)
    }
  }
  toast.show('支付结果已返回，但聊天权限仍在同步，请稍后再试', 'info', 3500)
}

async function handlePaid() {
  if (state.value === 'paid') return
  state.value = 'paid'
  stopPoll()
  notifyOpenerPaid()
  if (!applicationId.value) {
    toast.show('支付成功', 'success', 2200)
  }

  if (window.opener) {
    window.setTimeout(() => {
      window.close()
    }, 800)
    return
  }

  await redirectAfterPaid()
}

function startTick() {
  if (tickTimer != null) return
  tickTimer = window.setInterval(() => {
    nowMs.value = Date.now()
    const left = leftSeconds.value
    if (left != null && left <= 0 && state.value === 'ready') {
      state.value = 'expired'
      stopPoll()
    }
  }, 500)
}

function stopTick() {
  if (tickTimer == null) return
  window.clearInterval(tickTimer)
  tickTimer = null
}

function stopPoll() {
  if (pollTimer == null) return
  window.clearInterval(pollTimer)
  pollTimer = null
}

function startPoll() {
  if (pollTimer != null) return
  pollTimer = window.setInterval(async () => {
    if (!orderNo.value) return
    if (state.value !== 'ready') return
    const ex = expireMs.value
    if (ex != null && Date.now() >= ex) {
      state.value = 'expired'
      stopPoll()
      return
    }
    await refreshStatus()
  }, 2000)
}

async function refreshStatus() {
  try {
    const s = await getPaymentOrderStatus(orderNo.value)
    amountFen.value = s.amountFen
    if (s.expireTime) expireTime.value = s.expireTime
    if (s.status === 'SUCCESS') {
      await handlePaid()
      return
    }
    if (s.status === 'FAILED') {
      state.value = 'failed'
      stopPoll()
      return
    }
    if (s.status === 'CLOSED') {
      state.value = 'expired'
      stopPoll()
      return
    }
  } catch {
    //
  }
}

async function doPrepay() {
  if (!contextType.value || !contextId.value) {
    error.value = '缺少支付参数'
    state.value = 'failed'
    return
  }

  state.value = 'loading'
  error.value = null
  qrCodeUrl.value = ''
  codeUrl.value = ''
  expireTime.value = ''

  try {
    const r = await prepay({
      contextType: contextType.value,
      contextId: contextId.value,
      channel: channel.value,
    })
    orderNo.value = r.orderNo
    amountFen.value = r.amountFen
    qrCodeUrl.value = r.qrCodeUrl || ''
    codeUrl.value = r.codeUrl || ''
    expireTime.value = r.expireTime || ''
    state.value = 'ready'
    await refreshStatus()
    startPoll()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '下单失败'
    state.value = 'failed'
  }
}

function changeChannel(next: PayChannel) {
  if (channel.value === next) return
  channel.value = next
  void doPrepay()
}

function back() {
  if (window.opener) {
    window.close()
    return
  }
  router.back()
}

function refreshQr() {
  void doPrepay()
}

onMounted(() => {
  startTick()
  void doPrepay()
})

onUnmounted(() => {
  stopTick()
  stopPoll()
})
</script>

<template>
  <div class="page">
    <div class="wrap">
      <div class="head">
        <button class="btn" type="button" @click="back">返回</button>
        <div class="title">订单支付</div>
        <div class="right">
          <span v-if="leftSeconds != null && state === 'ready'" class="time">剩余 {{ leftSeconds }}s</span>
        </div>
      </div>

      <div class="card trust-card">
        <div class="trust-mark">平台保障</div>
        <div class="trust-title">先预付信息费，再沟通详细需求</div>
        <div class="trust-copy">支付成功后会自动解锁聊天，双方才能继续确认孩子情况、试课安排与合作方式；若沟通未成立，可按平台规则申请处理。</div>
        <div class="trust-pills">
          <span class="trust-pill">真实需求撮合</span>
          <span class="trust-pill">沟通记录可追踪</span>
          <span class="trust-pill">符合规则可申请退款</span>
        </div>
      </div>

      <div class="card panel">
        <div class="row">
          <div class="k">订单号</div>
          <div class="v mono">{{ orderNo || '-' }}</div>
        </div>
        <div class="row">
          <div class="k">支付金额</div>
          <div class="v price">¥{{ amountYuan || '--' }}</div>
        </div>
      </div>

      <div class="card pay">
        <div class="tabs">
          <button class="tab" :class="{ active: channel === 'WECHAT' }" type="button" @click="changeChannel('WECHAT')">
            微信支付
          </button>
          <button class="tab" :class="{ active: channel === 'ALIPAY' }" type="button" @click="changeChannel('ALIPAY')">
            支付宝支付
          </button>
        </div>

        <div class="body">
          <div class="qr">
            <div v-if="state === 'loading'" class="qr-box skeleton" />
            <template v-else>
              <img v-if="qrCodeUrl" class="qr-box" :src="qrCodeUrl" alt="支付二维码" />
              <div v-else class="qr-box empty">
                <div class="hint">暂无二维码</div>
                <div v-if="codeUrl" class="mono link">{{ codeUrl }}</div>
              </div>
            </template>
          </div>
          <div class="guide">
            <div class="g-title">扫码完成支付</div>
            <div class="g-sub">请使用{{ channel === 'WECHAT' ? '微信' : '支付宝' }}扫一扫</div>
            <div v-if="state === 'expired'" class="g-warn">二维码已过期，请刷新重试</div>
            <div v-else-if="state === 'paid'" class="g-ok">支付成功</div>
            <div v-else-if="state === 'failed'" class="g-warn">支付失败，请重试</div>
            <div v-if="error" class="g-warn">{{ error }}</div>
            <div class="g-actions">
              <button class="btn" type="button" :disabled="state === 'loading'" @click="refreshQr">刷新二维码</button>
            </div>
          </div>
        </div>
      </div>

      <div class="card compliance-card">
        <div class="compliance-title">合作通过前请勿泄露联系方式</div>
        <div class="compliance-copy">在合作通过前，双方不得交换或泄露微信、电话等联系方式。若存在提前泄露联系方式、绕过平台私下成交等行为，平台将不予退还信息费。</div>
      </div>

      <div v-if="state === 'paid'" class="card result ok">
        <div class="r-title">支付完成</div>
        <div class="r-sub">可返回业务页面继续操作</div>
        <div class="r-actions">
          <button class="btn btn-primary" type="button" @click="back">返回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 24px 0;
}

.wrap {
  width: min(980px, calc(100% - 40px));
  margin: 0 auto;
  display: grid;
  gap: 12px;
}

.head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
}

.title {
  text-align: center;
  font-size: 16px;
  font-weight: 900;
}

.right {
  display: flex;
  justify-content: flex-end;
}

.time {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
}

.panel {
  padding: 12px;
  display: grid;
  gap: 8px;
}

.trust-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 22px;
  border-color: rgba(15, 118, 110, 0.12);
  background:
    radial-gradient(circle at top left, rgba(199, 240, 226, 0.7), transparent 34%),
    linear-gradient(180deg, rgba(247, 252, 250, 0.98), rgba(255, 255, 255, 0.98));
}

.trust-mark {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  width: fit-content;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.trust-title {
  color: #12212a;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 900;
}

.trust-copy {
  color: #52636d;
  font-size: 14px;
  line-height: 1.75;
  font-weight: 700;
}

.trust-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.trust-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(18, 33, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  color: #12212a;
  font-size: 12px;
  font-weight: 800;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.k {
  color: var(--muted);
  font-weight: 800;
}

.v {
  color: var(--text);
  font-weight: 900;
}

.price {
  color: #00a870;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.pay {
  padding: 12px;
}

.tabs {
  display: flex;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.tab {
  height: 34px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  font-weight: 900;
  cursor: pointer;
}

.tab.active {
  border-color: var(--primary);
  background: var(--primary-weak);
}

.body {
  padding-top: 12px;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 14px;
  align-items: center;
}

.qr {
  display: flex;
  justify-content: center;
}

.qr-box {
  width: 260px;
  height: 260px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  object-fit: contain;
}

.qr-box.empty {
  display: grid;
  place-items: center;
  padding: 10px;
  text-align: center;
}

.hint {
  color: var(--muted);
  font-weight: 800;
  font-size: 12px;
}

.link {
  margin-top: 8px;
  font-size: 11px;
  word-break: break-all;
  color: var(--muted);
}

.guide {
  display: grid;
  gap: 8px;
}

.g-title {
  font-size: 16px;
  font-weight: 900;
}

.g-sub {
  color: var(--muted);
  font-weight: 700;
  font-size: 13px;
}

.g-warn {
  color: #d4380d;
  font-weight: 800;
  font-size: 13px;
}

.g-ok {
  color: #00a870;
  font-weight: 900;
  font-size: 14px;
}

.g-actions {
  margin-top: 6px;
}

.compliance-card {
  padding: 16px;
  border-radius: 20px;
  border-color: rgba(196, 92, 39, 0.16);
  background: linear-gradient(180deg, rgba(255, 244, 235, 0.96), rgba(255, 252, 248, 0.96));
}

.compliance-title {
  color: #8e4316;
  font-size: 14px;
  font-weight: 900;
}

.compliance-copy {
  margin-top: 8px;
  color: #9a5b00;
  font-size: 13px;
  line-height: 1.7;
  font-weight: 700;
}

.result {
  padding: 14px;
  display: grid;
  gap: 8px;
}

.result.ok {
  border-color: rgba(0, 168, 112, 0.28);
}

.r-title {
  font-size: 16px;
  font-weight: 900;
}

.r-sub {
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

.r-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 820px) {
  .body {
    grid-template-columns: 1fr;
  }
}
</style>
