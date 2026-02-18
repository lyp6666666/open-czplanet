import { describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import StudentPostPage from './StudentPostPage.vue'

const mocks = vi.hoisted(() => ({
  createDemand: vi.fn(),
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    createDemand: mocks.createDemand,
  },
}))

vi.mock('@/api/homeGuest', () => ({
  homeGuestApi: {
    getSubjectTree: vi.fn().mockResolvedValue([
      { id: 200, name: '初中', children: [{ id: 201, name: '初中数学', children: [] }] },
    ]),
  },
}))

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/student/post', name: 'studentPost', component: { template: '<div />' } },
      { path: '/student/jobs/mine', name: 'studentMineJobs', component: { template: '<div />' } },
    ],
  })
}

describe('StudentPostPage', () => {
  it('blocks publish when offline without address', async () => {
    mocks.createDemand.mockReset()
    const router = createTestRouter()
    await router.push('/student/post')
    await router.isReady()

    const wrapper = mount(StudentPostPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.find('input[placeholder="例如：小学三年级数学家教"]').setValue('初中数学一对一')
    await wrapper.find('textarea').setValue('希望老师能带着建立解题框架。')

    const selects = wrapper.findAll('select')
    await selects[1]!.setValue('offline')

    await wrapper.findAll('button').find((b) => b.text().trim() === '发布')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('线下授课必须填写城市与授课地址')
    expect(mocks.createDemand).not.toHaveBeenCalled()
  })

  it('submits required fields and calls createDemand', async () => {
    mocks.createDemand.mockReset()
    mocks.createDemand.mockResolvedValue(3001)

    const router = createTestRouter()
    await router.push('/student/post')
    await router.isReady()

    const wrapper = mount(StudentPostPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.find('input[placeholder="例如：小学三年级数学家教"]').setValue('初中数学一对一')
    await wrapper.find('textarea').setValue('希望老师重点讲解函数与几何。')

    await wrapper.findAll('button').find((b) => b.text().trim() === '发布')!.trigger('click')
    await flushPromises()

    expect(mocks.createDemand).toHaveBeenCalledTimes(1)
    const payload = mocks.createDemand.mock.calls[0]![0] as Record<string, unknown>
    expect(payload).toMatchObject({
      subjectId: 200,
      title: '初中数学一对一',
      description: '希望老师重点讲解函数与几何。',
      classMode: 'online',
      frequencyPerWeek: 2,
      stageCode: 'PRIMARY',
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
    })
  })
})
