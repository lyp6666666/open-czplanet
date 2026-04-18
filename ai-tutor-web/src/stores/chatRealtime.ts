import { defineStore } from 'pinia'

import { chatApi } from '@/api/chat'
import { notifyAuthInvalid } from '@/api/http'
import { BRAND_NAME } from '@/constants/brand'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

type StreamMsgEvent = {
  msgId: number
  roomId: number
  fromUid: number
  toUid: number
  sendTime: unknown
  body: unknown
}

type RealtimeEnvelope = {
  eventId?: number
  eventType?: string
  bizType?: string
  targetUid?: number
  roomId?: number | null
  msgId?: number | null
  occurredAt?: string | number | Date | null
  clientId?: string | null
  payload?: unknown
}

type ApplicationRealtimeEvent = {
  eventType: string
  applicationId: number
  status?: string
  occurredAt?: unknown
  payload: unknown
}

type ChatReadRealtimeEvent = {
  roomId: number
  readerUid?: number
  lastReadMsgId: number
}

type ChatDeliveryRealtimeEvent = {
  roomId: number
  deliverUid?: number
  lastDeliveredMsgId: number
}

type ChatTypingRealtimeEvent = {
  roomId: number
  typingUid?: number
  typing: boolean
}

type ChatPresenceRealtimeEvent = {
  uid: number
  online: boolean
  lastOnlineAt?: string | number | Date | null
}

type QueuedMessageEvent = {
  serial: number
  event: StreamMsgEvent
}

type StreamReadyEvent = {
  clientId?: string | null
  lastEventId?: number | null
  replayedCount?: number | null
}

function sleep(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms))
}

function normalizeBaseUrl(raw: unknown) {
  const s = typeof raw === 'string' ? raw.trim() : ''
  return s.length > 0 ? s : ''
}

function apiUrl(path: string) {
  const baseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
  return `${baseUrl}${path}`
}

const ackPendingByRoom = new Map<number, number>()
const ackInFlightByRoom = new Map<number, Promise<void>>()
const deliveryPendingByRoom = new Map<number, number>()
const deliveryInFlightByRoom = new Map<number, Promise<void>>()
const READ_MARKS_STORAGE_PREFIX = 'ai_tutor_chat_read_marks:'
const REALTIME_LAST_EVENT_STORAGE_PREFIX = 'ai_tutor_realtime_last_event:'
const REALTIME_CLIENT_ID_STORAGE_PREFIX = 'ai_tutor_realtime_client:'
const MESSAGE_EVENT_LOG_LIMIT = 200
const REALTIME_IDLE_TIMEOUT_MS = 45_000
const REALTIME_WATCHDOG_INTERVAL_MS = 10_000
const PEER_TYPING_EXPIRE_MS = 3_500
let realtimeSyncInFlight: Promise<void> | null = null
const peerTypingTimerByRoom = new Map<number, ReturnType<typeof globalThis.setTimeout>>()

function clearPeerTypingTimer(roomId: number) {
  const timer = peerTypingTimerByRoom.get(roomId)
  if (timer == null) return
  globalThis.clearTimeout(timer)
  peerTypingTimerByRoom.delete(roomId)
}

function clearAllPeerTypingTimers() {
  for (const roomId of peerTypingTimerByRoom.keys()) {
    clearPeerTypingTimer(roomId)
  }
}

function buildReadMarksStorageKey(uid: number) {
  return `${READ_MARKS_STORAGE_PREFIX}${uid}`
}

function buildLastEventStorageKey(uid: number) {
  return `${REALTIME_LAST_EVENT_STORAGE_PREFIX}${uid}`
}

function buildClientIdStorageKey(uid: number) {
  return `${REALTIME_CLIENT_ID_STORAGE_PREFIX}${uid}`
}

function normalizeRoomIdMap(raw: unknown): Record<number, number> {
  if (!raw || typeof raw !== 'object') return {}
  const result: Record<number, number> = {}
  for (const [key, value] of Object.entries(raw as Record<string, unknown>)) {
    const roomId = Number(key)
    const lastReadMsgId = typeof value === 'number' ? value : Number(value)
    if (Number.isFinite(roomId) && roomId > 0 && Number.isFinite(lastReadMsgId) && lastReadMsgId > 0) {
      result[roomId] = lastReadMsgId
    }
  }
  return result
}

function readPersistedReadMarks(uid: number): Record<number, number> {
  if (typeof window === 'undefined') return {}
  try {
    const raw = window.sessionStorage.getItem(buildReadMarksStorageKey(uid))
    if (!raw) return {}
    return normalizeRoomIdMap(JSON.parse(raw) as unknown)
  } catch {
    return {}
  }
}

function writePersistedReadMarks(uid: number, marks: Record<number, number>) {
  if (typeof window === 'undefined') return
  try {
    const next = normalizeRoomIdMap(marks)
    if (Object.keys(next).length <= 0) {
      window.sessionStorage.removeItem(buildReadMarksStorageKey(uid))
      return
    }
    window.sessionStorage.setItem(buildReadMarksStorageKey(uid), JSON.stringify(next))
  } catch {
    void 0
  }
}

function readPersistedPositiveNumber(key: string): number {
  if (typeof window === 'undefined') return 0
  const raw = window.localStorage.getItem(key)
  if (!raw) return 0
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0
}

function writePersistedPositiveNumber(key: string, value: number) {
  if (typeof window === 'undefined') return
  if (!(value > 0)) {
    window.localStorage.removeItem(key)
    return
  }
  window.localStorage.setItem(key, String(value))
}

function readPersistedText(key: string): string {
  if (typeof window === 'undefined') return ''
  return String(window.localStorage.getItem(key) || '').trim()
}

function writePersistedText(key: string, value: string) {
  if (typeof window === 'undefined') return
  const normalized = value.trim()
  if (!normalized) {
    window.localStorage.removeItem(key)
    return
  }
  window.localStorage.setItem(key, normalized)
}

function buildRealtimeClientId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `web-${crypto.randomUUID()}`
  }
  return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function normalizeApplicationRealtimeEvent(payload: unknown, eventType: string, occurredAt?: unknown): ApplicationRealtimeEvent | null {
  if (!payload || typeof payload !== 'object') return null
  const any = payload as Record<string, unknown>
  const applicationId = typeof any.applicationId === 'number' ? any.applicationId : Number(any.applicationId)
  if (!(applicationId > 0)) return null
  const status = typeof any.status === 'string' ? any.status : undefined
  return { eventType, applicationId, status, occurredAt, payload }
}

function textPreview(raw: unknown): string {
  if (!raw) return '您有一条新消息'
  if (typeof raw === 'string') {
    const trimmed = raw.trim()
    return trimmed ? `收到新消息：${trimmed}` : '您有一条新消息'
  }
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    const type = typeof any.type === 'string' ? any.type.trim() : ''
    if (typeof any.content === 'string' && any.content.trim()) {
      return `收到新消息：${any.content.trim()}`
    }
    if (type === 'recall') return '收到新消息：[消息已撤回]'
    if (type === 'image') return '收到新消息：[图片]'
    if (type === 'tutor_application') return '收到新消息：家教申请'
    if (type === 'tutor_application_status') return '收到新消息：家教申请状态更新'
    if (type === 'collaboration_proposal') return '收到新消息：合作提案'
    if (type === 'collaboration_status') return '收到新消息：合作提案状态更新'
    if (type === 'lesson_request') return '收到新消息：授课申请'
    if (type === 'lesson_status') return '收到新消息：课程状态更新'
    if (type === 'brokerage_required') return '收到新消息：信息费支付提醒'
    if (type === 'contact_unlocked') return '收到新消息：聊天功能已开启'
    if (type === 'brokerage_refund_request' || type === 'brokerage_refund_status') return '收到新消息：退款进度更新'
    if (type === 'end_chat_request' || type === 'end_chat_status') return '收到新消息：结束沟通状态更新'
  }
  return '您有一条新消息'
}

export const useChatRealtimeStore = defineStore('chatRealtime', {
  state: () => ({
    connected: false,
    totalUnread: 0,
    roomUnread: {} as Record<number, number>,
    unreadSnapshotLoaded: false,
    optimisticReadMsgIdByRoom: {} as Record<number, number>,
    peerDeliveredMsgIdByRoom: {} as Record<number, number>,
    peerReadMsgIdByRoom: {} as Record<number, number>,
    peerTypingByRoom: {} as Record<number, boolean>,
    peerPresenceByUid: {} as Record<number, ChatPresenceRealtimeEvent>,
    latestMsgIdByRoom: {} as Record<number, number>,
    persistedReadMarksOwnerUid: null as number | null,
    activeRoomId: null as number | null,
    lastEvent: null as StreamMsgEvent | null,
    messageEventSerial: 0,
    messageEventLog: [] as QueuedMessageEvent[],
    lastApplicationEvent: null as ApplicationRealtimeEvent | null,
    lastRealtimeEnvelope: null as RealtimeEnvelope | null,
    lastRealtimeEventId: 0,
    clientId: '',
    streamAbort: null as AbortController | null,
    lastStreamActivityAt: 0,
    realtimeWatchdogTimer: null as ReturnType<typeof globalThis.setInterval> | null,
  }),
  actions: {
    resetState() {
      this.connected = false
      this.totalUnread = 0
      this.roomUnread = {}
      this.unreadSnapshotLoaded = false
      this.optimisticReadMsgIdByRoom = {}
      this.peerDeliveredMsgIdByRoom = {}
      this.peerReadMsgIdByRoom = {}
      this.peerTypingByRoom = {}
      this.peerPresenceByUid = {}
      this.latestMsgIdByRoom = {}
      this.persistedReadMarksOwnerUid = null
      this.activeRoomId = null
      this.lastEvent = null
      this.messageEventSerial = 0
      this.messageEventLog = []
      this.lastApplicationEvent = null
      this.lastRealtimeEnvelope = null
      this.lastRealtimeEventId = 0
      this.clientId = ''
      this.lastStreamActivityAt = 0
      // 这些队列是模块级别的内存态，若不清空，切账号/切测试实例后可能把旧会话的 ACK 状态带到新会话里。
      ackPendingByRoom.clear()
      ackInFlightByRoom.clear()
      deliveryPendingByRoom.clear()
      deliveryInFlightByRoom.clear()
      clearAllPeerTypingTimers()
      this.stopRealtimeWatchdog()
    },

    setActiveRoom(roomId: number | null) {
      this.activeRoomId = roomId
    },

    ensureReadMarksLoaded() {
      const auth = useAuthStore()
      const uid = auth.user?.id
      if (typeof uid !== 'number' || uid <= 0) {
        this.persistedReadMarksOwnerUid = null
        this.optimisticReadMsgIdByRoom = {}
        return
      }
      if (this.persistedReadMarksOwnerUid === uid) return
      this.optimisticReadMsgIdByRoom = readPersistedReadMarks(uid)
      this.persistedReadMarksOwnerUid = uid
    },

    persistReadMarks() {
      const auth = useAuthStore()
      const uid = auth.user?.id
      if (typeof uid !== 'number' || uid <= 0) return
      writePersistedReadMarks(uid, this.optimisticReadMsgIdByRoom)
      this.persistedReadMarksOwnerUid = uid
    },

    ensureRealtimeSessionLoaded() {
      const auth = useAuthStore()
      const uid = auth.user?.id
      if (typeof uid !== 'number' || uid <= 0) {
        this.lastRealtimeEventId = 0
        this.clientId = ''
        return
      }
      if (!(this.lastRealtimeEventId > 0)) {
        this.lastRealtimeEventId = readPersistedPositiveNumber(buildLastEventStorageKey(uid))
      }
      if (!this.clientId) {
        const persisted = readPersistedText(buildClientIdStorageKey(uid))
        this.clientId = persisted || buildRealtimeClientId()
        writePersistedText(buildClientIdStorageKey(uid), this.clientId)
      }
    },

    persistRealtimeWatermark() {
      const auth = useAuthStore()
      const uid = auth.user?.id
      if (typeof uid !== 'number' || uid <= 0) return
      writePersistedPositiveNumber(buildLastEventStorageKey(uid), this.lastRealtimeEventId)
      if (this.clientId) {
        writePersistedText(buildClientIdStorageKey(uid), this.clientId)
      }
    },

    recordMessageEvent(ev: StreamMsgEvent) {
      const nextSerial = this.messageEventSerial + 1
      const nextLog = [...this.messageEventLog, { serial: nextSerial, event: ev }]
      this.messageEventSerial = nextSerial
      // 保留一个短窗口事件队列，供页面在批量补偿时按序补消费，避免只看到最后一条。
      this.messageEventLog = nextLog.length > MESSAGE_EVENT_LOG_LIMIT ? nextLog.slice(-MESSAGE_EVENT_LOG_LIMIT) : nextLog
    },

    listMessageEventsAfter(serial: number) {
      const watermark = Number.isFinite(serial) ? serial : 0
      return this.messageEventLog.filter((item) => item.serial > watermark)
    },

    hasQueuedMessageEvent(roomId: number, msgId: number) {
      if (!(roomId > 0) || !(msgId > 0)) return false
      return this.messageEventLog.some((item) => item.event.roomId === roomId && item.event.msgId === msgId)
    },

    noteRealtimeActivity() {
      this.lastStreamActivityAt = Date.now()
    },

    stopRealtimeWatchdog() {
      if (this.realtimeWatchdogTimer == null) return
      globalThis.clearInterval(this.realtimeWatchdogTimer)
      this.realtimeWatchdogTimer = null
    },

    startRealtimeWatchdog(controller: AbortController, idleTimeoutMs = REALTIME_IDLE_TIMEOUT_MS, intervalMs = REALTIME_WATCHDOG_INTERVAL_MS) {
      this.stopRealtimeWatchdog()
      this.noteRealtimeActivity()
      if (typeof globalThis.setInterval !== 'function') return

      // 某些代理或网络异常会让连接“看起来还活着”，但实际上已经不再下发任何数据。
      // 这里用心跳/消息的最近活动时间做兜底，超时后主动打断当前流，交给现有重连逻辑恢复。
      this.realtimeWatchdogTimer = globalThis.setInterval(() => {
        if (controller.signal.aborted) return
        if (Date.now() - this.lastStreamActivityAt < idleTimeoutMs) return
        controller.abort('realtime_idle_timeout')
      }, intervalMs)
    },

    async syncMissedRealtimeEvents(serverLatestEventId?: number | null) {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) return
      const normalizedServerLatest =
        typeof serverLatestEventId === 'number' && Number.isFinite(serverLatestEventId) ? serverLatestEventId : 0
      // 只对“已有本地水位”的场景做补偿，避免新设备首次登录时把全部历史事件都拉下来。
      if (!(this.lastRealtimeEventId > 0) || !(normalizedServerLatest > this.lastRealtimeEventId)) {
        return
      }
      if (realtimeSyncInFlight) {
        await realtimeSyncInFlight
        return
      }

      realtimeSyncInFlight = (async () => {
        let cursor = this.lastRealtimeEventId
        for (let i = 0; i < 10; i += 1) {
          const page = await chatApi.syncRealtimeEvents({
            lastEventId: cursor > 0 ? cursor : undefined,
            pageSize: 100,
          })
          const list = Array.isArray(page?.list) ? page.list : []
          for (const envelope of list) {
            this.consumeRealtimeEnvelope(envelope)
          }
          const nextCursor =
            typeof page?.cursor === 'number' && Number.isFinite(page.cursor) ? Number(page.cursor) : this.lastRealtimeEventId
          if (page?.isLast !== false) break
          if (!(nextCursor > cursor)) break
          cursor = nextCursor
        }
      })()

      try {
        await realtimeSyncInFlight
      } catch {
        // 补偿拉取失败时保留现有轮询/在线推送兜底，不阻断主链路。
        void 0
      } finally {
        realtimeSyncInFlight = null
      }
    },

    async handleStreamReady(payload: unknown) {
      const ready = payload as StreamReadyEvent | null
      if (!ready || typeof ready !== 'object') return

      const serverClientId = typeof ready.clientId === 'string' ? ready.clientId.trim() : ''
      if (serverClientId && serverClientId !== this.clientId) {
        this.clientId = serverClientId
        this.persistRealtimeWatermark()
      }

      const serverLatestEventId =
        typeof ready.lastEventId === 'number' ? ready.lastEventId : Number(ready.lastEventId || 0)
      if (Number.isFinite(serverLatestEventId) && serverLatestEventId > 0) {
        await this.syncMissedRealtimeEvents(serverLatestEventId)
      }
    },

    clearRoomUnread(roomId: number) {
      const prev = this.roomUnread[roomId] || 0
      if (prev > 0) {
        this.totalUnread = Math.max(0, this.totalUnread - prev)
      }
      this.roomUnread = { ...this.roomUnread, [roomId]: 0 }
    },

    markRoomReadOptimistic(roomId: number, lastReadMsgId: number) {
      this.ensureReadMarksLoaded()
      const prev = this.optimisticReadMsgIdByRoom[roomId] || 0
      if (lastReadMsgId > prev) {
        this.optimisticReadMsgIdByRoom = {
          ...this.optimisticReadMsgIdByRoom,
          [roomId]: lastReadMsgId,
        }
        this.persistReadMarks()
      }
      this.clearRoomUnread(roomId)
    },

    rememberLatestMsg(roomId: number, msgId: number) {
      const prev = this.latestMsgIdByRoom[roomId] || 0
      if (msgId > prev) {
        this.latestMsgIdByRoom = {
          ...this.latestMsgIdByRoom,
          [roomId]: msgId,
        }
      }
    },

    syncServerReadMark(roomId: number, serverLastReadMsgId: number | null | undefined) {
      const confirmed = typeof serverLastReadMsgId === 'number' ? serverLastReadMsgId : 0
      if (confirmed <= 0) return
      const prev = this.optimisticReadMsgIdByRoom[roomId] || 0
      if (confirmed > prev) {
        this.optimisticReadMsgIdByRoom = {
          ...this.optimisticReadMsgIdByRoom,
          [roomId]: confirmed,
        }
        this.persistReadMarks()
      }
    },

    syncPeerReadMark(roomId: number, peerLastReadMsgId: number | null | undefined) {
      const confirmed = typeof peerLastReadMsgId === 'number' ? peerLastReadMsgId : 0
      if (confirmed <= 0) return
      this.syncPeerDeliveredMark(roomId, confirmed)
      const prev = this.peerReadMsgIdByRoom[roomId] || 0
      if (confirmed > prev) {
        this.peerReadMsgIdByRoom = {
          ...this.peerReadMsgIdByRoom,
          [roomId]: confirmed,
        }
      }
    },

    syncPeerDeliveredMark(roomId: number, peerLastDeliveredMsgId: number | null | undefined) {
      const confirmed = typeof peerLastDeliveredMsgId === 'number' ? peerLastDeliveredMsgId : 0
      if (confirmed <= 0) return
      const prev = this.peerDeliveredMsgIdByRoom[roomId] || 0
      if (confirmed > prev) {
        this.peerDeliveredMsgIdByRoom = {
          ...this.peerDeliveredMsgIdByRoom,
          [roomId]: confirmed,
        }
      }
    },

    setPeerTyping(roomId: number, typing: boolean) {
      if (!(roomId > 0)) return
      clearPeerTypingTimer(roomId)
      if (!typing) {
        if (!this.peerTypingByRoom[roomId]) return
        const next = { ...this.peerTypingByRoom }
        delete next[roomId]
        this.peerTypingByRoom = next
        return
      }

      this.peerTypingByRoom = {
        ...this.peerTypingByRoom,
        [roomId]: true,
      }

      // “正在输入”只属于瞬时在线态，不应该参与历史补偿或持久化，因此到时后自动失效。
      peerTypingTimerByRoom.set(
        roomId,
        globalThis.setTimeout(() => {
          const next = { ...this.peerTypingByRoom }
          delete next[roomId]
          this.peerTypingByRoom = next
          peerTypingTimerByRoom.delete(roomId)
        }, PEER_TYPING_EXPIRE_MS),
      )
    },

    setPeerPresenceSnapshot(payload: Partial<ChatPresenceRealtimeEvent> | null | undefined) {
      if (!payload || typeof payload !== 'object') return
      const uid = typeof payload.uid === 'number' ? payload.uid : Number(payload.uid)
      if (!(uid > 0)) return
      const online =
        typeof payload.online === 'boolean'
          ? payload.online
          : typeof payload.online === 'string'
            ? String(payload.online).trim().toLowerCase() === 'true'
            : Boolean(payload.online)
      this.peerPresenceByUid = {
        ...this.peerPresenceByUid,
        [uid]: {
          uid,
          online,
          lastOnlineAt: payload.lastOnlineAt ?? null,
        },
      }
    },

    notifyIncomingMessage(ev: StreamMsgEvent) {
      const preview = textPreview(ev.body)
      if (typeof window !== 'undefined' && typeof document !== 'undefined' && document.hidden && 'Notification' in window) {
        if (Notification.permission === 'granted') {
          const notice = new Notification(`${BRAND_NAME}新消息`, {
            body: preview,
            tag: `chat-room-${ev.roomId}`,
          })
          window.setTimeout(() => notice.close(), 4000)
          return
        }
      }
      const toast = useToastStore()
      toast.show(preview, 'info', 3200)
    },

    consumeApplicationEvent(payload: unknown, eventType: string, occurredAt?: unknown) {
      const normalized = normalizeApplicationRealtimeEvent(payload, eventType, occurredAt)
      if (!normalized) return
      this.lastApplicationEvent = normalized
    },

    consumeReadEvent(payload: unknown) {
      if (!payload || typeof payload !== 'object') return
      const any = payload as Partial<ChatReadRealtimeEvent> & Record<string, unknown>
      const roomId = typeof any.roomId === 'number' ? any.roomId : Number(any.roomId)
      const lastReadMsgId = typeof any.lastReadMsgId === 'number' ? any.lastReadMsgId : Number(any.lastReadMsgId)
      if (!(roomId > 0) || !(lastReadMsgId > 0)) return
      this.syncPeerReadMark(roomId, lastReadMsgId)
    },

    consumeDeliveryEvent(payload: unknown) {
      if (!payload || typeof payload !== 'object') return
      const any = payload as Partial<ChatDeliveryRealtimeEvent> & Record<string, unknown>
      const roomId = typeof any.roomId === 'number' ? any.roomId : Number(any.roomId)
      const lastDeliveredMsgId =
        typeof any.lastDeliveredMsgId === 'number' ? any.lastDeliveredMsgId : Number(any.lastDeliveredMsgId)
      if (!(roomId > 0) || !(lastDeliveredMsgId > 0)) return
      this.syncPeerDeliveredMark(roomId, lastDeliveredMsgId)
    },

    consumeTypingEvent(payload: unknown) {
      if (!payload || typeof payload !== 'object') return
      const any = payload as Partial<ChatTypingRealtimeEvent> & Record<string, unknown>
      const roomId = typeof any.roomId === 'number' ? any.roomId : Number(any.roomId)
      if (!(roomId > 0)) return
      const typing =
        typeof any.typing === 'boolean'
          ? any.typing
          : typeof any.typing === 'string'
            ? String(any.typing).trim().toLowerCase() === 'true'
            : Boolean(any.typing)
      this.setPeerTyping(roomId, typing)
    },

    consumePresenceEvent(payload: unknown) {
      if (!payload || typeof payload !== 'object') return
      this.setPeerPresenceSnapshot(payload as Partial<ChatPresenceRealtimeEvent>)
    },

    consumeRealtimeEnvelope(envelope: RealtimeEnvelope) {
      const eventId = typeof envelope.eventId === 'number' ? envelope.eventId : Number(envelope.eventId)
      if (Number.isFinite(eventId) && eventId > 0) {
        if (eventId <= this.lastRealtimeEventId) return
        this.lastRealtimeEventId = eventId
        this.persistRealtimeWatermark()
      }

      this.lastRealtimeEnvelope = envelope
      const eventType = typeof envelope.eventType === 'string' ? envelope.eventType.trim() : ''
      if (!eventType) return

      if (eventType === 'chat.message.created') {
        const payload = envelope.payload as StreamMsgEvent | undefined
        if (!payload || typeof payload.roomId !== 'number') return
        this.onMessageEvent(payload)
        return
      }

      if (eventType === 'chat.read.updated') {
        this.consumeReadEvent(envelope.payload)
        return
      }

      if (eventType === 'chat.delivery.updated') {
        this.consumeDeliveryEvent(envelope.payload)
        return
      }

      if (eventType === 'chat.typing.updated') {
        this.consumeTypingEvent(envelope.payload)
        return
      }

      if (eventType === 'chat.presence.updated') {
        this.consumePresenceEvent(envelope.payload)
        return
      }

      if (eventType.startsWith('application.')) {
        this.consumeApplicationEvent(envelope.payload, eventType, envelope.occurredAt)
      }
    },

    async ackRoomRead(roomId: number, lastReadMsgId: number) {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token) return
      this.ensureReadMarksLoaded()
      this.markRoomReadOptimistic(roomId, lastReadMsgId)

      const prevPending = ackPendingByRoom.get(roomId) || 0
      const nextPending = Math.max(prevPending, lastReadMsgId)
      if (nextPending <= 0) return
      ackPendingByRoom.set(roomId, nextPending)

      const inflight = ackInFlightByRoom.get(roomId)
      if (inflight) return

      const runner = (async () => {
        let lastAcked = 0
        let retryCount = 0
        while (true) {
          if (!auth.isLoggedIn || !auth.token) return
          const target = ackPendingByRoom.get(roomId) || 0
          if (target <= lastAcked) return
          try {
            const confirmed = await chatApi.ackRead(roomId, target)
            const confirmedLastReadMsgId =
              typeof confirmed?.lastReadMsgId === 'number' && confirmed.lastReadMsgId > 0 ? confirmed.lastReadMsgId : target
            lastAcked = confirmedLastReadMsgId
            retryCount = 0
            this.markRoomReadOptimistic(roomId, confirmedLastReadMsgId)
            void this.refreshUnreadFromServer()
          } catch {
            retryCount += 1
            if (retryCount >= 3) return
            await sleep(300 * retryCount)
          }
        }
      })()

      ackInFlightByRoom.set(
        roomId,
        runner.finally(() => {
          ackInFlightByRoom.delete(roomId)
        }),
      )
    },

    ackRoomReadKeepalive(roomId: number, lastReadMsgId: number) {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token || !(roomId > 0) || !(lastReadMsgId > 0)) return
      this.ensureReadMarksLoaded()
      this.markRoomReadOptimistic(roomId, lastReadMsgId)
      try {
        void fetch(apiUrl('/chat/read/ack'), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${auth.token}`,
          },
          body: JSON.stringify({ roomId, lastReadMsgId }),
          keepalive: true,
        })
      } catch {
        void 0
      }
    },

    async ackRoomDelivered(roomId: number, lastDeliveredMsgId: number) {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token || !(lastDeliveredMsgId > 0)) return

      const prevPending = deliveryPendingByRoom.get(roomId) || 0
      const nextPending = Math.max(prevPending, lastDeliveredMsgId)
      if (nextPending <= 0) return
      deliveryPendingByRoom.set(roomId, nextPending)

      const inflight = deliveryInFlightByRoom.get(roomId)
      if (inflight) return

      const runner = (async () => {
        let lastAcked = 0
        let retryCount = 0
        while (true) {
          if (!auth.isLoggedIn || !auth.token) return
          const target = deliveryPendingByRoom.get(roomId) || 0
          if (target <= lastAcked) return
          try {
            await chatApi.ackDelivered(roomId, target)
            lastAcked = target
            retryCount = 0
          } catch {
            retryCount += 1
            if (retryCount >= 3) return
            await sleep(300 * retryCount)
          }
        }
      })()

      deliveryInFlightByRoom.set(
        roomId,
        runner.finally(() => {
          deliveryInFlightByRoom.delete(roomId)
        }),
      )
    },

    async refreshUnreadFromServer() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.resetState()
        return
      }
      this.ensureReadMarksLoaded()
      try {
        let cursor: number | null = null
        let total = 0
        const map: Record<number, number> = {}
        for (let i = 0; i < 50; i++) {
          const page = await chatApi.listRooms({ pageSize: 50, cursor })
          const list = page.list || []
          for (const r of list) {
            const latestMsgId = typeof r.lastMsgId === 'number' ? r.lastMsgId : 0
            const serverReadMsgId = typeof r.myLastReadMsgId === 'number' ? r.myLastReadMsgId : 0
            this.rememberLatestMsg(r.roomId, latestMsgId)
            if (Object.prototype.hasOwnProperty.call(r, 'myLastReadMsgId')) {
              this.syncServerReadMark(r.roomId, serverReadMsgId)
            }
            const peerLastReadMsgId =
              Object.prototype.hasOwnProperty.call(r, 'peerLastReadMsgId') && typeof r.peerLastReadMsgId === 'number'
                ? r.peerLastReadMsgId
                : Object.prototype.hasOwnProperty.call(r, 'peerLastReadMsgId')
                ? Number(r.peerLastReadMsgId)
                  : 0
            this.syncPeerReadMark(r.roomId, peerLastReadMsgId)
            const optimisticReadMsgId = this.optimisticReadMsgIdByRoom[r.roomId] || 0
            // 首页红点判断必须同时考虑“服务端已确认水位”和“本地刚读完但服务端尚未回刷的水位”。
            // 否则用户在聊天页读完消息后，一回到首页就可能被旧的 unreadCount 再次打出红点。
            const effectiveReadMsgId = Math.max(serverReadMsgId, optimisticReadMsgId)
            let c = typeof r.unreadCount === 'number' ? r.unreadCount : 0
            if (latestMsgId > 0 && effectiveReadMsgId >= latestMsgId) {
              c = 0
            }
            if (c > 0) {
              map[r.roomId] = c
              total += c
            }
          }
          cursor = page.cursor ?? null
          if (page.isLast || list.length === 0) break
        }
        const active = this.activeRoomId
        if (active != null && map[active]) {
          total = Math.max(0, total - map[active])
          map[active] = 0
        }
        this.totalUnread = total
        this.roomUnread = map
        this.unreadSnapshotLoaded = true
      } catch {
        void 0
      }
    },

    async start() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token) return
      if (this.streamAbort) return
      this.ensureRealtimeSessionLoaded()
      const controller = new AbortController()
      this.streamAbort = controller
      this.connected = false

      let attempt = 0
      while (!controller.signal.aborted) {
        try {
          let streamMode: 'v2' | 'legacy' = 'v2'
          const search = new URLSearchParams()
          if (this.clientId) search.set('clientId', this.clientId)
          if (this.lastRealtimeEventId > 0) search.set('lastEventId', String(this.lastRealtimeEventId))

          // 先尝试 v2 流；如果服务端还没升级，则无缝降级到旧协议。
          let res = await fetch(apiUrl(`/chat/stream/v2${search.size > 0 ? `?${search.toString()}` : ''}`), {
            method: 'GET',
            headers: { Authorization: `Bearer ${auth.token}` },
            signal: controller.signal,
          })
          if (res.status === 404 || res.status === 405) {
            streamMode = 'legacy'
            res = await fetch(apiUrl('/chat/stream'), {
              method: 'GET',
              headers: { Authorization: `Bearer ${auth.token}` },
              signal: controller.signal,
            })
          }
          if (res.status === 401 || res.status === 403) {
            notifyAuthInvalid(`sse_http_${res.status}`)
            controller.abort()
            break
          }
          if (!res.ok || !res.body) throw new Error('stream_failed')
          this.connected = true
          this.startRealtimeWatchdog(controller)
          void this.refreshUnreadFromServer()

          const reader = res.body.getReader()
          const decoder = new TextDecoder('utf-8')
          let buffer = ''

          while (!controller.signal.aborted) {
            const { value, done } = await reader.read()
            if (done) break
            if (value && value.length > 0) {
              this.noteRealtimeActivity()
            }
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
              try {
                const payload = JSON.parse(dataRaw) as unknown
                if (streamMode === 'v2') {
                  if (event === 'heartbeat') {
                    this.noteRealtimeActivity()
                    continue
                  }
                  if (event === 'ready') {
                    this.noteRealtimeActivity()
                    await this.handleStreamReady(payload)
                    continue
                  }
                  if (event !== 'event') continue
                  this.consumeRealtimeEnvelope(payload as RealtimeEnvelope)
                  continue
                }

                if (event === 'message') {
                  const ev = payload as StreamMsgEvent
                  if (!ev || typeof ev.roomId !== 'number') continue
                  this.onMessageEvent(ev)
                  continue
                }

                if (event === 'application') {
                  this.consumeApplicationEvent(payload, 'application.legacy', null)
                }
              } catch {
                void 0
              }
            }
          }
        } catch {
          void 0
        } finally {
          this.connected = false
          this.stopRealtimeWatchdog()
        }

        if (controller.signal.aborted) break
        attempt += 1
        const baseDelay = Math.min(30_000, 500 * 2 ** Math.min(6, attempt))
        const jitter = Math.floor(Math.random() * 250)
        await sleep(baseDelay + jitter)
      }

      this.streamAbort = null
    },

    stop() {
      this.streamAbort?.abort()
      this.streamAbort = null
      this.connected = false
      this.stopRealtimeWatchdog()
    },

    onMessageEvent(ev: StreamMsgEvent) {
      const auth = useAuthStore()
      const myUid = auth.user?.id
      if (!myUid) return
      this.ensureReadMarksLoaded()
      const knownLatest = this.latestMsgIdByRoom[ev.roomId] || 0
      const isNewerThanKnown = ev.msgId > knownLatest
      if (ev.msgId > 0 && this.hasQueuedMessageEvent(ev.roomId, ev.msgId)) return
      this.recordMessageEvent(ev)
      this.lastEvent = ev
      if (isNewerThanKnown) {
        this.rememberLatestMsg(ev.roomId, ev.msgId)
      }
      if (ev.toUid !== myUid) return
      // 收到在线消息后先回送“送达”，这样发送方能先看到“已送达”，再等待“已读”。
      if (ev.msgId > 0) {
        void this.ackRoomDelivered(ev.roomId, ev.msgId)
      }
      // If active in this room, we assume it's read immediately
      if (this.activeRoomId != null && ev.roomId === this.activeRoomId) {
        if (ev.msgId > 0) {
          this.markRoomReadOptimistic(ev.roomId, ev.msgId)
          void this.ackRoomRead(ev.roomId, ev.msgId)
        }
        return
      }
      // 如果房间最新消息水位已经被列表刷新拿到了，就不要再次累计未读；
      // 但消息事件本身仍要保留给会话列表/聊天页做即时渲染。
      if (!isNewerThanKnown) return
      const prev = this.roomUnread[ev.roomId] || 0
      this.roomUnread = { ...this.roomUnread, [ev.roomId]: prev + 1 }
      this.totalUnread += 1
      this.unreadSnapshotLoaded = true
      this.notifyIncomingMessage(ev)
    },
  },
})
