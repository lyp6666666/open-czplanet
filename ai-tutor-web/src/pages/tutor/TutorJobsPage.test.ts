import { describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import TutorJobsPage from './TutorJobsPage.vue'

const mocks = vi.hoisted(() => ({
  feedDemands: vi.fn(),
  getDemandView: vi.fn(),
  checkDemandFavorites: vi.fn(),
  listSent: vi.fn(),
  applicationDetail: vi.fn(),
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    feedDemands: mocks.feedDemands,
    getDemandView: mocks.getDemandView,
  },
}))

vi.mock('@/api/favorites', () => ({
  favoritesApi: {
    checkDemandFavorites: mocks.checkDemandFavorites,
    favoriteDemand: vi.fn(),
    unfavoriteDemand: vi.fn(),
  },
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    getOrCreateRoom: vi.fn(),
    startRoom: vi.fn(),
  },
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    listSent: mocks.listSent,
    detail: mocks.applicationDetail,
    startChat: vi.fn(),
  },
}))

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [{ path: '/tutor/jobs', name: 'tutorJobs', component: { template: '<div />' } }],
  })
}

describe('TutorJobsPage', () => {
  it('applies budget filter when clicking search without confirming budget menu', async () => {
    localStorage.setItem('ai_tutor_city', '北京')
    mocks.feedDemands.mockReset()
    mocks.getDemandView.mockReset()
    mocks.checkDemandFavorites.mockReset()
    mocks.listSent.mockReset()

    mocks.feedDemands.mockResolvedValue({
      nextCursor: null,
      isLast: true,
      list: [],
    })
    mocks.checkDemandFavorites.mockResolvedValue([])
    mocks.listSent.mockResolvedValue({ cursor: null, isLast: true, list: [] })

    const router = createTestRouter()
    await router.push('/tutor/jobs')
    await router.isReady()

    const wrapper = mount(TutorJobsPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('薪资待遇'))!.trigger('click')
    const inputs = wrapper.findAll('input')
    await inputs.find((i) => i.attributes('placeholder') === '下限')!.setValue('120')
    await inputs.find((i) => i.attributes('placeholder') === '上限')!.setValue('180')

    await wrapper.findAll('button').find((b) => b.text().trim() === '搜索')!.trigger('click')
    await flushPromises()

    const lastCall = mocks.feedDemands.mock.calls[mocks.feedDemands.mock.calls.length - 1]![0] as Record<string, unknown>
    expect(lastCall).toMatchObject({ budgetMin: 120, budgetMax: 180 })
  })

  it('renders demand view without address block when online', async () => {
    localStorage.setItem('ai_tutor_city', '北京')
    mocks.feedDemands.mockReset()
    mocks.getDemandView.mockReset()
    mocks.checkDemandFavorites.mockReset()
    mocks.listSent.mockReset()

    mocks.feedDemands.mockResolvedValue({
      nextCursor: null,
      isLast: true,
      list: [
        {
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
        },
      ],
    })
    mocks.checkDemandFavorites.mockResolvedValue([])
    mocks.listSent.mockResolvedValue({ cursor: null, isLast: true, list: [] })
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

    const router = createTestRouter()
    await router.push('/tutor/jobs')
    await router.isReady()

    const wrapper = mount(TutorJobsPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('初中数学一对一')
    expect(wrapper.text()).toContain('每周2次')
    expect(wrapper.text()).toContain('不限')
    expect(wrapper.text()).toContain('林女士')
    expect(wrapper.text()).toContain('学生家长')
    expect(wrapper.text()).not.toContain('工作地址')
  })

  it('renders address block when offline', async () => {
    localStorage.setItem('ai_tutor_city', '北京')
    mocks.feedDemands.mockReset()
    mocks.getDemandView.mockReset()
    mocks.checkDemandFavorites.mockReset()

    mocks.feedDemands.mockResolvedValue({
      nextCursor: null,
      isLast: true,
      list: [
        {
          id: 3002,
          parentId: 102,
          subjectId: 201,
          subjectName: null,
          subjectIsOther: 0,
          title: '小学语文阅读写作提升',
          description: '描述',
          childAge: 10,
          classMode: 'offline',
          city: '北京',
          address: '海淀区中关村',
          frequencyPerWeek: 3,
          budgetMin: '120',
          budgetMax: '180',
          stageCode: 'PRIMARY',
          educationRequirement: 'BACHELOR',
          publisherIdentity: 'PARENT',
          schedule: null,
          status: 1,
          createTime: '',
          updateTime: '',
        },
      ],
    })
    mocks.checkDemandFavorites.mockResolvedValue([])
    mocks.getDemandView.mockResolvedValue({
      id: 3002,
      parentId: 102,
      subjectId: 201,
      subjectName: null,
      subjectIsOther: 0,
      title: '小学语文阅读写作提升',
      description: '描述',
      childAge: 10,
      classMode: 'offline',
      city: '北京',
      address: '海淀区中关村',
      frequencyPerWeek: 3,
      budgetMin: '120',
      budgetMax: '180',
      stageCode: 'PRIMARY',
      educationRequirement: 'BACHELOR',
      publisherIdentity: 'PARENT',
      schedule: null,
      status: 1,
      createTime: '',
      updateTime: '',
      publisher: { uid: 102, displayName: '王先生', avatar: null, identityLabel: '学生家长' },
    })

    const router = createTestRouter()
    await router.push('/tutor/jobs')
    await router.isReady()

    const wrapper = mount(TutorJobsPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('工作地址')
    expect(wrapper.text()).toContain('北京')
    expect(wrapper.text()).toContain('海淀区中关村')
  })
})
