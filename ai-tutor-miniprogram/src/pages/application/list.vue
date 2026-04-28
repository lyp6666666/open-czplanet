<template>
  <view class="page">
    <AppStateCard
      v-if="!userStore.isLoggedIn"
      title="登录后查看申请"
      description="老师和学生的沟通申请都会收在这里。"
      action-text="去登录"
      variant="soft"
      @action="goLogin"
    />

    <template v-else>
      <view class="hero">
        <view>
          <text class="eyebrow">申请中心</text>
          <text class="title">先确认，再聊天</text>
          <text class="subtitle">申请通过后，教师完成信息费支付才会开放聊天和试课合作。</text>
        </view>
        <view v-if="unreadCount > 0" class="unread">{{ unreadCount }}</view>
      </view>

      <view class="tabs">
        <view class="tab" :class="{ active: tab === 'received' }" @click="switchTab('received')">
          <text>收到的</text>
          <text v-if="unreadCount > 0" class="dot">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
        </view>
        <view class="tab" :class="{ active: tab === 'sent' }" @click="switchTab('sent')">发出的</view>
      </view>

      <AppStateCard
        v-if="error"
        title="申请列表加载失败"
        :description="error"
        action-text="重试"
        variant="error"
        @action="reload"
      />

      <AppStateCard
        v-else-if="list.length === 0 && !loading"
        title="暂无申请"
        description="从需求页或老师详情发起申请后，会在这里同步状态。"
        variant="soft"
      />

      <view v-else class="list">
        <view v-for="it in list" :key="it.id" class="card" @click="openDetail(it.id)">
          <view class="card-head">
            <view>
              <text class="card-title">{{ contextText(it) }}</text>
              <text class="card-sub">对方 ID {{ otherUid(it) }}</text>
            </view>
            <text class="status" :class="statusTone(it.status)">{{ statusText(it.status) }}</text>
          </view>
          <text class="content">{{ it.content || '暂无申请内容' }}</text>
          <view class="meta">
            <text>{{ accessText(it) }}</text>
            <text>{{ formatTime(it.createTime) }}</text>
          </view>
        </view>
      </view>

      <view class="load-more">
        <u-button v-if="!isLast" :loading="loading" shape="circle" @click="loadPage">{{ loading ? '加载中' : '加载更多' }}</u-button>
        <text v-else-if="list.length > 0" class="end">没有更多了</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app';
import { applicationApi, type TutorApplication } from '@/api/application';
import { useUserStore } from '@/stores/user';
import AppStateCard from '@/components/AppStateCard.vue';

type TabKey = 'received' | 'sent';

const userStore = useUserStore();
const tab = ref<TabKey>('received');
const received = ref<TutorApplication[]>([]);
const sent = ref<TutorApplication[]>([]);
const receivedCursor = ref<number | null>(null);
const sentCursor = ref<number | null>(null);
const receivedLast = ref(false);
const sentLast = ref(false);
const loading = ref(false);
const error = ref('');
const unreadCount = ref(0);
const contextIdFilter = ref<number | null>(null);
const contextTypeFilter = ref<string>('');

const rawList = computed(() => (tab.value === 'received' ? received.value : sent.value));
const list = computed(() => {
  const id = contextIdFilter.value;
  const type = String(contextTypeFilter.value || '').trim().toUpperCase();
  if (!id && !type) return rawList.value;
  return rawList.value.filter((it) => {
    const sameId = id ? Number(it.contextId) === id : true;
    const sameType = type ? String(it.contextType || '').toUpperCase() === type : true;
    return sameId && sameType;
  });
});
const isLast = computed(() => (tab.value === 'received' ? receivedLast.value : sentLast.value));

function statusText(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '待处理';
  if (s === 'ACCEPTED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  return s || '未知';
}

function statusTone(status: string) {
  const s = String(status || '').toUpperCase();
  return {
    pending: s === 'PENDING',
    accepted: s === 'ACCEPTED',
    rejected: s === 'REJECTED',
  };
}

function contextText(it: TutorApplication) {
  if (it.contextType === 'DEMAND') return `需求申请 #${it.contextId}`;
  if (it.contextType === 'ORG_POSTING') return `机构需求 #${it.contextId}`;
  return `老师申请 #${it.contextId}`;
}

function accessText(it: TutorApplication) {
  if (it.status === 'PENDING') return '等待对方处理';
  if (it.status === 'REJECTED') return '本次申请已结束';
  if (it.chatAccessStatus === 'PAYMENT_REQUIRED') return '待教师支付信息费';
  if (it.chatAccessStatus === 'CHAT_ENABLED') return '聊天已开放';
  return '查看下一步';
}

function otherUid(it: TutorApplication) {
  const mine = userStore.userInfo?.id;
  if (mine && it.senderUid === mine) return it.receiverUid;
  if (mine && it.receiverUid === mine) return it.senderUid;
  return it.senderUid;
}

function formatTime(v?: string) {
  if (!v) return '';
  return String(v).slice(5, 16).replace('T', ' ');
}

async function loadUnread() {
  try {
    const res = await applicationApi.unread();
    unreadCount.value = Number(res.unreadCount || 0);
  } catch {
    unreadCount.value = 0;
  }
}

async function loadPage() {
  if (!userStore.isLoggedIn || loading.value || isLast.value) return;
  loading.value = true;
  error.value = '';
  try {
    const params = { pageSize: 12, cursor: tab.value === 'received' ? receivedCursor.value : sentCursor.value };
    const page = tab.value === 'received' ? await applicationApi.received(params) : await applicationApi.sent(params);
    const rows = page.list || [];
    if (tab.value === 'received') {
      received.value = [...received.value, ...rows];
      receivedCursor.value = page.cursor ?? null;
      receivedLast.value = !!page.isLast;
    } else {
      sent.value = [...sent.value, ...rows];
      sentCursor.value = page.cursor ?? null;
      sentLast.value = !!page.isLast;
    }
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载申请失败';
  } finally {
    loading.value = false;
  }
}

async function reload() {
  if (tab.value === 'received') {
    received.value = [];
    receivedCursor.value = null;
    receivedLast.value = false;
  } else {
    sent.value = [];
    sentCursor.value = null;
    sentLast.value = false;
  }
  await loadUnread();
  await loadPage();
}

function switchTab(next: TabKey) {
  if (tab.value === next) return;
  tab.value = next;
  if (rawList.value.length === 0) void loadPage();
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/application/detail?id=${id}` });
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

onLoad((options: any) => {
  const nextTab = String(options?.tab || '').trim();
  if (nextTab === 'received' || nextTab === 'sent') tab.value = nextTab;
  const contextId = Number(options?.contextId);
  contextIdFilter.value = Number.isFinite(contextId) && contextId > 0 ? contextId : null;
  contextTypeFilter.value = String(options?.contextType || '').trim().toUpperCase();
});

onShow(() => {
  if (!userStore.isLoggedIn) return;
  void loadUnread();
  if (rawList.value.length === 0) void loadPage();
});

onReachBottom(() => {
  void loadPage();
});

onPullDownRefresh(async () => {
  await reload();
  uni.stopPullDownRefresh();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px;
  background: #f5f7f8;
  box-sizing: border-box;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #0f1720 0%, #173a3b 58%, #0f766e 100%);
  color: #fff;
  box-shadow: 0 16px 36px rgba(15, 23, 32, 0.22);
}

.eyebrow,
.title,
.subtitle {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.72;
  margin-bottom: 6px;
}

.title {
  font-size: 22px;
  font-weight: 900;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.82;
}

.unread {
  min-width: 34px;
  height: 34px;
  border-radius: 17px;
  background: #ff5a5f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px;
  margin: 14px 0;
  border-radius: 14px;
  background: #e9eef0;
}

.tab {
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #5f6b73;
  font-weight: 800;
  font-size: 14px;
}

.tab.active {
  color: #0f1720;
  background: #fff;
  box-shadow: 0 6px 16px rgba(15, 23, 32, 0.08);
}

.dot {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  color: #fff;
  background: #ff5a5f;
  font-size: 10px;
  line-height: 18px;
}

.list {
  display: grid;
  gap: 12px;
}

.card,
.state,
.login-panel {
  background: #fff;
  border: 1px solid rgba(15, 23, 32, 0.08);
  border-radius: 16px;
  box-shadow: 0 10px 24px rgba(15, 23, 32, 0.06);
}

.card {
  padding: 14px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.card-title,
.card-sub,
.content,
.meta {
  display: block;
}

.card-title {
  color: #111827;
  font-weight: 900;
  font-size: 15px;
  margin-bottom: 3px;
}

.card-sub,
.meta {
  color: #6b7280;
  font-size: 12px;
}

.status {
  flex-shrink: 0;
  height: 24px;
  padding: 0 9px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 900;
  line-height: 24px;
}

.status.pending {
  color: #9a5b00;
  background: #fff3d4;
}

.status.accepted {
  color: #087268;
  background: #dff7f4;
}

.status.rejected {
  color: #b42318;
  background: #ffe4e0;
}

.content {
  color: #27313a;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 10px;
}

.meta {
  display: flex;
  justify-content: space-between;
}

.state,
.login-panel {
  padding: 30px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
}

.state-title,
.login-title {
  font-size: 17px;
  font-weight: 900;
  color: #111827;
}

.state-desc,
.login-desc {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.error {
  color: #b42318;
}

.load-more {
  padding: 16px 0 6px;
  text-align: center;
}

.end {
  color: #8a949c;
  font-size: 12px;
}
</style>
