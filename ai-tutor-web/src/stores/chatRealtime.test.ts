import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'

import { chatApi } from '@/api/chat'

import { useAuthStore } from './auth'
import { useChatRealtimeStore } from './chatRealtime'

vi.mock('@/api/chat', () => ({
  chatApi: {
    listRooms: vi.fn(),
    ackRead: vi.fn(),
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

  it('still shows unread after refresh when the server has not confirmed the local read watermark', async () => {
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

    expect(chatRealtime.totalUnread).toBe(1)
    expect(chatRealtime.roomUnread[666004]).toBe(1)
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
    expect(queued[0].event.roomId).toBe(9001)
    expect(queued[1].event.roomId).toBe(9002)
  })
})
