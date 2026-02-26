import { defineStore } from 'pinia'

import { chatApi } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'

type StreamMsgEvent = {
  msgId: number
  roomId: number
  fromUid: number
  toUid: number
  sendTime: unknown
  body: unknown
}

function normalizeBaseUrl(raw: unknown) {
  const s = typeof raw === 'string' ? raw.trim() : ''
  return s.length > 0 ? s : ''
}

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
      if (roomId != null) {
        this.clearRoomUnread(roomId)
      }
    },

    clearRoomUnread(roomId: number) {
      const prev = this.roomUnread[roomId] || 0
      if (prev > 0) {
        this.totalUnread = Math.max(0, this.totalUnread - prev)
      }
      this.roomUnread = { ...this.roomUnread, [roomId]: 0 }
    },

    async refreshUnreadFromServer() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.totalUnread = 0
        this.roomUnread = {}
        return
      }
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
    },

    async start() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn || !auth.token) return
      if (this.streamAbort) return
      const controller = new AbortController()
      this.streamAbort = controller
      this.connected = false

      const baseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
      const url = `${baseUrl}/chat/stream`
      const res = await fetch(url, {
        method: 'GET',
        headers: { Authorization: `Bearer ${auth.token}` },
        signal: controller.signal,
      })
      if (!res.ok || !res.body) {
        this.streamAbort = null
        this.connected = false
        return
      }
      this.connected = true

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
            if (!ev || typeof ev.roomId !== 'number') continue
            this.lastEvent = ev
            this.onMessageEvent(ev)
          } catch {
            void 0
          }
        }
      }

      this.streamAbort = null
      this.connected = false
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
      if (this.activeRoomId != null && ev.roomId === this.activeRoomId) return
      const prev = this.roomUnread[ev.roomId] || 0
      this.roomUnread = { ...this.roomUnread, [ev.roomId]: prev + 1 }
      this.totalUnread += 1
    },
  },
})
