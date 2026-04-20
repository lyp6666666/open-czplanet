<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

import { chatApi } from '@/api/chat'
import type { ChatMessageBody, ChatMessageResp } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const props = defineProps<{
  roomId: number | null
  peerName?: string | null
}>()

const auth = useAuthStore()
const chatRealtime = useChatRealtimeStore()

const loading = ref(false)
const sending = ref(false)
const error = ref<string | null>(null)
const input = ref('')
const messages = ref<ChatMessageResp[]>([])
const cursor = ref<string | null>(null)
const isLast = ref(true)
const listRef = ref<HTMLElement | null>(null)
let refreshTimer: number | null = null

const myUid = computed(() => auth.user?.id ?? 0)

function normalizeBody(raw: unknown): ChatMessageBody {
  if (!raw) return { type: 'text', content: '' }
  if (typeof raw === 'string') return { type: 'text', content: raw }
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    if (typeof any.type === 'string') return any as ChatMessageBody
    if (typeof any.content === 'string') return { type: 'text', content: any.content }
  }
  return { type: 'system', content: String(raw || '') }
}

function bodyText(body: ChatMessageBody) {
  if (body.type === 'text') return body.content
  if (body.type === 'system') return body.content || '系统消息'
  if (body.type === 'image') return '[图片]'
  if (body.type === 'lesson_request') return `[约课] ${body.title}`
  if (body.type === 'lesson_status') return `[约课状态] ${body.title}`
  if (body.type === 'collaboration_proposal') return '[合作提案]'
  if (body.type === 'collaboration_status') return '[合作状态更新]'
  if (body.type === 'recall') return '消息已撤回'
  return `[${body.type}]`
}

function messageText(item: ChatMessageResp) {
  return bodyText(normalizeBody(item.message?.body))
}

function formatTime(value: unknown) {
  const parsed = Date.parse(String(value || ''))
  if (!Number.isFinite(parsed)) return ''
  const date = new Date(parsed)
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

function mergeMessages(list: ChatMessageResp[], mode: 'append' | 'prepend') {
  const merged = mode === 'prepend' ? [...list, ...messages.value] : [...messages.value, ...list]
  const deduped = new Map<number, ChatMessageResp>()
  for (const item of merged) {
    if (!item?.message?.id) continue
    deduped.set(item.message.id, item)
  }
  messages.value = [...deduped.values()].sort((a, b) => (a.message?.id || 0) - (b.message?.id || 0))
}

function setActiveRoom() {
  chatRealtime.setActiveRoom(props.roomId && props.roomId > 0 ? props.roomId : null)
}

async function scrollToBottom() {
  await nextTick()
  if (!listRef.value) return
  listRef.value.scrollTop = listRef.value.scrollHeight
}

async function loadInitial() {
  if (!(props.roomId && props.roomId > 0)) {
    messages.value = []
    cursor.value = null
    isLast.value = true
    return
  }
  loading.value = true
  error.value = null
  try {
    const page = await chatApi.listMessages({ roomId: props.roomId, pageSize: 20, cursor: null })
    messages.value = [...(page.list || [])].sort((a, b) => (a.message?.id || 0) - (b.message?.id || 0))
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast
    await scrollToBottom()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课中消息失败'
  } finally {
    loading.value = false
  }
}

async function refreshLatest() {
  if (loading.value || !(props.roomId && props.roomId > 0)) return
  error.value = null
  try {
    const page = await chatApi.listMessages({ roomId: props.roomId, pageSize: 20, cursor: null })
    messages.value = [...(page.list || [])].sort((a, b) => (a.message?.id || 0) - (b.message?.id || 0))
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast
    await scrollToBottom()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '同步课中消息失败'
  }
}

async function loadMore() {
  if (loading.value || isLast.value || !(props.roomId && props.roomId > 0)) return
  loading.value = true
  error.value = null
  const previousHeight = listRef.value?.scrollHeight || 0
  try {
    const page = await chatApi.listMessages({ roomId: props.roomId, pageSize: 20, cursor: cursor.value })
    mergeMessages(page.list || [], 'prepend')
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight - previousHeight
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载更多失败'
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || sending.value || !(props.roomId && props.roomId > 0)) return
  sending.value = true
  error.value = null
  try {
    const message = await chatApi.sendText(props.roomId, text)
    mergeMessages([message], 'append')
    input.value = ''
    await scrollToBottom()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    sending.value = false
  }
}

watch(
  () => props.roomId,
  () => {
    setActiveRoom()
    void loadInitial()
  },
  { immediate: true },
)

watch(
  () => chatRealtime.messageEventSerial,
  async () => {
    if (!(props.roomId && props.roomId > 0)) return
    const incoming = chatRealtime.listMessageEventsAfter(0).map((item) => item.event)
    const matched = incoming
      .filter((event) => event.roomId === props.roomId)
      .map((event) => ({
        fromUser: { uid: event.fromUid },
        message: {
          id: event.msgId,
          roomId: event.roomId,
          sendTime: typeof event.sendTime === 'string' ? event.sendTime : String(event.sendTime || ''),
          body: event.body,
        },
      } satisfies ChatMessageResp))
    if (matched.length <= 0) return
    mergeMessages(matched, 'append')
    await scrollToBottom()
  },
)

onMounted(() => {
  setActiveRoom()
  refreshTimer = window.setInterval(() => {
    void refreshLatest()
  }, 4000)
})

onUnmounted(() => {
  if (refreshTimer != null) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
  chatRealtime.setActiveRoom(null)
})
</script>

<template>
  <div class="live-chat-panel">
    <div class="chat-head">
      <div>
        <div class="chat-title">课中聊天</div>
        <div class="chat-subtitle">与 {{ peerName || '对方' }} 保持同步</div>
      </div>
      <button class="chat-more" type="button" :disabled="loading || isLast" @click="loadMore">
        {{ loading ? '加载中...' : isLast ? '已到底部' : '查看更多' }}
      </button>
    </div>

    <div ref="listRef" class="chat-list" data-testid="live-chat-list">
      <div v-if="messages.length <= 0 && !loading" class="chat-empty">课堂内消息会在这里实时同步展示。</div>
      <div
        v-for="item in messages"
        :key="item.message.id"
        class="chat-item"
        :class="{ me: item.fromUser.uid === myUid }"
      >
        <div class="chat-bubble">{{ messageText(item) }}</div>
        <div class="chat-meta">{{ item.fromUser.uid === myUid ? '我' : peerName || '对方' }} · {{ formatTime(item.message.sendTime) }}</div>
      </div>
    </div>

    <div v-if="error" class="chat-error">{{ error }}</div>

    <div class="chat-composer">
      <input
        v-model="input"
        class="chat-input"
        type="text"
        placeholder="发送课中消息"
        data-testid="live-chat-input"
        @keydown.enter.prevent="sendMessage"
      />
      <button class="chat-send" type="button" :disabled="sending || !input.trim()" data-testid="live-chat-send" @click="sendMessage">
        {{ sending ? '发送中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.live-chat-panel {
  display: grid;
  gap: 12px;
}

.chat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.chat-title {
  font-size: 18px;
  font-weight: 700;
}

.chat-subtitle {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.56);
  font-size: 13px;
}

.chat-more {
  border: none;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  border-radius: 999px;
  padding: 8px 12px;
}

.chat-list {
  min-height: 320px;
  max-height: 420px;
  overflow: auto;
  display: grid;
  gap: 10px;
  padding-right: 4px;
}

.chat-item {
  display: grid;
  justify-items: flex-start;
  gap: 4px;
}

.chat-item.me {
  justify-items: flex-end;
}

.chat-bubble {
  max-width: 100%;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(15, 118, 110, 0.08);
  color: rgba(0, 0, 0, 0.8);
  word-break: break-word;
}

.chat-item.me .chat-bubble {
  background: #0f766e;
  color: #fff;
}

.chat-meta,
.chat-empty,
.chat-error {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.52);
}

.chat-error {
  color: #b42318;
}

.chat-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.chat-input {
  min-width: 0;
}

.chat-send {
  border: none;
  border-radius: 999px;
  background: #0f766e;
  color: #fff;
  padding: 0 18px;
}
</style>
