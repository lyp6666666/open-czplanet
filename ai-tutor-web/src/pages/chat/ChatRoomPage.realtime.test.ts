import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import ChatRoomPage from './ChatRoomPage.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const mocks = vi.hoisted(() => ({
  listMessages: vi.fn(),
  listRooms: vi.fn(),
  getChatRefundState: vi.fn(),
  ackRead: vi.fn(),
  batch: vi.fn(),
  sendText: vi.fn(),
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    listMessages: mocks.listMessages,
    listRooms: mocks.listRooms,
    getChatRefundState: mocks.getChatRefundState,
    ackRead: mocks.ackRead,
    sendText: mocks.sendText,
    requestBrokerageRefund: vi.fn(),
    requestEndChat: vi.fn(),
    respondEndChat: vi.fn(),
    createCollaborationProposal: vi.fn(),
    updateCollaborationProposal: vi.fn(),
    respondCollaborationProposal: vi.fn(),
  },
}))

vi.mock('@/api/contact', () => ({
  contactApi: {
    unlock: vi.fn(),
  },
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    startChat: vi.fn(),
    decideMessage: vi.fn(),
  },
}))

vi.mock('@/api/schedule', () => ({
  scheduleApi: {
    respond: vi.fn(),
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    batch: mocks.batch,
    me: vi.fn(),
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
    routes: [{ path: '/chat/:roomId', name: 'chatRoom', component: ChatRoomPage }],
  })
}

async function mountChatRoomPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore(pinia)
  auth.me = {
    id: 2001,
    name: '学生2001',
    phone: '13800138000',
    avatar: '',
    sex: 2,
    userType: 2,
    studentProfile: {
      id: 1,
      userId: 2001,
      realName: '学生2001',
      age: 18,
      childAge: 18,
      address: '',
      demandDescription: '',
      budget: '',
      status: 1,
      createTime: '2026-04-18T10:00:00',
      updateTime: '2026-04-18T10:00:00',
    },
    teacherProfile: null,
    organizationProfile: null,
  }

  const router = createTestRouter()
  await router.push('/chat/10?otherUid=3001')
  await router.isReady()

  const wrapper = mount(ChatRoomPage, {
    global: {
      plugins: [pinia, router],
      stubs: {
        BrokerageRequiredCard: { template: '<div />' },
        CollaborationProposalCard: { template: '<div />' },
        CollaborationProposalModal: { template: '<div />' },
        ContactUnlockedCard: { template: '<div />' },
        LessonRequestCard: { template: '<div />' },
        TutorApplicationCard: { template: '<div />' },
        UnlockedContactModal: { template: '<div />' },
        UserCardModal: { template: '<div />' },
      },
    },
  })
  await flushPromises()
  return { pinia, wrapper }
}

describe('ChatRoomPage realtime read receipt', () => {
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
      JSON.stringify({ id: 2001, token: 'token', userType: 2, name: '学生2001', phone: '13800138000', avatar: '' }),
    )

    mocks.listMessages.mockReset()
    mocks.listRooms.mockReset()
    mocks.getChatRefundState.mockReset()
    mocks.ackRead.mockReset()
    mocks.batch.mockReset()
    mocks.sendText.mockReset()

    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 2001 },
          message: {
            id: 501,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: { type: 'text', content: '你好' },
          },
        },
      ],
    })
    mocks.listRooms.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          roomId: 10,
          otherUid: 3001,
          lastMsgId: 501,
          lastMsgBody: { content: '你好' },
          myLastReadMsgId: 501,
          peerLastReadMsgId: null,
          unreadCount: 0,
          activeTime: '2026-04-18T10:00:00',
        },
      ],
    })
    mocks.getChatRefundState.mockResolvedValue({ canApply: false })
    mocks.ackRead.mockResolvedValue({ roomId: 10, lastReadMsgId: 501 })
    mocks.batch.mockResolvedValue([{ id: 3001, name: '教师3001', realName: '张老师', avatar: '', userType: 1 }])
    mocks.sendText.mockResolvedValue({
      fromUser: { uid: 2001 },
      message: {
        id: 502,
        roomId: 10,
        sendTime: '2026-04-18T10:00:01',
        body: { type: 'text', content: '重试成功' },
      },
    })
  })

  it('shows read receipt after peer read realtime event arrives', async () => {
    const { pinia, wrapper } = await mountChatRoomPage()

    expect(wrapper.text()).toContain('未读')

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1401,
      eventType: 'chat.read.updated',
      bizType: 'chat',
      payload: {
        roomId: 10,
        readerUid: 3001,
        lastReadMsgId: 501,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('对方已读')
  })

  it('keeps a failed outgoing text visible and retries it from the chat page', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 500,
            roomId: 10,
            sendTime: '2026-04-18T09:59:00',
            body: { type: 'contact_unlocked', proposalId: 1, orderId: 1, status: 'PAID' },
          },
        },
      ],
    })
    mocks.sendText
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce({
        fromUser: { uid: 2001 },
        message: {
          id: 503,
          roomId: 10,
          sendTime: '2026-04-18T10:00:02',
          body: { type: 'text', content: '补发成功' },
        },
      })

    const { wrapper } = await mountChatRoomPage()

    await wrapper.find('.input').setValue('补发成功')
    await wrapper.find('.send .btn.btn-primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('发送失败，点击重试')

    await wrapper.find('.retry-link').trigger('click')
    await flushPromises()

    expect(mocks.sendText).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('补发成功')
    expect(wrapper.text()).not.toContain('发送失败，点击重试')
  })
})
