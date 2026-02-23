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

    await wrapper.find('select').setValue('male')
    await wrapper.findAll('select')[1]!.setValue('JUNIOR1')

    await wrapper.findAll('select')[2]!.setValue('offline')

    await wrapper.findAll('button').find((b) => b.text().trim() === '发布')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('上门辅导必须填写城市与上课地址')
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

    await wrapper.findAll('select')[1]!.setValue('JUNIOR1')
    await wrapper.findAll('select')[2]!.setValue('online')
    await wrapper.find('textarea').setValue('希望老师重点讲解函数与几何。')

    await wrapper.findAll('button').find((b) => b.text().trim() === '发布')!.trigger('click')
    await flushPromises()

    expect(mocks.createDemand).toHaveBeenCalledTimes(1)
    const payload = mocks.createDemand.mock.calls[0]![0] as Record<string, unknown>
    expect(payload).toMatchObject({
      subjectId: 201,
      title: '初中 / 初中数学',
      description: '希望老师重点讲解函数与几何。',
      classMode: 'online',
      gradeCode: 'JUNIOR1',
      stageCode: 'JUNIOR',
      teacherGenderPreference: 'both',
    })
  })
})
