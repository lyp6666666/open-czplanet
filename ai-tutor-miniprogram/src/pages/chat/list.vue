<template>
  <view class="container">
    <view class="chat-list">
      <view v-for="room in roomList" :key="room.roomId" class="chat-item" @click="enterRoom(room.roomId)">
        <image class="avatar" :src="room.targetAvatar || '/static/logo.png'" mode="aspectFill"></image>
        <view class="content">
            <view class="top">
                <text class="name">{{ room.targetName }}</text>
                <text class="time">{{ formatTime(room.lastMsgTime) }}</text>
            </view>
            <view class="bottom">
                <text class="msg">{{ room.lastMsgContent }}</text>
                <view v-if="room.unreadCount > 0" class="badge">{{ room.unreadCount }}</view>
            </view>
        </view>
      </view>
      <view v-if="roomList.length === 0" class="empty">
          <text>No messages yet.</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue';
import { chatApi } from '@/api/chat';
import { onShow, onHide } from '@dcloudio/uni-app';

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
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #fff;
}
.chat-item {
    display: flex;
    padding: 15px;
    border-bottom: 1px solid #f0f0f0;
    
    .avatar {
        width: 50px;
        height: 50px;
        border-radius: 50%;
        margin-right: 15px;
        background-color: #f0f0f0;
    }
    .content {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: center;
        
        .top {
            display: flex;
            justify-content: space-between;
            margin-bottom: 5px;
            .name {
                font-weight: bold;
                font-size: 16px;
            }
            .time {
                font-size: 12px;
                color: #999;
            }
        }
        .bottom {
            display: flex;
            justify-content: space-between;
            align-items: center;
            .msg {
                font-size: 14px;
                color: #666;
                flex: 1;
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
                margin-right: 10px;
            }
            .badge {
                background-color: #ff5500;
                color: #fff;
                font-size: 10px;
                padding: 2px 6px;
                border-radius: 10px;
            }
        }
    }
}
.empty {
    text-align: center;
    color: #999;
    padding-top: 50px;
}
</style>
