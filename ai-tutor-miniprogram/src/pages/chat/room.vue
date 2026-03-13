<template>
  <view class="container">
    <scroll-view class="msg-list" scroll-y :scroll-top="scrollTop" :scroll-with-animation="true">
      <view v-for="msg in msgList" :key="msg.msgId" :class="['msg-item', msg.fromUid === myUid ? 'self' : 'other']">
        <!-- Avatar placeholder -->
        <view class="bubble">
            <text>{{ msg.body?.content || '[Unsupported Message]' }}</text>
        </view>
      </view>
    </scroll-view>
    <view class="input-area">
        <input class="input" v-model="inputText" placeholder="Type a message..." confirm-type="send" @confirm="send" />
        <button class="send-btn" size="mini" type="primary" @click="send">Send</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted, nextTick } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { chatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const myUid = userStore.userInfo?.id;
const roomId = ref<number>(0);
const msgList = ref<any[]>([]);
const inputText = ref('');
const scrollTop = ref(0);
let timer: any = null;

onLoad((options: any) => {
    if (options.id) {
        roomId.value = Number(options.id);
        startPolling();
    }
});

const fetchMessages = async () => {
    if (!roomId.value) return;
    try {
        // Simple polling: fetch last 50 messages
        // Optimization: use cursor to fetch only new messages
        const res: any = await chatApi.listMessages({ roomId: roomId.value, pageSize: 50 });
        if (res && res.list) {
            // Reverse list because backend usually returns latest first for pagination, 
            // but chat view needs oldest first. 
            // Check API response order. Usually 'list' is chronological or reverse chronological.
            // Assuming reverse chronological (newest first), so we reverse it.
            const newMsgs = [...res.list].reverse();
            
            // If new messages added, scroll to bottom
            if (newMsgs.length > msgList.value.length) {
                msgList.value = newMsgs;
                scrollToBottom();
            }
        }
    } catch (error) {
        console.error(error);
    }
};

const send = async () => {
    if (!inputText.value.trim()) return;
    const content = inputText.value;
    inputText.value = ''; // Clear input immediately
    
    try {
        await chatApi.sendText(roomId.value, content);
        fetchMessages(); // Refresh immediately
    } catch (error) {
        console.error(error);
        uni.showToast({ title: 'Send failed', icon: 'none' });
        inputText.value = content; // Restore input on fail
    }
};

const scrollToBottom = () => {
    nextTick(() => {
        scrollTop.value = 9999999;
    });
};

const startPolling = () => {
    fetchMessages();
    timer = setInterval(fetchMessages, 3000);
};

onUnload(() => {
    if (timer) clearInterval(timer);
});
</script>

<style lang="scss" scoped>
.container {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: #f5f5f5;
}
.msg-list {
    flex: 1;
    padding: 10px;
    box-sizing: border-box;
    overflow-y: auto;
}
.msg-item {
    display: flex;
    margin-bottom: 15px;
    
    &.self {
        justify-content: flex-end;
        .bubble {
            background-color: #007aff;
            color: #fff;
            border-top-right-radius: 2px;
        }
    }
    &.other {
        justify-content: flex-start;
        .bubble {
            background-color: #fff;
            color: #333;
            border-top-left-radius: 2px;
        }
    }
    
    .bubble {
        max-width: 70%;
        padding: 10px 15px;
        border-radius: 15px;
        font-size: 15px;
        word-break: break-word;
    }
}
.input-area {
    background-color: #fff;
    padding: 10px;
    display: flex;
    align-items: center;
    border-top: 1px solid #ddd;
    
    .input {
        flex: 1;
        background-color: #f5f5f5;
        height: 40px;
        border-radius: 20px;
        padding: 0 15px;
        margin-right: 10px;
    }
    .send-btn {
        width: 60px;
    }
}
</style>
