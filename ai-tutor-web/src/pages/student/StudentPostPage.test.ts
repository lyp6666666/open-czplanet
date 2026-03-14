import { describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import StudentPostPage from './StudentPostPage.vue'

const mocks = vi.hoisted(() => ({
  createDemand: vi.fn(),
  toastShow: vi.fn(),
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    createDemand: mocks.createDemand,
  },
}))

vi.mock('@/stores/toast', () => ({
  useToastStore: () => ({ show: mocks.toastShow }),
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
    mocks.toastShow.mockReset()
    const router = createTestRouter()
    await router.push('/student/post')
    await router.isReady()

    const wrapper = mount(StudentPostPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.find('select').setValue('male')
    await wrapper.findAll('select')[1]!.setValue('JUNIOR1')
    await wrapper.find('input[type="checkbox"][value="数学"]').setValue(true)
    await wrapper.findAll('select')[2]!.setValue('offline')
    await wrapper.findAll('select')[2]!.setValue('offline')

    await wrapper.findAll('textarea')[0]!.setValue('孩子基础一般，希望补习巩固。')

    await wrapper.findAll('button').find((b) => b.text().trim() === '发布')!.trigger('click')
    await flushPromises()

    expect(mocks.toastShow).toHaveBeenCalledWith('上门辅导必须填写城市与上课地址', 'error')
    expect(mocks.createDemand).not.toHaveBeenCalled()
  })

  it('submits required fields and calls createDemand', async () => {
    mocks.createDemand.mockReset()
    mocks.toastShow.mockReset()
    mocks.createDemand.mockResolvedValue(3001)

    const router = createTestRouter()
    await router.push('/student/post')
    await router.isReady()

    const wrapper = mount(StudentPostPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.findAll('select')[0]!.setValue('male')
    await wrapper.findAll('select')[1]!.setValue('JUNIOR1')
    await wrapper.find('input[type="checkbox"][value="数学"]').setValue(true)
    await wrapper.findAll('select')[2]!.setValue('online')
    await wrapper.findAll('textarea')[0]!.setValue('希望老师重点讲解函数与几何。')
    await wrapper.findAll('textarea')[1]!.setValue('希望老师有耐心，教学经验丰富。')
    await wrapper.find('input[type="number"]').setValue('100')

    await wrapper.findAll('button').find((b) => b.text().trim() === '发布')!.trigger('click')
    await flushPromises()

    expect(mocks.createDemand).toHaveBeenCalledTimes(1)
    const payload = mocks.createDemand.mock.calls[0]![0] as Record<string, unknown>
    expect(payload).toMatchObject({
      title: '初一数学家教',
      subjectName: '数学',
      subjectOther: false,
      description: '希望老师重点讲解函数与几何。',
      classMode: 'online',
      studentGender: 'male',
      gradeCode: 'JUNIOR1',
      stageCode: 'JUNIOR',
      teacherGenderPreference: 'both',
      frequencyPerWeek: 2,
      budgetMin: 100,
      budgetMax: 100,
    })
  })
})
