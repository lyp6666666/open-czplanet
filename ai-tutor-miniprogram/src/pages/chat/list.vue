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

      <view v-if="roomList.length === 0" class="empty-state in-page">
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
import { ref, onUnmounted } from 'vue';
import { chatApi } from '@/api/chat';
import { applicationApi } from '@/api/application';
import { onShow, onHide } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { resolveImageUrl } from '@/utils/request';

const userStore = useUserStore();
const roomList = ref<any[]>([]);
const applicationUnread = ref(0);
let timer: any = null;

const fetchRooms = async () => {
  try {
    const res: any = await chatApi.listRooms({ pageSize: 20 });
    if (res && res.list) {
        roomList.value = res.list;
    }
  } catch (error) {
    console.error(error);
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
    fetchRooms();
    fetchApplicationUnread();
    timer = setInterval(() => {
      fetchRooms();
      fetchApplicationUnread();
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

const goLogin = () => {
  uni.switchTab({ url: '/pages/me/index' });
};

const goApplications = () => {
  uni.navigateTo({ url: '/pages/application/list' });
};
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
