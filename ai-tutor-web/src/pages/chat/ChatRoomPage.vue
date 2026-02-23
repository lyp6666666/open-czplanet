<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { scheduleApi } from '@/api/schedule'
import { userApi } from '@/api/user'
import type { ChatMessageBody, ChatMessageResp, CollaborationProposalStatus, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import CollaborationProposalCard from '@/ui/chat/CollaborationProposalCard.vue'
import CollaborationProposalModal from '@/ui/chat/CollaborationProposalModal.vue'
import LessonRequestCard from '@/ui/chat/LessonRequestCard.vue'
import UserCardModal from '@/ui/user/UserCardModal.vue'

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

const collabOpen = ref(false)
const collabCreateBusy = ref(false)
const collabCreateError = ref<string | null>(null)

const cursor = ref<string | null>(null)
const isLast = ref(false)
const messages = ref<ChatMessageResp[]>([])

const myUid = computed(() => auth.user?.id ?? 0)
const myUser = computed<UserSimpleVO | null>(() => {
  if (!auth.user?.id) return null
  return {
    id: auth.user.id,
    name: auth.user.name || `用户${auth.user.id}`,
    avatar: auth.user.avatar,
    userType: auth.user.userType,
  }
})

const userByUid = computed(() => {
  const map = new Map<number, UserSimpleVO>()
  if (myUser.value) map.set(myUser.value.id, myUser.value)
  if (otherUser.value) map.set(otherUser.value.id, otherUser.value)
  return map
})

function getUser(uid: number | null | undefined): UserSimpleVO | null {
  const id = typeof uid === 'number' && Number.isFinite(uid) ? uid : null
  if (!id) return null
  return userByUid.value.get(id) ?? null
}

function userName(uid: number): string {
  if (uid === myUid.value) return myUser.value?.name || `用户${uid}`
  if (otherUid.value && uid === otherUid.value) return otherUser.value?.name || `用户${uid}`
  return getUser(uid)?.name || `用户${uid}`
}

function userAvatar(uid: number): string {
  const v = uid === myUid.value ? myUser.value?.avatar : uid === otherUid.value ? otherUser.value?.avatar : getUser(uid)?.avatar
  return typeof v === 'string' ? v.trim() : ''
}

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

const collabActionBusy = ref<Record<number, boolean>>({})
const collabActionError = ref<Record<number, string>>({})

const cardOpen = ref(false)
const cardUid = ref<number | null>(null)

function openCard(uid: number) {
  if (!uid || uid === myUid.value) return
  cardUid.value = uid
  cardOpen.value = true
}

function closeCard() {
  cardOpen.value = false
}

function openCollaboration() {
  collabCreateError.value = null
  collabOpen.value = true
}

function closeCollaboration() {
  if (collabCreateBusy.value) return
  collabOpen.value = false
}

// #region debug-point
async function dbgReport(event: string, payload: Record<string, unknown>) {
  if (!import.meta.env.DEV) return
  const url = 'http://localhost:39090/report'
  try {
    const body = JSON.stringify({ ts: new Date().toISOString(), event, ...payload })
    const ctrl = new AbortController()
    setTimeout(() => ctrl.abort(), 800)
    await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body, signal: ctrl.signal })
  } catch (e) {
    void e
  }
}
// #endregion debug-point

async function createCollaboration(payload: { pricePerHour: string; classTime: string; frequencyPerWeek: number }) {
  if (collabCreateBusy.value) return
  collabCreateBusy.value = true
  collabCreateError.value = null
  try {
    const msg = await chatApi.createCollaborationProposal({ roomId: roomId.value, ...payload })
    mergeMessages([msg], 'append')
    collabOpen.value = false
  } catch (e) {
    // #region debug-point
    const ax =
      typeof e === 'object' && e !== null ? (e as { response?: { data?: unknown; status?: unknown } }) : null
    void dbgReport('collab_create_failed', {
      roomId: roomId.value,
      payload,
      errorMessage: e instanceof Error ? e.message : String(e),
      responseData: ax?.response?.data ?? null,
      status: typeof ax?.response?.status === 'number' ? ax.response.status : null,
    })
    // #endregion debug-point
    collabCreateError.value = e instanceof Error ? e.message : '发起失败'
  } finally {
    collabCreateBusy.value = false
  }
}

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

function isCollaborationProposalBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'collaboration_proposal' }> {
  return body.type === 'collaboration_proposal'
}

function isCollaborationStatusBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'collaboration_status' }> {
  return body.type === 'collaboration_status'
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

function collabStatusText(s: unknown): string {
  const v = typeof s === 'string' ? s.trim().toUpperCase() : ''
  if (v === 'ACCEPTED') return '已同意'
  if (v === 'REJECTED') return '已拒绝'
  if (v === 'PENDING') return '待确认'
  return v || '状态未知'
}

async function respondCollaboration(proposalId: number, action: 'ACCEPT' | 'REJECT', msgId: number) {
  const current = effectiveCollabStatus(proposalId, 'PENDING')
  if (current !== 'PENDING') return
  if (collabActionBusy.value[proposalId]) return
  collabActionBusy.value = { ...collabActionBusy.value, [proposalId]: true }
  collabActionError.value = { ...collabActionError.value, [proposalId]: '' }
  try {
    const statusMsg = await chatApi.respondCollaborationProposal(proposalId, action)
    mergeMessages([statusMsg], 'append')

    const statusBody = normalizeBody(statusMsg.message.body)
    const nextStatus = isCollaborationStatusBody(statusBody) ? statusBody.status : action === 'ACCEPT' ? 'ACCEPTED' : 'REJECTED'
    const next = messages.value.map((m) => {
      if (m.message.id !== msgId) return m
      const body = normalizeBody(m.message.body)
      if (!isCollaborationProposalBody(body)) return m
      return {
        ...m,
        message: {
          ...m.message,
          body: { ...body, status: nextStatus },
        },
      }
    })
    messages.value = next
  } catch (e) {
    collabActionError.value = { ...collabActionError.value, [proposalId]: e instanceof Error ? e.message : '操作失败' }
  } finally {
    collabActionBusy.value = { ...collabActionBusy.value, [proposalId]: false }
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

const collabStatusByProposalId = computed<Record<number, CollaborationProposalStatus>>(() => {
  const out: Record<number, CollaborationProposalStatus> = {}
  for (const m of renderMessages.value) {
    const b = m.body
    if (isCollaborationProposalBody(b)) {
      out[b.proposalId] = b.status
      continue
    }
    if (isCollaborationStatusBody(b)) {
      out[b.proposalId] = b.status
    }
  }
  return out
})

const hasAnyCollabProposal = computed(() => {
  return renderMessages.value.some((m) => isCollaborationProposalBody(m.body))
})

function effectiveCollabStatus(proposalId: number, fallback: CollaborationProposalStatus): CollaborationProposalStatus {
  return collabStatusByProposalId.value[proposalId] || fallback
}

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
  } catch (e) {
    void e
  }
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
      } catch (e) {
        void e
      }
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
          <button class="avatar" type="button" :class="{ clickable: m.fromUser.uid !== myUid }" @click="openCard(m.fromUser.uid)">
            <img v-if="userAvatar(m.fromUser.uid)" :src="userAvatar(m.fromUser.uid)" alt="" />
            <span v-else class="avatar-fallback">{{ userName(m.fromUser.uid).slice(0, 1) }}</span>
          </button>
          <div class="content">
            <div class="meta">{{ userName(m.fromUser.uid) }}</div>
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
              <template v-else-if="isCollaborationProposalBody(m.body)">
                <CollaborationProposalCard
                  :body="{ ...m.body, status: effectiveCollabStatus(m.body.proposalId, m.body.status) }"
                  :from-me="m.fromUser.uid === myUid"
                  :busy="collabActionBusy[m.body.proposalId]"
                  @accept="respondCollaboration(m.body.proposalId, 'ACCEPT', m.message.id)"
                  @reject="respondCollaboration(m.body.proposalId, 'REJECT', m.message.id)"
                />
                <div v-if="collabActionError[m.body.proposalId]" class="card-hint error">
                  {{ collabActionError[m.body.proposalId] }}
                </div>
              </template>
              <template v-else-if="isCollaborationStatusBody(m.body)">
                <div class="sys">合作提案：{{ collabStatusText(m.body.status) }}</div>
              </template>
              <template v-else>
                {{ msgText(m.body) }}
              </template>
            </div>
          </div>
        </div>
      </div>

      <div class="composer">
        <div class="actions">
          <button class="btn" type="button" :disabled="hasAnyCollabProposal" @click="openCollaboration">
            {{ hasAnyCollabProposal ? '已发起合作' : '发起合作' }}
          </button>
        </div>
        <div class="send">
          <input v-model="input" class="input" placeholder="请输入消息" @keydown.enter.prevent="onSend" />
          <button class="btn btn-primary" type="button" :disabled="sending" @click="onSend">
            {{ sending ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>

    <UserCardModal :open="cardOpen" :uid="cardUid" @close="closeCard" />
    <CollaborationProposalModal
      :open="collabOpen"
      :busy="collabCreateBusy"
      :error="collabCreateError"
      @close="closeCollaboration"
      @submit="createCollaboration"
    />
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
  grid-template-columns: 1fr;
  justify-items: stretch;
  gap: 10px;
  background: #fbfcfe;
  align-content: start;
}

.msg {
  display: flex;
  width: 100%;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 10px;
}

.msg.me {
  justify-content: flex-end;
  flex-direction: row;
}

.msg.me .avatar {
  order: 2;
}

.msg.me .content {
  order: 1;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  overflow: hidden;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border);
  padding: 0;
  cursor: default;
  appearance: none;
}

.avatar.clickable {
  cursor: pointer;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  font-weight: 900;
  color: rgba(0, 0, 0, 0.6);
  font-size: 14px;
}

.content {
  display: grid;
  gap: 4px;
  max-width: min(560px, 80%);
}

.msg.me .content {
  justify-items: end;
}

.meta {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1;
}

.bubble {
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

.composer {
  padding: 12px;
  border-top: 1px solid var(--border);
  display: grid;
  gap: 10px;
  background: #fff;
}

.actions {
  display: flex;
  justify-content: flex-start;
}

.send {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
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
