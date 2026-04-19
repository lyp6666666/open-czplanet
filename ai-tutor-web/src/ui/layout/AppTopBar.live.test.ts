import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AppTopBar from './AppTopBar.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const remindersMock = vi.fn()

vi.mock('@/api/live', () => ({
  liveApi: {
    reminders: (...args: unknown[]) => remindersMock(...args),
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
    vi.useFakeTimers()
    remindersMock.mockReset()
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: vi.fn(() => null),
        setItem: vi.fn(),
        removeItem: vi.fn(),
      },
      configurable: true,
    })
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it('shows quick live entry when there is a joinable class', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.token = 'token'
    auth.user = { id: 1001, name: '张老师', phone: '13800000000', avatar: null, sex: null, userType: 1, token: 'token' }
    auth.me = { id: 1001, name: '张老师', phone: '13800000000', avatar: null, sex: null, userType: 1, teacherProfile: null, studentProfile: null, organizationProfile: null }

    remindersMock.mockResolvedValue([
      {
      sessionId: 8,
      courseId: 66,
      title: '课程 #66',
      joinableNow: true,
      canJoin: true,
      status: 'JOIN_OPEN',
      scheduledStartAt: new Date(Date.now() + 5 * 60_000).toISOString(),
    },
    ])

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
    expect(wrapper.text()).toContain('进行中')
  })

  it('shows upcoming class reminder modal and stores toast message', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.token = 'token'
    auth.user = { id: 1001, name: '张老师', phone: '13800000000', avatar: null, sex: null, userType: 1, token: 'token' }
    auth.me = { id: 1001, name: '张老师', phone: '13800000000', avatar: null, sex: null, userType: 1, teacherProfile: null, studentProfile: null, organizationProfile: null }

    const startAt = new Date(Date.now() + 8 * 60_000).toISOString()
    remindersMock.mockResolvedValue([
      {
      sessionId: 9,
      courseId: 77,
      title: '课程 #77',
      joinableNow: false,
      canJoin: true,
      status: 'CREATED',
      scheduledStartAt: startAt,
    },
    ])

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

    const toast = useToastStore()
    expect(wrapper.text()).toContain('即将开始')
    expect(wrapper.text()).toContain('实时课堂提醒')
    expect(toast.message).toContain('课程提醒')
  })
})
