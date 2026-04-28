<template>
  <view class="page">
    <view class="ticket">
      <text class="eyebrow">信息费支付</text>
      <text class="amount">{{ amountText }}</text>
      <text class="desc">支付成功后会自动解锁聊天，随后可以继续确认试课与合作安排。</text>
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
    </view>

    <view v-if="message" class="message" :class="{ success: paid }">{{ message }}</view>

    <view class="actions">
      <button class="action-btn primary" :disabled="busy || paid" @click="startPay">
        {{ paid ? '已支付' : busy ? '处理中...' : '立即支付' }}
      </button>
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

const userStore = useUserStore();
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

const statusText = computed(() => {
  const s = String(status.value || '').toUpperCase();
  if (!s) return '待支付';
  if (s === 'SUCCESS' || s === 'PAID') return '支付成功';
  if (s === 'FAILED') return '支付失败';
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
    message.value = e?.message || e?.errMsg || '支付失败或已取消';
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
