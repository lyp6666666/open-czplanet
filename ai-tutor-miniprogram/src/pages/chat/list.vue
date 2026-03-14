<template>
  <view class="container">
    <view v-if="!userStore.isLoggedIn" class="empty-state">
      <text class="tip">登录后查看消息</text>
      <u-button type="primary" color="#00bebd" shape="circle" @click="goLogin" customStyle="width: 160px;">去登录</u-button>
    </view>
    
    <view v-else-if="roomList.length === 0" class="empty-state">
      <text class="tip">暂无消息</text>
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
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue';
import { chatApi } from '@/api/chat';
import { onShow, onHide } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { resolveImageUrl } from '@/utils/request';

const userStore = useUserStore();
const roomList = ref<any[]>([]);
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
    timer = setInterval(fetchRooms, 5000);
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
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg);
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
}

.chat-list {
  background-color: #ffffff;
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
