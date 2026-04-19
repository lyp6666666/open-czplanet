import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AppTopBar from './AppTopBar.vue'
import { useAuthStore } from '@/stores/auth'

const myCoursesMock = vi.fn()
const getByCourseMock = vi.fn()

vi.mock('@/api/course', () => ({
  courseApi: {
    myCourses: (...args: unknown[]) => myCoursesMock(...args),
  },
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    getByCourse: (...args: unknown[]) => getByCourseMock(...args),
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    updateUserInfo: vi.fn(),
  },
}))

describe('AppTopBar live pill', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    myCoursesMock.mockReset()
    getByCourseMock.mockReset()
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: vi.fn(() => null),
        setItem: vi.fn(),
        removeItem: vi.fn(),
      },
      configurable: true,
    })
  })

  it('shows quick live entry when there is a joinable class', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.token = 'token'
    auth.user = { id: 1001, name: '张老师', phone: '13800000000', avatar: null, sex: null, userType: 1, token: 'token' }
    auth.me = { id: 1001, name: '张老师', phone: '13800000000', avatar: null, sex: null, userType: 1, teacherProfile: null, studentProfile: null, organizationProfile: null }

    myCoursesMock.mockResolvedValue([{ courseId: 66 }])
    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      joinableNow: true,
    })

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' } }, { path: '/live/prepare/:courseId', component: { template: '<div />' } }],
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(AppTopBar, {
      global: {
        plugins: [router, pinia],
        stubs: {
          BrandLogoMark: { template: '<div />' },
          CitySelectModal: { template: '<div />' },
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('进入课堂')
  })
})
