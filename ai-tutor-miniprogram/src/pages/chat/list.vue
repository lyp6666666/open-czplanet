<template>
  <view class="container">
    <view v-if="!userStore.isLoggedIn" class="empty-state">
      <text class="tip">登录后查看消息</text>
      <u-button type="primary" color="#00bebd" shape="circle" @click="goLogin" customStyle="width: 160px;">去登录</u-button>
    </view>

    <template v-else>
      <view class="hub">
        <view class="hub-copy">
          <text class="hub-kicker">消息</text>
          <text class="hub-title">申请与聊天</text>
          <text class="hub-desc">先处理申请，通过并支付后再进入聊天。</text>
        </view>
        <view class="application-entry" @click="goApplications">
          <text>申请中心</text>
          <view v-if="applicationUnread > 0" class="app-badge">{{ applicationUnread > 99 ? '99+' : applicationUnread }}</view>
        </view>
      </view>

      <view v-if="loadError" class="empty-state in-page">
        <text class="tip">消息加载失败</text>
        <text class="subtip">{{ loadError }}</text>
        <button class="retry-btn" @click="refreshAll">重新加载</button>
      </view>

      <view v-else-if="todoCards.length" class="todo-strip">
        <view
          v-for="card in todoCards"
          :key="card.key"
          class="todo-card"
          :class="card.tone"
          @click="openTodo(card)"
        >
          <text class="todo-num">{{ card.count }}</text>
          <text class="todo-title">{{ card.title }}</text>
          <text class="todo-desc">{{ card.desc }}</text>
        </view>
      </view>

      <view v-else-if="roomList.length === 0" class="empty-state in-page">
        <text class="tip">暂无聊天</text>
        <text class="subtip">发起申请并完成信息费支付后，会出现聊天会话。</text>
      </view>

      <view v-else class="chat-list">
        <view v-for="room in roomList" :key="room.roomId" class="chat-item" @click="enterRoom(room.roomId)">
          <image class="avatar" :src="resolveImageUrl(room.targetAvatar)" mode="aspectFill"></image>
          <view class="content">
            <view class="header">
              <text class="name">{{ room.targetName }}</text>
              <text class="time">{{ formatTime(room.lastMsgTime) }}</text>
            </view>
            <view class="footer">
              <text class="msg">{{ room.lastMsgContent }}</text>
              <view v-if="room.unreadCount > 0" class="badge">{{ room.unreadCount > 99 ? '99+' : room.unreadCount }}</view>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue';
import { chatApi } from '@/api/chat';
import { applicationApi } from '@/api/application';
import { onShow, onHide, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { resolveImageUrl } from '@/utils/request';

const userStore = useUserStore();
const roomList = ref<any[]>([]);
const applicationUnread = ref(0);
const cursor = ref<number | null>(null);
const isLast = ref(false);
const loadingMore = ref(false);
const loadError = ref('');
const receivedPendingCount = ref(0);
const sentPendingCount = ref(0);
const chatReadyApplicationCount = ref(0);
let timer: any = null;

const todoCards = computed(() => {
  return [
    {
      key: 'received',
      title: '待你处理',
      desc: '优先处理收到的申请',
      count: receivedPendingCount.value,
      tone: 'pending',
      tab: 'received',
    },
    {
      key: 'sent',
      title: '等待回复',
      desc: '查看已发出申请进度',
      count: sentPendingCount.value,
      tone: 'soft',
      tab: 'sent',
    },
    {
      key: 'ready',
      title: '已可聊天',
      desc: '信息费已完成，可继续推进',
      count: chatReadyApplicationCount.value,
      tone: 'ready',
      tab: 'received',
      chatReadyOnly: true,
    },
  ].filter((card) => card.count > 0);
});

function normalizeCursor(v: unknown): number | null {
  if (v == null) return null;
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  const n = Number(String(v || '').trim());
  return Number.isFinite(n) ? n : null;
}

const fetchRooms = async (reset = false) => {
  try {
    const res: any = await chatApi.listRooms({
      pageSize: 20,
      cursor: reset ? null : cursor.value,
    });
    if (res && res.list) {
      const nextList = Array.isArray(res.list) ? res.list : [];
      roomList.value = reset ? nextList : [...roomList.value, ...nextList];
      cursor.value = normalizeCursor(res.nextCursor ?? res.cursor);
      isLast.value = !!res.isLast || !nextList.length;
    }
    loadError.value = '';
  } catch (error) {
    console.error(error);
    if (reset) roomList.value = [];
    loadError.value = '会话列表暂时不可用，请稍后重试。';
  } finally {
    loadingMore.value = false;
  }
};

const fetchApplicationUnread = async () => {
  try {
    const res = await applicationApi.unread();
    applicationUnread.value = Number(res.unreadCount || 0);
  } catch {
    applicationUnread.value = 0;
  }
};

const fetchApplicationSummary = async () => {
  try {
    const [receivedPage, sentPage] = await Promise.all([
      applicationApi.received({ pageSize: 20 }),
      applicationApi.sent({ pageSize: 20 }),
    ]);
    const receivedList = Array.isArray(receivedPage?.list) ? receivedPage.list : [];
    const sentList = Array.isArray(sentPage?.list) ? sentPage.list : [];
    receivedPendingCount.value = receivedList.filter((it: any) => String(it.status || '').toUpperCase() === 'PENDING').length;
    sentPendingCount.value = sentList.filter((it: any) => String(it.status || '').toUpperCase() === 'PENDING').length;
    chatReadyApplicationCount.value = [...receivedList, ...sentList].filter(
      (it: any) => String(it.chatAccessStatus || '').toUpperCase() === 'CHAT_ENABLED',
    ).length;
  } catch {
    receivedPendingCount.value = 0;
    sentPendingCount.value = 0;
    chatReadyApplicationCount.value = 0;
  }
};

const enterRoom = (roomId: number) => {
  uni.navigateTo({ url: `/pages/chat/room?id=${roomId}` });
};

const formatTime = (ts: number) => {
    if (!ts) return '';
    const date = new Date(ts);
    const now = new Date();
    if (date.toDateString() === now.toDateString()) {
        return date.getHours() + ':' + date.getMinutes().toString().padStart(2, '0');
    }
    return date.getMonth() + 1 + '/' + date.getDate();
};

const startPolling = () => {
    if (!userStore.isLoggedIn) return;
    refreshAll();
    timer = setInterval(() => {
      fetchRooms(true);
      fetchApplicationUnread();
      fetchApplicationSummary();
    }, 5000);
};

const stopPolling = () => {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
};

onShow(() => {
    startPolling();
});

onHide(() => {
    stopPolling();
});

onUnmounted(() => {
    stopPolling();
});

function refreshAll() {
  cursor.value = null;
  isLast.value = false;
  loadingMore.value = false;
  void fetchRooms(true);
  void fetchApplicationUnread();
  void fetchApplicationSummary();
}

function loadMore() {
  if (loadingMore.value || isLast.value || !userStore.isLoggedIn) return;
  loadingMore.value = true;
  void fetchRooms(false);
}

onReachBottom(() => {
  loadMore();
});

onPullDownRefresh(async () => {
  refreshAll();
  setTimeout(() => uni.stopPullDownRefresh(), 300);
});

const goLogin = () => {
  uni.switchTab({ url: '/pages/me/index' });
};

const goApplications = () => {
  uni.navigateTo({ url: '/pages/application/list' });
};

function openTodo(card: any) {
  if (card.chatReadyOnly) {
    uni.navigateTo({ url: '/pages/application/list?tab=received&chatAccessStatus=CHAT_ENABLED' });
    return;
  }
  const status = card.key === 'received' || card.key === 'sent' ? 'PENDING' : '';
  const query = [`tab=${card.tab}`];
  if (status) query.push(`status=${status}`);
  uni.navigateTo({ url: `/pages/application/list?${query.join('&')}` });
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 14px;
  box-sizing: border-box;
  background-color: #f5f7f8;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 100px;

  .tip {
    font-size: 14px;
    color: var(--muted);
    margin-bottom: 20px;
  }

  .subtip {
    color: #8a949c;
    font-size: 12px;
    line-height: 1.6;
    text-align: center;
  }
}

.empty-state.in-page {
  background: #fff;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 16px;
  padding: 40px 20px;
  margin-top: 12px;
}

.retry-btn {
  margin-top: 14px;
  min-width: 120px;
  height: 38px;
  line-height: 38px;
  border: 0;
  border-radius: 999px;
  background: #00bebd;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
}

.retry-btn::after {
  border: 0;
}

.hub {
  display: grid;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 18px;
  background: #111827;
  color: #fff;
  box-shadow: 0 14px 34px rgba(17, 24, 39, 0.18);
}

.hub-kicker,
.hub-title,
.hub-desc {
  display: block;
}

.hub-kicker {
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
  margin-bottom: 4px;
}

.hub-title {
  font-size: 22px;
  font-weight: 900;
  margin-bottom: 6px;
}

.hub-desc {
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
}

.application-entry {
  height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 900;
}

.app-badge {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  background: #ff5a5f;
  color: #fff;
  line-height: 22px;
  text-align: center;
  font-size: 11px;
}

.todo-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.todo-card {
  min-height: 88px;
  padding: 12px 10px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 22px rgba(17, 24, 39, 0.06);
  box-sizing: border-box;
}

.todo-card.pending {
  background: #fff4dc;
}

.todo-card.soft {
  background: #eef3f5;
}

.todo-card.ready {
  background: #e3f7f1;
}

.todo-num,
.todo-title,
.todo-desc {
  display: block;
}

.todo-num {
  color: #111827;
  font-size: 20px;
  font-weight: 900;
}

.todo-title {
  margin-top: 6px;
  color: #1f2937;
  font-size: 13px;
  font-weight: 900;
}

.todo-desc {
  margin-top: 4px;
  color: #6b7280;
  font-size: 11px;
  line-height: 1.5;
}

.chat-list {
  background-color: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 16px;
  overflow: hidden;
}

.chat-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid rgba(31, 35, 41, 0.08);
  align-items: center;

  &:active {
    background-color: #f9f9f9;
  }

  .avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    margin-right: 12px;
    background-color: #f0f0f0;
    flex-shrink: 0;
  }

  .content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      margin-bottom: 4px;

      .name {
        font-weight: 700;
        font-size: 16px;
        color: var(--text);
      }

      .time {
        font-size: 11px;
        color: var(--muted);
      }
    }

    .footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .msg {
        font-size: 13px;
        color: var(--muted);
        flex: 1;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        margin-right: 10px;
      }

      .badge {
        background-color: #ff4d4f;
        color: #fff;
        font-size: 10px;
        height: 16px;
        min-width: 16px;
        padding: 0 4px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
      }
    }
  }
}
</style>
