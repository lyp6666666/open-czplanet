import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import ChatListPage from './ChatListPage.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const mocks = vi.hoisted(() => ({
  listRooms: vi.fn(),
  batch: vi.fn(),
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    listRooms: mocks.listRooms,
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    batch: mocks.batch,
  },
}))

function createStorage(): Storage {
  const store = new Map<string, string>()
  return {
    get length() {
      return store.size
    },
    clear() {
      store.clear()
    },
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    key(index: number) {
      return Array.from(store.keys())[index] ?? null
    },
    removeItem(key: string) {
      store.delete(key)
    },
    setItem(key: string, value: string) {
      store.set(key, String(value))
    },
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/chat', name: 'chatList', component: ChatListPage },
      { path: '/chat/:roomId', name: 'chatRoom', component: { template: '<div>chat room</div>' } },
    ],
  })
}

describe('ChatListPage realtime', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    const sessionStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'sessionStorage', { value: sessionStorageMock, configurable: true })

    localStorage.setItem('ai_tutor_token', 'token')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 2001, token: 'token', userType: 2, name: '学生2001', phone: '13800138000' }),
    )

    mocks.listRooms.mockReset()
    mocks.batch.mockReset()
    mocks.listRooms.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [],
    })
    mocks.batch.mockResolvedValue([])
  })

  it('renders all queued message events after a batched realtime catch-up', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore(pinia)

    const router = createTestRouter()
    await router.push('/chat')
    await router.isReady()

    const wrapper = mount(ChatListPage, {
      global: {
        plugins: [pinia, router],
        stubs: {
          RouterView: { template: '<div data-test="room-view" />' },
        },
      },
    })
    await flushPromises()

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1101,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 6001,
        roomId: 7101,
        fromUid: 3001,
        toUid: 2001,
        sendTime: '2026-04-17T10:00:00',
        body: { content: '第一条补偿消息' },
      },
    })
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1102,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 6002,
        roomId: 7102,
        fromUid: 3002,
        toUid: 2001,
        sendTime: '2026-04-17T10:00:01',
        body: { content: '第二条补偿消息' },
      },
    })
    await flushPromises()

    const items = wrapper.findAll('.item')
    expect(items).toHaveLength(2)
    expect(wrapper.text()).toContain('第一条补偿消息')
    expect(wrapper.text()).toContain('第二条补偿消息')
  })

  it('shows recall preview when realtime catch-up contains a recall event', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore(pinia)

    const router = createTestRouter()
    await router.push('/chat')
    await router.isReady()

    const wrapper = mount(ChatListPage, {
      global: {
        plugins: [pinia, router],
        stubs: {
          RouterView: { template: '<div data-test="room-view" />' },
        },
      },
    })
    await flushPromises()

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1201,
      eventType: 'chat.message.created',
      bizType: 'chat',
      payload: {
        msgId: 6101,
        roomId: 7101,
        fromUid: 3001,
        toUid: 2001,
        sendTime: '2026-04-17T10:00:02',
        body: { type: 'recall', targetMsgId: 6001, operatorUid: 3001 },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('[消息已撤回]')
  })

  it('keeps pinned rooms at the top of the chat list', async () => {
    localStorage.setItem('ai_tutor_chat_pins:2001', JSON.stringify([7102]))
    mocks.listRooms.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        { roomId: 7101, otherUid: 3001, lastMsgId: 6001, lastMsgBody: { content: '第一条' }, myLastReadMsgId: null, unreadCount: 0, activeTime: '2026-04-17T10:00:00' },
        { roomId: 7102, otherUid: 3002, lastMsgId: 6002, lastMsgBody: { content: '第二条' }, myLastReadMsgId: null, unreadCount: 0, activeTime: '2026-04-17T10:00:01' },
      ],
    })
    mocks.batch.mockResolvedValue([
      { id: 3001, name: '教师甲', realName: '教师甲', avatar: '', userType: 1 },
      { id: 3002, name: '教师乙', realName: '教师乙', avatar: '', userType: 1 },
    ])

    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore(pinia)

    const router = createTestRouter()
    await router.push('/chat')
    await router.isReady()

    const wrapper = mount(ChatListPage, {
      global: {
        plugins: [pinia, router],
        stubs: {
          RouterView: { template: '<div data-test="room-view" />' },
        },
      },
    })
    await flushPromises()

    const names = wrapper.findAll('.name').map((item) => item.text())
    expect(names[0]).toBe('教师乙')
    expect(wrapper.text()).toContain('置顶')
  })

  it('does not re-show the left unread badge after the unread snapshot confirms the room is cleared', async () => {
    mocks.listRooms.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        { roomId: 7101, otherUid: 3001, lastMsgId: 6001, lastMsgBody: { content: '第一条' }, myLastReadMsgId: null, unreadCount: 1, activeTime: '2026-04-17T10:00:00' },
      ],
    })
    mocks.batch.mockResolvedValue([{ id: 3001, name: '教师甲', realName: '教师甲', avatar: '', userType: 1 }])

    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore(pinia)

    const router = createTestRouter()
    await router.push('/chat')
    await router.isReady()

    const wrapper = mount(ChatListPage, {
      global: {
        plugins: [pinia, router],
        stubs: {
          RouterView: { template: '<div data-test="room-view" />' },
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.unread-badge').exists()).toBe(true)

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.unreadSnapshotLoaded = true
    chatRealtime.roomUnread = {}
    await flushPromises()

    expect(wrapper.find('.unread-badge').exists()).toBe(false)
  })
})
