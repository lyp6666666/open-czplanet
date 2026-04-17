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
const READ_MARKS_STORAGE_PREFIX = 'ai_tutor_chat_read_marks:'

function buildReadMarksStorageKey(uid: number) {
  return `${READ_MARKS_STORAGE_PREFIX}${uid}`
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
    optimisticReadMsgIdByRoom: {} as Record<number, number>,
    latestMsgIdByRoom: {} as Record<number, number>,
    persistedReadMarksOwnerUid: null as number | null,
    activeRoomId: null as number | null,
    lastEvent: null as StreamMsgEvent | null,
    streamAbort: null as AbortController | null,
  }),
  actions: {
    resetState() {
      this.connected = false
      this.totalUnread = 0
      this.roomUnread = {}
      this.optimisticReadMsgIdByRoom = {}
      this.latestMsgIdByRoom = {}
      this.persistedReadMarksOwnerUid = null
      this.activeRoomId = null
      this.lastEvent = null
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
      if (!auth.isLoggedIn || !auth.token || !(lastReadMsgId > 0)) return
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
            const hasServerReadMsgId = Object.prototype.hasOwnProperty.call(r, 'myLastReadMsgId')
            const serverReadMsgId = typeof r.myLastReadMsgId === 'number' ? r.myLastReadMsgId : 0
            this.rememberLatestMsg(r.roomId, latestMsgId)
            if (hasServerReadMsgId) {
              this.syncServerReadMark(r.roomId, serverReadMsgId)
            }
            const optimisticReadMsgId = this.optimisticReadMsgIdByRoom[r.roomId] || 0
            let c = typeof r.unreadCount === 'number' ? r.unreadCount : 0
            if (latestMsgId > 0 && serverReadMsgId >= latestMsgId) {
              c = 0
            } else if (!hasServerReadMsgId && latestMsgId > 0 && optimisticReadMsgId >= latestMsgId) {
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
      } catch {
        void 0
      }
    },

    async start() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token) return
      if (this.streamAbort) return
      const controller = new AbortController()
      this.streamAbort = controller
      this.connected = false

      let attempt = 0
      while (!controller.signal.aborted) {
        try {
          const res = await fetch(apiUrl('/chat/stream'), {
            method: 'GET',
            headers: { Authorization: `Bearer ${auth.token}` },
            signal: controller.signal,
          })
          if (res.status === 401 || res.status === 403) {
            notifyAuthInvalid(`sse_http_${res.status}`)
            controller.abort()
            break
          }
          if (!res.ok || !res.body) throw new Error('stream_failed')
          this.connected = true
          void this.refreshUnreadFromServer()

          const reader = res.body.getReader()
          const decoder = new TextDecoder('utf-8')
          let buffer = ''

          while (!controller.signal.aborted) {
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
                if (!ev || typeof ev.roomId !== 'number') continue
                this.lastEvent = ev
                this.onMessageEvent(ev)
              } catch {
                void 0
              }
            }
          }
        } catch {
          void 0
        } finally {
          this.connected = false
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
    },

    onMessageEvent(ev: StreamMsgEvent) {
      const auth = useAuthStore()
      const myUid = auth.user?.id
      if (!myUid) return
      this.ensureReadMarksLoaded()
      const knownLatest = this.latestMsgIdByRoom[ev.roomId] || 0
      if (ev.msgId <= knownLatest) return
      this.rememberLatestMsg(ev.roomId, ev.msgId)
      if (ev.toUid !== myUid) return
      // If active in this room, we assume it's read immediately
      if (this.activeRoomId != null && ev.roomId === this.activeRoomId) {
        this.markRoomReadOptimistic(ev.roomId, ev.msgId)
        void this.ackRoomRead(ev.roomId, ev.msgId)
        return
      }
      const prev = this.roomUnread[ev.roomId] || 0
      this.roomUnread = { ...this.roomUnread, [ev.roomId]: prev + 1 }
      this.totalUnread += 1
      this.notifyIncomingMessage(ev)
    },
  },
})
