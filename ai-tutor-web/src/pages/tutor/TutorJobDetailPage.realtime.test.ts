import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import TutorJobDetailPage from './TutorJobDetailPage.vue'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const mocks = vi.hoisted(() => ({
  getDemandView: vi.fn(),
  checkDemandFavorites: vi.fn(),
  favoriteDemand: vi.fn(),
  unfavoriteDemand: vi.fn(),
  listSent: vi.fn(),
  applicationDetail: vi.fn(),
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    getDemandView: mocks.getDemandView,
  },
}))

vi.mock('@/api/favorites', () => ({
  favoritesApi: {
    checkDemandFavorites: mocks.checkDemandFavorites,
    favoriteDemand: mocks.favoriteDemand,
    unfavoriteDemand: mocks.unfavoriteDemand,
  },
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    listRooms: vi.fn(),
    listMessages: vi.fn(),
  },
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    listSent: mocks.listSent,
    detail: mocks.applicationDetail,
    startChat: vi.fn(),
  },
}))

function createRouterForPage() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/tutor/jobs/:id',
        name: 'tutorJobDetail',
        component: TutorJobDetailPage,
      },
      {
        path: '/chat/:roomId',
        name: 'chatRoom',
        component: { template: '<div />' },
      },
    ],
  })
}

describe('TutorJobDetailPage realtime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    mocks.getDemandView.mockReset()
    mocks.checkDemandFavorites.mockReset()
    mocks.favoriteDemand.mockReset()
    mocks.unfavoriteDemand.mockReset()
    mocks.listSent.mockReset()
    mocks.applicationDetail.mockReset()

    mocks.getDemandView.mockResolvedValue({
      id: 3001,
      parentId: 101,
      subjectId: 201,
      subjectName: null,
      subjectIsOther: 0,
      title: '初中数学一对一',
      description: '描述',
      childAge: 14,
      classMode: 'online',
      city: null,
      address: null,
      frequencyPerWeek: 2,
      budgetMin: '180',
      budgetMax: '240',
      stageCode: 'JUNIOR',
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
      schedule: null,
      status: 1,
      createTime: '',
      updateTime: '',
      publisher: { uid: 101, displayName: '林女士', avatar: null, identityLabel: '学生家长' },
    })
    mocks.checkDemandFavorites.mockResolvedValue([])
    mocks.listSent.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          id: 9527,
          senderUid: 2001,
          receiverUid: 101,
          senderRole: 'TEACHER',
          receiverRole: 'STUDENT',
          contextType: 'DEMAND',
          contextId: 3001,
          content: '申请内容',
          status: 'PENDING',
          chatAccessStatus: 'NONE',
          paymentPayerRole: 'TEACHER',
          orderId: null,
          roomId: 7001,
          receiverRead: false,
          decidedAt: null,
          createTime: '2026-04-17T00:00:00',
        },
      ],
    })
    mocks.applicationDetail.mockResolvedValue({
      id: 9527,
      senderUid: 2001,
      receiverUid: 101,
      senderRole: 'TEACHER',
      receiverRole: 'STUDENT',
      contextType: 'DEMAND',
      contextId: 3001,
      content: '申请内容',
      status: 'ACCEPTED',
      chatAccessStatus: 'CHAT_ENABLED',
      paymentPayerRole: 'TEACHER',
      orderId: 30001,
      roomId: 7001,
      receiverRead: true,
      decidedAt: '2026-04-17T00:10:00',
      createTime: '2026-04-17T00:00:00',
    })
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it('refreshes the current application when the global realtime store receives a matching application event', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouterForPage()
    await router.push('/tutor/jobs/3001')
    await router.isReady()

    const wrapper = mount(TutorJobDetailPage, {
      global: {
        plugins: [pinia, router],
        stubs: {
          UserCardModal: true,
          OrgCardModal: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('已发起申请')
    expect(mocks.applicationDetail).not.toHaveBeenCalled()

    const realtime = useChatRealtimeStore(pinia)
    realtime.consumeApplicationEvent({ applicationId: 9527, status: 'ACCEPTED' }, 'application.decided')
    await flushPromises()

    expect(mocks.applicationDetail).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('已发起沟通')

    wrapper.unmount()
  })
})
