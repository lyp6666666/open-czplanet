import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import ChatRoomPage from './ChatRoomPage.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const mountedWrappers: VueWrapper[] = []
const mountedPinia: Pinia[] = []
const fetchMock = vi.fn()
const originalCreateObjectURL = URL.createObjectURL
const originalRevokeObjectURL = URL.revokeObjectURL
const originalImage = globalThis.Image

class MockImage {
  onload: null | (() => void) = null
  onerror: null | (() => void) = null
  naturalWidth = 640
  naturalHeight = 480

  set src(_value: string) {
    queueMicrotask(() => {
      this.onload?.()
    })
  }
}

const mocks = vi.hoisted(() => ({
  uploadImage: vi.fn(),
  listMessages: vi.fn(),
  listRooms: vi.fn(),
  getChatRefundState: vi.fn(),
  ackRead: vi.fn(),
  ackDelivered: vi.fn(),
  reportTyping: vi.fn(),
  batch: vi.fn(),
  sendText: vi.fn(),
  sendImage: vi.fn(),
  recallMessage: vi.fn(),
}))

vi.mock('@/api/assets', () => ({
  assetsApi: {
    uploadImage: mocks.uploadImage,
  },
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    listMessages: mocks.listMessages,
    listRooms: mocks.listRooms,
    getChatRefundState: mocks.getChatRefundState,
    ackRead: mocks.ackRead,
    ackDelivered: mocks.ackDelivered,
    reportTyping: mocks.reportTyping,
    sendText: mocks.sendText,
    sendImage: mocks.sendImage,
    recallMessage: mocks.recallMessage,
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
  mountedPinia.push(pinia)
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
  mountedWrappers.push(wrapper)
  await flushPromises()
  return { pinia, wrapper }
}

describe('ChatRoomPage realtime read receipt', () => {
  beforeEach(() => {
    vi.useRealTimers()
    fetchMock.mockReset()
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({}),
      text: async () => '',
    })
    vi.stubGlobal('fetch', fetchMock)
    Object.defineProperty(URL, 'createObjectURL', {
      value: vi.fn(() => 'blob:chat-room-test-image'),
      configurable: true,
      writable: true,
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      value: vi.fn(),
      configurable: true,
      writable: true,
    })
    vi.stubGlobal('Image', MockImage as unknown as typeof Image)
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
    mocks.uploadImage.mockReset()
    mocks.ackRead.mockReset()
    mocks.ackDelivered.mockReset()
    mocks.reportTyping.mockReset()
    mocks.batch.mockReset()
    mocks.sendText.mockReset()
    mocks.sendImage.mockReset()
    mocks.recallMessage.mockReset()

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
    mocks.ackDelivered.mockResolvedValue(true)
    mocks.reportTyping.mockResolvedValue(true)
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
    mocks.sendImage.mockResolvedValue({
      fromUser: { uid: 2001 },
      message: {
        id: 504,
        roomId: 10,
        sendTime: '2026-04-18T10:00:03',
        body: {
          type: 'image',
          url: '/api/v1/public/assets/chat/1001/a.png',
          objectKey: 'chat/1001/a.png',
          contentType: 'image/png',
          size: 1024,
          width: 320,
          height: 200,
        },
      },
    })
    mocks.recallMessage.mockResolvedValue({
      fromUser: { uid: 2001 },
      message: {
        id: 505,
        roomId: 10,
        sendTime: '2026-04-18T10:00:04',
        body: { type: 'recall', targetMsgId: 502, operatorUid: 2001 },
      },
    })
  })

  afterEach(() => {
    while (mountedPinia.length > 0) {
      useChatRealtimeStore(mountedPinia.pop()!).resetState()
    }
    // 主动卸载每次 mount 的页面实例，避免测试进程残留页面监听器和定时器。
    while (mountedWrappers.length > 0) {
      mountedWrappers.pop()?.unmount()
    }
    if (vi.isFakeTimers()) {
      vi.runOnlyPendingTimers()
    }
    vi.useRealTimers()
    vi.unstubAllGlobals()
    Object.defineProperty(URL, 'createObjectURL', {
      value: originalCreateObjectURL,
      configurable: true,
      writable: true,
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      value: originalRevokeObjectURL,
      configurable: true,
      writable: true,
    })
    vi.stubGlobal('Image', originalImage)
  })

  it('shows read receipt after peer read realtime event arrives', async () => {
    const { pinia, wrapper } = await mountChatRoomPage()

    expect(wrapper.text()).toContain('已发送')

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

  it('shows delivered receipt after peer delivery realtime event arrives', async () => {
    const { pinia, wrapper } = await mountChatRoomPage()

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.consumeRealtimeEnvelope({
      eventId: 1400,
      eventType: 'chat.delivery.updated',
      bizType: 'chat',
      payload: {
        roomId: 10,
        deliverUid: 3001,
        lastDeliveredMsgId: 501,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('已送达')
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

  it('shows peer typing hint after realtime typing event and hides it after expiration', async () => {
    vi.useFakeTimers()
    const { pinia, wrapper } = await mountChatRoomPage()

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.consumeRealtimeEnvelope({
      eventType: 'chat.typing.updated',
      bizType: 'chat',
      payload: {
        roomId: 10,
        typingUid: 3001,
        typing: true,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('对方正在输入...')

    vi.advanceTimersByTime(3600)
    await flushPromises()

    expect(wrapper.text()).not.toContain('对方正在输入...')
  })

  it('reports typing when the local user is composing and clears it after sending', async () => {
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

    const { wrapper } = await mountChatRoomPage()

    await wrapper.find('.input').setValue('正在输入')
    await flushPromises()
    expect(mocks.reportTyping).toHaveBeenCalledWith(10, true)

    await wrapper.find('.send .btn.btn-primary').trigger('click')
    await flushPromises()

    expect(mocks.reportTyping).toHaveBeenCalledWith(10, false)
  })

  it('renders image messages from history', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 700,
            roomId: 10,
            sendTime: '2026-04-18T10:02:00',
            body: { type: 'image', url: '/api/v1/public/assets/chat/3001/history.png', size: 2048, width: 320, height: 240 },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()

    const image = wrapper.find('.chat-image')
    expect(image.exists()).toBe(true)
    expect(image.attributes('src')).toContain('/api/v1/public/assets/chat/3001/history.png')
  })

  it('uploads then sends image messages from the chat page', async () => {
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
    mocks.uploadImage.mockResolvedValue({
      objectKey: 'chat/1001/a.png',
      url: 'https://assets.example.com/ai-tutor-assets/chat/1001/a.png',
      contentType: 'image/png',
      size: 1024,
    })

    const { wrapper } = await mountChatRoomPage()
    const file = new File([new Uint8Array([1, 2, 3])], 'a.png', { type: 'image/png' })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()
    await flushPromises()

    expect(mocks.uploadImage).toHaveBeenCalledTimes(1)
    expect(mocks.sendImage).toHaveBeenCalledTimes(1)
    expect(mocks.sendImage.mock.calls[0]?.[0]).toBe(10)
    expect(wrapper.find('.chat-image').exists()).toBe(true)
  })

  it('renders recalled messages from history', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 2001 },
          message: {
            id: 502,
            roomId: 10,
            sendTime: '2026-04-18T10:00:01',
            body: { type: 'text', content: '这条消息将被撤回' },
          },
        },
        {
          fromUser: { uid: 2001 },
          message: {
            id: 503,
            roomId: 10,
            sendTime: '2026-04-18T10:00:02',
            body: { type: 'recall', targetMsgId: 502, operatorUid: 2001 },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()

    expect(wrapper.text()).toContain('你撤回了一条消息')
    expect(wrapper.text()).not.toContain('这条消息将被撤回')
  })

  it('recalls own text message from the chat page', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 2001 },
          message: {
            id: 502,
            roomId: 10,
            sendTime: '2026-04-18T10:00:01',
            body: { type: 'text', content: '准备撤回的消息' },
          },
        },
      ],
    })
    mocks.recallMessage.mockResolvedValue({
      fromUser: { uid: 2001 },
      message: {
        id: 506,
        roomId: 10,
        sendTime: '2026-04-18T10:00:05',
        body: { type: 'recall', targetMsgId: 502, operatorUid: 2001 },
      },
    })

    const { wrapper } = await mountChatRoomPage()

    await wrapper.find('.recall-link').trigger('click')
    await flushPromises()

    expect(mocks.recallMessage).toHaveBeenCalledWith(10, 502)
    expect(wrapper.text()).toContain('你撤回了一条消息')
    expect(wrapper.text()).not.toContain('准备撤回的消息')
  })

  it('toggles room pin status from the chat header', async () => {
    const { wrapper } = await mountChatRoomPage()

    expect(wrapper.text()).toContain('会话置顶')

    const pinButton = wrapper.find('.pin-toggle')
    await pinButton.trigger('click')
    await flushPromises()

    expect(localStorage.getItem('ai_tutor_chat_pins:2001')).toContain('10')
    expect(wrapper.text()).toContain('取消置顶')
  })
})
