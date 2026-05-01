<template>
  <view class="page">
    <AppStateCard
      v-if="loading"
      title="课堂准备加载中"
      description="正在同步课堂开放时间、支付状态和设备检查要求。"
      variant="soft"
    />
    <AppStateCard
      v-else-if="error"
      title="课堂准备加载失败"
      :description="error"
      action-text="重试"
      variant="error"
      @action="load"
    />

    <template v-else-if="prepareData">
      <view class="hero">
        <text class="eyebrow">{{ prepareData.courseKindLabel || '线上课堂' }}</text>
        <text class="title">{{ prepareData.courseTitle || '家教线上课堂' }}</text>
        <text class="subtitle">
          {{ prepareData.peerDisplayName || '对方' }}
          <template v-if="prepareData.subjectLabel"> · {{ prepareData.subjectLabel }}</template>
        </text>
      </view>

      <view v-if="prepareData.joinBlockedReason" class="notice" :class="{ danger: paymentBlocked }">
        <text class="notice-title">{{ paymentBlocked ? '暂不能进入课堂' : '课堂暂未开放' }}</text>
        <text class="notice-copy">{{ prepareData.joinBlockedReason }}</text>
      </view>

      <view class="panel">
        <view class="row">
          <text class="k">课堂状态</text>
          <text class="v">{{ liveStatusText }}</text>
        </view>
        <view class="row">
          <text class="k">进入权限</text>
          <text class="v">{{ accessText }}</text>
        </view>
        <view class="row">
          <text class="k">设备检查</text>
          <text class="v">{{ prepareData.deviceCheckRequired ? '需要检查' : '可直接进入' }}</text>
        </view>
        <view v-if="prepareData.blockingLessonId" class="row">
          <text class="k">阻塞课节</text>
          <text class="v">#{{ prepareData.blockingLessonId }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">设备自检</text>
          <text class="pill">{{ deviceReady ? '已就绪' : '待检查' }}</text>
        </view>
        <view class="check-grid">
          <button class="check" :class="{ on: cameraChecked }" @click="cameraChecked = !cameraChecked">
            <text class="check-title">摄像头</text>
            <text class="check-copy">{{ cameraChecked ? '已确认可用' : '点击确认' }}</text>
          </button>
          <button class="check" :class="{ on: micChecked }" @click="micChecked = !micChecked">
            <text class="check-title">麦克风</text>
            <text class="check-copy">{{ micChecked ? '已确认可用' : '点击确认' }}</text>
          </button>
          <button class="check" :class="{ on: networkChecked }" @click="networkChecked = !networkChecked">
            <text class="check-title">网络</text>
            <text class="check-copy">{{ networkChecked ? '网络稳定' : '点击确认' }}</text>
          </button>
        </view>
        <text class="hint">小程序端会先完成轻量自检，再进入课堂承接页并桥接到真实 H5 课堂容器。</text>
      </view>

      <view class="panel">
        <text class="section-title">AI 课堂记录</text>
        <view class="switch-row">
          <view>
            <text class="switch-title">实时课堂摘要</text>
            <text class="switch-copy">进入课堂后同步记录阶段重点。</text>
          </view>
          <switch :checked="realtimeSummaryEnabled" color="#0f766e" @change="setRealtimeSummary" />
        </view>
        <view class="switch-row">
          <view>
            <text class="switch-title">课后总结报告</text>
            <text class="switch-copy">结课后生成学生与教师可查看的总结。</text>
          </view>
          <switch :checked="postClassSummaryEnabled" color="#0f766e" @change="setPostClassSummary" />
        </view>
      </view>

      <view v-if="joinError" class="op-error">{{ joinError }}</view>

      <view v-if="joinTokenData" class="panel token-panel">
        <text class="section-title">课堂令牌已签发</text>
        <text class="hint">已通过真实接口签发入会 token，现在可以继续进入课堂承接页。</text>
        <view class="row">
          <text class="k">服务商</text>
          <text class="v">{{ joinTokenData.provider || 'LIVEKIT' }}</text>
        </view>
        <view class="row">
          <text class="k">房间</text>
          <text class="v">{{ joinTokenData.roomName }}</text>
        </view>
      </view>

      <view class="actions">
        <button v-if="paymentBlocked" class="action primary" :disabled="busy" @click="goPay">
          去支付上一节课
        </button>
        <button
          v-else
          class="action primary"
          :disabled="busy || (!joinTokenData && !canEnter)"
          @click="joinTokenData ? goLaunch() : enterClassroom()"
        >
          {{ busy ? '准备中...' : joinTokenData ? '继续进入课堂' : canEnter ? '进入课堂' : '暂不能进入' }}
        </button>
        <button class="action ghost" :disabled="busy" @click="load">刷新状态</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app';
import { liveApi, type IssueJoinTokenResp, type PrepareLiveSessionResp } from '@/api/live';
import { useUserStore } from '@/stores/user';
import { getBaseUrl } from '@/utils/request';
import AppStateCard from '@/components/AppStateCard.vue';

const userStore = useUserStore();
const courseId = ref<number | null>(null);
const eventId = ref<number | null>(null);
const prepareData = ref<PrepareLiveSessionResp | null>(null);
const joinTokenData = ref<IssueJoinTokenResp | null>(null);
const loading = ref(false);
const busy = ref(false);
const error = ref('');
const joinError = ref('');
const cameraChecked = ref(false);
const micChecked = ref(false);
const networkChecked = ref(false);
const realtimeSummaryEnabled = ref(true);
const postClassSummaryEnabled = ref(true);

const paymentBlocked = computed(() => !!prepareData.value?.blockingPaymentOrderId);
const deviceReady = computed(() => cameraChecked.value && micChecked.value && networkChecked.value);
const canEnter = computed(() => {
  const data = prepareData.value;
  if (!data) return false;
  if (!data.canJoin || !data.joinableNow || paymentBlocked.value) return false;
  return data.deviceCheckRequired ? deviceReady.value : true;
});
const accessText = computed(() => {
  const data = prepareData.value;
  if (!data) return '待确认';
  if (paymentBlocked.value) return '需先支付';
  if (data.canJoin && data.joinableNow) return '可进入';
  if (data.joinableNow) return '无进入权限';
  return '未到开放时间';
});
const liveStatusText = computed(() => {
  const s = String(prepareData.value?.status || '').toUpperCase();
  if (s === 'READY') return '待开课';
  if (s === 'IN_PROGRESS') return '进行中';
  if (s === 'ENDED') return '已结束';
  if (s === 'CANCELLED' || s === 'CANCELED') return '已取消';
  return prepareData.value?.status || '待确认';
});

async function load() {
  if (!courseId.value) {
    error.value = '缺少课程 ID';
    return;
  }
  loading.value = true;
  error.value = '';
  joinError.value = '';
  joinTokenData.value = null;
  try {
    const data = await liveApi.prepare(courseId.value, {
      clientType: 'MP_WEIXIN',
      sourcePage: 'MINIPROGRAM_LIVE_PREPARE',
    });
    prepareData.value = data;
    realtimeSummaryEnabled.value = data.realtimeSummaryEnabled ?? true;
    postClassSummaryEnabled.value = data.postClassSummaryEnabled ?? true;
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载课堂准备信息失败';
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}

async function enterClassroom() {
  const data = prepareData.value;
  if (!data?.sessionId || !canEnter.value || busy.value) return;
  busy.value = true;
  joinError.value = '';
  joinTokenData.value = null;
  try {
    await liveApi.reportDevice(data.sessionId, {
      reportStage: 'MINIPROGRAM_PREPARE',
      cameraStatus: cameraChecked.value ? 'READY' : 'UNTESTED',
      micStatus: micChecked.value ? 'READY' : 'UNTESTED',
      speakerStatus: 'UNTESTED',
      networkLevel: networkChecked.value ? 'GOOD' : 'UNKNOWN',
      browserInfo: 'MP_WEIXIN',
      osInfo: uni.getSystemInfoSync?.().platform || 'unknown',
      deviceInfo: {
        cameraChecked: cameraChecked.value,
        micChecked: micChecked.value,
        networkChecked: networkChecked.value,
      },
    });
    await liveApi.updateAiOptions(data.sessionId, {
      realtimeSummaryEnabled: realtimeSummaryEnabled.value,
      postClassSummaryEnabled: postClassSummaryEnabled.value,
    });
    joinTokenData.value = await liveApi.joinToken(data.sessionId, {
      clientType: 'MP_WEIXIN',
      joinMode: 'MINIPROGRAM',
      deviceFingerprint: `mp-${data.sessionId}-${Date.now()}`,
    });
    goLaunch();
  } catch (e: any) {
    joinError.value = e?.message || e?.msg || '进入课堂准备失败';
  } finally {
    busy.value = false;
  }
}

function normalizeBase(raw: string) {
  const trimmed = String(raw || '').trim();
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed;
}

function resolveWebOrigin() {
  const apiBase = normalizeBase(getBaseUrl());
  if (!apiBase) return '';
  if (apiBase.endsWith('/api/v1')) return apiBase.slice(0, -7);
  if (apiBase.endsWith('/api')) return apiBase.slice(0, -4);
  return apiBase;
}

function buildBridgeUrl() {
  if (!courseId.value || !prepareData.value || !joinTokenData.value) return '';
  const webOrigin = resolveWebOrigin();
  const token = String(userStore.token || '').trim();
  if (!webOrigin || !token || !userStore.userInfo) return '';
  const user = JSON.stringify({
    id: userStore.userInfo.id,
    name: userStore.userInfo.name,
    phone: userStore.userInfo.phone,
    avatar: userStore.userInfo.avatar || null,
    sex: userStore.userInfo.sex ?? null,
    userType: userStore.userInfo.userType,
    token,
  });
  const params = new URLSearchParams({
    courseId: String(courseId.value),
    token,
    user,
    sessionId: String(prepareData.value.sessionId),
    serverUrl: String(joinTokenData.value.serverUrl || ''),
    roomName: String(joinTokenData.value.roomName || ''),
    participantIdentity: String(joinTokenData.value.participantIdentity || ''),
    participantName: String(joinTokenData.value.participantName || ''),
    accessToken: String(joinTokenData.value.accessToken || ''),
    expireAt: String(joinTokenData.value.expireAt || ''),
    source: 'mp_weixin',
  });
  return `${webOrigin}/#/live/mp-bridge?${params.toString()}`;
}

function goLaunch() {
  const bridgeUrl = buildBridgeUrl();
  if (!bridgeUrl || !prepareData.value || !courseId.value) {
    joinError.value = '课堂桥接地址生成失败，请检查当前环境配置';
    return;
  }
  uni.navigateTo({
    url:
      `/pages/live/launch?url=${encodeURIComponent(bridgeUrl)}` +
      `&courseId=${courseId.value}` +
      `&sessionId=${prepareData.value.sessionId}` +
      `&eventId=${eventId.value || ''}` +
      `&courseTitle=${encodeURIComponent(prepareData.value.courseTitle || '')}` +
      `&peerName=${encodeURIComponent(prepareData.value.peerDisplayName || '')}`,
  });
}

function goPay() {
  const orderId = prepareData.value?.blockingPaymentOrderId;
  if (!orderId) return;
  uni.navigateTo({
    url: `/pages/pay/cashier?contextType=LESSON_PAYMENT_ORDER&contextId=${orderId}`,
  });
}

function setRealtimeSummary(event: any) {
  realtimeSummaryEnabled.value = Boolean(event?.detail?.value);
}

function setPostClassSummary(event: any) {
  postClassSummaryEnabled.value = Boolean(event?.detail?.value);
}

onLoad((options: any) => {
  const cid = Number(options?.courseId);
  const eid = Number(options?.eventId);
  courseId.value = Number.isFinite(cid) ? cid : null;
  eventId.value = Number.isFinite(eid) ? eid : null;
  void load();
});

onPullDownRefresh(() => {
  void load();
});

onShow(() => {
  if (courseId.value) {
    void load();
  }
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px calc(112px + env(safe-area-inset-bottom));
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #122529 0%, #1f6f68 72%, #d49f52 140%);
  box-shadow: 0 18px 38px rgba(18, 37, 41, 0.2);
}

.eyebrow,
.title,
.subtitle,
.section-title,
.hint,
.notice-title,
.notice-copy,
.k,
.v,
.check-title,
.check-copy,
.switch-title,
.switch-copy {
  display: block;
}

.eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.72;
}

.title {
  font-size: 22px;
  font-weight: 900;
  line-height: 1.25;
}

.subtitle {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  opacity: 0.82;
}

.notice,
.panel {
  margin-top: 12px;
  padding: 15px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 12px 28px rgba(18, 37, 41, 0.06);
}

.notice {
  background: #fff9ed;
  border-color: rgba(213, 159, 84, 0.26);
}

.notice.danger {
  background: #fff4f2;
  border-color: rgba(194, 65, 65, 0.18);
}

.notice-title {
  color: #172326;
  font-size: 15px;
  font-weight: 900;
}

.notice-copy,
.hint {
  margin-top: 8px;
  color: #65717a;
  font-size: 13px;
  line-height: 1.65;
}

.panel-head,
.row,
.switch-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.panel-head,
.switch-row {
  align-items: center;
}

.section-title {
  color: #172326;
  font-size: 16px;
  font-weight: 900;
}

.row {
  padding: 10px 0;
  border-bottom: 1px solid rgba(18, 37, 41, 0.07);
}

.row:last-child {
  border-bottom: none;
}

.k {
  flex-shrink: 0;
  color: #82909a;
  font-size: 13px;
}

.v {
  color: #162326;
  font-size: 14px;
  font-weight: 800;
  text-align: right;
  word-break: break-all;
}

.pill {
  padding: 4px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: #edf6f5;
  font-size: 12px;
  font-weight: 900;
}

.check-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.check {
  min-height: 82px;
  padding: 10px 6px;
  border: 1px solid rgba(18, 37, 41, 0.08);
  border-radius: 14px;
  background: #f6f8f8;
  text-align: center;
  line-height: 1.3;
}

.check::after {
  border: 0;
}

.check.on {
  background: #edf8f6;
  border-color: rgba(15, 118, 110, 0.24);
}

.check-title {
  color: #172326;
  font-size: 14px;
  font-weight: 900;
}

.check-copy {
  margin-top: 8px;
  color: #65717a;
  font-size: 12px;
}

.switch-row {
  padding: 14px 0 0;
}

.switch-title {
  color: #172326;
  font-size: 14px;
  font-weight: 900;
}

.switch-copy {
  margin-top: 4px;
  color: #65717a;
  font-size: 12px;
  line-height: 1.5;
}

.op-error {
  margin-top: 12px;
  padding: 12px;
  border-radius: 14px;
  color: #c24141;
  background: #fff0f0;
  font-size: 13px;
}

.token-panel {
  border-color: rgba(15, 118, 110, 0.18);
}

.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  gap: 10px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(18, 37, 41, 0.08);
  box-sizing: border-box;
}

.action {
  height: 44px;
  line-height: 44px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 14px;
  font-weight: 900;
}

.action::after {
  border: 0;
}

.action.ghost {
  color: #33424a;
  background: #eef2f3;
}

.action[disabled] {
  opacity: 0.55;
}
</style>
