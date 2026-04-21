import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import StudentMineJobDetailPage from './StudentMineJobDetailPage.vue'

const mocks = vi.hoisted(() => ({
  getDemandView: vi.fn(),
  updateDemand: vi.fn(),
  toastShow: vi.fn(),
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    getDemandView: mocks.getDemandView,
    updateDemand: mocks.updateDemand,
  },
}))

vi.mock('@/stores/toast', () => ({
  useToastStore: () => ({ show: mocks.toastShow }),
}))

function createRouterForPage() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/student/jobs/:id', name: 'studentMineJobDetail', component: StudentMineJobDetailPage },
      { path: '/student/jobs/:id/edit', name: 'studentEditJob', component: { template: '<div />' } },
    ],
  })
}

describe('StudentMineJobDetailPage', () => {
  beforeEach(() => {
    mocks.getDemandView.mockReset()
    mocks.updateDemand.mockReset()
    mocks.toastShow.mockReset()
  })

  it('已关闭需求可重新公开', async () => {
    mocks.getDemandView
      .mockResolvedValueOnce({
        id: 5001,
        parentId: 101,
        title: '初中数学一对一',
        description: '描述',
        classMode: 'online',
        city: null,
        address: null,
        frequencyPerWeek: 2,
        budgetMin: '180',
        budgetMax: '220',
        educationRequirement: 'UNLIMITED',
        publisherIdentity: 'PARENT',
        status: 0,
        bizStatus: 6,
        createTime: '',
        updateTime: '',
        schedule: null,
        publisher: { uid: 101, displayName: '林女士', avatar: null, identityLabel: '学生家长' },
      })
      .mockResolvedValueOnce({
        id: 5001,
        parentId: 101,
        title: '初中数学一对一',
        description: '描述',
        classMode: 'online',
        city: null,
        address: null,
        frequencyPerWeek: 2,
        budgetMin: '180',
        budgetMax: '220',
        educationRequirement: 'UNLIMITED',
        publisherIdentity: 'PARENT',
        status: 1,
        bizStatus: 1,
        createTime: '',
        updateTime: '',
        schedule: null,
        publisher: { uid: 101, displayName: '林女士', avatar: null, identityLabel: '学生家长' },
      })
    mocks.updateDemand.mockResolvedValue('ok')

    const router = createRouterForPage()
    await router.push('/student/jobs/5001')
    await router.isReady()

    const wrapper = mount(StudentMineJobDetailPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    const reopenBtn = wrapper.findAll('button').find((b) => b.text().includes('重新公开'))
    expect(reopenBtn).toBeTruthy()
    await reopenBtn!.trigger('click')
    await flushPromises()

    expect(mocks.updateDemand).toHaveBeenCalledWith(5001, { status: 1 })
    expect(mocks.toastShow).toHaveBeenCalledWith('需求已重新公开', 'success')
  })
})
