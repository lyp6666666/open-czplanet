import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import AuthPage from './AuthPage.vue'

const mocks = vi.hoisted(() => ({
  sendCode: vi.fn(),
  loginOrRegister: vi.fn(),
  me: vi.fn(),
  batch: vi.fn(),
}))

vi.mock('@/api/user', () => ({
  userApi: {
    sendCode: mocks.sendCode,
    loginOrRegister: mocks.loginOrRegister,
    me: mocks.me,
    batch: mocks.batch,
  },
}))

function createStorage() {
  const store = new Map<string, string>()
  return {
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    setItem(key: string, value: string) {
      store.set(key, String(value))
    },
    removeItem(key: string) {
      store.delete(key)
    },
    clear() {
      store.clear()
    },
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/auth/student',
        name: 'authStudent',
        component: AuthPage,
        props: { role: 'STUDENT' },
      },
    ],
  })
}

describe('AuthPage invite code prefill', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
  })

  it('prefills invite code from route query', async () => {
    const router = createTestRouter()
    await router.push('/auth/student?inviteCode=abC123')
    await router.isReady()

    const wrapper = mount(AuthPage, {
      props: { role: 'STUDENT' },
      global: {
        plugins: [createPinia(), router],
      },
    })

    await flushPromises()

    const inviteInput = wrapper.find('input[placeholder="请输入邀请码（选填）"]')
    expect((inviteInput.element as HTMLInputElement).value).toBe('ABC123')
  })
})
