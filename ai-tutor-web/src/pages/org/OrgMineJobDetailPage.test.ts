import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import OrgMineJobDetailPage from './OrgMineJobDetailPage.vue'

const mocks = vi.hoisted(() => ({
  getDemandView: vi.fn(),
  updateOrgDemand: vi.fn(),
  toastShow: vi.fn(),
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    getDemandView: mocks.getDemandView,
    updateOrgDemand: mocks.updateOrgDemand,
  },
}))

vi.mock('@/stores/toast', () => ({
  useToastStore: () => ({ show: mocks.toastShow }),
}))

function createRouterForPage() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/org/jobs/:id', name: 'orgMineJobDetail', component: OrgMineJobDetailPage },
      { path: '/org/jobs/:id/edit', name: 'orgEditJob', component: { template: '<div />' } },
    ],
  })
}

describe('OrgMineJobDetailPage', () => {
  beforeEach(() => {
    mocks.getDemandView.mockReset()
    mocks.updateOrgDemand.mockReset()
    mocks.toastShow.mockReset()
  })

  it('已关闭机构需求可重新公开', async () => {
    mocks.getDemandView
      .mockResolvedValueOnce({
        id: 6001,
        parentId: 301,
        title: '机构单｜高一英语提升',
        description: '描述',
        classMode: 'offline',
        city: '北京',
        address: '朝阳区',
        frequencyPerWeek: 2,
        budgetMin: '200',
        budgetMax: '260',
        educationRequirement: 'BACHELOR',
        publisherIdentity: 'ORGANIZATION',
        status: 0,
        bizStatus: 6,
        createTime: '',
        updateTime: '',
        schedule: null,
        publisher: { uid: 301, displayName: '示例机构', avatar: null, identityLabel: '家教机构' },
      })
      .mockResolvedValueOnce({
        id: 6001,
        parentId: 301,
        title: '机构单｜高一英语提升',
        description: '描述',
        classMode: 'offline',
        city: '北京',
        address: '朝阳区',
        frequencyPerWeek: 2,
        budgetMin: '200',
        budgetMax: '260',
        educationRequirement: 'BACHELOR',
        publisherIdentity: 'ORGANIZATION',
        status: 1,
        bizStatus: 1,
        createTime: '',
        updateTime: '',
        schedule: null,
        publisher: { uid: 301, displayName: '示例机构', avatar: null, identityLabel: '家教机构' },
      })
    mocks.updateOrgDemand.mockResolvedValue('ok')

    const router = createRouterForPage()
    await router.push('/org/jobs/6001')
    await router.isReady()

    const wrapper = mount(OrgMineJobDetailPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    const reopenBtn = wrapper.findAll('button').find((b) => b.text().includes('重新公开'))
    expect(reopenBtn).toBeTruthy()
    await reopenBtn!.trigger('click')
    await flushPromises()

    expect(mocks.updateOrgDemand).toHaveBeenCalledWith(6001, { status: 1 })
    expect(mocks.toastShow).toHaveBeenCalledWith('需求已重新公开', 'success')
  })
})
