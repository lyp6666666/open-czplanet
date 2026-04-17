import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

import ApplicationCenterPage from './ApplicationCenterPage.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const mocks = vi.hoisted(() => ({
  unread: vi.fn(),
  listSent: vi.fn(),
  listReceived: vi.fn(),
  batch: vi.fn(),
  push: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.push,
  }),
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    unread: mocks.unread,
    listSent: mocks.listSent,
    listReceived: mocks.listReceived,
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    batch: mocks.batch,
  },
}))

function createStorage(): Storage {
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

describe('ApplicationCenterPage realtime', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    const sessionStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'sessionStorage', { value: sessionStorageMock, configurable: true })

    localStorage.setItem('ai_tutor_token', 'token')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 2001, token: 'token', userType: 1, name: '教师2001', phone: '13800138000' }),
    )

    mocks.push.mockReset()
    mocks.unread.mockReset()
    mocks.listSent.mockReset()
    mocks.listReceived.mockReset()
    mocks.batch.mockReset()

    mocks.unread.mockResolvedValue({ unreadCount: 1 })
    mocks.listReceived.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [
        {
          id: 1,
          senderUid: 3001,
          receiverUid: 2001,
          senderRole: 'STUDENT',
          receiverRole: 'TEACHER',
          contextType: 'DEMAND',
          contextId: 99,
          content: '申请内容',
          status: 'PENDING',
          chatAccessStatus: 'NONE',
          paymentPayerRole: 'TEACHER',
          orderId: null,
          roomId: null,
          receiverRead: false,
          decidedAt: null,
          createTime: '2026-04-17T00:00:00',
        },
      ],
    })
    mocks.listSent.mockResolvedValue({
      cursor: null,
      isLast: true,
      list: [],
    })
    mocks.batch.mockResolvedValue([{ id: 3001, name: '学生3001', avatar: '' }])
  })

  it('reloads unread and current tab when receiving a global application realtime event', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore(pinia)

    const wrapper = mount(ApplicationCenterPage, {
      global: {
        plugins: [pinia],
      },
    })
    await flushPromises()

    expect(mocks.unread).toHaveBeenCalledTimes(1)
    expect(mocks.listReceived).toHaveBeenCalledTimes(1)

    const realtime = useChatRealtimeStore(pinia)
    realtime.consumeApplicationEvent({ applicationId: 9527, status: 'ACCEPTED' }, 'application.decided')
    await flushPromises()

    expect(mocks.unread).toHaveBeenCalledTimes(2)
    expect(mocks.listReceived).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('学生3001')
  })
})
