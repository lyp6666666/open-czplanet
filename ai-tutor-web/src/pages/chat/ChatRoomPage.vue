<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { scheduleApi } from '@/api/schedule'
import { userApi } from '@/api/user'
import type { ChatMessageBody, ChatMessageResp, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import LessonRequestCard from '@/ui/chat/LessonRequestCard.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const roomId = computed(() => Number(route.params.roomId))
const otherUid = computed(() => {
  const raw = route.query.otherUid
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

const otherUser = ref<UserSimpleVO | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

const sending = ref(false)
const input = ref('')

const cursor = ref<string | null>(null)
const isLast = ref(false)
const messages = ref<ChatMessageResp[]>([])

const myUid = computed(() => auth.user?.id ?? 0)

type StreamMsgEvent = {
  msgId: number
  roomId: number
  fromUid: number
  toUid: number
  sendTime: unknown
  body: unknown
}

const streamAbort = ref<AbortController | null>(null)

const lessonActionBusy = ref<Record<number, boolean>>({})
const lessonActionError = ref<Record<number, string>>({})

function normalizeBody(raw: unknown): ChatMessageBody {
  if (!raw) return { type: 'text', content: '' }
  if (typeof raw === 'string') return { type: 'text', content: raw }
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    if (typeof any.type === 'string') return any as ChatMessageBody
    if (typeof any.content === 'string') return { type: 'text', content: any.content }
  }
  return { type: 'system', content: msgText(raw) }
}

function isLessonRequestBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'lesson_request' }> {
  return body.type === 'lesson_request'
}

function isLessonStatusBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'lesson_status' }> {
  return body.type === 'lesson_status'
}

async function respondLesson(eventId: number, action: 'ACCEPT' | 'REJECT', msgId: number) {
  if (lessonActionBusy.value[eventId]) return
  lessonActionBusy.value = { ...lessonActionBusy.value, [eventId]: true }
  lessonActionError.value = { ...lessonActionError.value, [eventId]: '' }
  try {
    const updated = await scheduleApi.respond(eventId, action)
    const next = messages.value.map((m) => {
      if (m.message.id !== msgId) return m
      const body = normalizeBody(m.message.body)
      if (!isLessonRequestBody(body)) return m
      return {
        ...m,
        message: {
          ...m.message,
          body: { ...body, status: updated.status },
        },
      }
    })
    messages.value = next
  } catch (e) {
    lessonActionError.value = { ...lessonActionError.value, [eventId]: e instanceof Error ? e.message : '操作失败' }
  } finally {
    lessonActionBusy.value = { ...lessonActionBusy.value, [eventId]: false }
  }
}

function msgText(raw: unknown): string {
  if (!raw) return ''
  if (typeof raw === 'string') return raw
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    if (typeof any.content === 'string') return any.content
  }
  try {
    return JSON.stringify(raw)
  } catch {
    return String(raw)
  }
}

const sortedMessages = computed(() => {
  const list = messages.value.slice()
  list.sort((a, b) => (a.message?.id || 0) - (b.message?.id || 0))
  return list
})

type RenderMessage = ChatMessageResp & { body: ChatMessageBody }

const renderMessages = computed<RenderMessage[]>(() => {
  return sortedMessages.value.map((m) => ({
    ...m,
    body: normalizeBody(m.message?.body),
  }))
})

async function loadOtherUser() {
  if (!otherUid.value) return
  const users = await userApi.batch([otherUid.value])
  otherUser.value = users[0] || null
}

function mergeMessages(incoming: ChatMessageResp[], mode: 'prepend' | 'append') {
  const byId = new Map<number, ChatMessageResp>()
  messages.value.forEach((m) => byId.set(m.message.id, m))
  incoming.forEach((m) => byId.set(m.message.id, m))
  const merged = Array.from(byId.values())
  merged.sort((a, b) => a.message.id - b.message.id)
  messages.value = merged
  if (mode === 'prepend') {
    return
  }
}

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await chatApi.listMessages({ roomId: roomId.value, pageSize: 20, cursor: cursor.value })
    const list = page.list || []
    mergeMessages(list, cursor.value ? 'prepend' : 'append')
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function ackLatest() {
  const latest = messages.value.reduce((max, m) => Math.max(max, m.message?.id || 0), 0)
  if (latest <= 0) return
  try {
    await chatApi.ackRead(roomId.value, latest)
  } catch {}
}

async function onSend() {
  const text = input.value.trim()
  if (!text) return
  if (sending.value) return
  sending.value = true
  error.value = null
  try {
    const msg = await chatApi.sendText(roomId.value, text)
    mergeMessages([msg], 'append')
    input.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    sending.value = false
  }
}

function normalizeBaseUrl(raw: unknown) {
  const s = typeof raw === 'string' ? raw.trim() : ''
  return s.length > 0 ? s : ''
}

async function startStream() {
  if (!auth.isLoggedIn || !auth.token) return
  if (streamAbort.value) return
  const controller = new AbortController()
  streamAbort.value = controller

  const baseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
  const url = `${baseUrl}/chat/stream`
  const res = await fetch(url, {
    method: 'GET',
    headers: { Authorization: `Bearer ${auth.token}` },
    signal: controller.signal,
  })
  if (!res.ok || !res.body) {
    streamAbort.value = null
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const lines = part.split('\n').map((l) => l.trimEnd())
      let event = 'message'
      const dataLines: string[] = []
      for (const line of lines) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      const dataRaw = dataLines.join('\n')
      if (!dataRaw) continue
      if (event !== 'message') continue
      try {
        const ev = JSON.parse(dataRaw) as StreamMsgEvent
        if (!ev || ev.roomId !== roomId.value) continue
        const msg: ChatMessageResp = {
          fromUser: { uid: ev.fromUid },
          message: { id: ev.msgId, roomId: ev.roomId, sendTime: ev.sendTime == null ? '' : String(ev.sendTime), body: ev.body },
        }
        mergeMessages([msg], 'append')
        await ackLatest()
      } catch {}
    }
  }
  streamAbort.value = null
}

function stopStream() {
  streamAbort.value?.abort()
  streamAbort.value = null
}

onMounted(() => {
  void loadOtherUser()
  void loadMore().then(() => ackLatest())
  void startStream()
})

onBeforeUnmount(() => {
  stopStream()
})

watch(
  () => roomId.value,
  () => {
    stopStream()
    void startStream()
  },
)
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <button class="btn back" type="button" @click="router.push({ name: 'chatList' })">返回</button>
      <div class="name">{{ otherUser?.name || (otherUid ? `用户${otherUid}` : `会话 ${roomId}`) }}</div>
      <div />
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card panel">
      <div class="top">
        <button class="btn" type="button" :disabled="loading || isLast" @click="loadMore">
          <span v-if="isLast">没有更多历史消息</span>
          <span v-else>{{ loading ? '加载中...' : '加载历史' }}</span>
        </button>
      </div>

      <div class="msgs">
        <div v-for="m in renderMessages" :key="m.message.id" class="msg" :class="{ me: m.fromUser.uid === myUid }">
          <div class="bubble">
            <template v-if="isLessonRequestBody(m.body)">
              <LessonRequestCard
                :body="m.body"
                :from-me="m.fromUser.uid === myUid"
                :busy="lessonActionBusy[m.body.eventId]"
                @accept="respondLesson(m.body.eventId, 'ACCEPT', m.message.id)"
                @reject="respondLesson(m.body.eventId, 'REJECT', m.message.id)"
              />
              <div v-if="lessonActionError[m.body.eventId]" class="card-hint error">
                {{ lessonActionError[m.body.eventId] }}
              </div>
            </template>
            <template v-else-if="isLessonStatusBody(m.body)">
              <div class="sys">
                课程状态：{{ m.body.status }}（{{ m.body.title }}）
              </div>
            </template>
            <template v-else>
              {{ msgText(m.body) }}
            </template>
          </div>
        </div>
      </div>

      <div class="send">
        <input
          v-model="input"
          class="input"
          placeholder="请输入消息"
          @keydown.enter.prevent="onSend"
        />
        <button class="btn btn-primary" type="button" :disabled="sending" @click="onSend">
          {{ sending ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
  height: 100%;
}

.head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: 10px 12px;
}

.name {
  text-align: center;
  font-weight: 900;
}

.panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 520px;
  overflow: hidden;
}

.top {
  padding: 12px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: center;
}

.msgs {
  padding: 14px;
  display: grid;
  gap: 10px;
  background: #fbfcfe;
  align-content: start;
}

.msg {
  display: flex;
  justify-content: flex-start;
}

.msg.me {
  justify-content: flex-end;
}

.bubble {
  max-width: min(560px, 80%);
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #fff;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.msg.me .bubble {
  border-color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
}

.send {
  padding: 12px;
  border-top: 1px solid var(--border);
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  background: #fff;
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.hint {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: #fff;
}

.hint.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
}

@media (min-width: 981px) {
  .back {
    display: none;
  }
}
</style>
