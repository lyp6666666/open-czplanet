import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import StudentOnboardingFirstDemandPage from './StudentOnboardingFirstDemandPage.vue'

const mocks = vi.hoisted(() => ({
  uploadImage: vi.fn(),
  updateUserInfo: vi.fn(),
  me: vi.fn(),
  createDemand: vi.fn(),
}))

vi.mock('@/api/assets', () => ({
  assetsApi: {
    uploadImage: mocks.uploadImage,
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    updateUserInfo: mocks.updateUserInfo,
    me: mocks.me,
  },
}))

vi.mock('@/api/jobs', () => ({
  jobsApi: {
    createDemand: mocks.createDemand,
  },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    isLoggedIn: true,
    refreshMe: mocks.me,
  }),
}))

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/auth/student', name: 'authStudent', component: { template: '<div>auth</div>' } },
      { path: '/student/post', name: 'studentPost', component: { template: '<div>post</div>' } },
      { path: '/student/onboarding/first-demand', name: 'studentOnboardingFirstDemand', component: { template: '<div>onboarding</div>' } },
      { path: '/student/jobs/mine', name: 'studentMineJobs', component: { template: '<div>mine</div>' } },
      { path: '/tutor/jobs', name: 'tutorJobs', component: { template: '<div>tutor</div>' } },
    ],
  })
}

function createStorageMock(): Storage {
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

describe('StudentOnboardingFirstDemandPage', () => {
  beforeEach(() => {
    const localStorageMock = createStorageMock()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.setItem('ai_tutor_city', '北京')
  })

  it('does not share completion state across accounts', async () => {
    localStorage.setItem('ai_tutor_student_first_demand_completed:1001', '1')
    localStorage.removeItem('ai_tutor_student_first_demand_completed:1002')
    mocks.uploadImage.mockReset()
    mocks.updateUserInfo.mockReset()
    mocks.me.mockReset()
    mocks.createDemand.mockReset()

    mocks.me.mockResolvedValue({ id: 1002, userType: 2, name: '', avatar: null })

    const router = createTestRouter()
    await router.push('/student/onboarding/first-demand')
    await router.isReady()

    mount(StudentOnboardingFirstDemandPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/student/onboarding/first-demand')
  })

  it('allows skipping at step2 and routes home', async () => {
    localStorage.removeItem('ai_tutor_student_first_demand_completed:1002')
    mocks.uploadImage.mockReset()
    mocks.updateUserInfo.mockReset()
    mocks.me.mockReset()
    mocks.createDemand.mockReset()

    mocks.me.mockResolvedValue({ id: 1002, userType: 2, name: '', avatar: null })
    mocks.uploadImage.mockResolvedValue({ objectKey: 'k', url: 'https://assets/1.png', contentType: 'image/png', size: 1 })
    mocks.updateUserInfo.mockResolvedValue('OK')

    const router = createTestRouter()
    await router.push('/student/onboarding/first-demand')
    await router.isReady()

    setActivePinia(createPinia())
    const wrapper = mount(StudentOnboardingFirstDemandPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    const file = new File(['x'], 'a.png', { type: 'image/png' })
    const fileInput = wrapper.find('input[type="file"]').element as HTMLInputElement
    Object.defineProperty(fileInput, 'files', { value: [file] })
    await wrapper.find('input[type="file"]').trigger('change')
    await flushPromises()

    await wrapper.find('input[placeholder="请输入姓名"]').setValue('小明')
    await wrapper.find('button.btn-primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('发布您的第一个需求')

    await wrapper.findAll('button').find((b) => b.text().trim() === '跳过')!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/')
    expect(mocks.createDemand).not.toHaveBeenCalled()
  })

  it('submits demand at step3', async () => {
    localStorage.removeItem('ai_tutor_student_first_demand_completed:1002')
    mocks.uploadImage.mockReset()
    mocks.updateUserInfo.mockReset()
    mocks.me.mockReset()
    mocks.createDemand.mockReset()

    mocks.me.mockResolvedValue({ id: 1002, userType: 2, name: '', avatar: null })
    mocks.uploadImage.mockResolvedValue({ objectKey: 'k', url: 'https://assets/1.png', contentType: 'image/png', size: 1 })
    mocks.updateUserInfo.mockResolvedValue('OK')
    mocks.createDemand.mockResolvedValue(3001)

    const router = createTestRouter()
    await router.push('/student/onboarding/first-demand')
    await router.isReady()

    const wrapper = mount(StudentOnboardingFirstDemandPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    const file = new File(['x'], 'a.png', { type: 'image/png' })
    const fileInput = wrapper.find('input[type="file"]').element as HTMLInputElement
    Object.defineProperty(fileInput, 'files', { value: [file] })
    await wrapper.find('input[type="file"]').trigger('change')
    await flushPromises()

    await wrapper.find('input[placeholder="请输入姓名"]').setValue('小明')
    await wrapper.find('button.btn-primary').trigger('click')
    await flushPromises()

    await wrapper.findAll('select')[0]!.setValue('male')
    await wrapper.findAll('select')[1]!.setValue('JUNIOR1')
    await wrapper.find('input[type="checkbox"][value="数学"]').setValue(true)
    await wrapper.findAll('select')[2]!.setValue('online')
    await wrapper.find('textarea').setValue('学生基础一般，需要巩固提升。')
    await wrapper.findAll('button').find((b) => b.text().trim() === '下一步')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('填写您对教师的要求')
    await wrapper.findAll('select')[0]!.setValue('female')
    await wrapper.find('textarea').setValue('希望老师有耐心，教学经验丰富。')
    await wrapper.find('input[type="number"]').setValue('100')
    await wrapper.findAll('button').find((b) => b.text().trim() === '发布需求')!.trigger('click')
    await flushPromises()

    expect(mocks.createDemand).toHaveBeenCalledTimes(1)
    const payload = mocks.createDemand.mock.calls[0]![0] as Record<string, unknown>
    expect(payload).toMatchObject({
      title: '初一数学家教',
      subjectName: '数学',
      subjectOther: false,
      gradeCode: 'JUNIOR1',
      stageCode: 'JUNIOR',
      classMode: 'online',
      studentGender: 'male',
      teacherGenderPreference: 'female',
      teacherRequirementDetail: '希望老师有耐心，教学经验丰富。',
      frequencyPerWeek: 2,
      budgetMin: 100,
      budgetMax: 100,
    })
    expect(router.currentRoute.value.name).toBe('studentMineJobs')
  })
})
