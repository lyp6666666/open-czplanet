import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import App from './App.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

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

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/auth/student', name: 'authStudent', component: { template: '<div>auth</div>' } },
      { path: '/tutor/jobs', name: 'tutorJobs', component: { template: '<div>jobs</div>' } },
      { path: '/chat/:roomId', name: 'chatRoom', component: { template: '<div>chat</div>' } },
    ],
  })
}

describe('App realtime lifecycle', () => {
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
  })

  it('starts realtime from the app root and stops it when logging out', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore(pinia)
    const realtime = useChatRealtimeStore(pinia)
    const router = createTestRouter()
    await router.push('/tutor/jobs')
    await router.isReady()

    const startSpy = vi.spyOn(realtime, 'start').mockResolvedValue()
    const stopSpy = vi.spyOn(realtime, 'stop')
    const refreshSpy = vi.spyOn(realtime, 'refreshUnreadFromServer').mockResolvedValue()
    const resetSpy = vi.spyOn(realtime, 'resetState')

    mount(App, {
      global: {
        plugins: [pinia, router],
        stubs: {
          AppTopBar: { template: '<div data-test="topbar" />' },
          Toast: { template: '<div data-test="toast" />' },
        },
      },
    })
    await flushPromises()

    expect(startSpy).toHaveBeenCalledTimes(1)
    expect(refreshSpy).toHaveBeenCalledTimes(1)

    auth.logout()
    await flushPromises()

    expect(stopSpy).toHaveBeenCalled()
    expect(resetSpy).toHaveBeenCalled()
  })
})
