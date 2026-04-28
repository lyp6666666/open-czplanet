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
  searchMessages: vi.fn(),
  batchPresence: vi.fn(),
  listRooms: vi.fn(),
  getChatRefundState: vi.fn(),
  ackRead: vi.fn(),
  ackDelivered: vi.fn(),
  reportTyping: vi.fn(),
  batch: vi.fn(),
  sendText: vi.fn(),
  sendImage: vi.fn(),
  recallMessage: vi.fn(),
  createEvent: vi.fn(),
  listCourseEvents: vi.fn(),
  dayAvailability: vi.fn(),
  getLiveByCourse: vi.fn(),
  unlockContact: vi.fn(),
  courseByRoom: vi.fn(),
  getDemandView: vi.fn(),
}))

vi.mock('@/api/assets', () => ({
  assetsApi: {
    uploadImage: mocks.uploadImage,
  },
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    listMessages: mocks.listMessages,
    searchMessages: mocks.searchMessages,
    batchPresence: mocks.batchPresence,
    listRooms: mocks.listRooms,
    getChatRefundState: mocks.getChatRefundState,
    ackRead: mocks.ackRead,
    ackDelivered: mocks.ackDelivered,
    reportTyping: mocks.reportTyping,
    sendText: mocks.sendText,
    sendImage: mocks.sendImage,
    recallMessage: mocks.recallMessage,
    requestBrokerageRefund: vi.fn(),
    createCollaborationProposal: vi.fn(),
    updateCollaborationProposal: vi.fn(),
    respondCollaborationProposal: vi.fn(),
  },
}))

vi.mock('@/api/contact', () => ({
  contactApi: {
    unlock: mocks.unlockContact,
  },
}))

vi.mock('@/api/course', () => ({
  courseApi: {
    byRoom: mocks.courseByRoom,
  },
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    getDemandView: mocks.getDemandView,
  },
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    startChat: vi.fn(),
    decideMessage: vi.fn(),
    enterChat: vi.fn(),
  },
}))

vi.mock('@/api/schedule', () => ({
  scheduleApi: {
    createEvent: mocks.createEvent,
    listCourseEvents: mocks.listCourseEvents,
    dayAvailability: mocks.dayAvailability,
    respond: vi.fn(),
  },
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    getByCourse: mocks.getLiveByCourse,
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    batch: mocks.batch,
    me: vi.fn(async () => ({
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
    })),
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
      { path: '/chat/:roomId', name: 'chatRoom', component: ChatRoomPage },
      { path: '/pay/cashier', name: 'cashierPay', component: { template: '<div>cashier</div>' } },
      { path: '/courses/:courseId/ai-summary', name: 'lessonAiSummary', component: { template: '<div>lesson-ai-summary</div>' } },
      { path: '/tutor/jobs/:id', name: 'tutorJobDetail', component: { template: '<div>job-detail</div>' } },
    ],
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
        ContactUnlockedCard: {
          props: ['canView'],
          emits: ['view'],
          template:
            '<button v-if="canView" class="contact-unlocked-trigger" type="button" @click="$emit(\'view\')">查看对方的联系方式</button>',
        },
        LessonRequestCard: {
          props: ['body'],
          template: '<div class="lesson-request-stub">授课申请 {{ body?.title }}</div>',
        },
        TutorApplicationCard: { template: '<div />' },
        UnlockedContactModal: { template: '<div />' },
        UserCardModal: {
          props: ['open', 'uid', 'unlockedContactPhone'],
          template:
            '<div v-if="open" class="user-card-modal-stub">用户卡片 {{ uid }} <span class="user-card-contact">{{ unlockedContactPhone }}</span></div>',
        },
      },
    },
  })
  mountedWrappers.push(wrapper)
  await flushPromises()
  return { pinia, wrapper, router }
}

async function mountChatRoomPageAt(path: string) {
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
  await router.push(path)
  await router.isReady()

  const wrapper = mount(ChatRoomPage, {
    global: {
      plugins: [pinia, router],
      stubs: {
        BrokerageRequiredCard: { template: '<div />' },
        CollaborationProposalCard: { template: '<div />' },
        CollaborationProposalModal: { template: '<div />' },
        ContactUnlockedCard: {
          props: ['canView'],
          emits: ['view'],
          template:
            '<button v-if="canView" class="contact-unlocked-trigger" type="button" @click="$emit(\'view\')">查看对方的联系方式</button>',
        },
        LessonRequestCard: {
          props: ['body'],
          template: '<div class="lesson-request-stub">授课申请 {{ body?.title }}</div>',
        },
        TutorApplicationCard: { template: '<div />' },
        UnlockedContactModal: { template: '<div />' },
        UserCardModal: {
          props: ['open', 'uid', 'unlockedContactPhone'],
          template:
            '<div v-if="open" class="user-card-modal-stub">用户卡片 {{ uid }} <span class="user-card-contact">{{ unlockedContactPhone }}</span></div>',
        },
      },
    },
  })
  mountedWrappers.push(wrapper)
  await flushPromises()
  return { pinia, wrapper, router }
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
    mocks.searchMessages.mockReset()
    mocks.batchPresence.mockReset()
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
    mocks.createEvent.mockReset()
    mocks.unlockContact.mockReset()
    mocks.courseByRoom.mockReset()
    mocks.getDemandView.mockReset()

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
    mocks.searchMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 801,
            roomId: 10,
            sendTime: '2026-04-18T10:10:00',
            body: { type: 'text', content: '数学作业今晚发给你' },
          },
        },
      ],
    })
    mocks.batchPresence.mockResolvedValue([{ uid: 3001, online: true, lastOnlineAt: null }])
    mocks.courseByRoom.mockRejectedValue(new Error('no course'))
    mocks.getDemandView.mockResolvedValue({
      id: 99,
      parentId: 2001,
      subjectId: 1,
      subjectName: '初中数学',
      subjectIsOther: 0,
      title: '初二数学线下提分',
      description: '想找老师补基础',
      studentGender: 'male',
      gradeCode: 'JUNIOR',
      availableTime: '',
      teacherGenderPreference: 'both',
      teacherRequirementDetail: '',
      childAge: 13,
      classMode: 'offline',
      city: '北京',
      address: '',
      frequencyPerWeek: 2,
      budgetMin: '180',
      budgetMax: '260',
      stageCode: 'JUNIOR',
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
      schedule: '',
      bizStatus: 1,
      status: 1,
      createTime: '2026-04-18T10:00:00',
      updateTime: '2026-04-18T10:00:00',
      publisher: {
        uid: 2001,
        displayName: '家长',
        avatar: null,
        identityLabel: '家长',
      },
    })
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
    mocks.createEvent.mockImplementation(async (payload: { title: string; startAt: number; endAt: number; participantUserId: number }) => ({
      id: 601,
      title: payload.title,
      description: null,
      startAt: payload.startAt,
      endAt: payload.endAt,
      status: 'PENDING',
      creatorUserId: 2001,
      participant: { id: payload.participantUserId, name: '张老师', userType: 1 },
      chatRoomId: 10,
    }))
    mocks.unlockContact.mockResolvedValue({
      uid: 3001,
      phone: '13800138001',
    })
    mocks.listCourseEvents.mockResolvedValue([
      {
        id: 701,
        courseId: 66,
        title: '最近的一节课',
        description: '通过聊天继续确认本节重点',
        startAt: Date.now() + 30 * 60 * 1000,
        endAt: Date.now() + 90 * 60 * 1000,
        status: 'ACCEPTED',
        creatorUserId: 2001,
        participant: { id: 3001, name: '张老师', realName: '张老师', avatar: '', userType: 1 },
        chatRoomId: 10,
      },
    ])
    mocks.getLiveByCourse.mockResolvedValue({
      sessionId: 888,
      courseId: 66,
      status: 'SCHEDULED',
      joinableNow: false,
    })
    mocks.courseByRoom.mockResolvedValue({
      courseId: 66,
      applicationId: 501,
      roomId: 10,
      teacherUid: 3001,
      studentUid: 2001,
      liveSessionId: 888,
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

  it('loads and renders peer online presence in the chat header', async () => {
    const { wrapper } = await mountChatRoomPage()

    expect(mocks.batchPresence).toHaveBeenCalledWith([3001])
    expect(wrapper.find('.presence-text').text()).toBe('在线')
  })

  it('renders peer offline presence with last online time', async () => {
    mocks.batchPresence.mockResolvedValueOnce([
      {
        uid: 3001,
        online: false,
        lastOnlineAt: '2026-04-18T10:12:00',
      },
    ])

    const { wrapper } = await mountChatRoomPage()

    expect(wrapper.find('.presence-text').text()).toContain('离线')
    expect(wrapper.find('.presence-text').text()).toContain('最后在线')
    expect(wrapper.find('.presence-text').text()).toContain('10:12')
  })

  it('updates peer presence immediately after a realtime presence event arrives', async () => {
    mocks.batchPresence.mockResolvedValueOnce([{ uid: 3001, online: false, lastOnlineAt: '2026-04-18T10:12:00' }])

    const { pinia, wrapper } = await mountChatRoomPage()
    expect(wrapper.find('.presence-text').text()).toContain('离线')

    const chatRealtime = useChatRealtimeStore(pinia)
    chatRealtime.consumeRealtimeEnvelope({
      eventType: 'chat.presence.updated',
      bizType: 'chat',
      payload: {
        uid: 3001,
        online: true,
        lastOnlineAt: null,
      },
    })
    await flushPromises()

    expect(wrapper.find('.presence-text').text()).toBe('在线')
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

  it('redirects teacher to cashier after accepting an application that requires info fee payment', async () => {
    const { applicationApi } = await import('@/api/application')
    const decideMessage = vi.mocked(applicationApi.decideMessage)
    const enterChat = vi.mocked(applicationApi.enterChat)

    decideMessage.mockResolvedValueOnce({
      fromUser: { uid: 3001 },
      message: {
        id: 602,
        roomId: 10,
        sendTime: '2026-04-18T10:00:02',
        body: { type: 'tutor_application_status', applicationId: 9527, status: 'ACCEPTED', actorUserId: 2001 },
      },
    })
    enterChat.mockResolvedValueOnce({
      paymentRequired: true,
      waitingForTeacherPayment: false,
      orderId: 9001,
      roomId: null,
    })
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 601,
            roomId: 10,
            sendTime: '2026-04-18T10:00:01',
            body: {
              type: 'tutor_application',
              applicationId: 9527,
              content: '您好，方便聊聊吗？',
              status: 'PENDING',
              creatorUserId: 3001,
              contextType: 'DEMAND',
              contextId: 81,
            },
          },
        },
        {
          fromUser: { uid: 2001 },
          message: {
            id: 603,
            roomId: 10,
            sendTime: '2026-04-18T10:00:03',
            body: {
              type: 'brokerage_required',
              orderId: 9001,
              proposalId: 9527,
              amountFen: 19900,
              status: 'PENDING',
              payerUserId: 2001,
            },
          },
        },
      ],
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    mountedPinia.push(pinia)
    const auth = useAuthStore(pinia)
    auth.me = {
      id: 2001,
      name: '教师2001',
      phone: '13800138001',
      avatar: '',
      sex: 1,
      userType: 1,
      studentProfile: null,
      teacherProfile: {
        id: 1,
        userId: 2001,
        realName: '张老师',
        education: '本科',
        subject: '数学',
        experienceYears: 5,
        ratePerHour: '300',
        introduction: '',
        certificateUrls: null,
        status: 1,
        createTime: '2026-04-18T10:00:00',
        updateTime: '2026-04-18T10:00:00',
      },
      organizationProfile: null,
    }
    auth.user = {
      id: 2001,
      name: '教师2001',
      phone: '13800138001',
      avatar: '',
      sex: 1,
      userType: 1,
      token: 'token',
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
          TutorApplicationCard: {
            emits: ['accept', 'reject'],
            template: '<button class="accept-app" type="button" @click="$emit(\'accept\')">accept</button>',
          },
          UnlockedContactModal: { template: '<div />' },
          UserCardModal: { template: '<div />' },
        },
      },
    })
    mountedWrappers.push(wrapper)
    await flushPromises()

    await wrapper.find('.accept-app').trigger('click')
    await flushPromises()

    expect(decideMessage).toHaveBeenCalledWith(9527, 'ACCEPT')
    expect(mocks.listMessages).toHaveBeenCalledWith({ roomId: 10, pageSize: 20, cursor: null })
    expect(enterChat).toHaveBeenCalledWith(9527)
    expect(router.currentRoute.value.name).toBe('cashierPay')
    expect(router.currentRoute.value.query.contextId).toBe('9001')
    expect(router.currentRoute.value.query.applicationId).toBe('9527')
    expect(router.currentRoute.value.query.otherUid).toBe('3001')
  })

  it('reuses teachingMode when sending tutor application again', async () => {
    const { applicationApi } = await import('@/api/application')
    const startChat = vi.mocked(applicationApi.startChat)
    startChat.mockResolvedValueOnce({
      fromUser: { uid: 2001 },
      message: {
        id: 701,
        roomId: 10,
        sendTime: '2026-04-18T10:10:03',
        body: {
          type: 'tutor_application',
          applicationId: 9902,
          content: '想继续沟通课程安排',
          status: 'PENDING',
          creatorUserId: 2001,
          contextType: 'TUTOR',
          contextId: 81,
          teachingMode: 'ONLINE',
        },
      },
    })
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 2001 },
          message: {
            id: 700,
            roomId: 10,
            sendTime: '2026-04-18T10:10:00',
            body: {
              type: 'tutor_application',
              applicationId: 9901,
              content: '想继续沟通课程安排',
              status: 'REJECTED',
              creatorUserId: 2001,
              contextType: 'TUTOR',
              contextId: 81,
              teachingMode: 'ONLINE',
            },
          },
        },
      ],
    })

    const router = createTestRouter()
    await router.push('/chat/10?otherUid=3001')
    await router.isReady()

    const wrapper = mount(ChatRoomPage, {
      global: {
        plugins: [createPinia(), router],
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

    await wrapper.find('button.btn.btn-primary').trigger('click')
    await flushPromises()

    expect(startChat).toHaveBeenCalledWith({
      receiverUid: 3001,
      contextType: 'TUTOR',
      contextId: 81,
      content: '想继续沟通课程安排',
      teachingMode: 'ONLINE',
      clientRequestId: expect.stringContaining('reapply-'),
    })
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

  it('auto loads unlocked contact only after contact_unlocked message appears', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 800,
            roomId: 10,
            sendTime: '2026-04-18T10:05:00',
            body: {
              type: 'collaboration_status',
              proposalId: 88,
              status: 'ACCEPTED',
              actorUserId: 3001,
            },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 801,
            roomId: 10,
            sendTime: '2026-04-18T10:05:02',
            body: {
              type: 'contact_unlocked',
              proposalId: 88,
              orderId: 9,
              status: 'PAID',
            },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    await flushPromises()

    expect(mocks.unlockContact).toHaveBeenCalledWith(10, 3001)
    expect(wrapper.text()).not.toContain('请求数据不存在')
  })

  it('renders the updated unlocked contact action copy', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 800,
            roomId: 10,
            sendTime: '2026-04-18T10:05:00',
            body: { type: 'contact_unlocked', proposalId: 88, orderId: 9, status: 'PAID' },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    await flushPromises()

    expect(wrapper.find('.contact-unlocked-trigger').text()).toBe('查看对方的联系方式')
  })

  it('opens unlocked contact even when route query misses otherUid', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 800,
            roomId: 10,
            sendTime: '2026-04-18T10:05:00',
            body: { type: 'contact_unlocked', proposalId: 88, orderId: 9, status: 'PAID' },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPageAt('/chat/10')
    await flushPromises()

    await wrapper.find('.contact-unlocked-trigger').trigger('click')
    await flushPromises()

    expect(mocks.unlockContact).toHaveBeenCalledWith(10, 3001)
  })

  it('does not reload messages in a loop when otherUid is inferred from loaded messages', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 800,
            roomId: 10,
            sendTime: '2026-04-18T10:05:00',
            body: { type: 'text', content: '支付后回来看看消息' },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPageAt('/chat/10')
    await flushPromises()

    expect(mocks.listMessages).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('支付后回来看看消息')
  })

  it('does not reinitialize the room when only otherUid query changes', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 800,
            roomId: 10,
            sendTime: '2026-04-18T10:05:00',
            body: { type: 'text', content: '支付成功后进入会话' },
          },
        },
      ],
    })

    const { router } = await mountChatRoomPageAt('/chat/10')
    await flushPromises()

    expect(mocks.listMessages).toHaveBeenCalledTimes(1)
    expect(mocks.courseByRoom).toHaveBeenCalledTimes(1)

    await router.replace('/chat/10?otherUid=3001')
    await flushPromises()

    expect(mocks.listMessages).toHaveBeenCalledTimes(1)
    expect(mocks.courseByRoom).toHaveBeenCalledTimes(1)
  })

  it('loads unlocked contact into the peer avatar card after contact access is opened', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 800,
            roomId: 10,
            sendTime: '2026-04-18T10:05:00',
            body: { type: 'contact_unlocked', proposalId: 88, orderId: 9, status: 'PAID' },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 801,
            roomId: 10,
            sendTime: '2026-04-18T10:06:00',
            body: { type: 'text', content: '线下见面前先电话沟通一下' },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    await flushPromises()

    const peerAvatarButton = wrapper.find('.avatar.clickable')
    expect(peerAvatarButton.exists()).toBe(true)
    await peerAvatarButton.trigger('click')
    await flushPromises()

    expect(mocks.unlockContact).toHaveBeenCalledWith(10, 3001)
    expect(wrapper.find('.user-card-contact').text()).toContain('13800138001')
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

    expect(wrapper.find('.more-trigger').exists()).toBe(true)

    await wrapper.find('.more-trigger').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('置顶聊天')

    const pinButton = wrapper.find('.pin-action')
    await pinButton.trigger('click')
    await flushPromises()

    expect(localStorage.getItem('ai_tutor_chat_pins:2001')).toContain('10')
    expect(wrapper.text()).toContain('已置顶')
  })

  it('searches messages through the frontend api and renders the results list', async () => {
    const { wrapper } = await mountChatRoomPage()

    await wrapper.find('.more-trigger').trigger('click')
    await flushPromises()
    await wrapper.find('.search-action').trigger('click')
    await flushPromises()

    await wrapper.find('.search-input').setValue('数学')
    await wrapper.find('.search-submit').trigger('click')
    await flushPromises()

    expect(mocks.searchMessages).toHaveBeenCalledWith({ roomId: 10, keyword: '数学', pageSize: 20, cursor: null })
    expect(wrapper.text()).toContain('共命中 1 条消息')
    expect(wrapper.text()).toContain('数学作业今晚发给你')
  })

  it('locates a searched message after selecting a search result', async () => {
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

    await wrapper.find('.more-trigger').trigger('click')
    await flushPromises()
    await wrapper.find('.search-action').trigger('click')
    await flushPromises()

    await wrapper.find('.search-input').setValue('数学')
    await wrapper.find('.search-submit').trigger('click')
    await flushPromises()

    await wrapper.find('.search-hit').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('数学作业今晚发给你')
    expect(wrapper.find('[data-msg-id="801"]').classes()).toContain('search-focused')
  })

  it('renders centered 24 hour time dividers for evening messages', async () => {
    mocks.listMessages.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 901,
            roomId: 10,
            sendTime: '2026-04-18T21:00:00',
            body: { type: 'text', content: '晚上见' },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    const divider = wrapper.find('.time-divider')

    expect(divider.exists()).toBe(true)
    expect(divider.text()).toContain('21:00')
  })

  it('does not expose direct lesson booking action in chat composer area', async () => {
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

    const openButton = wrapper.find('.schedule-action')
    expect(openButton.exists()).toBe(false)
    expect(wrapper.text()).not.toContain('发起线上约课')
  })

  it('renders the recent lesson card with the completed primary action', async () => {
    mocks.listCourseEvents.mockResolvedValueOnce([
      {
        id: 702,
        courseId: 66,
        title: '最近的一节课',
        description: '通过聊天继续确认本节重点',
        startAt: Date.parse('2026-04-18T09:00:00'),
        endAt: Date.parse('2026-04-18T10:00:00'),
        status: 'ACCEPTED',
        creatorUserId: 2001,
        participant: { id: 3001, name: '张老师', realName: '张老师', avatar: '', userType: 1 },
        chatRoomId: 10,
      },
    ])
    mocks.getLiveByCourse.mockResolvedValueOnce({
      sessionId: 888,
      courseId: 66,
      status: 'ENDED',
      joinableNow: false,
    })

    const { wrapper } = await mountChatRoomPage()

    expect(wrapper.text()).toContain('最近的一节课')
    expect(wrapper.text()).toContain('查看课后总结')
    expect(wrapper.text()).toContain('已完课')
  })

  it('auto opens unlocked contact only once after offline collaboration accepted', async () => {
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 2001 },
          message: {
            id: 500,
            roomId: 10,
            sendTime: '2026-04-18T09:58:00',
            body: {
              type: 'tutor_application',
              applicationId: 9,
              content: '线下试课',
              status: 'ACCEPTED',
              creatorUserId: 2001,
              contextType: 'TUTOR',
              contextId: 88,
              teachingMode: 'OFFLINE',
            },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 501,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: { type: 'collaboration_status', proposalId: 21, status: 'ACCEPTED', actorUserId: 3001 },
          },
        },
      ],
    })

    await mountChatRoomPage()
    await flushPromises()

    expect(mocks.unlockContact).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('ai_tutor_contact_auto_shown:2001:10')).toBe('1')
  })

  it('does not auto open unlocked contact again after refresh when already shown', async () => {
    localStorage.setItem('ai_tutor_contact_auto_shown:2001:10', '1')
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 2001 },
          message: {
            id: 500,
            roomId: 10,
            sendTime: '2026-04-18T09:58:00',
            body: {
              type: 'tutor_application',
              applicationId: 9,
              content: '线下试课',
              status: 'ACCEPTED',
              creatorUserId: 2001,
              contextType: 'TUTOR',
              contextId: 88,
              teachingMode: 'OFFLINE',
            },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 501,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: { type: 'collaboration_status', proposalId: 21, status: 'ACCEPTED', actorUserId: 3001 },
          },
        },
      ],
    })

    await mountChatRoomPage()
    await flushPromises()

    expect(mocks.unlockContact).not.toHaveBeenCalled()
  })

  it('renders lesson ai result card and opens summary route', async () => {
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 880,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: {
              type: 'lesson_ai_result',
              eventId: 66,
              title: '本节课 AI 总结已生成',
              status: 'READY',
              contextType: 'COURSE',
              contextId: 66,
              content: '本节课重点完成一次函数图像与应用题梳理',
              reportStatus: 'READY',
            },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()

    expect(wrapper.text()).toContain('本节课 AI 总结已生成')
    expect(wrapper.text()).toContain('本节课重点完成一次函数图像与应用题梳理')

    await wrapper.get('.lesson-ai-card .btn').trigger('click')
    await flushPromises()

    expect(mocks.courseByRoom).toHaveBeenCalledWith(10)
  })

  it('shows the bound demand bar and opens demand detail', async () => {
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 901,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: {
              type: 'tutor_application',
              applicationId: 901,
              content: '想沟通初二数学',
              status: 'PENDING',
              creatorUserId: 3001,
              contextType: 'DEMAND',
              contextId: 99,
              teachingMode: 'OFFLINE',
            },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    await flushPromises()

    expect(mocks.getDemandView).toHaveBeenCalledWith(99)
    expect(wrapper.text()).toContain('当前沟通需求')
    expect(wrapper.text()).toContain('初二数学线下提分')
    expect(wrapper.text()).toContain('初中数学')

    await wrapper.get('.bound-demand-bar').trigger('click')
    await flushPromises()

    expect(wrapper.vm.$router.currentRoute.value.name).toBe('tutorJobDetail')
    expect(wrapper.vm.$router.currentRoute.value.params.id).toBe('99')
  })

  it('hides the bound demand after collaboration is accepted and removes end chat action', async () => {
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 911,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: {
              type: 'tutor_application',
              applicationId: 911,
              content: '初二数学',
              status: 'ACCEPTED',
              creatorUserId: 3001,
              contextType: 'DEMAND',
              contextId: 99,
              teachingMode: 'ONLINE',
            },
          },
        },
        {
          fromUser: { uid: 2001 },
          message: {
            id: 912,
            roomId: 10,
            sendTime: '2026-04-18T10:02:00',
            body: { type: 'contact_unlocked', proposalId: 3, orderId: 3, status: 'PAID' },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 913,
            roomId: 10,
            sendTime: '2026-04-18T10:03:00',
            body: { type: 'collaboration_status', proposalId: 3, status: 'ACCEPTED', actorUserId: 3001 },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    await flushPromises()

    expect(wrapper.find('.bound-demand-bar').exists()).toBe(false)
    expect(mocks.getDemandView).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('结束沟通')
  })

  it('binds a new demand posted after an accepted collaboration', async () => {
    mocks.getDemandView.mockResolvedValueOnce({
      id: 101,
      parentId: 2001,
      subjectId: 2,
      subjectName: '高中物理',
      subjectIsOther: 0,
      title: '高一物理线上同步',
      description: '同步巩固',
      studentGender: 'male',
      gradeCode: 'SENIOR1',
      availableTime: '',
      teacherGenderPreference: 'both',
      teacherRequirementDetail: '',
      childAge: 15,
      classMode: 'online',
      city: '上海',
      address: '',
      frequencyPerWeek: 1,
      budgetMin: '220',
      budgetMax: '300',
      stageCode: 'SENIOR',
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
      schedule: '',
      bizStatus: 1,
      status: 1,
      createTime: '2026-04-18T10:00:00',
      updateTime: '2026-04-18T10:00:00',
      publisher: {
        uid: 2001,
        displayName: '家长',
        avatar: null,
        identityLabel: '家长',
      },
    })
    mocks.listMessages.mockResolvedValueOnce({
      cursor: null,
      isLast: true,
      list: [
        {
          fromUser: { uid: 3001 },
          message: {
            id: 921,
            roomId: 10,
            sendTime: '2026-04-18T10:00:00',
            body: {
              type: 'tutor_application',
              applicationId: 921,
              content: '旧需求',
              status: 'ACCEPTED',
              creatorUserId: 3001,
              contextType: 'DEMAND',
              contextId: 99,
              teachingMode: 'OFFLINE',
            },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 922,
            roomId: 10,
            sendTime: '2026-04-18T10:03:00',
            body: { type: 'collaboration_status', proposalId: 4, status: 'ACCEPTED', actorUserId: 3001 },
          },
        },
        {
          fromUser: { uid: 3001 },
          message: {
            id: 923,
            roomId: 10,
            sendTime: '2026-04-18T10:10:00',
            body: {
              type: 'tutor_application',
              applicationId: 923,
              content: '新需求',
              status: 'PENDING',
              creatorUserId: 3001,
              contextType: 'DEMAND',
              contextId: 101,
              teachingMode: 'ONLINE',
            },
          },
        },
      ],
    })

    const { wrapper } = await mountChatRoomPage()
    await flushPromises()

    expect(mocks.getDemandView).toHaveBeenCalledTimes(1)
    expect(mocks.getDemandView).toHaveBeenCalledWith(101)
    expect(wrapper.text()).toContain('高一物理线上同步')
    expect(wrapper.text()).toContain('高中物理')
  })
})
