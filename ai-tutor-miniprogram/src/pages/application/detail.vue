<template>
  <view class="page">
    <view v-if="loading" class="state">加载中...</view>
    <view v-else-if="error" class="state error">
      <text>{{ error }}</text>
      <u-button size="mini" type="primary" color="#00bebd" @click="load">重试</u-button>
    </view>

    <template v-else-if="detail">
      <view class="top">
        <text class="label">申请详情</text>
        <text class="title">{{ statusText(detail.status) }}</text>
        <text class="desc">{{ flowTip(detail) }}</text>
      </view>

      <view class="panel">
        <view class="row">
          <text class="k">聊天状态</text>
          <text class="v">{{ accessText(detail) }}</text>
        </view>
        <view class="row">
          <text class="k">申请类型</text>
          <text class="v">{{ contextText(detail) }}</text>
        </view>
        <view class="row">
          <text class="k">对方用户</text>
          <text class="v">ID {{ otherUid(detail) }}</text>
        </view>
        <view class="row">
          <text class="k">创建时间</text>
          <text class="v">{{ formatDateTime(detail.createTime) }}</text>
        </view>
        <view v-if="detail.decidedAt" class="row">
          <text class="k">处理时间</text>
          <text class="v">{{ formatDateTime(detail.decidedAt) }}</text>
        </view>
      </view>

      <view class="panel">
        <text class="section-title">申请内容</text>
        <text class="content">{{ detail.content || '暂无申请内容' }}</text>
      </view>

      <view v-if="opError" class="op-error">{{ opError }}</view>

      <view class="actions">
        <template v-if="canDecide">
          <button class="action-btn ghost" :disabled="busy" @click="decide('REJECT')">拒绝</button>
          <button class="action-btn primary" :disabled="busy" @click="decide('ACCEPT')">
            {{ busy ? '处理中...' : '通过申请' }}
          </button>
        </template>
        <template v-else-if="detail.status === 'ACCEPTED'">
          <button class="action-btn primary single" :disabled="busy" @click="enterChat">
            {{ busy ? '处理中...' : primaryActionText }}
          </button>
        </template>
        <button v-else class="action-btn ghost single" @click="back">返回</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { applicationApi, type ApplicationDecision, type TutorApplication } from '@/api/application';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const id = ref<number | null>(null);
const detail = ref<TutorApplication | null>(null);
const loading = ref(false);
const busy = ref(false);
const error = ref('');
const opError = ref('');

const canDecide = computed(() => {
  return !!detail.value && detail.value.status === 'PENDING' && detail.value.receiverUid === userStore.userInfo?.id;
});

const primaryActionText = computed(() => {
  if (!detail.value) return '进入聊天';
  if (detail.value.chatAccessStatus === 'PAYMENT_REQUIRED' && userStore.currentRole === 'tutor') return '去支付信息费';
  if (detail.value.chatAccessStatus === 'PAYMENT_REQUIRED') return '查看进度';
  return '进入聊天';
});

function statusText(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '等待处理';
  if (s === 'ACCEPTED') return '申请已通过';
  if (s === 'REJECTED') return '申请已拒绝';
  return s || '状态未知';
}

function accessText(it: TutorApplication) {
  if (it.status === 'PENDING') return '申请通过后开放下一步';
  if (it.status === 'REJECTED') return '聊天未开放';
  if (it.chatAccessStatus === 'PAYMENT_REQUIRED') return '待教师支付信息费';
  if (it.chatAccessStatus === 'CHAT_ENABLED') return '可进入聊天';
  return '等待系统确认';
}

function flowTip(it: TutorApplication) {
  if (it.status === 'PENDING') return '接收方通过后才会进入信息费支付与聊天解锁。';
  if (it.status === 'ACCEPTED' && it.chatAccessStatus === 'PAYMENT_REQUIRED') return '申请已通过，教师支付信息费后双方才能继续聊天。';
  if (it.status === 'ACCEPTED' && it.chatAccessStatus === 'CHAT_ENABLED') return '聊天已开放，可以继续确认试课和正式课安排。';
  if (it.status === 'REJECTED') return '本次申请已结束，可调整内容后重新发起。';
  return '';
}

function contextText(it: TutorApplication) {
  if (it.contextType === 'DEMAND') return `学生需求 #${it.contextId}`;
  if (it.contextType === 'ORG_POSTING') return `机构需求 #${it.contextId}`;
  return `老师主页 #${it.contextId}`;
}

function otherUid(it: TutorApplication) {
  const mine = userStore.userInfo?.id;
  if (mine && it.senderUid === mine) return it.receiverUid;
  if (mine && it.receiverUid === mine) return it.senderUid;
  return it.senderUid;
}

function formatDateTime(v?: string) {
  if (!v) return '';
  return String(v).slice(0, 16).replace('T', ' ');
}

async function load() {
  if (!id.value) return;
  loading.value = true;
  error.value = '';
  try {
    detail.value = await applicationApi.detail(id.value);
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载申请失败';
  } finally {
    loading.value = false;
  }
}

async function decide(action: ApplicationDecision) {
  if (!id.value || busy.value) return;
  busy.value = true;
  opError.value = '';
  try {
    detail.value = await applicationApi.decide(id.value, action);
  } catch (e: any) {
    opError.value = e?.message || e?.msg || '操作失败';
  } finally {
    busy.value = false;
  }
}

async function enterChat() {
  if (!id.value || busy.value) return;
  busy.value = true;
  opError.value = '';
  try {
    const res = await applicationApi.enterChat(id.value);
    if (res.paymentRequired && res.orderId) {
      uni.navigateTo({ url: `/pages/pay/cashier?contextType=BROKERAGE_ORDER&contextId=${res.orderId}&applicationId=${id.value}` });
      return;
    }
    if (res.waitingForTeacherPayment) {
      opError.value = '请等待教师完成信息费支付';
      return;
    }
    if (res.roomId) {
      uni.navigateTo({ url: `/pages/chat/room?id=${res.roomId}` });
      return;
    }
    opError.value = '暂时无法进入聊天';
  } catch (e: any) {
    opError.value = e?.message || e?.msg || '进入聊天失败';
  } finally {
    busy.value = false;
  }
}

function back() {
  uni.navigateBack();
}

onLoad((options: any) => {
  const n = Number(options?.id);
  id.value = Number.isFinite(n) ? n : null;
});

onShow(() => {
  void load();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px 96px;
  background: #f5f7f8;
  box-sizing: border-box;
}

.top {
  padding: 20px;
  border-radius: 20px;
  background: #111827;
  color: #fff;
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.2);
}

.label,
.title,
.desc,
.section-title,
.content {
  display: block;
}

.label {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  margin-bottom: 8px;
}

.title {
  font-size: 23px;
  font-weight: 900;
  margin-bottom: 10px;
}

.desc {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.65;
}

.panel {
  margin-top: 12px;
  padding: 15px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(17, 24, 39, 0.08);
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.06);
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(17, 24, 39, 0.07);
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
}

.section-title {
  font-size: 15px;
  color: #111827;
  font-weight: 900;
  margin-bottom: 10px;
}

.content {
  color: #27313a;
  font-size: 14px;
  line-height: 1.75;
}

.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.94);
  border-top: 1px solid rgba(17, 24, 39, 0.08);
  backdrop-filter: blur(12px);
}

.actions :deep(.u-button:only-child) {
  grid-column: 1 / -1;
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

.action-btn.single {
  grid-column: 1 / -1;
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

.state {
  margin-top: 40px;
  padding: 28px 16px;
  border-radius: 16px;
  text-align: center;
  background: #fff;
  color: #6b7280;
}

.error,
.op-error {
  color: #b42318;
}

.op-error {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #ffe4e0;
  font-size: 13px;
}
</style>
