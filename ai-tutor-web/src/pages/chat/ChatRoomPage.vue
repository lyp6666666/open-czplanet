<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import type { ChatPresenceResp, ChatRefundStateResp } from '@/api/chat'
import { assetsApi } from '@/api/assets'
import { contactApi } from '@/api/contact'
import { applicationApi } from '@/api/application'
import { liveApi } from '@/api/live'
import { scheduleApi } from '@/api/schedule'
import { userApi } from '@/api/user'
import type { ChatMessageBody, ChatMessageResp, CollaborationProposalStatus, TutorApplicationCardStatus, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'
import { isRoomPinned, setRoomPinned, subscribeChatPinChange } from '@/utils/chatPins'
import { normalizeAssetUrl, normalizeAvatarUrl } from '@/utils/avatar'
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
const otherPresence = ref<ChatPresenceResp | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

const sending = ref(false)
const input = ref('')
const recallingMsgId = ref<number | null>(null)
const actionMenuOpen = ref(false)
const searchPanelOpen = ref(false)
const searchKeyword = ref('')
const searchedKeyword = ref('')
const searchLoading = ref(false)
const searchError = ref<string | null>(null)
const searchCursor = ref<string | null>(null)
const searchIsLast = ref(true)
const searchFocusedMsgId = ref<number | null>(null)
const searchResults = ref<ChatMessageResp[]>([])

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
const imageInputRef = ref<HTMLInputElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)
const headerActionsRef = ref<HTMLElement | null>(null)
const roomVersion = ref(0)
const avatarBroken = ref<Record<number, boolean>>({})
const lastConsumedMessageSerial = ref(0)
const TYPING_REPORT_THROTTLE_MS = 2_000
const TYPING_IDLE_TIMEOUT_MS = 2_200
const typingReportRoomId = ref<number | null>(null)
const typingReported = ref(false)
const lastTypingReportAt = ref(0)
const typingStopTimer = ref<ReturnType<typeof globalThis.setTimeout> | null>(null)
const roomPinned = ref(false)
let stopPinSync: (() => void) | null = null
let presenceRefreshTimer: ReturnType<typeof globalThis.setInterval> | null = null
const PRESENCE_REFRESH_INTERVAL_MS = 15_000

const myUid = computed(() => auth.user?.id ?? 0)
const peerLastDeliveredMsgId = computed(() => chatRealtime.peerDeliveredMsgIdByRoom[roomId.value] || 0)
const peerLastReadMsgId = computed(() => chatRealtime.peerReadMsgIdByRoom[roomId.value] || 0)
const peerTyping = computed(() => chatRealtime.peerTypingByRoom[roomId.value] === true)
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

function clearPresenceRefreshTimer() {
  if (presenceRefreshTimer == null) return
  globalThis.clearInterval(presenceRefreshTimer)
  presenceRefreshTimer = null
}

function parseDateLike(value: string | number | Date | null | undefined): number | null {
  if (value instanceof Date) {
    const ts = value.getTime()
    return Number.isFinite(ts) ? ts : null
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }
  const raw = String(value || '').trim()
  if (!raw) return null
  const directNumber = Number(raw)
  if (Number.isFinite(directNumber)) return directNumber
  const parsed = Date.parse(raw)
  return Number.isFinite(parsed) ? parsed : null
}

function formatPresenceTime(value: string | number | Date | null | undefined): string {
  const ts = parseDateLike(value)
  if (ts == null) return ''
  const date = new Date(ts)
  const day = `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
  const hm = `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
  return day === localTodayDay() ? `今天 ${hm}` : `${day} ${hm}`
}

const otherPresenceText = computed(() => {
  if (!otherUid.value) return ''
  if (otherPresence.value?.online) return '在线'
  const lastOnlineText = formatPresenceTime(otherPresence.value?.lastOnlineAt)
  return lastOnlineText ? `离线 · 最后在线 ${lastOnlineText}` : '离线'
})

watch(
  () => (otherUid.value ? chatRealtime.peerPresenceByUid[otherUid.value] ?? null : null),
  (presence) => {
    if (!otherUid.value) {
      otherPresence.value = null
      return
    }
    if (!presence) {
      if (otherPresence.value?.uid === otherUid.value) return
      otherPresence.value = null
      return
    }
    otherPresence.value = { ...presence }
  },
  { immediate: true },
)

function userAvatar(uid: number): string {
  if (avatarBroken.value[uid]) return ''
  const v = uid === myUid.value ? myUser.value?.avatar : uid === otherUid.value ? otherUser.value?.avatar : getUser(uid)?.avatar
  return normalizeAvatarUrl(v)
}

function messageImageUrl(body: Extract<ChatMessageBody, { type: 'image' }>): string {
  return normalizeAssetUrl(body.url)
}

function markAvatarBroken(uid: number) {
  if (!uid) return
  if (avatarBroken.value[uid]) return
  avatarBroken.value = { ...avatarBroken.value, [uid]: true }
}

function refreshRoomPinned() {
  roomPinned.value = isRoomPinned(auth.user?.id, roomId.value)
}

function toggleRoomPinned() {
  if (!(roomId.value > 0)) return
  const next = !roomPinned.value
  setRoomPinned(auth.user?.id, roomId.value, next)
  roomPinned.value = next
}

function closeActionMenu() {
  actionMenuOpen.value = false
}

function toggleActionMenu() {
  actionMenuOpen.value = !actionMenuOpen.value
}

async function openSearchPanel() {
  searchPanelOpen.value = true
  closeActionMenu()
  await nextTick()
  searchInputRef.value?.focus()
  searchInputRef.value?.select()
}

function closeSearchPanel() {
  searchPanelOpen.value = false
}

function toggleRoomPinnedFromMenu() {
  toggleRoomPinned()
  closeActionMenu()
}

// 点击菜单外区域时自动收起，避免右上角浮层残留。
function handleDocumentPointerDown(event: PointerEvent) {
  if (!actionMenuOpen.value) return
  const target = event.target
  if (!(target instanceof Node)) return
  if (headerActionsRef.value?.contains(target)) return
  closeActionMenu()
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeActionMenu()
  }
}

function resetMessageSearch(clearKeyword = false) {
  searchedKeyword.value = ''
  searchError.value = null
  searchCursor.value = null
  searchIsLast.value = true
  searchFocusedMsgId.value = null
  searchResults.value = []
  if (clearKeyword) {
    searchKeyword.value = ''
  }
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
const imageSending = ref(false)

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
  refreshRoomPinned()
  stopPinSync = subscribeChatPinChange(() => refreshRoomPinned())
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

function isImageBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'image' }> {
  return body.type === 'image'
}

function isRecallBody(body: ChatMessageBody): body is Extract<ChatMessageBody, { type: 'recall' }> {
  return body.type === 'recall'
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

function openLiveFromLesson(eventId: number) {
  void router.push({ name: 'livePrepare', params: { courseId: String(eventId) } })
}

async function openLiveByStatus(eventId: number) {
  try {
    const live = await liveApi.getByCourse(eventId)
    if (!live.sessionId) return
    openLiveFromLesson(eventId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课堂暂不可进入'
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
    if (any.type === 'recall') return '[消息已撤回]'
    if (typeof any.content === 'string') return any.content
  }
  try {
    return JSON.stringify(raw)
  } catch {
    return String(raw)
  }
}

function searchResultText(message: ChatMessageResp): string {
  const body = normalizeBody(message.message?.body)
  if (isRecallBody(body)) {
    return recallText(body, message.fromUser.uid)
  }
  if (isImageBody(body)) return '[图片]'
  return msgText(body)
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function escapeRegex(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function highlightSearchText(text: string): string {
  const raw = String(text || '')
  const keyword = searchedKeyword.value.trim()
  if (!keyword) return escapeHtml(raw)
  const matcher = new RegExp(escapeRegex(keyword), 'gi')
  let lastIndex = 0
  let out = ''
  for (const match of raw.matchAll(matcher)) {
    const start = match.index ?? 0
    const matched = match[0] ?? ''
    out += escapeHtml(raw.slice(lastIndex, start))
    out += `<mark>${escapeHtml(matched)}</mark>`
    lastIndex = start + matched.length
  }
  out += escapeHtml(raw.slice(lastIndex))
  return out
}

const sortedMessages = computed(() => {
  const list = messages.value.slice()
  list.sort((a, b) => (a.message?.id || 0) - (b.message?.id || 0))
  return list
})

type RenderMessage = ChatMessageResp & { body: ChatMessageBody }

const recallByTargetMsgId = computed<Record<number, { operatorUid: number | null }>>(() => {
  const out: Record<number, { operatorUid: number | null }> = {}
  const messageIdSet = new Set(sortedMessages.value.map((m) => m.message?.id).filter((id): id is number => typeof id === 'number' && id > 0))
  for (const m of sortedMessages.value) {
    const body = normalizeBody(m.message?.body)
    if (!isRecallBody(body)) continue
    const targetMsgId = typeof body.targetMsgId === 'number' ? body.targetMsgId : Number(body.targetMsgId)
    if (!(targetMsgId > 0) || !messageIdSet.has(targetMsgId)) continue
    out[targetMsgId] = {
      operatorUid: typeof body.operatorUid === 'number' ? body.operatorUid : m.fromUser.uid,
    }
  }
  return out
})

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
    if (isRecallBody(body)) {
      const targetMsgId = typeof body.targetMsgId === 'number' ? body.targetMsgId : Number(body.targetMsgId)
      if (targetMsgId > 0 && recallByTargetMsgId.value[targetMsgId]) {
        continue
      }
      list.push({
        ...m,
        body: {
          ...body,
          operatorUid: typeof body.operatorUid === 'number' ? body.operatorUid : m.fromUser.uid,
        },
      })
      continue
    }
    const recallOverlay = recallByTargetMsgId.value[m.message.id]
    if (recallOverlay) {
      list.push({
        ...m,
        body: {
          type: 'recall',
          targetMsgId: m.message.id,
          operatorUid: recallOverlay.operatorUid,
        },
      })
      continue
    }
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
  if (isRecallBody(message.body)) return ''
  if (message.message.id !== latestOutgoingMsgId.value) return ''
  if (peerLastReadMsgId.value >= message.message.id) return '对方已读'
  if (peerLastDeliveredMsgId.value >= message.message.id) return '已送达'
  return '已发送'
}

function recallText(body: Extract<ChatMessageBody, { type: 'recall' }>, fallbackUid: number): string {
  const operatorUid = typeof body.operatorUid === 'number' ? body.operatorUid : fallbackUid
  return operatorUid === myUid.value ? '你撤回了一条消息' : `${userName(operatorUid)}撤回了一条消息`
}

function canRecallMessage(message: RenderMessage): boolean {
  if (message.fromUser.uid !== myUid.value) return false
  if (isRecallBody(message.body)) return false
  return message.body.type === 'text' || message.body.type === 'image'
}

async function recallMessage(messageId: number) {
  if (!(roomId.value > 0) || !(messageId > 0)) return
  if (recallingMsgId.value === messageId) return
  recallingMsgId.value = messageId
  error.value = null
  try {
    const msg = await chatApi.recallMessage(roomId.value, messageId)
    mergeMessages([msg], 'append')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '撤回失败'
  } finally {
    if (recallingMsgId.value === messageId) {
      recallingMsgId.value = null
    }
  }
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
  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T')
  const parsedTs = Date.parse(normalized)
  if (Number.isFinite(parsedTs)) {
    const date = new Date(parsedTs)
    const day = `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
    const hm = `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
    return day === localTodayDay() ? hm : `${day} ${hm}`
  }

  const twelveHourMatch = raw.match(/\b(\d{1,2}):(\d{2})\s*([AaPp][Mm])\b/)
  if (twelveHourMatch) {
    const rawHour = Number(twelveHourMatch[1] || 0)
    const minute = twelveHourMatch[2] || '00'
    const suffix = String(twelveHourMatch[3] || '').toUpperCase()
    const hour24 = suffix === 'PM' ? (rawHour % 12) + 12 : rawHour % 12
    return `${pad2(hour24)}:${minute}`
  }

  const s = raw.includes('T') ? raw.replace('T', ' ') : raw
  const m = s.match(/\b(\d{1,2}):(\d{2})\b/)
  return m ? `${pad2(Number(m[1] || 0))}:${m[2]}` : s
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

async function loadOtherPresence() {
  if (!otherUid.value) {
    otherPresence.value = null
    return
  }
  const v = roomVersion.value
  try {
    const list = await chatApi.batchPresence([otherUid.value])
    if (v !== roomVersion.value) return
    chatRealtime.setPeerPresenceSnapshot(list[0] || { uid: otherUid.value, online: false, lastOnlineAt: null })
  } catch {
    if (v !== roomVersion.value) return
    // 在线状态查询失败时不打断聊天主流程；若已有状态则继续沿用，避免页面闪烁误判。
    if (otherPresence.value?.uid === otherUid.value) return
    chatRealtime.setPeerPresenceSnapshot({ uid: otherUid.value, online: false, lastOnlineAt: null })
  }
}

function startPresenceRefresh() {
  clearPresenceRefreshTimer()
  if (typeof globalThis.setInterval !== 'function') return
  if (!otherUid.value) return
  presenceRefreshTimer = globalThis.setInterval(() => {
    void loadOtherPresence()
  }, PRESENCE_REFRESH_INTERVAL_MS)
}

function scrollToBottom() {
  if (!msgsRef.value) return
  const el = msgsRef.value
  requestAnimationFrame(() => {
    el.scrollTop = el.scrollHeight
  })
}

async function scrollToMessage(messageId: number) {
  await nextTick()
  const target = msgsRef.value?.querySelector<HTMLElement>(`[data-msg-id="${messageId}"]`)
  if (!target) return
  if (typeof target.scrollIntoView === 'function') {
    target.scrollIntoView({ block: 'center' })
  }
}

async function locateSearchResult(message: ChatMessageResp) {
  mergeMessages([message], 'prepend')
  searchFocusedMsgId.value = message.message.id
  await scrollToMessage(message.message.id)
}

async function searchMessages(reset = true) {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    resetMessageSearch()
    return
  }
  if (!(roomId.value > 0) || searchLoading.value) return
  searchPanelOpen.value = true
  searchLoading.value = true
  searchError.value = null
  if (reset) {
    searchFocusedMsgId.value = null
  }
  try {
    const page = await chatApi.searchMessages({
      roomId: roomId.value,
      keyword,
      pageSize: 20,
      cursor: reset ? null : searchCursor.value,
    })
    const incoming = page.list || []
    searchedKeyword.value = keyword
    searchCursor.value = page.cursor ?? null
    searchIsLast.value = !!page.isLast
    searchResults.value = reset
      ? incoming
      : Array.from(new Map([...searchResults.value, ...incoming].map((item) => [item.message.id, item])).values())
  } catch (e) {
    searchError.value = e instanceof Error ? e.message : '搜索失败'
    if (reset) {
      searchResults.value = []
      searchCursor.value = null
      searchIsLast.value = true
      searchedKeyword.value = keyword
    }
  } finally {
    searchLoading.value = false
  }
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
  if (!(roomId.value > 0)) return
  const latest = messages.value.reduce((max, m) => Math.max(max, m.message?.id || 0), 0)
  if (latest <= 0) return
  void chatRealtime.ackRoomRead(roomId.value, latest)
}

function keepaliveAckLatest() {
  if (!(roomId.value > 0)) return
  const latest = messages.value.reduce((max, m) => Math.max(max, m.message?.id || 0), 0)
  if (latest <= 0) return
  chatRealtime.ackRoomReadKeepalive(roomId.value, latest)
}

function clearTypingStopTimer() {
  if (typingStopTimer.value == null) return
  globalThis.clearTimeout(typingStopTimer.value)
  typingStopTimer.value = null
}

async function reportTypingState(targetRoomId: number, typing: boolean, force = false) {
  if (!(targetRoomId > 0)) return

  if (!typing) {
    if (!typingReported.value || typingReportRoomId.value !== targetRoomId) return
    typingReported.value = false
    typingReportRoomId.value = null
    lastTypingReportAt.value = Date.now()
    try {
      await chatApi.reportTyping(targetRoomId, false)
    } catch {
      void 0
    }
    return
  }

  if (!composerEnabled.value || targetRoomId !== roomId.value) return
  const now = Date.now()
  const isSameRoom = typingReportRoomId.value === targetRoomId
  if (!force && isSameRoom && typingReported.value && now - lastTypingReportAt.value < TYPING_REPORT_THROTTLE_MS) {
    return
  }

  // “正在输入”只做在线短态展示，这里按节流续期，避免每次按键都打到后端。
  typingReported.value = true
  typingReportRoomId.value = targetRoomId
  lastTypingReportAt.value = now
  try {
    await chatApi.reportTyping(targetRoomId, true)
  } catch {
    void 0
  }
}

function scheduleTypingStop() {
  clearTypingStopTimer()
  typingStopTimer.value = globalThis.setTimeout(() => {
    void reportTypingState(roomId.value, false, true)
  }, TYPING_IDLE_TIMEOUT_MS)
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
  clearTypingStopTimer()
  void reportTypingState(roomId.value, false, true)
  const pending = createPendingOutgoingMessage(text)
  pendingOutgoingMessages.value = [...pendingOutgoingMessages.value, pending]
  input.value = ''
  void scrollToBottom()
  await dispatchPendingOutgoingMessage(pending.localId, pending.roomId, roomVersion.value)
}

function openImagePicker() {
  if (!composerEnabled.value) {
    error.value = composerLockedHint.value
    return
  }
  if (imageSending.value) return
  imageInputRef.value?.click()
}

async function onSelectImage(event: Event) {
  const inputElement = event.target as HTMLInputElement | null
  const file = inputElement?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    if (inputElement) inputElement.value = ''
    return
  }
  if (!composerEnabled.value) {
    error.value = composerLockedHint.value
    if (inputElement) inputElement.value = ''
    return
  }

  clearTypingStopTimer()
  void reportTypingState(roomId.value, false, true)
  imageSending.value = true
  error.value = null
  try {
    const upload = await assetsApi.uploadImage(file, 'other')
    const dimensions = await readImageDimensions(file)
    const msg = await chatApi.sendImage(roomId.value, {
      url: upload.url,
      objectKey: upload.objectKey,
      contentType: upload.contentType,
      size: upload.size,
      width: dimensions.width,
      height: dimensions.height,
    })
    mergeMessages([msg], 'append')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '图片发送失败'
  } finally {
    imageSending.value = false
    if (inputElement) inputElement.value = ''
  }
}

function readImageDimensions(file: File): Promise<{ width: number | null; height: number | null }> {
  return new Promise((resolve) => {
    if (typeof Image === 'undefined' || typeof URL.createObjectURL !== 'function') {
      resolve({ width: null, height: null })
      return
    }
    const objectUrl = URL.createObjectURL(file)
    let finished = false
    const finish = (width: number | null, height: number | null) => {
      if (finished) return
      finished = true
      if (typeof URL.revokeObjectURL === 'function') {
        URL.revokeObjectURL(objectUrl)
      }
      resolve({ width, height })
    }
    const image = new Image()
    const timeout = globalThis.setTimeout(() => finish(null, null), 80)
    image.onload = () => {
      globalThis.clearTimeout(timeout)
      const width = Number.isFinite(image.naturalWidth) ? image.naturalWidth : null
      const height = Number.isFinite(image.naturalHeight) ? image.naturalHeight : null
      finish(width, height)
    }
    image.onerror = () => {
      globalThis.clearTimeout(timeout)
      finish(null, null)
    }
    image.src = objectUrl
  })
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
  clearTypingStopTimer()
  clearPresenceRefreshTimer()
  const currentTypingRoomId = typingReportRoomId.value
  if (currentTypingRoomId) {
    void chatApi.reportTyping(currentTypingRoomId, false).catch(() => undefined)
  }
  keepaliveAckLatest()
  chatRealtime.setActiveRoom(null)
  stopPinSync?.()
  stopPinSync = null
})

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  document.addEventListener('keydown', handleDocumentKeydown)
  window.addEventListener('pagehide', keepaliveAckLatest)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  document.removeEventListener('keydown', handleDocumentKeydown)
  window.removeEventListener('pagehide', keepaliveAckLatest)
})

watch(
  () => input.value,
  (value) => {
    const hasText = value.trim().length > 0
    if (!hasText) {
      clearTypingStopTimer()
      void reportTypingState(roomId.value, false, true)
      return
    }
    if (!composerEnabled.value) return
    void reportTypingState(roomId.value, true)
    scheduleTypingStop()
  },
)

watch(
  composerEnabled,
  (enabled) => {
    if (enabled) return
    clearTypingStopTimer()
    void reportTypingState(roomId.value, false, true)
  },
)

watch(
  () => roomId.value,
  (nextRoomId, previousRoomId) => {
    if (previousRoomId && previousRoomId !== nextRoomId) {
      clearTypingStopTimer()
      void reportTypingState(previousRoomId, false, true)
    }
    closeActionMenu()
    refreshRoomPinned()
  },
)

watch(
  () => [roomId.value, otherUid.value] as const,
  () => {
    closeActionMenu()
    searchPanelOpen.value = false
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
    otherPresence.value = null
    resetMessageSearch(true)
    pendingOutgoingMessages.value = []
    sending.value = false
    contactAutoShown.value = false
    void loadOtherUser()
    void loadOtherPresence()
    startPresenceRefresh()
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
      <div class="head-left">
        <button class="back-trigger" type="button" aria-label="返回会话列表" @click="router.push({ name: 'chatList' })">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M14.5 5.5L8 12l6.5 6.5" />
          </svg>
        </button>
        <div class="head-main">
          <div class="name-row">
            <div class="name">{{ otherUid ? pickDisplayName(otherUser, otherUid) : `会话 ${roomId}` }}</div>
            <span v-if="roomPinned" class="pin-badge">已置顶</span>
          </div>
          <div v-if="otherPresenceText" class="presence-text">{{ otherPresenceText }}</div>
          <div v-if="peerTyping" class="typing-hint">对方正在输入...</div>
        </div>
      </div>
      <div ref="headerActionsRef" class="head-actions">
        <button
          class="more-trigger"
          type="button"
          aria-label="更多会话操作"
          :aria-expanded="actionMenuOpen"
          @click.stop="toggleActionMenu"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="5" cy="12" r="1.8" />
            <circle cx="12" cy="12" r="1.8" />
            <circle cx="19" cy="12" r="1.8" />
          </svg>
        </button>
        <div v-if="actionMenuOpen" class="more-menu card" @click.stop>
          <button class="chat-menu-item search-action" type="button" @click="openSearchPanel">
            <span>查找聊天内容</span>
            <span class="menu-arrow">›</span>
          </button>
          <button class="chat-menu-item pin-action" type="button" @click="toggleRoomPinnedFromMenu">
            <span>{{ roomPinned ? '取消置顶聊天' : '置顶聊天' }}</span>
            <span class="menu-state" :class="{ active: roomPinned }">{{ roomPinned ? '已开启' : '未开启' }}</span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="searchPanelOpen" class="card search-panel">
      <div class="search-panel-head">
        <div class="search-panel-title">查找聊天内容</div>
        <div class="search-panel-actions">
          <button v-if="searchedKeyword || searchResults.length > 0 || searchKeyword.trim()" class="btn-text search-tool" type="button" @click="resetMessageSearch(true)">
            清空
          </button>
          <button class="btn-text search-tool" type="button" @click="closeSearchPanel">收起</button>
        </div>
      </div>
      <div class="search-row">
        <label class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10.5 5a5.5 5.5 0 1 0 0 11a5.5 5.5 0 0 0 0-11Zm0 0l7 7" />
          </svg>
          <input
            ref="searchInputRef"
            v-model="searchKeyword"
            class="search-input"
            type="text"
            placeholder="搜索本会话消息"
            @keydown.enter.prevent="searchMessages(true)"
          />
        </label>
        <button class="btn btn-primary search-submit" type="button" :disabled="searchLoading || !searchKeyword.trim()" @click="searchMessages(true)">
          {{ searchLoading ? '搜索中...' : '搜索' }}
        </button>
      </div>
      <div v-if="searchError" class="hint error search-hint">{{ searchError }}</div>
      <div v-else-if="searchResults.length > 0" class="search-results">
        <div class="search-summary">“{{ searchedKeyword }}” 共命中 {{ searchResults.length }} 条消息</div>
        <button
          v-for="result in searchResults"
          :key="`search-${result.message.id}`"
          class="search-hit"
          type="button"
          @click="locateSearchResult(result)"
        >
          <div class="search-hit-meta">
            <span>{{ userName(result.fromUser.uid) }}</span>
            <span>{{ formatMsgTime(result.message.sendTime) }}</span>
          </div>
          <div class="search-hit-text" v-html="highlightSearchText(searchResultText(result))"></div>
        </button>
        <button v-if="!searchIsLast" class="btn btn-text search-more" type="button" :disabled="searchLoading" @click="searchMessages(false)">
          {{ searchLoading ? '加载中...' : '查看更多结果' }}
        </button>
      </div>
      <div v-else-if="searchedKeyword && !searchLoading" class="search-empty">未找到包含“{{ searchedKeyword }}”的消息</div>
    </div>

    <div class="card panel">
      <div class="msgs" ref="msgsRef">
        <div class="top-hint" v-if="!isLast && !loading">
          <button class="btn btn-text" type="button" @click="loadMore">查看更多历史消息</button>
        </div>
        <div class="top-hint" v-if="loading">加载中...</div>
        <div class="top-hint muted" v-if="isLast">没有更多历史消息了</div>

        <template v-for="it in renderItems" :key="it.key">
          <div v-if="it.kind === 'time'" class="time-divider">{{ it.text }}</div>
          <div v-else class="msg" :class="{ me: it.m.fromUser.uid === myUid, 'search-focused': searchFocusedMsgId === it.m.message.id }" :data-msg-id="it.m.message.id">
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
                <div class="sys sys-with-action">
                  <span>课程状态：{{ it.m.body.status }}（{{ it.m.body.title }}）</span>
                  <button v-if="it.m.body.status === 'ACCEPTED'" class="btn btn-text join-lesson-btn" type="button" @click="openLiveByStatus(it.m.body.eventId)">
                    进入课堂
                  </button>
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
              <template v-else-if="isImageBody(it.m.body)">
                <img class="chat-image" :src="messageImageUrl(it.m.body)" alt="聊天图片" loading="lazy" />
              </template>
              <template v-else-if="isRecallBody(it.m.body)">
                <div class="sys recall-text">{{ recallText(it.m.body, it.m.fromUser.uid) }}</div>
              </template>
              <template v-else>
                {{ msgText(it.m.body) }}
              </template>
            </div>
            <div v-if="canRecallMessage(it.m)" class="msg-actions">
              <button class="msg-op-link recall-link" type="button" :disabled="recallingMsgId === it.m.message.id" @click="recallMessage(it.m.message.id)">
                {{ recallingMsgId === it.m.message.id ? '撤回中...' : '撤回' }}
              </button>
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
          <input ref="imageInputRef" class="image-input" type="file" accept="image/*" @change="onSelectImage" />
          <button class="btn" type="button" :disabled="!composerEnabled || imageSending" @click="openImagePicker">
            {{ imageSending ? '上传中...' : '发图片' }}
          </button>
          <input v-model="input" class="input" :disabled="!composerEnabled" :placeholder="composerEnabled ? '请输入消息' : composerLockedHint" @keydown.enter.prevent="onSend" />
          <button class="btn btn-primary" type="button" :disabled="sending || imageSending || !composerEnabled" @click="onSend">
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.head-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.name {
  font-size: 20px;
  font-weight: 900;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pin-badge {
  flex: 0 0 auto;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(32, 128, 240, 0.1);
  color: #1767c4;
  font-size: 12px;
  line-height: 1;
}

.presence-text {
  font-size: 12px;
  color: var(--muted);
  line-height: 1;
}

.typing-hint {
  font-size: 12px;
  color: var(--muted);
  line-height: 1;
}

.back-trigger,
.more-trigger {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: #f7f8fa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1f2937;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.back-trigger:hover,
.more-trigger:hover {
  background: #eef2f7;
  border-color: rgba(15, 23, 42, 0.14);
  transform: translateY(-1px);
}

.back-trigger svg,
.more-trigger svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.more-trigger svg {
  fill: currentColor;
  stroke: none;
}

.head-actions {
  position: relative;
  flex: 0 0 auto;
}

.more-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 30;
  min-width: 220px;
  padding: 8px;
  display: grid;
  gap: 6px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12);
}

.chat-menu-item {
  border: none;
  border-radius: 14px;
  background: #fff;
  min-height: 48px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: var(--text);
  cursor: pointer;
  text-align: left;
}

.chat-menu-item:hover {
  background: #f5f7fb;
}

.menu-arrow {
  color: #94a3b8;
  font-size: 20px;
  line-height: 1;
}

.menu-state {
  font-size: 12px;
  color: #94a3b8;
}

.menu-state.active {
  color: #1767c4;
}

.panel {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.search-panel {
  display: grid;
  gap: 12px;
  padding: 14px 16px;
  background:
    radial-gradient(circle at top right, rgba(32, 128, 240, 0.08), transparent 34%),
    linear-gradient(180deg, #fbfcff 0%, #f6f8fb 100%);
}

.search-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.search-panel-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.search-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.search-box {
  height: 44px;
  min-width: 0;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
}

.search-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.search-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  color: #9aa4b2;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.search-input {
  height: 100%;
  min-width: 0;
  border: none;
  padding: 0;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--text);
}

.search-submit {
  min-width: 88px;
}

.search-tool {
  padding: 4px 10px;
  border-radius: 999px;
  color: #516173;
}

.search-tool:hover {
  text-decoration: none;
  background: rgba(15, 23, 42, 0.05);
}

.search-results {
  display: grid;
  gap: 8px;
}

.search-summary,
.search-empty {
  font-size: 12px;
  color: var(--muted);
}

.search-hit {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
  text-align: left;
  display: grid;
  gap: 6px;
  cursor: pointer;
}

.search-hit-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--muted);
}

.search-hit-text {
  font-size: 13px;
  color: var(--text);
  line-height: 1.5;
  word-break: break-word;
}

.search-hit-text :deep(mark) {
  background: rgba(255, 196, 0, 0.26);
  color: inherit;
  padding: 0 2px;
  border-radius: 4px;
}

.search-more,
.search-hint {
  justify-self: start;
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
  align-self: center;
  margin: 4px 0 2px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 11px;
  line-height: 1.2;
  color: rgba(0, 0, 0, 0.35);
  letter-spacing: 0.2px;
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

.search-focused .bubble,
.search-focused .sys,
.search-focused .refund-card,
.search-focused .end-card {
  box-shadow: 0 0 0 2px rgba(255, 196, 0, 0.45);
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

.sys {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.03);
  font-size: 13px;
  line-height: 1.6;
}

.sys-with-action {
  justify-content: space-between;
  min-width: 280px;
}

.join-lesson-btn {
  flex: 0 0 auto;
  color: #0f766e;
  font-weight: 800;
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
  grid-template-columns: auto 1fr auto;
  gap: 10px;
}

.image-input {
  display: none;
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

.chat-image {
  display: block;
  max-width: min(320px, 65vw);
  max-height: 320px;
  border-radius: 12px;
  object-fit: cover;
  background: rgba(0, 0, 0, 0.04);
}

.msg-actions {
  margin-top: 6px;
}

.msg-op-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--muted);
  font-size: 12px;
  cursor: pointer;
}

.msg-op-link:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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
