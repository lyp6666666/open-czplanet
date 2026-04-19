import { beforeEach, describe, expect, it } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'

import { useAuthStore } from './auth'

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

describe('auth store', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('logs in and persists token/user', async () => {
    const auth = useAuthStore()

    // 通过 MSW mock 的后端接口完成登录闭环（验证码校验由后端负责，这里只验证前端行为）
    const user = await auth.loginOrRegister('TEACHER', '13800138000', '1234')

    expect(user.userType).toBe(1)
    expect(auth.isLoggedIn).toBe(true)
    expect(localStorage.getItem('ai_tutor_token')).toBeTruthy()
    expect(localStorage.getItem('ai_tutor_user')).toBeTruthy()
  })

  it('logs out and clears persisted auth', async () => {
    const auth = useAuthStore()
    await auth.loginOrRegister('STUDENT', '13800138001', '1234')

    auth.logout()

    expect(auth.isLoggedIn).toBe(false)
    expect(localStorage.getItem('ai_tutor_token')).toBeNull()
    expect(localStorage.getItem('ai_tutor_user')).toBeNull()
  })

  it('passes invite code during login or register request', async () => {
    const auth = useAuthStore()

    const user = await auth.loginOrRegister('STUDENT', '13800138002', '1234', 'abc123')

    expect(user.userType).toBe(2)
    expect(auth.isLoggedIn).toBe(true)
  })
})
