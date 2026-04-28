<template>
  <view class="container">
    <scroll-view class="msg-list" scroll-y :scroll-top="scrollTop" :scroll-with-animation="true">
      <view v-if="hasMoreHistory" class="history-entry" @click="loadOlderMessages">
        {{ historyBusy ? '正在加载更多消息...' : '查看更早消息' }}
      </view>
      <view v-for="msg in msgList" :key="messageId(msg)" :class="['msg-item', msg.fromUid === myUid ? 'self' : 'other']">
        <view v-if="isTextMessage(msg)" class="bubble">
          <text>{{ textContent(msg) }}</text>
          <view v-if="msg.localStatus" class="send-state" :class="msg.localStatus" @click.stop="retryMessage(msg)">
            {{ sendStateText(msg.localStatus) }}
          </view>
        </view>

        <view v-else-if="isImageMessage(msg)" class="image-wrap" @click="previewImage(imageUrl(msg))">
          <image class="chat-image" :src="imageUrl(msg)" mode="widthFix"></image>
          <view v-if="msg.localStatus" class="send-state image-state" :class="msg.localStatus" @click.stop="retryMessage(msg)">
            {{ sendStateText(msg.localStatus) }}
          </view>
        </view>

        <view v-else-if="bodyType(msg) === 'tutor_application'" class="sys-card accent">
          <view class="sys-head">
            <text class="sys-title">家教申请</text>
            <text class="sys-badge">{{ toApplicationStatusText(msg.body.status, msg.body.creatorUserId === myUid) }}</text>
          </view>
          <text class="sys-desc">{{ msg.body.content || '对方发起了家教申请。' }}</text>
          <view v-if="msg.body.creatorUserId !== myUid && msg.body.status === 'PENDING'" class="sys-ops">
            <button class="mini ghost" :disabled="decisionBusy[msg.body.applicationId]" @click="decide(msg.body.applicationId, 'REJECT')">拒绝</button>
            <button class="mini" :disabled="decisionBusy[msg.body.applicationId]" @click="decide(msg.body.applicationId, 'ACCEPT')">
              {{ decisionBusy[msg.body.applicationId] ? '处理中' : '通过' }}
            </button>
          </view>
        </view>

        <view v-else-if="bodyType(msg) === 'tutor_application_status'" class="sys-card">
          <view class="sys-head">
            <text class="sys-title">申请状态</text>
            <text class="sys-badge">{{ toApplicationStatusText(msg.body.status, false) }}</text>
          </view>
          <text class="sys-desc">申请状态已更新，请按页面提示完成下一步。</text>
        </view>

        <view v-else-if="bodyType(msg) === 'brokerage_required'" class="sys-card pay">
          <view class="sys-head">
            <view class="sys-head-main">
              <text class="sys-title">支付信息费后开放聊天</text>
              <button class="sys-link" @click="openInfoFeePolicy(msg.body.payerUserId === myUid ? 'teacher' : 'student')">为什么先收费</button>
            </view>
            <text class="sys-badge">{{ orderStatusText(msg.body.status) }}</text>
          </view>
          <view class="amount">{{ formatFen(msg.body.amountFen) }}</view>
          <text class="sys-desc">{{ msg.body.payerUserId === myUid ? '支付后可继续确认详细需求与合作安排。' : '教师支付后，双方可继续确认详细需求与合作安排。' }}</text>
          <view v-if="msg.body.payerUserId === myUid && msg.body.status === 'PENDING'" class="sys-ops">
            <button class="mini" :disabled="payBusy" @click="payBrokerage(msg.body.orderId)">
              {{ payBusy ? '拉起支付' : '去支付' }}
            </button>
          </view>
        </view>

        <view v-else-if="bodyType(msg) === 'contact_unlocked'" class="sys-card success">
          <view class="sys-head">
            <text class="sys-title">联系方式已解锁</text>
            <text class="sys-badge green">可沟通</text>
          </view>
          <text class="sys-desc">现在可以自由沟通，并确认试课合作。</text>
        </view>

        <view v-else-if="bodyType(msg) === 'collaboration_proposal'" class="sys-card proposal">
          <view class="sys-head">
            <text class="sys-title">试课合作提案</text>
            <text class="sys-badge">{{ proposalStatusText(msg.body.status) }}</text>
          </view>
          <view class="proposal-grid">
            <view>
              <text class="k">课时费</text>
              <text class="v">{{ msg.body.pricePerHour || '待确认' }}</text>
            </view>
            <view>
              <text class="k">试课时间</text>
              <text class="v">{{ formatRange(msg.body.trialStartAt, msg.body.trialEndAt) }}</text>
            </view>
          </view>
          <text v-if="msg.body.remark" class="sys-desc">{{ msg.body.remark }}</text>
          <view v-if="msg.body.creatorUserId !== myUid && msg.body.status === 'PENDING'" class="sys-ops">
            <button class="mini ghost" :disabled="proposalBusy[msg.body.proposalId]" @click="respondProposal(msg.body.proposalId, 'REJECT')">拒绝</button>
            <button class="mini" :disabled="proposalBusy[msg.body.proposalId]" @click="respondProposal(msg.body.proposalId, 'ACCEPT')">
              {{ proposalBusy[msg.body.proposalId] ? '处理中' : '接受' }}
            </button>
          </view>
        </view>

        <view v-else-if="bodyType(msg) === 'collaboration_status'" class="sys-card">
          <view class="sys-head">
            <text class="sys-title">合作提案状态</text>
            <text class="sys-badge">{{ proposalStatusText(msg.body.status) }}</text>
          </view>
          <text class="sys-desc">{{ msg.body.status === 'ACCEPTED' ? '合作已确认，可进入我的合作查看试课和课程进展。' : '合作提案状态已更新。' }}</text>
          <view v-if="msg.body.status === 'ACCEPTED'" class="sys-ops">
            <button class="mini" @click="goCourseByRoom">查看合作</button>
          </view>
        </view>

        <view v-else-if="bodyType(msg) === 'brokerage_refund_request' || bodyType(msg) === 'brokerage_refund_status'" class="sys-card refund">
          <view class="sys-head">
            <text class="sys-title">退款进度</text>
            <text class="sys-badge red">{{ refundStatusText(msg.body.status) }}</text>
          </view>
          <text class="sys-desc">信息费退费状态已更新，可在我的合作中继续查看。</text>
        </view>

        <view v-else class="sys-card">
          <text class="sys-title">系统消息</text>
          <text class="sys-desc">{{ fallbackText(msg) }}</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="lockedHint" class="lock-bar">
      <text>{{ lockedHint }}</text>
    </view>

    <view v-if="peerTyping" class="typing-hint">对方正在输入...</view>

    <view class="input-area">
      <button class="tool-btn image-tool" :disabled="!chatUnlocked || imageBusy" @click="chooseAndSendImage">图片</button>
      <button class="tool-btn" :disabled="!chatUnlocked" @click="openProposal">合作</button>
      <input class="input" v-model="inputText" :disabled="!chatUnlocked" :placeholder="chatUnlocked ? '输入消息...' : '聊天待解锁'" confirm-type="send" @input="onTextInput" @confirm="send" />
      <button class="send-btn" :disabled="!chatUnlocked" @click="send">发送</button>
    </view>

    <view v-if="proposalOpen" class="sheet-mask" @click.self="proposalOpen = false">
      <view class="sheet">
        <text class="sheet-title">发起试课合作</text>
        <input v-model="proposalPrice" class="field" placeholder="课时费，如 200元/小时" />
        <picker mode="date" :value="proposalDate" @change="proposalDate = String($event.detail.value)">
          <view class="field picker-value">试课日期：{{ proposalDate }}</view>
        </picker>
        <picker mode="time" :value="proposalStartTime" @change="proposalStartTime = String($event.detail.value)">
          <view class="field picker-value">开始时间：{{ proposalStartTime }}</view>
        </picker>
        <picker mode="time" :value="proposalEndTime" @change="proposalEndTime = String($event.detail.value)">
          <view class="field picker-value">结束时间：{{ proposalEndTime }}</view>
        </picker>
        <textarea v-model="proposalRemark" class="textarea" placeholder="补充说明（选填）" maxlength="200" />
        <button class="submit" :disabled="proposalSubmitBusy" @click="submitProposal">
          {{ proposalSubmitBusy ? '发送中...' : '发送提案' }}
        </button>
      </view>
    </view>

    <InfoFeePolicySheet :open="infoFeePolicyOpen" :viewer-role="infoFeePolicyRole" @close="closeInfoFeePolicy" />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { assetsApi } from '@/api/assets';
import { chatApi } from '@/api/chat';
import { courseApi } from '@/api/course';
import { paymentApi } from '@/api/payment';
import { useUserStore } from '@/stores/user';
import InfoFeePolicySheet from '@/components/InfoFeePolicySheet.vue';

type LocalStatus = 'sending' | 'failed';

const userStore = useUserStore();
const myUid = userStore.userInfo?.id;
const roomId = ref<number>(0);
const msgList = ref<any[]>([]);
const inputText = ref('');
const scrollTop = ref(0);
const decisionBusy = ref<Record<number, boolean>>({});
const proposalBusy = ref<Record<number, boolean>>({});
const payBusy = ref(false);
const proposalOpen = ref(false);
const proposalSubmitBusy = ref(false);
const proposalPrice = ref('');
const proposalDate = ref(todayString());
const proposalStartTime = ref('19:00');
const proposalEndTime = ref('20:00');
const proposalRemark = ref('');
const infoFeePolicyOpen = ref(false);
const infoFeePolicyRole = ref<'teacher' | 'student'>('teacher');
const imageBusy = ref(false);
const peerTyping = ref(false);
const lastEventId = ref<number | null>(null);
const lastAckReadMsgId = ref<number | null>(null);
const lastDeliveredMsgId = ref<number | null>(null);
const typingReported = ref(false);
const historyBusy = ref(false);
const historyCursor = ref<number | string | null>(null);
const hasMoreHistory = ref(false);
let timer: any = null;
let typingTimer: any = null;
let peerTypingTimer: any = null;

const chatUnlocked = computed(() => {
  const bodies = msgList.value.map((it) => it.body || {});
  const hasUnlock = bodies.some((it) => it.type === 'contact_unlocked');
  const pendingPay = bodies.some((it) => it.type === 'brokerage_required' && String(it.status || '').toUpperCase() === 'PENDING');
  if (hasUnlock) return true;
  return !pendingPay;
});

const lockedHint = computed(() => (chatUnlocked.value ? '' : '教师完成信息费支付后，双方才可以继续聊天和发起试课合作。'));

onLoad((options: any) => {
  if (options.id) {
    roomId.value = Number(options.id);
    startPolling();
  }
});

const fetchMessages = async () => {
  if (!roomId.value) return;
  try {
    const res: any = await chatApi.listMessages({ roomId: roomId.value, pageSize: 50 });
    if (res && res.list) {
      const newMsgs = [...res.list].reverse().map(normalizeMessage);
      if (newMsgs.length !== msgList.value.length || messageId(newMsgs[newMsgs.length - 1]) !== messageId(msgList.value[msgList.value.length - 1])) {
        msgList.value = [...msgList.value.filter((it) => it.localStatus), ...newMsgs];
        scrollToBottom();
      }
      historyCursor.value = res?.cursor ?? res?.nextCursor ?? null;
      hasMoreHistory.value = !res?.isLast && newMsgs.length > 0;
      ackLatestMessage(newMsgs);
    }
  } catch (error) {
    console.error(error);
  }
};

async function loadOlderMessages() {
  if (!roomId.value || historyBusy.value || !hasMoreHistory.value) return;
  historyBusy.value = true;
  try {
    const res: any = await chatApi.listMessages({
      roomId: roomId.value,
      pageSize: 30,
      cursor: historyCursor.value,
    });
    const older = Array.isArray(res?.list) ? [...res.list].reverse().map(normalizeMessage) : [];
    const exists = new Set(msgList.value.map((it) => String(messageId(it))));
    const mergedOlder = older.filter((it) => !exists.has(String(messageId(it))));
    msgList.value = [...mergedOlder, ...msgList.value];
    historyCursor.value = res?.cursor ?? res?.nextCursor ?? null;
    hasMoreHistory.value = !res?.isLast && older.length > 0;
  } catch (error: any) {
    uni.showToast({ title: error?.message || '加载历史消息失败', icon: 'none' });
  } finally {
    historyBusy.value = false;
  }
}

function normalizeMessage(raw: any) {
  if (raw?.message) {
    return {
      msgId: raw.message.id,
      roomId: raw.message.roomId,
      sendTime: raw.message.sendTime,
      fromUid: raw.fromUser?.uid,
      msgType: raw.message.msgType || raw.msgType || 1,
      body: raw.message.body
    };
  }
  return raw;
}

function messageId(msg: any) {
  return msg?.msgId || msg?.id || msg?.message?.id || `${msg?.sendTime || ''}-${msg?.fromUid || ''}`;
}

function bodyType(msg: any) {
  return msg?.body?.type || '';
}

function isTextMessage(msg: any) {
  const type = bodyType(msg);
  return (msg?.msgType === 1 || msg?.localKind === 'text') && (!type || type === 'text' || msg.body?.content);
}

function textContent(msg: any) {
  return msg?.body?.content || '';
}

function isImageMessage(msg: any) {
  return msg?.msgType === 3 || msg?.body?.type === 'image' || msg?.localKind === 'image';
}

function imageUrl(msg: any) {
  return msg?.body?.url || msg?.body?.localPath || '';
}

function sendStateText(status: LocalStatus) {
  if (status === 'sending') return '发送中';
  return '发送失败，点此重试';
}

const send = async () => {
  if (!chatUnlocked.value) {
    uni.showToast({ title: '聊天待解锁', icon: 'none' });
    return;
  }
  if (!inputText.value.trim()) return;
  const content = inputText.value;
  inputText.value = '';
  await reportTyping(false);
  const temp = createLocalMessage('text', { content });
  msgList.value = [...msgList.value, temp];
  scrollToBottom();
  try {
    await chatApi.sendText(roomId.value, content);
    removeLocalMessage(temp.localId);
    await fetchMessages();
  } catch (error: any) {
    uni.showToast({ title: error?.message || '发送失败', icon: 'none' });
    markLocalFailed(temp.localId);
  }
};

function createLocalMessage(kind: 'text' | 'image', body: any) {
  return {
    localId: `local-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    localKind: kind,
    localStatus: 'sending' as LocalStatus,
    msgType: kind === 'image' ? 3 : 1,
    fromUid: myUid,
    sendTime: new Date().toISOString(),
    body
  };
}

function removeLocalMessage(localId: string) {
  msgList.value = msgList.value.filter((it) => it.localId !== localId);
}

function markLocalFailed(localId: string) {
  msgList.value = msgList.value.map((it) => (it.localId === localId ? { ...it, localStatus: 'failed' } : it));
}

async function retryMessage(msg: any) {
  if (msg.localStatus !== 'failed') return;
  msgList.value = msgList.value.map((it) => (it.localId === msg.localId ? { ...it, localStatus: 'sending' } : it));
  if (msg.localKind === 'text') {
    try {
      await chatApi.sendText(roomId.value, msg.body?.content || '');
      removeLocalMessage(msg.localId);
      await fetchMessages();
    } catch {
      markLocalFailed(msg.localId);
    }
    return;
  }
  await sendImageFromPath(msg.body?.localPath || msg.body?.url, msg.localId);
}

async function chooseAndSendImage() {
  if (!chatUnlocked.value || imageBusy.value) return;
  try {
    const res: any = await new Promise((resolve, reject) => {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: resolve,
        fail: reject
      });
    });
    const path = res.tempFilePaths?.[0];
    if (!path) return;
    const temp = createLocalMessage('image', { type: 'image', url: path, localPath: path, size: Number(res.tempFiles?.[0]?.size || 0) });
    msgList.value = [...msgList.value, temp];
    scrollToBottom();
    await sendImageFromPath(path, temp.localId);
  } catch (e: any) {
    if (e?.errMsg && String(e.errMsg).includes('cancel')) return;
    uni.showToast({ title: e?.message || '选择图片失败', icon: 'none' });
  }
}

async function sendImageFromPath(path: string, localId: string) {
  if (!path) {
    markLocalFailed(localId);
    return;
  }
  imageBusy.value = true;
  try {
    const info: any = await new Promise((resolve) => {
      uni.getImageInfo({ src: path, success: resolve, fail: () => resolve({}) });
    });
    const upload = await assetsApi.uploadImage(path, 'chat');
    await chatApi.sendImage(roomId.value, {
      url: upload.url,
      objectKey: upload.objectKey,
      contentType: upload.contentType,
      size: upload.size,
      width: info.width || undefined,
      height: info.height || undefined
    });
    removeLocalMessage(localId);
    await fetchMessages();
  } catch (e: any) {
    uni.showToast({ title: e?.message || '图片发送失败', icon: 'none' });
    markLocalFailed(localId);
  } finally {
    imageBusy.value = false;
  }
}

function previewImage(url: string) {
  if (!url) return;
  uni.previewImage({ urls: [url], current: url });
}

function toApplicationStatusText(status: string, fromMe: boolean) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return fromMe ? '等待回复' : '待处理';
  if (s === 'ACCEPTED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  return '状态未知';
}

function proposalStatusText(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '待确认';
  if (s === 'ACCEPTED') return '已接受';
  if (s === 'REJECTED') return '已拒绝';
  if (s === 'EXPIRED') return '已过期';
  if (s === 'CANCELED') return '已取消';
  return s || '未知';
}

function orderStatusText(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '待支付';
  if (s === 'PAID') return '已支付';
  if (s === 'CANCELED') return '已取消';
  return s || '待确认';
}

function refundStatusText(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '审核中';
  if (s === 'APPROVED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  if (s === 'PAID') return '已退款';
  return s || '待确认';
}

function formatFen(fen: any) {
  const v = Number(fen);
  if (!Number.isFinite(v)) return '';
  return `¥${(v / 100).toFixed(2)}`;
}

function formatDate(value: number) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  return `${month}-${day} ${h}:${m}`;
}

function formatRange(startAt?: number | null, endAt?: number | null) {
  if (!startAt || !endAt) return '待确认';
  return `${formatDate(startAt)} - ${formatDate(endAt).slice(-5)}`;
}

function fallbackText(msg: any) {
  return msg?.body?.content || msg?.body?.type || '[系统消息]';
}

function ackLatestMessage(messages: any[]) {
  const latest = messages[messages.length - 1];
  const id = Number(messageId(latest));
  if (!Number.isFinite(id) || !roomId.value) return;
  if (lastAckReadMsgId.value !== id) {
    lastAckReadMsgId.value = id;
    void chatApi.ackRead(roomId.value, id).catch(() => {});
  }
  if (latest?.fromUid !== myUid && lastDeliveredMsgId.value !== id) {
    lastDeliveredMsgId.value = id;
    void chatApi.ackDelivered(roomId.value, id).catch(() => {});
  }
}

async function syncEvents() {
  try {
    const res: any = await chatApi.syncRealtimeEvents({ lastEventId: lastEventId.value, pageSize: 30 });
    const list = Array.isArray(res?.list) ? res.list : [];
    if (res?.latestEventId != null) lastEventId.value = Number(res.latestEventId);
    let shouldRefresh = false;
    list.forEach((event: any) => {
      if (event?.eventId != null) lastEventId.value = Math.max(Number(lastEventId.value || 0), Number(event.eventId));
      if (Number(event?.roomId) !== roomId.value) return;
      if (event.eventType === 'chat.typing.updated') {
        const payload = event.payload || {};
        if (payload.typingUid !== myUid && payload.typing) showPeerTyping();
      } else {
        shouldRefresh = true;
      }
    });
    if (shouldRefresh) await fetchMessages();
  } catch {
    // Polling messages remains the fallback.
  }
}

function showPeerTyping() {
  peerTyping.value = true;
  if (peerTypingTimer) clearTimeout(peerTypingTimer);
  peerTypingTimer = setTimeout(() => {
    peerTyping.value = false;
  }, 3500);
}

async function reportTyping(typing: boolean) {
  if (!roomId.value || !chatUnlocked.value) return;
  if (typingReported.value === typing && typing) return;
  typingReported.value = typing;
  await chatApi.reportTyping(roomId.value, typing).catch(() => {});
}

function onTextInput() {
  if (!inputText.value.trim()) return;
  void reportTyping(true);
  if (typingTimer) clearTimeout(typingTimer);
  typingTimer = setTimeout(() => {
    void reportTyping(false);
  }, 1800);
}

const decide = async (applicationId: number, action: 'ACCEPT' | 'REJECT') => {
  if (!applicationId || decisionBusy.value[applicationId]) return;
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

const respondProposal = async (proposalId: number, action: 'ACCEPT' | 'REJECT') => {
  if (!proposalId || proposalBusy.value[proposalId]) return;
  proposalBusy.value = { ...proposalBusy.value, [proposalId]: true };
  try {
    await chatApi.respondCollaborationProposal(proposalId, action);
    await fetchMessages();
  } catch (error: any) {
    uni.showToast({ title: error?.message || '操作失败', icon: 'none' });
  } finally {
    proposalBusy.value = { ...proposalBusy.value, [proposalId]: false };
  }
};

const payBrokerage = async (orderId: number) => {
  if (!orderId || payBusy.value) return;
  payBusy.value = true;
  try {
    const payRes = await paymentApi.prepay({
      channel: 'WECHAT',
      tradeType: 'JSAPI',
      contextId: orderId,
      contextType: 'BROKERAGE_ORDER',
      openid: userStore.userInfo?.openid
    });
    const params = payRes?.payParams;
    if (params?.mock) {
      uni.showToast({ title: '模拟支付成功', icon: 'success' });
      await fetchMessages();
      return;
    }
    if (!params) {
      uni.showToast({ title: '暂未返回支付参数', icon: 'none' });
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
        success: () => resolve(),
        fail: (err: any) => reject(err)
      } as any);
    });
    uni.showToast({ title: '支付成功', icon: 'success' });
    await fetchMessages();
  } catch (error: any) {
    uni.showToast({ title: error?.message || '支付失败', icon: 'none' });
  } finally {
    payBusy.value = false;
  }
};

function openInfoFeePolicy(role: 'teacher' | 'student') {
  infoFeePolicyRole.value = role;
  infoFeePolicyOpen.value = true;
}

function closeInfoFeePolicy() {
  infoFeePolicyOpen.value = false;
}

function openProposal() {
  if (!chatUnlocked.value) {
    uni.showToast({ title: '聊天解锁后可发起合作', icon: 'none' });
    return;
  }
  proposalOpen.value = true;
}

function todayString() {
  const d = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function localTimeToMs(date: string, time: string) {
  const [y, mo, d] = date.split('-').map(Number);
  const [h, mi] = time.split(':').map(Number);
  return new Date(y, mo - 1, d, h, mi, 0, 0).getTime();
}

async function submitProposal() {
  if (proposalSubmitBusy.value) return;
  const start = localTimeToMs(proposalDate.value, proposalStartTime.value);
  const end = localTimeToMs(proposalDate.value, proposalEndTime.value);
  if (!proposalPrice.value.trim()) {
    uni.showToast({ title: '请填写课时费', icon: 'none' });
    return;
  }
  if (!(end > start)) {
    uni.showToast({ title: '结束时间需晚于开始时间', icon: 'none' });
    return;
  }
  proposalSubmitBusy.value = true;
  try {
    await chatApi.createCollaborationProposal({
      roomId: roomId.value,
      pricePerHour: proposalPrice.value.trim(),
      trialStartAt: start,
      trialEndAt: end,
      remark: proposalRemark.value.trim(),
      clientRequestId: `mp-${Date.now()}`
    });
    proposalOpen.value = false;
    proposalRemark.value = '';
    await fetchMessages();
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发送提案失败', icon: 'none' });
  } finally {
    proposalSubmitBusy.value = false;
  }
}

async function goCourseByRoom() {
  try {
    const course = await courseApi.byRoom(roomId.value);
    if (course?.courseId) {
      uni.navigateTo({ url: `/pages/course/detail?id=${course.courseId}` });
      return;
    }
  } catch {
    // Fall through to list.
  }
  uni.navigateTo({ url: '/pages/course/list' });
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollTop.value = 9999999;
  });
};

const startPolling = () => {
  void fetchMessages();
  timer = setInterval(() => {
    void syncEvents();
    void fetchMessages();
  }, 3000);
};

onUnload(() => {
  if (timer) clearInterval(timer);
  if (typingTimer) clearTimeout(typingTimer);
  if (peerTypingTimer) clearTimeout(peerTypingTimer);
  void reportTyping(false);
});
</script>

<style lang="scss" scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f4f7f7;
}

.msg-list {
  flex: 1;
  padding: 12px 12px 128px;
  box-sizing: border-box;
  overflow-y: auto;
}

.history-entry {
  margin-bottom: 12px;
  text-align: center;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
}

.msg-item {
  display: flex;
  margin-bottom: 14px;
}

.msg-item.self {
  justify-content: flex-end;
}

.msg-item.other {
  justify-content: flex-start;
}

.bubble {
  max-width: 72%;
  padding: 10px 13px;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}

.self .bubble {
  color: #fff;
  background: #0f766e;
  border-top-right-radius: 4px;
}

.other .bubble {
  color: #182326;
  background: #fff;
  border-top-left-radius: 4px;
  box-shadow: 0 8px 20px rgba(18, 37, 41, 0.06);
}

.image-wrap {
  position: relative;
  max-width: 62%;
  padding: 4px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(18, 37, 41, 0.06);
}

.self .image-wrap {
  background: #fff;
  border: 2px solid rgba(15, 118, 110, 0.18);
}

.chat-image {
  display: block;
  width: 180px;
  max-height: 260px;
  border-radius: 12px;
  overflow: hidden;
}

.send-state {
  margin-top: 5px;
  font-size: 10px;
  line-height: 1.4;
  opacity: 0.78;
}

.self .send-state {
  color: rgba(255, 255, 255, 0.86);
  text-align: right;
}

.other .send-state {
  color: #8a949d;
}

.send-state.failed {
  color: #c24141;
  font-weight: 800;
}

.self .send-state.failed {
  color: #ffe1e1;
}

.image-state {
  padding: 2px 4px;
}

.sys-card {
  width: 82%;
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 10px 24px rgba(18, 37, 41, 0.06);
  box-sizing: border-box;
}

.sys-card.accent { border-left: 4px solid #0f766e; }
.sys-card.pay { border-left: 4px solid #d29134; }
.sys-card.success { border-left: 4px solid #23a55a; }
.sys-card.proposal { border-left: 4px solid #2f80ed; }
.sys-card.refund { border-left: 4px solid #c24141; }

.sys-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.sys-head-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sys-title,
.k,
.v,
.amount,
.sheet-title {
  display: block;
}

.sys-title {
  color: #162326;
  font-size: 15px;
  font-weight: 900;
}

.sys-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #0f766e;
  font-size: 12px;
  line-height: 1.3;
  font-weight: 800;
  text-align: left;
}

.sys-link::after {
  display: none;
}

.sys-badge {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: #edf6f5;
  color: #0f766e;
  font-size: 11px;
  font-weight: 800;
}

.sys-badge.green { background: #eaf7ef; color: #168447; }
.sys-badge.red { background: #fff0f0; color: #c24141; }

.sys-desc {
  color: #66727c;
  font-size: 13px;
  line-height: 1.55;
}

.amount {
  margin: 6px 0 8px;
  color: #172326;
  font-size: 24px;
  font-weight: 900;
}

.proposal-grid {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 10px;
  margin-bottom: 8px;
}

.proposal-grid > view {
  padding: 9px;
  border-radius: 12px;
  background: #f4f7f7;
}

.k {
  color: #8a949d;
  font-size: 11px;
  margin-bottom: 4px;
}

.v {
  color: #172326;
  font-size: 13px;
  font-weight: 800;
}

.sys-ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.mini {
  min-width: 66px;
  height: 32px;
  line-height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 12px;
}

.mini.ghost {
  color: #5d6872;
  background: #eef2f3;
}

.lock-bar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: 66px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #fff4dc;
  color: #9a6400;
  font-size: 12px;
  line-height: 1.5;
  box-shadow: 0 8px 18px rgba(154, 100, 0, 0.12);
}

.typing-hint {
  position: fixed;
  left: 16px;
  bottom: 64px;
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
}

.input-area {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(18, 37, 41, 0.08);
  box-sizing: border-box;
}

.input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border-radius: 20px;
  background: #f0f3f3;
  color: #172326;
  font-size: 14px;
  box-sizing: border-box;
}

.tool-btn,
.send-btn,
.submit {
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 13px;
}

.tool-btn {
  width: 50px;
  height: 38px;
  line-height: 38px;
  padding: 0;
  background: #315b4f;
}

.image-tool {
  background: #edf3f2;
  color: #315b4f;
}

.send-btn {
  width: 58px;
  height: 38px;
  line-height: 38px;
  padding: 0;
}

.tool-btn[disabled],
.send-btn[disabled] {
  color: #a3abb2;
  background: #e5e9eb;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(12, 20, 22, 0.45);
}

.sheet {
  width: 100%;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  border-radius: 22px 22px 0 0;
  background: #fff;
  box-sizing: border-box;
}

.sheet-title {
  margin-bottom: 14px;
  color: #142326;
  font-size: 18px;
  font-weight: 900;
}

.field,
.textarea {
  width: 100%;
  margin-top: 10px;
  padding: 12px;
  border-radius: 12px;
  background: #f4f7f7;
  color: #172326;
  box-sizing: border-box;
  font-size: 14px;
}

.picker-value {
  color: #32444a;
}

.textarea {
  height: 92px;
}

.submit {
  width: 100%;
  height: 44px;
  line-height: 44px;
  margin-top: 14px;
}
</style>
