import { defineStore } from 'pinia'

import { chatApi } from '@/api/chat'
import { notifyAuthInvalid } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

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

const ackPendingByRoom = new Map<number, number>()
const ackInFlightByRoom = new Map<number, Promise<void>>()

export const useChatRealtimeStore = defineStore('chatRealtime', {
  state: () => ({
    connected: false,
    totalUnread: 0,
    roomUnread: {} as Record<number, number>,
    activeRoomId: null as number | null,
    lastEvent: null as StreamMsgEvent | null,
    streamAbort: null as AbortController | null,
  }),
  actions: {
    setActiveRoom(roomId: number | null) {
      this.activeRoomId = roomId
    },

    clearRoomUnread(roomId: number) {
      const prev = this.roomUnread[roomId] || 0
      if (prev > 0) {
        this.totalUnread = Math.max(0, this.totalUnread - prev)
      }
      this.roomUnread = { ...this.roomUnread, [roomId]: 0 }
    },

    async ackRoomRead(roomId: number, lastReadMsgId: number) {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token) return

      const prevPending = ackPendingByRoom.get(roomId) || 0
      const nextPending = Math.max(prevPending, lastReadMsgId)
      if (nextPending <= 0) return
      ackPendingByRoom.set(roomId, nextPending)

      const inflight = ackInFlightByRoom.get(roomId)
      if (inflight) return

      const runner = (async () => {
        let lastAcked = 0
        while (true) {
          if (!auth.isLoggedIn || !auth.token) return
          const target = ackPendingByRoom.get(roomId) || 0
          if (target <= lastAcked) return
          try {
            await chatApi.ackRead(roomId, target)
            lastAcked = target
            this.clearRoomUnread(roomId)
          } catch {
            return
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

    async refreshUnreadFromServer() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.totalUnread = 0
        this.roomUnread = {}
        return
      }
      try {
        let cursor: number | null = null
        let total = 0
        const map: Record<number, number> = {}
        for (let i = 0; i < 50; i++) {
          const page = await chatApi.listRooms({ pageSize: 50, cursor })
          const list = page.list || []
          for (const r of list) {
            const c = typeof r.unreadCount === 'number' ? r.unreadCount : 0
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
          const baseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
          const url = `${baseUrl}/chat/stream`
          const res = await fetch(url, {
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
      if (ev.toUid !== myUid) return
      // If active in this room, we assume it's read immediately
      if (this.activeRoomId != null && ev.roomId === this.activeRoomId) {
        void this.ackRoomRead(ev.roomId, ev.msgId)
        return
      }
      const prev = this.roomUnread[ev.roomId] || 0
      this.roomUnread = { ...this.roomUnread, [ev.roomId]: prev + 1 }
      this.totalUnread += 1
    },
  },
})
