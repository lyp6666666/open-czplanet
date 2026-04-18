import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'

import { chatApi } from '@/api/chat'

import { useAuthStore } from './auth'
import { useChatRealtimeStore } from './chatRealtime'

vi.mock('@/api/chat', () => ({
  chatApi: {
    listRooms: vi.fn(),
    ackRead: vi.fn(),
    ackDelivered: vi.fn(),
    reportTyping: vi.fn(),
    syncRealtimeEvents: vi.fn(),
  },
}))

function createMemoryStorage(): Storage {
  const data = new Map<string, string>()
  return {
    get length() {
      return data.size
    },
    clear() {
      data.clear()
    },
    getItem(key: string) {
      return data.has(key) ? data.get(key) ?? null : null
    },
    key(index: number) {
      return Array.from(data.keys())[index] ?? null
    },
    removeItem(key: string) {
      data.delete(key)
    },
    setItem(key: string, value: string) {
      data.set(key, String(value))
    },
  }
}

function seedAuth(uid = 2001) {
  window.localStorage.setItem('ai_tutor_token', 'mock.student.token')
  window.localStorage.setItem(
    'ai_tutor_user',
    JSON.stringify({
      id: uid,
      name: `学生${uid}`,
      phone: '13800138001',
      avatar: '',
      sex: null,
      userType: 2,
      token: 'mock.student.token',
    }),
  )
}

function clearAuthAndReadMarkStorage(uid = 2001) {
  window.localStorage.removeItem('ai_tutor_token')
  window.localStorage.removeItem('ai_tutor_user')
  window.sessionStorage.removeItem(`ai_tutor_chat_read_marks:${uid}`)
}

describe('chatRealtime store', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'localStorage', { value: createMemoryStorage(), configurable: true })
    Object.defineProperty(window, 'sessionStorage', { value: createMemoryStorage(), configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: window.localStorage, configurable: true })
    Object.defineProperty(globalThis, 'sessionStorage', { value: window.sessionStorage, configurable: true })
    clearAuthAndReadMarkStorage()
    vi.clearAllMocks()
    vi.useRealTimers()
    setActivePinia(createPinia())
  })

  it('keeps a room read after refresh when the server confirms the same last message was already read', async () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.markRoomReadOptimistic(666004, 120)

    expect(window.sessionStorage.getItem('ai_tutor_chat_read_marks:2001')).toBe(JSON.stringify({ 666004: 120 }))

    setActivePinia(createPinia())
    useAuthStore()
    const reloaded = useChatRealtimeStore()

    vi.mocked(chatApi.listRooms).mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          roomId: 666004,
          otherUid: 1001,
          lastMsgId: 120,
          lastMsgBody: null,
          myLastReadMsgId: 120,
          unreadCount: 1,
          activeTime: '2026-04-16T00:00:00',
        },
      ],
    })

    await reloaded.refreshUnreadFromServer()

    expect(reloaded.totalUnread).toBe(0)
    expect(reloaded.roomUnread[666004]).toBeUndefined()
  })

  it('keeps a room read after refresh when the local read watermark already covers the latest message', async () => {
    seedAuth()
    window.sessionStorage.setItem('ai_tutor_chat_read_marks:2001', JSON.stringify({ 666004: 120 }))
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()

    vi.mocked(chatApi.listRooms).mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          roomId: 666004,
          otherUid: 1001,
          lastMsgId: 120,
          lastMsgBody: null,
          myLastReadMsgId: null,
          unreadCount: 1,
          activeTime: '2026-04-16T00:00:00',
        },
      ],
    })

    await chatRealtime.refreshUnreadFromServer()

    expect(chatRealtime.totalUnread).toBe(0)
    expect(chatRealtime.roomUnread[666004]).toBeUndefined()
  })

  it('keeps a room read after refresh when the server watermark is stale but the local read watermark is newer', async () => {
    seedAuth()
    window.sessionStorage.setItem('ai_tutor_chat_read_marks:2001', JSON.stringify({ 666004: 120 }))
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()

    vi.mocked(chatApi.listRooms).mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          roomId: 666004,
          otherUid: 1001,
          lastMsgId: 120,
          lastMsgBody: null,
          myLastReadMsgId: 118,
          unreadCount: 2,
          activeTime: '2026-04-16T00:00:00',
        },
      ],
    })

    await chatRealtime.refreshUnreadFromServer()

    expect(chatRealtime.totalUnread).toBe(0)
    expect(chatRealtime.roomUnread[666004]).toBeUndefined()
  })

  it('still queues a realtime message for UI rendering even when the room latest watermark was already refreshed from the server', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.latestMsgIdByRoom = { 7101: 6002 }

    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1301,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 6002,
        roomId: 7101,
        fromUid: 3001,
        toUid: 2001,
        sendTime: '2026-04-18T16:00:00',
        body: { content: '无需刷新也要立刻显示' },
      },
    })

    expect(chatRealtime.messageEventSerial).toBe(1)
    expect(chatRealtime.listMessageEventsAfter(0)[0]?.event.msgId).toBe(6002)
    expect(chatRealtime.roomUnread[7101]).toBeUndefined()
  })

  it('still shows unread after refresh when a newer message arrives beyond the confirmed read watermark', async () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()

    vi.mocked(chatApi.listRooms).mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          roomId: 666004,
          otherUid: 1001,
          lastMsgId: 121,
          lastMsgBody: null,
          myLastReadMsgId: 120,
          unreadCount: 1,
          activeTime: '2026-04-16T00:00:00',
        },
      ],
    })

    await chatRealtime.refreshUnreadFromServer()

    expect(chatRealtime.totalUnread).toBe(1)
    expect(chatRealtime.roomUnread[666004]).toBe(1)
  })

  it('persists realtime watermark and routes application events through the unified envelope', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 801,
      eventType: 'application.decided',
      bizType: 'application',
      payload: {
        applicationId: 9527,
        status: 'ACCEPTED',
      },
    })

    expect(chatRealtime.lastRealtimeEventId).toBe(801)
    expect(chatRealtime.lastApplicationEvent).toEqual({
      eventType: 'application.decided',
      applicationId: 9527,
      status: 'ACCEPTED',
      occurredAt: undefined,
      payload: {
        applicationId: 9527,
        status: 'ACCEPTED',
      },
    })
    expect(window.localStorage.getItem('ai_tutor_realtime_last_event:2001')).toBe('801')
  })

  it('ignores duplicated or older unified realtime envelopes', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 900,
      eventType: 'application.created',
      bizType: 'application',
      payload: {
        applicationId: 1,
        status: 'PENDING',
      },
    })
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 899,
      eventType: 'application.decided',
      bizType: 'application',
      payload: {
        applicationId: 1,
        status: 'REJECTED',
      },
    })

    expect(chatRealtime.lastRealtimeEventId).toBe(900)
    expect(chatRealtime.lastApplicationEvent?.eventType).toBe('application.created')
    expect(chatRealtime.lastApplicationEvent?.status).toBe('PENDING')
  })

  it('syncs missed realtime events after stream ready when the server watermark is newer', async () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.lastRealtimeEventId = 800
    chatRealtime.clientId = 'web-local'

    vi.mocked(chatApi.syncRealtimeEvents).mockResolvedValueOnce({
      cursor: 905,
      isLast: true,
      latestEventId: 905,
      list: [
        {
          eventId: 905,
          eventType: 'application.decided',
          bizType: 'application',
          payload: {
            applicationId: 9527,
            status: 'ACCEPTED',
          },
        },
      ],
    })

    await chatRealtime.handleStreamReady({
      clientId: 'web-server',
      lastEventId: 905,
      replayedCount: 0,
    })

    expect(chatApi.syncRealtimeEvents).toHaveBeenCalledWith({
      lastEventId: 800,
      pageSize: 100,
    })
    expect(chatRealtime.clientId).toBe('web-server')
    expect(chatRealtime.lastRealtimeEventId).toBe(905)
    expect(chatRealtime.lastApplicationEvent).toEqual({
      eventType: 'application.decided',
      applicationId: 9527,
      status: 'ACCEPTED',
      occurredAt: undefined,
      payload: {
        applicationId: 9527,
        status: 'ACCEPTED',
      },
    })
    expect(window.localStorage.getItem('ai_tutor_realtime_client:2001')).toBe('web-server')
    expect(window.localStorage.getItem('ai_tutor_realtime_last_event:2001')).toBe('905')
  })

  it('keeps a consumable message event queue for pages when multiple realtime messages arrive together', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1001,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 501,
        roomId: 9001,
        fromUid: 1001,
        toUid: 2001,
        sendTime: '2026-04-17T10:00:00',
        body: { content: '第一条' },
      },
    })
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1002,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 502,
        roomId: 9002,
        fromUid: 1002,
        toUid: 2001,
        sendTime: '2026-04-17T10:00:01',
        body: { content: '第二条' },
      },
    })

    const queued = chatRealtime.listMessageEventsAfter(0)

    expect(chatRealtime.messageEventSerial).toBe(2)
    expect(queued).toHaveLength(2)
    expect(queued[0]?.event.roomId).toBe(9001)
    expect(queued[1]?.event.roomId).toBe(9002)
  })

  it('updates peer read watermark from unified chat read events', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1201,
      eventType: 'chat.read.updated',
      bizType: 'chat',
      payload: {
        roomId: 7101,
        readerUid: 3001,
        lastReadMsgId: 6001,
      },
    })

    expect(chatRealtime.peerReadMsgIdByRoom[7101]).toBe(6001)
  })

  it('updates peer delivered watermark from unified chat delivery events', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1202,
      eventType: 'chat.delivery.updated',
      bizType: 'chat',
      payload: {
        roomId: 7101,
        deliverUid: 3001,
        lastDeliveredMsgId: 6001,
      },
    })

    expect(chatRealtime.peerDeliveredMsgIdByRoom[7101]).toBe(6001)
  })

  it('acks delivery when a new incoming realtime message reaches the current user', async () => {
    seedAuth()
    useAuthStore()
    vi.mocked(chatApi.ackDelivered).mockResolvedValue(true)

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1203,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 6002,
        roomId: 7101,
        fromUid: 3001,
        toUid: 2001,
        sendTime: '2026-04-18T10:00:00',
        body: { content: '你好' },
      },
    })

    await Promise.resolve()

    expect(chatApi.ackDelivered).toHaveBeenCalledWith(7101, 6002)
  })

  it('shows peer typing only for a short online window and then expires automatically', () => {
    vi.useFakeTimers()
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventType: 'chat.typing.updated',
      bizType: 'chat',
      payload: {
        roomId: 7101,
        typingUid: 3001,
        typing: true,
      },
    })

    expect(chatRealtime.peerTypingByRoom[7101]).toBe(true)

    vi.advanceTimersByTime(3600)

    expect(chatRealtime.peerTypingByRoom[7101]).toBeUndefined()
  })

  it('updates peer presence from unified presence events', () => {
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventType: 'chat.presence.updated',
      bizType: 'chat',
      payload: {
        uid: 3001,
        online: false,
        lastOnlineAt: '2026-04-18T10:30:00',
      },
    })

    expect(chatRealtime.peerPresenceByUid[3001]).toEqual({
      uid: 3001,
      online: false,
      lastOnlineAt: '2026-04-18T10:30:00',
    })
  })

  it('clears peer typing immediately when the peer reports typing stopped', () => {
    vi.useFakeTimers()
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    chatRealtime.consumeRealtimeEnvelope({
      eventType: 'chat.typing.updated',
      bizType: 'chat',
      payload: {
        roomId: 7101,
        typingUid: 3001,
        typing: true,
      },
    })
    chatRealtime.consumeRealtimeEnvelope({
      eventType: 'chat.typing.updated',
      bizType: 'chat',
      payload: {
        roomId: 7101,
        typingUid: 3001,
        typing: false,
      },
    })

    expect(chatRealtime.peerTypingByRoom[7101]).toBeUndefined()
  })

  it('aborts a silent realtime stream when watchdog timeout is reached', () => {
    vi.useFakeTimers()
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    const controller = new AbortController()

    chatRealtime.startRealtimeWatchdog(controller, 1000, 200)
    vi.advanceTimersByTime(1200)

    expect(controller.signal.aborted).toBe(true)
    chatRealtime.stopRealtimeWatchdog()
  })

  it('keeps the realtime stream alive when activity continues before timeout', () => {
    vi.useFakeTimers()
    seedAuth()
    useAuthStore()

    const chatRealtime = useChatRealtimeStore()
    const controller = new AbortController()

    chatRealtime.startRealtimeWatchdog(controller, 1000, 200)
    vi.advanceTimersByTime(600)
    chatRealtime.noteRealtimeActivity()
    vi.advanceTimersByTime(600)

    expect(controller.signal.aborted).toBe(false)

    vi.advanceTimersByTime(1200)
    expect(controller.signal.aborted).toBe(true)
    chatRealtime.stopRealtimeWatchdog()
  })
})
