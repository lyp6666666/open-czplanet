<template>
  <view class="page">
    <view class="ticket">
      <text class="eyebrow">信息费支付</text>
      <text class="amount">{{ amountText }}</text>
      <text class="desc">支付成功后会自动解锁聊天，随后可以继续确认试课与合作安排。</text>
    </view>

    <view class="trust-card">
      <text class="trust-mark">平台保障</text>
      <text class="trust-title">先预付信息费，再沟通详细需求</text>
      <text class="trust-copy">支付成功后才会继续解锁详细沟通，双方再确认孩子情况、试课安排与合作方式；若沟通未成立，可按平台规则申请处理。</text>
      <view class="trust-pills">
        <text class="trust-pill">真实需求撮合</text>
        <text class="trust-pill">记录可追踪</text>
        <text class="trust-pill">符合规则可申请退款</text>
      </view>
    </view>

    <view class="panel">
      <view class="row">
        <text class="k">支付渠道</text>
        <text class="v">微信支付</text>
      </view>
      <view class="row">
        <text class="k">订单号</text>
        <text class="v">{{ orderNo || '待创建' }}</text>
      </view>
      <view class="row">
        <text class="k">订单状态</text>
        <text class="v">{{ statusText }}</text>
      </view>
      <view v-if="expireText" class="row">
        <text class="k">支付截止</text>
        <text class="v">{{ expireText }}</text>
      </view>
    </view>

    <view class="compliance-card">
      <text class="compliance-title">合作通过前请勿泄露联系方式</text>
      <text class="compliance-copy">在合作通过前，双方不得交换或泄露微信、电话等联系方式。若存在提前泄露联系方式、绕过平台私下成交等行为，平台将不予退还信息费。</text>
    </view>

    <view v-if="message" class="message" :class="{ success: paid }">{{ message }}</view>

    <AppStateCard
      v-if="!prepay && !busy && message"
      title="支付单暂未就绪"
      :description="message"
      action-text="重新获取"
      variant="error"
      @action="retryPrepay"
    />

    <view class="actions">
      <button class="action-btn primary" :disabled="busy || paid" @click="startPay">
        {{ paid ? '已支付' : busy ? '处理中...' : '立即支付' }}
      </button>
      <button v-if="showMockAction" class="action-btn ghost" :disabled="busy || paid" @click="mockSuccess">开发态模拟成功</button>
      <button v-if="applicationId" class="action-btn ghost" @click="backToApplication">返回申请</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { paymentApi, type PrepayResponse } from '@/api/payment';
import { applicationApi } from '@/api/application';
import { useUserStore } from '@/stores/user';
import AppStateCard from '@/components/AppStateCard.vue';

const userStore = useUserStore();
const accountInfo = typeof uni.getAccountInfoSync === 'function' ? uni.getAccountInfoSync() : null;
const showMockAction = Boolean(
  (import.meta as any).env?.DEV ||
  (accountInfo as any)?.miniProgram?.envVersion === 'develop' ||
  (accountInfo as any)?.miniProgram?.envVersion === 'trial',
);
const contextType = ref('BROKERAGE_ORDER');
const contextId = ref<number | null>(null);
const applicationId = ref<number | null>(null);
const prepay = ref<PrepayResponse | null>(null);
const status = ref('');
const busy = ref(false);
const paid = ref(false);
const message = ref('');
let timer: any = null;

const orderNo = computed(() => prepay.value?.orderNo || '');
const amountText = computed(() => {
  const fen = prepay.value?.amountFen;
  if (typeof fen !== 'number') return '待确认';
  return `¥ ${(fen / 100).toFixed(2)}`;
});

const expireText = computed(() => {
  const value = prepay.value?.expireTime;
  if (!value) return '';
  return formatDateTime(value);
});

const statusText = computed(() => {
  const s = String(status.value || '').toUpperCase();
  if (!s) return '待支付';
  if (s === 'SUCCESS' || s === 'PAID') return '支付成功';
  if (s === 'FAILED') return '支付失败';
  if (s === 'CANCELED') return '已取消';
  if (s === 'EXPIRED') return '已过期';
  return s;
});

async function ensurePrepay() {
  if (prepay.value) return prepay.value;
  if (!contextId.value) throw new Error('缺少支付上下文');
  prepay.value = await paymentApi.prepay({
    contextType: contextType.value,
    contextId: contextId.value,
    channel: 'WECHAT',
    tradeType: 'JSAPI',
    openid: userStore.userInfo?.openid,
  });
  status.value = 'PENDING';
  return prepay.value;
}

async function startPay() {
  if (busy.value || paid.value) return;
  busy.value = true;
  message.value = '';
  try {
    const p = await ensurePrepay();
    if (!p.payParams) {
      message.value = p.codeUrl || p.qrCodeUrl ? '当前返回的是扫码支付参数，小程序生产支付需要 JSAPI payParams。' : '后端暂未返回小程序支付参数。';
      return;
    }
    const params = p.payParams;
    await new Promise<void>((resolve, reject) => {
      uni.requestPayment({
        provider: 'wxpay',
        timeStamp: params.timeStamp,
        nonceStr: params.nonceStr,
        package: params.package,
        signType: params.signType,
        paySign: params.paySign,
        success: () => resolve(),
        fail: (err: any) => reject(err),
      } as any);
    });
    message.value = '支付已提交，正在确认到账状态。';
    startPolling();
  } catch (e: any) {
    const raw = String(e?.errMsg || e?.message || '').toLowerCase();
    if (raw.includes('cancel')) {
      status.value = 'CANCELED';
      message.value = '你已取消支付，可以稍后继续。';
      return;
    }
    message.value = e?.message || e?.errMsg || '支付失败';
  } finally {
    busy.value = false;
  }
}

function startPolling() {
  if (!orderNo.value) return;
  stopPolling();
  void pollOnce();
  timer = setInterval(pollOnce, 1800);
}

async function pollOnce() {
  if (!orderNo.value) return;
  try {
    const res = await paymentApi.orderStatus(orderNo.value);
    status.value = res.status;
    const s = String(res.status || '').toUpperCase();
    if (s === 'SUCCESS' || s === 'PAID') {
      paid.value = true;
      message.value = '支付成功，正在打开聊天。';
      stopPolling();
      await enterChatAfterPay();
      return;
    }
    if (s === 'FAILED' || s === 'EXPIRED' || s === 'CANCELED') {
      stopPolling();
      message.value = s === 'EXPIRED' ? '支付单已过期，请重新获取支付单。' : s === 'CANCELED' ? '支付已取消，可稍后继续。' : '支付失败，请重试。';
    }
  } catch {
    // 查询失败时保留页面，下一轮继续查。
  }
}

async function enterChatAfterPay() {
  if (!applicationId.value) return;
  try {
    const res = await applicationApi.enterChat(applicationId.value);
    if (res.roomId) {
      uni.redirectTo({ url: `/pages/chat/room?id=${res.roomId}` });
      return;
    }
    uni.redirectTo({ url: `/pages/application/detail?id=${applicationId.value}` });
  } catch {
    uni.redirectTo({ url: `/pages/application/detail?id=${applicationId.value}` });
  }
}

function stopPolling() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

function backToApplication() {
  if (applicationId.value) uni.redirectTo({ url: `/pages/application/detail?id=${applicationId.value}` });
}

async function retryPrepay() {
  prepay.value = null;
  paid.value = false;
  status.value = '';
  message.value = '';
  try {
    await ensurePrepay();
  } catch (e: any) {
    message.value = e?.message || '创建支付单失败';
  }
}

async function mockSuccess() {
  if (!showMockAction || !orderNo.value || busy.value || paid.value) return;
  busy.value = true;
  try {
    await paymentApi.devMockSuccess(orderNo.value);
    message.value = '已标记为开发态支付成功，正在同步状态。';
    startPolling();
  } catch (e: any) {
    message.value = e?.message || '模拟支付成功失败';
  } finally {
    busy.value = false;
  }
}

function formatDateTime(value?: string | null) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  const h = `${d.getHours()}`.padStart(2, '0');
  const min = `${d.getMinutes()}`.padStart(2, '0');
  return `${m}-${day} ${h}:${min}`;
}

onLoad((options: any) => {
  contextType.value = String(options?.contextType || 'BROKERAGE_ORDER');
  const cid = Number(options?.contextId);
  contextId.value = Number.isFinite(cid) ? cid : null;
  const aid = Number(options?.applicationId);
  applicationId.value = Number.isFinite(aid) ? aid : null;
  void ensurePrepay().catch((e: any) => {
    message.value = e?.message || '创建支付单失败';
  });
});

onUnload(() => {
  stopPolling();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px 110px;
  background: #f5f7f8;
  box-sizing: border-box;
}

.ticket {
  padding: 22px 18px;
  border-radius: 22px;
  background: radial-gradient(circle at 20% 0%, rgba(0, 190, 189, 0.22), transparent 34%), #101820;
  color: #fff;
  box-shadow: 0 20px 44px rgba(16, 24, 32, 0.24);
}

.eyebrow,
.amount,
.desc {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.68;
  margin-bottom: 10px;
}

.amount {
  font-size: 34px;
  font-weight: 900;
  margin-bottom: 10px;
}

.desc {
  font-size: 13px;
  line-height: 1.65;
  opacity: 0.78;
}

.panel {
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(16, 24, 32, 0.08);
}

.trust-card {
  margin-top: 14px;
  padding: 16px;
  border-radius: 18px;
  background:
    radial-gradient(circle at top left, rgba(199, 240, 226, 0.72), transparent 34%),
    linear-gradient(180deg, rgba(247, 252, 250, 0.98), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(15, 118, 110, 0.12);
}

.trust-mark,
.trust-title,
.trust-copy {
  display: block;
}

.trust-mark {
  width: fit-content;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  line-height: 28px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.trust-title {
  margin-top: 12px;
  color: #12212a;
  font-size: 20px;
  line-height: 1.35;
  font-weight: 900;
}

.trust-copy {
  margin-top: 10px;
  color: #52636d;
  font-size: 13px;
  line-height: 1.7;
}

.trust-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.trust-pill {
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(18, 33, 42, 0.08);
  color: #12212a;
  font-size: 12px;
  line-height: 32px;
  font-weight: 800;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(16, 24, 32, 0.07);
}

.row:last-child {
  border-bottom: none;
}

.k {
  color: #7b8790;
  font-size: 13px;
}

.v {
  color: #111827;
  font-size: 13px;
  font-weight: 800;
  text-align: right;
  word-break: break-all;
}

.message {
  margin-top: 14px;
  padding: 12px;
  border-radius: 14px;
  color: #9a5b00;
  background: #fff3d4;
  font-size: 13px;
  line-height: 1.6;
}

.message.success {
  color: #087268;
  background: #dff7f4;
}

.compliance-card {
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(196, 92, 39, 0.16);
  background: linear-gradient(180deg, rgba(255, 244, 235, 0.96), rgba(255, 252, 248, 0.96));
}

.compliance-title,
.compliance-copy {
  display: block;
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
}

.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  gap: 10px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid rgba(16, 24, 32, 0.08);
  backdrop-filter: blur(12px);
}

.action-btn {
  height: 46px;
  border: 0;
  border-radius: 23px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 900;
  line-height: 46px;
}

.action-btn::after {
  display: none;
}

.action-btn.primary {
  background: #00bebd;
  color: #fff;
  box-shadow: 0 10px 22px rgba(0, 190, 189, 0.24);
}

.action-btn.ghost {
  background: #eef3f4;
  color: #111827;
}

.action-btn[disabled] {
  opacity: 0.55;
}
</style>
