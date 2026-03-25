<template>
  <view class="container">
    <scroll-view class="msg-list" scroll-y :scroll-top="scrollTop" :scroll-with-animation="true">
      <view v-for="msg in msgList" :key="msg.msgId" :class="['msg-item', msg.fromUid === myUid ? 'self' : 'other']">
        <!-- Avatar placeholder -->
        <view v-if="msg.msgType === 1" class="bubble">
          <text>{{ msg.body?.content || '' }}</text>
        </view>

        <view v-else-if="msg.msgType === 8 && msg.body?.type === 'tutor_application'" class="sys-card">
          <view class="sys-title">家教申请</view>
          <view class="sys-row">
            <text class="k">申请语</text>
            <text class="v">{{ msg.body.content }}</text>
          </view>
          <view class="sys-row">
            <text class="k">状态</text>
            <text class="v">{{ toApplicationStatusText(msg.body.status, msg.body.creatorUserId === myUid) }}</text>
          </view>
          <view v-if="msg.body.creatorUserId !== myUid && msg.body.status === 'PENDING'" class="sys-ops">
            <u-button size="mini" type="primary" :disabled="decisionBusy[msg.body.applicationId]" @click="decide(msg.body.applicationId, 'ACCEPT')">
              {{ decisionBusy[msg.body.applicationId] ? '处理中...' : '通过' }}
            </u-button>
            <u-button size="mini" type="default" :disabled="decisionBusy[msg.body.applicationId]" @click="decide(msg.body.applicationId, 'REJECT')">
              拒绝
            </u-button>
          </view>
        </view>

        <view v-else-if="msg.msgType === 8 && msg.body?.type === 'tutor_application_status'" class="sys-card">
          <view class="sys-title">家教申请</view>
          <view class="sys-row">
            <text class="k">状态</text>
            <text class="v">{{ toApplicationStatusText(msg.body.status, false) }}</text>
          </view>
        </view>

        <view v-else-if="msg.msgType === 8 && msg.body?.type === 'brokerage_required'" class="sys-card">
          <view class="sys-title">支付信息费后开放聊天</view>
          <view class="sys-row">
            <text class="k">金额</text>
            <text class="v">{{ formatFen(msg.body.amountFen) }}</text>
          </view>
          <view class="sys-row">
            <text class="k">状态</text>
            <text class="v">{{ msg.body.status }}</text>
          </view>
          <view v-if="msg.body.payerUserId === myUid && msg.body.status === 'PENDING'" class="sys-ops">
            <u-button size="mini" type="primary" :disabled="payBusy" @click="payBrokerage(msg.body.orderId)">
              {{ payBusy ? '拉起支付...' : '去支付' }}
            </u-button>
          </view>
        </view>

        <view v-else-if="msg.msgType === 8 && msg.body?.type === 'contact_unlocked'" class="sys-card">
          <view class="sys-title">联系方式已解锁</view>
          <view class="sys-row">
            <text class="v">现在可以开始自由沟通，并进入合作流程。</text>
          </view>
        </view>

        <view v-else class="bubble">
          <text>[系统消息]</text>
        </view>
      </view>
    </scroll-view>
    <view class="input-area">
        <input class="input" v-model="inputText" placeholder="Type a message..." confirm-type="send" @confirm="send" />
        <u-button class="send-btn" size="mini" type="primary" @click="send">Send</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted, nextTick } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { chatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';
import { request } from '@/utils/request';

const userStore = useUserStore();
const myUid = userStore.userInfo?.id;
const roomId = ref<number>(0);
const msgList = ref<any[]>([]);
const inputText = ref('');
const scrollTop = ref(0);
const decisionBusy = ref<Record<number, boolean>>({});
const payBusy = ref(false);
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

function toApplicationStatusText(status: string, fromMe: boolean) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return fromMe ? '等待对方回复' : '待你处理';
  if (s === 'ACCEPTED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  return '状态未知';
}

function formatFen(fen: any) {
  const v = Number(fen);
  if (!Number.isFinite(v)) return '';
  return `${(v / 100).toFixed(2)} 元`;
}

const decide = async (applicationId: number, action: 'ACCEPT' | 'REJECT') => {
  if (!applicationId) return;
  if (decisionBusy.value[applicationId]) return;
  decisionBusy.value = { ...decisionBusy.value, [applicationId]: true };
  try {
    await chatApi.decideApplication(applicationId, action);
    await fetchMessages();
  } catch (error: any) {
    uni.showToast({ title: error?.message || '操作失败', icon: 'none' });
  } finally {
    decisionBusy.value = { ...decisionBusy.value, [applicationId]: false };
  }
};

const payBrokerage = async (orderId: number) => {
  if (!orderId) return;
  if (payBusy.value) return;
  payBusy.value = true;
  try {
    const payRes: any = await request({
      url: '/payment/create',
      method: 'POST',
      data: {
        channel: 'WECHAT',
        contextId: orderId,
        contextType: 'BROKERAGE_ORDER',
        openid: userStore.userInfo?.openid
      },
      loading: true
    });

    if (payRes && payRes.payParams) {
      const params = payRes.payParams;
      if (params.mock) {
        uni.showModal({
          title: '模拟支付',
          content: '是否模拟支付成功？',
          success: (res) => {
            if (res.confirm) {
              uni.showToast({ title: '支付成功（模拟）', icon: 'success' });
            } else {
              uni.showToast({ title: '已取消（模拟）', icon: 'none' });
            }
          }
        });
        return;
      }

      await new Promise<void>((resolve, reject) => {
        uni.requestPayment({
          provider: 'wxpay',
          timeStamp: params.timeStamp,
          nonceStr: params.nonceStr,
          package: params.package,
          signType: params.signType,
          paySign: params.paySign,
          success: function () {
            uni.showToast({ title: '支付成功', icon: 'success' });
            resolve();
          },
          fail: function (err: any) {
            reject(err);
          }
        } as any);
      });
      await fetchMessages();
      return;
    }
    uni.showToast({ title: '支付参数缺失', icon: 'none' });
  } catch (error: any) {
    uni.showToast({ title: error?.message || '支付失败', icon: 'none' });
  } finally {
    payBusy.value = false;
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

.sys-card {
    max-width: 80%;
    padding: 12px 14px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.08);
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 13px;
}

.sys-title {
    font-weight: 900;
    color: #1f2329;
}

.sys-row {
    display: flex;
    gap: 8px;
    align-items: flex-start;
}

.sys-row .k {
    width: 44px;
    color: #646a73;
    flex: 0 0 auto;
}

.sys-row .v {
    flex: 1 1 auto;
    word-break: break-word;
    color: #1f2329;
}

.sys-ops {
    display: flex;
    gap: 10px;
    margin-top: 2px;
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
