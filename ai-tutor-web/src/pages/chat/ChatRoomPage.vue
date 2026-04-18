<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import type { ChatRefundStateResp } from '@/api/chat'
import { contactApi } from '@/api/contact'
import { applicationApi } from '@/api/application'
import { scheduleApi } from '@/api/schedule'
import { userApi } from '@/api/user'
import type { ChatMessageBody, ChatMessageResp, CollaborationProposalStatus, TutorApplicationCardStatus, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'
import { normalizeAvatarUrl } from '@/utils/avatar'
import BrokerageRequiredCard from '@/ui/chat/BrokerageRequiredCard.vue'
import CollaborationProposalCard from '@/ui/chat/CollaborationProposalCard.vue'
import CollaborationProposalModal from '@/ui/chat/CollaborationProposalModal.vue'
import ContactUnlockedCard from '@/ui/chat/ContactUnlockedCard.vue'
import LessonRequestCard from '@/ui/chat/LessonRequestCard.vue'
import TutorApplicationCard from '@/ui/chat/TutorApplicationCard.vue'
import UnlockedContactModal from '@/ui/chat/UnlockedContactModal.vue'
import UserCardModal from '@/ui/user/UserCardModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chatRealtime = useChatRealtimeStore()

const isTeacher = computed(() => auth.user?.userType === 1)

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

const endRequestBusy = ref(false)
const endActionBusy = ref(false)

const refundState = ref<ChatRefundStateResp | null>(null)
const refundStateLoading = ref(false)

const appActionBusy = ref<Record<number, boolean>>({})
const appActionError = ref<Record<number, string>>({})

const collabOpen = ref(false)
const collabCreateBusy = ref(false)
const collabCreateError = ref<string | null>(null)
const collabEdit = ref<{
  proposalId: number
  initial: { pricePerHour: string; classTime: string; frequencyPerWeek: number }
} | null>(null)

const cursor = ref<string | null>(null)
const isLast = ref(false)
const messages = ref<ChatMessageResp[]>([])
type PendingOutgoingMessage = {
  localId: string
  roomId: number
  content: string
  createdAt: string
  status: 'sending' | 'failed'
}

const pendingOutgoingMessages = ref<PendingOutgoingMessage[]>([])
const msgsRef = ref<HTMLElement | null>(null)
const roomVersion = ref(0)
const avatarBroken = ref<Record<number, boolean>>({})
const lastConsumedMessageSerial = ref(0)

const myUid = computed(() => auth.user?.id ?? 0)
const peerLastReadMsgId = computed(() => chatRealtime.peerReadMsgIdByRoom[roomId.value] || 0)
const myRealName = computed(() => {
  const teacherName = auth.me?.teacherProfile?.realName?.trim()
  if (teacherName) return teacherName
  const studentName = auth.me?.studentProfile?.realName?.trim()
  if (studentName) return studentName
  return null
})
const myUser = computed<UserSimpleVO | null>(() => {
  if (!auth.user?.id) return null
  return {
    id: auth.user.id,
    name: auth.user.name || `用户${auth.user.id}`,
    realName: myRealName.value,
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

function pickDisplayName(user: UserSimpleVO | null | undefined, uid: number | null | undefined): string {
  const realName = user?.realName?.trim()
  if (realName) return realName
  const name = user?.name?.trim()
  if (name) return name
  return uid ? `用户${uid}` : '用户'
}

function userName(uid: number): string {
  if (uid === myUid.value) return pickDisplayName(myUser.value, uid)
  if (otherUid.value && uid === otherUid.value) return pickDisplayName(otherUser.value, uid)
  return pickDisplayName(getUser(uid), uid)
}

function userAvatar(uid: number): string {
  if (avatarBroken.value[uid]) return ''
  const v = uid === myUid.value ? myUser.value?.avatar : uid === otherUid.value ? otherUser.value?.avatar : getUser(uid)?.avatar
  return normalizeAvatarUrl(v)
}

function markAvatarBroken(uid: number) {
  if (!uid) return
  if (avatarBroken.value[uid]) return
  avatarBroken.value = { ...avatarBroken.value, [uid]: true }
}

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

const unlockOpen = ref(false)
const unlockUid = ref<number | null>(null)
const unlockPhone = ref('')
const unlockLoading = ref(false)
const unlockError = ref<string | null>(null)
const contactAutoShown = ref(false)

function closeUnlock() {
  unlockOpen.value = false
}

async function viewUnlockedContact() {
  if (unlockLoading.value) return
  if (!otherUid.value) return
  unlockOpen.value = true
  unlockUid.value = otherUid.value
  unlockPhone.value = ''
  unlockError.value = null
  unlockLoading.value = true
  try {
    const contact = await contactApi.unlock(roomId.value, otherUid.value)
    unlockUid.value = contact.uid
    unlockPhone.value = contact.phone || ''
  } catch (e) {
    unlockError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    unlockLoading.value = false
  }
}

function goPay(orderId: number, applicationId?: number | null) {
  router.push({
    name: 'cashierPay',
    query: {
      contextType: 'BROKERAGE_ORDER',
      contextId: String(orderId),
      ...(applicationId ? { applicationId: String(applicationId) } : {}),
    },
  })
}

onMounted(() => {
  if (auth.isLoggedIn && !auth.me) {
    void auth.refreshMe()
  }
})

function openCollaboration() {
  if (!composerEnabled.value) {
    error.value = composerLockedHint.value
    return
  }
  collabCreateError.value = null
  collabEdit.value = null
  collabOpen.value = true
}

function openCollaborationEdit(payload: { proposalId: number; pricePerHour: string; classTime: string; frequencyPerWeek: number }) {
  if (!composerEnabled.value) {
    error.value = composerLockedHint.value
    return
  }
  collabCreateError.value = null
  collabEdit.value = {
    proposalId: payload.proposalId,
    initial: { pricePerHour: payload.pricePerHour, classTime: payload.classTime, frequencyPerWeek: payload.frequencyPerWeek },
  }
  collabOpen.value = true
}

function closeCollaboration() {
  if (collabCreateBusy.value) return
  collabOpen.value = false
  collabEdit.value = null
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
    const editing = !!collabEdit.value
    const msg = editing
      ? await chatApi.updateCollaborationProposal(collabEdit.value!.proposalId, { roomId: roomId.value, ...payload })
      : await chatApi.createCollaborationProposal({ roomId: roomId.value, ...payload })
    mergeMessages([msg], 'append')
    collabOpen.value = false
    collabEdit.value = null
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
    const editing = !!collabEdit.value
    collabCreateError.value = e instanceof Error ? e.message : editing ? '修改失败' : '发起失败'
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

function isBrokerageRequiredBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'brokerage_required' }> {
  return body.type === 'brokerage_required'
}

function isContactUnlockedBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'contact_unlocked' }> {
  return body.type === 'contact_unlocked'
}

function isTutorApplicationBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'tutor_application' }> {
  return body.type === 'tutor_application'
}

function isTutorApplicationStatusBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'tutor_application_status' }> {
  return body.type === 'tutor_application_status'
}

function isEndChatRequestBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'end_chat_request' }> {
  return body.type === 'end_chat_request'
}

function isEndChatStatusBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'end_chat_status' }> {
  return body.type === 'end_chat_status'
}

function isRefundRequestBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'brokerage_refund_request' }> {
  return body.type === 'brokerage_refund_request'
}

function isRefundStatusBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'brokerage_refund_status' }> {
  return body.type === 'brokerage_refund_status'
}

const chatUnlocked = computed(() => {
  return messages.value.some((m) => {
    const b = normalizeBody(m.message.body)
    return b.type === 'contact_unlocked'
  })
})

const chatLockedHint = computed(() => '当前仅可发送家教申请，申请通过并完成支付后再聊天')

const appStatusByApplicationId = computed<Record<number, TutorApplicationCardStatus>>(() => {
  const out: Record<number, TutorApplicationCardStatus> = {}
  for (const m of messages.value) {
    const b = normalizeBody(m.message.body)
    if (isTutorApplicationBody(b)) {
      out[b.applicationId] = appStatusText(b.status)
      continue
    }
    if (isTutorApplicationStatusBody(b)) {
      out[b.applicationId] = appStatusText(b.status)
    }
  }
  return out
})

function effectiveAppStatus(applicationId: number, fallback: TutorApplicationCardStatus): TutorApplicationCardStatus {
  return appStatusByApplicationId.value[applicationId] || fallback
}

const latestTutorApplication = computed(() => {
  let best: { msgId: number; body: Extract<ChatMessageBody, { type: 'tutor_application' }>; fromMe: boolean } | null = null
  for (const m of messages.value) {
    const b = normalizeBody(m.message.body)
    if (!isTutorApplicationBody(b)) continue
    const msgId = m.message.id
    if (!best || msgId > best.msgId) {
      const status = effectiveAppStatus(b.applicationId, appStatusText(b.status))
      best = { msgId, body: { ...b, status }, fromMe: m.fromUser.uid === myUid.value }
    }
  }
  return best
})

const canSendTutorApplication = computed(() => {
  if (chatUnlocked.value) return false
  const latest = latestTutorApplication.value
  if (!latest) return false
  if (!latest.fromMe) return false
  return latest.body.status === 'REJECTED'
})

async function sendTutorApplicationAgain() {
  const latest = latestTutorApplication.value
  if (!latest || !otherUid.value) return
  try {
    const msg = await applicationApi.startChat({
      receiverUid: otherUid.value,
      contextType: latest.body.contextType,
      contextId: latest.body.contextId,
      content: latest.body.content,
      clientRequestId: `reapply-${Date.now()}`,
    })
    mergeMessages([msg], 'append')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  }
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

function appStatusText(s: unknown): TutorApplicationCardStatus {
  const v = typeof s === 'string' ? s.trim().toUpperCase() : ''
  if (v === 'ACCEPTED') return 'ACCEPTED'
  if (v === 'REJECTED') return 'REJECTED'
  return 'PENDING'
}

async function respondTutorApplication(applicationId: number, action: 'ACCEPT' | 'REJECT', msgId: number) {
  if (appActionBusy.value[applicationId]) return
  appActionBusy.value = { ...appActionBusy.value, [applicationId]: true }
  appActionError.value = { ...appActionError.value, [applicationId]: '' }
  try {
    const statusMsg = await applicationApi.decideMessage(applicationId, action)
    mergeMessages([statusMsg], 'append')

    const statusBody = normalizeBody(statusMsg.message.body)
    const nextStatus = isTutorApplicationStatusBody(statusBody) ? appStatusText(statusBody.status) : action === 'ACCEPT' ? 'ACCEPTED' : 'REJECTED'
    const next = messages.value.map((m) => {
      if (m.message.id !== msgId) return m
      const b = normalizeBody(m.message.body)
      if (!isTutorApplicationBody(b)) return m
      return { ...m, message: { ...m.message, body: { ...b, status: nextStatus } } }
    })
    messages.value = next
  } catch (e) {
    appActionError.value = { ...appActionError.value, [applicationId]: e instanceof Error ? e.message : '操作失败' }
  } finally {
    appActionBusy.value = { ...appActionBusy.value, [applicationId]: false }
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

const endStatusByRequestId = computed<Record<number, string>>(() => {
  const out: Record<number, string> = {}
  for (const m of sortedMessages.value) {
    const b = normalizeBody(m.message?.body)
    if (isEndChatRequestBody(b) && typeof b.requestId === 'number' && Number.isFinite(b.requestId)) {
      if (typeof b.status === 'string' && b.status.trim()) out[b.requestId] = b.status
      continue
    }
    if (isEndChatStatusBody(b) && typeof b.requestId === 'number' && Number.isFinite(b.requestId)) {
      if (typeof b.status === 'string' && b.status.trim()) out[b.requestId] = b.status
    }
  }
  return out
})

const refundStatusByRequestId = computed<Record<number, string>>(() => {
  const out: Record<number, string> = {}
  for (const m of sortedMessages.value) {
    const b = normalizeBody(m.message?.body)
    if (isRefundRequestBody(b) && typeof b.requestId === 'number' && Number.isFinite(b.requestId)) {
      if (typeof b.status === 'string' && b.status.trim()) out[b.requestId] = b.status
      continue
    }
    if (isRefundStatusBody(b) && typeof b.requestId === 'number' && Number.isFinite(b.requestId)) {
      if (typeof b.status === 'string' && b.status.trim()) out[b.requestId] = b.status
    }
  }
  return out
})

const canApplyRefund = computed(() => !!refundState.value?.canApply)
const refundHoverText = computed(() => refundState.value?.hoverText || '')

async function refreshRefundState() {
  if (!roomId.value || !Number.isFinite(roomId.value)) return
  if (refundStateLoading.value) return
  refundStateLoading.value = true
  try {
    refundState.value = await chatApi.getChatRefundState(roomId.value)
  } catch {
    refundState.value = null
  } finally {
    refundStateLoading.value = false
  }
}

async function applyRefund() {
  if (!roomId.value || !Number.isFinite(roomId.value)) return
  if (!canApplyRefund.value) return
  try {
    const msg = await chatApi.requestBrokerageRefund(roomId.value)
    mergeMessages([msg], 'append')
    await refreshRefundState()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '申请退费失败'
  }
}

const latestRefundStatusFallback = computed(() => {
  let best: { msgId: number; status: string } | null = null
  for (const m of sortedMessages.value) {
    const b = normalizeBody(m.message?.body)
    if (!isRefundStatusBody(b)) continue
    const msgId = m.message?.id || 0
    const s = typeof b.status === 'string' ? b.status.trim() : ''
    if (!s) continue
    if (!best || msgId > best.msgId) best = { msgId, status: s }
  }
  return best?.status || ''
})

const renderMessages = computed<RenderMessage[]>(() => {
  const list: RenderMessage[] = []
  for (const m of sortedMessages.value) {
    const body = normalizeBody(m.message?.body)
    if (isEndChatStatusBody(body)) continue
    if (isEndChatRequestBody(body) && typeof body.requestId === 'number' && Number.isFinite(body.requestId)) {
      const effective = endStatusByRequestId.value[body.requestId]
      list.push({ ...m, body: { ...body, status: effective || body.status } })
      continue
    }
    if (isRefundStatusBody(body)) continue
    if (isRefundRequestBody(body) && typeof body.requestId === 'number' && Number.isFinite(body.requestId)) {
      const effective = refundStatusByRequestId.value[body.requestId]
      list.push({ ...m, body: { ...body, status: effective || body.status } })
      continue
    }
    if (isRefundRequestBody(body)) {
      list.push({ ...m, body: { ...body, status: latestRefundStatusFallback.value || body.status } })
      continue
    }
    list.push({ ...m, body })
  }
  return list
})

const latestOutgoingMsgId = computed(() => {
  let latest = 0
  for (const item of renderMessages.value) {
    if (item.fromUser.uid !== myUid.value) continue
    latest = Math.max(latest, item.message.id)
  }
  return latest
})

function messageReceiptText(message: RenderMessage): string {
  if (message.fromUser.uid !== myUid.value) return ''
  if (message.message.id !== latestOutgoingMsgId.value) return ''
  return peerLastReadMsgId.value >= message.message.id ? '对方已读' : '未读'
}

const paidBrokerageOrderIds = computed<Record<number, true>>(() => {
  const out: Record<number, true> = {}
  for (const m of renderMessages.value) {
    const b = m.body
    if (!isContactUnlockedBody(b)) continue
    if (typeof b.orderId === 'number' && Number.isFinite(b.orderId)) {
      out[b.orderId] = true
    }
  }
  return out
})

function effectiveBrokerageStatus(body: Extract<ChatMessageBody, { type: 'brokerage_required' }>): string | null {
  const fallback = typeof body.status === 'string' ? body.status : null
  if (typeof body.orderId === 'number' && Number.isFinite(body.orderId) && paidBrokerageOrderIds.value[body.orderId]) {
    return 'PAID'
  }
  return fallback
}

type RenderItem = { kind: 'time'; key: string; text: string } | { kind: 'msg'; key: string; m: RenderMessage }

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function localTodayDay(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function formatMsgTime(v: string): string {
  const raw = String(v || '').trim()
  if (!raw) return ''
  const s = raw.includes('T') ? raw.replace('T', ' ') : raw
  if (s.length >= 16) {
    const day = s.slice(0, 10)
    const hm = s.slice(11, 16)
    return day === localTodayDay() ? hm : `${day} ${hm}`
  }
  const m = s.match(/\b(\d{2}):(\d{2})\b/)
  return m ? `${m[1]}:${m[2]}` : s
}

function parseMsgTime(v: string): number | null {
  const t = Date.parse(v)
  return Number.isFinite(t) ? t : null
}

function msgDay(v: string): string {
  const s = String(v || '')
  return s.length >= 10 ? s.slice(0, 10) : s
}

const renderItems = computed<RenderItem[]>(() => {
  const out: RenderItem[] = []
  let prevTs: number | null = null
  let prevDay: string | null = null

  for (const m of renderMessages.value) {
    const iso = m.message?.sendTime ?? ''
    const ts = parseMsgTime(iso)
    const day = msgDay(iso)
    const text = formatMsgTime(iso)

    const showTime = !!text && (prevTs == null || prevDay == null || ts == null || day !== prevDay || ts - prevTs > 2 * 60 * 1000)
    if (showTime) out.push({ kind: 'time', key: `time-${m.message.id}`, text })
    out.push({ kind: 'msg', key: `msg-${m.message.id}`, m })

    if (ts != null) {
      prevTs = ts
      prevDay = day
    } else {
      prevTs = null
      prevDay = null
    }
  }

  return out
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

const hasAcceptedCollaboration = computed(() => {
  return Object.values(collabStatusByProposalId.value).some((s) => s === 'ACCEPTED')
})

const canViewContact = computed(() => {
  return hasAcceptedCollaboration.value
})

const hasActiveCollabProposal = computed(() => {
  return Object.values(collabStatusByProposalId.value).some((s) => s !== 'REJECTED')
})

function effectiveCollabStatus(proposalId: number, fallback: CollaborationProposalStatus): CollaborationProposalStatus {
  return collabStatusByProposalId.value[proposalId] || fallback
}

const hasAnyRefundRequest = computed(() => {
  return renderMessages.value.some((m) => isRefundRequestBody(m.body))
})

const composerEnabled = computed(() => {
  return chatUnlocked.value && !hasAnyRefundRequest.value
})

const hasPendingEndChatRequest = computed(() => {
  return renderMessages.value.some((m) => {
    const b = m.body
    if (!isEndChatRequestBody(b)) return false
    const s = typeof b.status === 'string' ? b.status.trim().toUpperCase() : ''
    return s === 'PENDING_CONFIRM'
  })
})

const canRequestEndChat = computed(() => {
  if (!chatUnlocked.value) return false
  if (hasAnyRefundRequest.value) return false
  if (!isTeacher.value) return false
  return !hasPendingEndChatRequest.value
})

async function requestEndChat() {
  if (!roomId.value || !Number.isFinite(roomId.value)) return
  if (!canRequestEndChat.value) return
  if (endRequestBusy.value) return
  endRequestBusy.value = true
  try {
    const msg = await chatApi.requestEndChat(roomId.value)
    mergeMessages([msg], 'append')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发起结束沟通失败'
  } finally {
    endRequestBusy.value = false
  }
}

const composerLockedHint = computed(() => {
  if (hasAnyRefundRequest.value) {
    return '沟通已结束，退款等待管理员审核'
  }
  return chatLockedHint.value
})

watch([roomId, () => messages.value.length], () => {
  refreshRefundState()
}, { immediate: true })

watch(
  hasAcceptedCollaboration,
  (v) => {
    if (!v) return
    if (contactAutoShown.value) return
    contactAutoShown.value = true
    void viewUnlockedContact()
  },
)

function refundStatusText(raw: unknown): string {
  const s = typeof raw === 'string' ? raw.trim().toUpperCase() : ''
  if (s === 'REFUNDED' || s === 'SUCCESS' || s === 'DONE') {
    return '已经退还支付宝/微信账户（到账可能有延迟），若有疑问请咨询后台电话'
  }
  return '已经发起，等待管理员审核中，平均审核时长约30分钟，超出8小时直接退回'
}

function endChatStatusText(raw: unknown): string {
  const s = typeof raw === 'string' ? raw.trim().toUpperCase() : ''
  if (s === 'CONFIRMED') return '对方已确认结束沟通'
  if (s === 'REJECTED') return '对方暂不结束'
  return isTeacher.value ? '等待对方确认结束' : '对方请求结束沟通'
}

async function respondEndChat(requestId: number, action: 'CONFIRMED' | 'REJECTED') {
  if (endActionBusy.value) return
  endActionBusy.value = true
  try {
    const statusMsg = await chatApi.respondEndChat(roomId.value, requestId, action)
    mergeMessages([statusMsg], 'append')
    if (action === 'CONFIRMED') {
      const msg = await chatApi.requestBrokerageRefund(roomId.value)
      mergeMessages([msg], 'append')
    }
  } finally {
    endActionBusy.value = false
  }
}

async function loadOtherUser() {
  if (!otherUid.value) return
  const v = roomVersion.value
  const users = await userApi.batch([otherUid.value])
  if (v !== roomVersion.value) return
  otherUser.value = users[0] || null
}

function scrollToBottom() {
  if (!msgsRef.value) return
  const el = msgsRef.value
  requestAnimationFrame(() => {
    el.scrollTop = el.scrollHeight
  })
}

function createPendingOutgoingMessage(content: string): PendingOutgoingMessage {
  const localId =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  return {
    localId,
    roomId: roomId.value,
    content,
    createdAt: new Date().toISOString(),
    status: 'sending',
  }
}

function updatePendingOutgoingMessage(localId: string, patch: Partial<PendingOutgoingMessage>) {
  pendingOutgoingMessages.value = pendingOutgoingMessages.value.map((item) =>
    item.localId === localId ? { ...item, ...patch } : item,
  )
}

function removePendingOutgoingMessage(localId: string) {
  pendingOutgoingMessages.value = pendingOutgoingMessages.value.filter((item) => item.localId !== localId)
}

async function dispatchPendingOutgoingMessage(localId: string, targetRoomId: number, versionAtSend: number) {
  const pending = pendingOutgoingMessages.value.find((item) => item.localId === localId)
  if (!pending) return

  sending.value = true
  try {
    // 先把消息留在本地“发送中”队列里，网络失败时用户能看到并直接重试。
    const msg = await chatApi.sendText(targetRoomId, pending.content)
    removePendingOutgoingMessage(localId)
    if (roomId.value === targetRoomId && roomVersion.value === versionAtSend) {
      mergeMessages([msg], 'append')
    }
  } catch (e) {
    if (roomId.value === targetRoomId && roomVersion.value === versionAtSend) {
      updatePendingOutgoingMessage(localId, { status: 'failed' })
      error.value = e instanceof Error ? e.message : '发送失败'
    }
  } finally {
    sending.value = pendingOutgoingMessages.value.some((item) => item.status === 'sending')
  }
}

function retryPendingOutgoingMessage(localId: string) {
  const pending = pendingOutgoingMessages.value.find((item) => item.localId === localId)
  if (!pending) return
  updatePendingOutgoingMessage(localId, { status: 'sending' })
  void dispatchPendingOutgoingMessage(localId, pending.roomId, roomVersion.value)
}

function mergeMessages(incoming: ChatMessageResp[], mode: 'prepend' | 'append') {
  const byId = new Map<number, ChatMessageResp>()
  messages.value.forEach((m) => byId.set(m.message.id, m))
  incoming.forEach((m) => byId.set(m.message.id, m))
  const merged = Array.from(byId.values())
  merged.sort((a, b) => a.message.id - b.message.id)
  messages.value = merged
  if (mode === 'append') {
    void scrollToBottom()
  }
}

async function loadMore() {
  if (loading.value || isLast.value) return
  const v = roomVersion.value
  const targetRoomId = roomId.value
  loading.value = true
  error.value = null
  const oldScrollHeight = msgsRef.value?.scrollHeight || 0
  const oldScrollTop = msgsRef.value?.scrollTop || 0

  try {
    const page = await chatApi.listMessages({ roomId: targetRoomId, pageSize: 20, cursor: cursor.value })
    if (v !== roomVersion.value || targetRoomId !== roomId.value) return
    const list = page.list || []
    mergeMessages(list, cursor.value ? 'prepend' : 'append')
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast

    if (cursor.value && msgsRef.value) {
      requestAnimationFrame(() => {
        if (!msgsRef.value) return
        const newScrollHeight = msgsRef.value.scrollHeight
        msgsRef.value.scrollTop = newScrollHeight - oldScrollHeight + oldScrollTop
      })
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function ackLatest() {
  const latest = messages.value.reduce((max, m) => Math.max(max, m.message?.id || 0), 0)
  if (latest <= 0) return
  void chatRealtime.ackRoomRead(roomId.value, latest)
}

function keepaliveAckLatest() {
  const latest = messages.value.reduce((max, m) => Math.max(max, m.message?.id || 0), 0)
  if (latest <= 0) return
  chatRealtime.ackRoomReadKeepalive(roomId.value, latest)
}

async function onSend() {
  const text = input.value.trim()
  if (!text) return
  if (!composerEnabled.value) {
    error.value = composerLockedHint.value
    return
  }
  if (sending.value) return
  error.value = null
  const pending = createPendingOutgoingMessage(text)
  pendingOutgoingMessages.value = [...pendingOutgoingMessages.value, pending]
  input.value = ''
  void scrollToBottom()
  await dispatchPendingOutgoingMessage(pending.localId, pending.roomId, roomVersion.value)
}

async function ackFromRoomList() {
  if (!auth.isLoggedIn) return
  const v = roomVersion.value
  const targetRoomId = roomId.value
  let cursor: number | null = null
  for (let i = 0; i < 10; i++) {
    try {
      const page = await chatApi.listRooms({ pageSize: 50, cursor })
      if (v !== roomVersion.value || targetRoomId !== roomId.value) return
      const list = page.list || []
      const found = list.find((r) => r.roomId === targetRoomId)
      if (found?.lastMsgId) {
        void chatRealtime.ackRoomRead(targetRoomId, found.lastMsgId)
        return
      }
      cursor = page.cursor ?? null
      if (page.isLast || list.length === 0) return
    } catch {
      return
    }
  }
}

onBeforeUnmount(() => {
  keepaliveAckLatest()
  chatRealtime.setActiveRoom(null)
})

onMounted(() => {
  window.addEventListener('pagehide', keepaliveAckLatest)
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', keepaliveAckLatest)
})

watch(
  () => [roomId.value, otherUid.value] as const,
  () => {
    roomVersion.value += 1
    loading.value = false
    error.value = null
    collabOpen.value = false
    collabCreateBusy.value = false
    collabCreateError.value = null
    collabEdit.value = null
    endActionBusy.value = false
    appActionBusy.value = {}
    appActionError.value = {}
    collabActionBusy.value = {}
    collabActionError.value = {}
    lessonActionBusy.value = {}
    lessonActionError.value = {}
    cursor.value = null
    isLast.value = false
    messages.value = []
    pendingOutgoingMessages.value = []
    sending.value = false
    contactAutoShown.value = false
    void loadOtherUser()
    void chatRealtime.refreshUnreadFromServer()
    void ackFromRoomList()
    void loadMore().then(() => ackLatest())
    chatRealtime.setActiveRoom(roomId.value)
  },
  { immediate: true },
)

watch(
  () => chatRealtime.messageEventSerial,
  () => {
    const pending = chatRealtime.listMessageEventsAfter(lastConsumedMessageSerial.value)
    if (!pending.length) return

    const incoming: ChatMessageResp[] = []
    let latestMsgId = 0
    for (const item of pending) {
      lastConsumedMessageSerial.value = item.serial
      const ev = item.event
      if (ev.roomId !== roomId.value) continue
      latestMsgId = Math.max(latestMsgId, ev.msgId || 0)
      incoming.push({
        fromUser: { uid: ev.fromUid },
        message: { id: ev.msgId, roomId: ev.roomId, sendTime: ev.sendTime == null ? '' : String(ev.sendTime), body: ev.body },
      })
    }

    if (!incoming.length) return
    mergeMessages(incoming, 'append')
    if (latestMsgId > 0) {
      void chatRealtime.ackRoomRead(roomId.value, latestMsgId)
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <button class="btn back" type="button" @click="router.push({ name: 'chatList' })">返回</button>
      <div class="name">{{ otherUid ? pickDisplayName(otherUser, otherUid) : `会话 ${roomId}` }}</div>
      <div />
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card panel">
      <div class="msgs" ref="msgsRef">
        <div class="top-hint" v-if="!isLast && !loading">
          <button class="btn btn-text" type="button" @click="loadMore">查看更多历史消息</button>
        </div>
        <div class="top-hint" v-if="loading">加载中...</div>
        <div class="top-hint muted" v-if="isLast">没有更多历史消息了</div>

        <template v-for="it in renderItems" :key="it.key">
          <div v-if="it.kind === 'time'" class="time-divider">{{ it.text }}</div>
          <div v-else class="msg" :class="{ me: it.m.fromUser.uid === myUid }">
            <button class="avatar" type="button" :class="{ clickable: it.m.fromUser.uid !== myUid }" @click="openCard(it.m.fromUser.uid)">
              <img v-if="userAvatar(it.m.fromUser.uid)" :src="userAvatar(it.m.fromUser.uid)" alt="" @error="markAvatarBroken(it.m.fromUser.uid)" />
              <span v-else class="avatar-fallback">{{ userName(it.m.fromUser.uid).slice(0, 1) }}</span>
            </button>
            <div class="content">
              <div class="meta">{{ userName(it.m.fromUser.uid) }}</div>
              <div class="bubble">
                <template v-if="isLessonRequestBody(it.m.body)">
                <LessonRequestCard
                  :body="it.m.body"
                  :from-me="it.m.fromUser.uid === myUid"
                  :busy="lessonActionBusy[it.m.body.eventId]"
                  @accept="respondLesson(it.m.body.eventId, 'ACCEPT', it.m.message.id)"
                  @reject="respondLesson(it.m.body.eventId, 'REJECT', it.m.message.id)"
                />
                <div v-if="lessonActionError[it.m.body.eventId]" class="card-hint error">
                  {{ lessonActionError[it.m.body.eventId] }}
                </div>
              </template>
              <template v-else-if="isLessonStatusBody(it.m.body)">
                <div class="sys">
                  课程状态：{{ it.m.body.status }}（{{ it.m.body.title }}）
                </div>
              </template>
              <template v-else-if="isTutorApplicationBody(it.m.body)">
                <TutorApplicationCard
                  :body="{ ...it.m.body, status: effectiveAppStatus(it.m.body.applicationId, it.m.body.status) }"
                  :from-me="it.m.fromUser.uid === myUid"
                  :busy="appActionBusy[it.m.body.applicationId]"
                  @accept="respondTutorApplication(it.m.body.applicationId, 'ACCEPT', it.m.message.id)"
                  @reject="respondTutorApplication(it.m.body.applicationId, 'REJECT', it.m.message.id)"
                />
                <div v-if="appActionError[it.m.body.applicationId]" class="card-hint error">
                  {{ appActionError[it.m.body.applicationId] }}
                </div>
              </template>
              <template v-else-if="isTutorApplicationStatusBody(it.m.body)">
                <div class="sys">家教申请：{{ it.m.body.status }}</div>
              </template>
              <template v-else-if="isCollaborationProposalBody(it.m.body)">
                <CollaborationProposalCard
                  :body="{ ...it.m.body, status: effectiveCollabStatus(it.m.body.proposalId, it.m.body.status) }"
                  :from-me="it.m.fromUser.uid === myUid"
                  :busy="collabActionBusy[it.m.body.proposalId]"
                  @edit="openCollaborationEdit"
                  @accept="respondCollaboration(it.m.body.proposalId, 'ACCEPT', it.m.message.id)"
                  @reject="respondCollaboration(it.m.body.proposalId, 'REJECT', it.m.message.id)"
                />
                <div v-if="collabActionError[it.m.body.proposalId]" class="card-hint error">
                  {{ collabActionError[it.m.body.proposalId] }}
                </div>
              </template>
              <template v-else-if="isCollaborationStatusBody(it.m.body)">
                <div class="sys">合作提案：{{ collabStatusText(it.m.body.status) }}</div>
              </template>
              <template v-else-if="isBrokerageRequiredBody(it.m.body)">
                <BrokerageRequiredCard
                  :body="{ ...it.m.body, status: effectiveBrokerageStatus(it.m.body) }"
                  :can-pay="
                    auth.user?.userType === 1 &&
                    (!it.m.body.payerUserId || it.m.body.payerUserId === myUid) &&
                    effectiveBrokerageStatus(it.m.body) === 'PENDING'
                  "
                  @pay="goPay(it.m.body.orderId, it.m.body.proposalId)"
                />
              </template>
              <template v-else-if="isContactUnlockedBody(it.m.body)">
                <ContactUnlockedCard :body="it.m.body" :can-view="canViewContact" @view="viewUnlockedContact" />
              </template>
              <template v-else-if="isEndChatRequestBody(it.m.body)">
                <div class="end-card">
                  <div class="h1">结束沟通</div>
                  <div class="hint">{{ endChatStatusText(it.m.body.status) }}</div>
                  <div
                    v-if="
                      !isTeacher &&
                      it.m.fromUser.uid !== myUid &&
                      typeof it.m.body.requestId === 'number' &&
                      String(it.m.body.status || '').trim().toUpperCase() === 'PENDING_CONFIRM'
                    "
                    class="ops"
                  >
                    <button class="btn" type="button" :disabled="endActionBusy" @click="respondEndChat(it.m.body.requestId, 'REJECTED')">
                      暂不结束
                    </button>
                    <button class="btn btn-primary" type="button" :disabled="endActionBusy" @click="respondEndChat(it.m.body.requestId, 'CONFIRMED')">
                      确认结束
                    </button>
                  </div>
                </div>
              </template>
              <template v-else-if="isRefundRequestBody(it.m.body)">
                <div class="refund-card">
                  <div class="h1">沟通已结束</div>
                  <div class="hint">已提交退款申请，等待管理员审核通过后原路退回信息费</div>
                  <div class="status">{{ refundStatusText(it.m.body.status) }}</div>
                </div>
              </template>
              <template v-else>
                {{ msgText(it.m.body) }}
              </template>
            </div>
            <div v-if="messageReceiptText(it.m)" class="receipt">{{ messageReceiptText(it.m) }}</div>
          </div>
        </div>
        </template>
        <div v-for="pending in pendingOutgoingMessages" :key="pending.localId" class="msg me pending">
          <button class="avatar" type="button">
            <img v-if="userAvatar(myUid)" :src="userAvatar(myUid)" alt="" @error="markAvatarBroken(myUid)" />
            <span v-else class="avatar-fallback">{{ userName(myUid).slice(0, 1) }}</span>
          </button>
          <div class="content">
            <div class="meta">{{ userName(myUid) }}</div>
            <div class="bubble">{{ pending.content }}</div>
            <div class="receipt pending-state">
              <span v-if="pending.status === 'sending'">发送中...</span>
              <button v-else class="retry-link" type="button" :disabled="sending" @click="retryPendingOutgoingMessage(pending.localId)">
                发送失败，点击重试
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="composer">
        <div class="actions">
          <button v-if="!chatUnlocked && canSendTutorApplication" class="btn btn-primary" type="button" @click="sendTutorApplicationAgain">
            重新发送家教申请
          </button>
          <template v-else>
            <div class="action-row">
              <button class="btn" type="button" :disabled="hasActiveCollabProposal || !composerEnabled" @click="openCollaboration">
                {{ hasActiveCollabProposal ? '已发起合作' : '发起合作' }}
              </button>
              <span v-if="chatUnlocked && isTeacher && !hasAnyRefundRequest" :title="refundHoverText">
                <button class="btn" type="button" :disabled="!canApplyRefund" @click="applyRefund">
                  申请退费
                </button>
              </span>
              <button v-if="chatUnlocked" class="btn" type="button" :disabled="!canRequestEndChat || endRequestBusy" @click="requestEndChat">
                {{ endRequestBusy ? '提交中...' : '结束沟通' }}
              </button>
            </div>
          </template>
        </div>
        <div class="send">
          <input v-model="input" class="input" :disabled="!composerEnabled" :placeholder="composerEnabled ? '请输入消息' : composerLockedHint" @keydown.enter.prevent="onSend" />
          <button class="btn btn-primary" type="button" :disabled="sending || !composerEnabled" @click="onSend">
            {{ sending ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>

    <UserCardModal :open="cardOpen" :uid="cardUid" @close="closeCard" />
    <UnlockedContactModal
      :open="unlockOpen"
      :uid="unlockUid"
      :phone="unlockPhone"
      :loading="unlockLoading"
      :error="unlockError"
      @close="closeUnlock"
    />
    <CollaborationProposalModal
      :open="collabOpen"
      :busy="collabCreateBusy"
      :error="collabCreateError"
      :title="collabEdit ? '修改提案' : '发起合作'"
      :submit-text="collabEdit ? '保存修改' : '发送提案'"
      :initial="collabEdit ? collabEdit.initial : null"
      @close="closeCollaboration"
      @submit="createCollaboration"
    />
  </div>
</template>

<style scoped>
.wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
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
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.msgs {
  flex: 1 1 auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fbfcfe;
  overflow-y: auto;
  min-height: 0;
}

.top-hint {
  text-align: center;
  padding: 8px;
  font-size: 12px;
  color: var(--muted);
}

.btn-text {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  padding: 4px 8px;
}

.btn-text:hover {
  text-decoration: underline;
}

.top-hint.muted {
  color: var(--muted);
}

.time-divider {
  justify-self: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  color: rgba(0, 0, 0, 0.55);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(31, 35, 41, 0.08);
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

.receipt {
  font-size: 12px;
  line-height: 1;
  color: rgba(0, 0, 0, 0.45);
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

.msg.pending .bubble {
  opacity: 0.85;
}

.pending-state {
  display: flex;
  justify-content: flex-end;
}

.retry-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(212, 60, 60, 0.9);
  font-size: 12px;
  cursor: pointer;
}

.retry-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.action-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.refund-card {
  min-width: 260px;
  max-width: 360px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 10px 10px 12px;
  background: rgba(255, 149, 0, 0.08);
}

.refund-card .h1 {
  font-weight: 900;
  margin-bottom: 6px;
}

.refund-card .hint {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
  padding: 0;
  border: 0;
  background: transparent;
}

.refund-card .status {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 800;
}

.end-card {
  min-width: 260px;
  max-width: 360px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 10px 10px 12px;
  background: rgba(18, 180, 171, 0.08);
  display: grid;
  gap: 8px;
}

.end-card .h1 {
  font-weight: 900;
}

.end-card .hint {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
}

.end-card .ops {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  z-index: 1000;
}

.modal {
  width: min(92vw, 420px);
  display: grid;
  gap: 10px;
  padding: 12px;
}

.m-title {
  font-weight: 900;
  font-size: 16px;
}

.m-desc {
  font-size: 13px;
  color: var(--muted);
  font-weight: 700;
}

.m-error {
  font-size: 12px;
  color: rgba(255, 0, 0, 0.8);
  font-weight: 800;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
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
