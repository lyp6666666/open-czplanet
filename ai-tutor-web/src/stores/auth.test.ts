import { beforeEach, describe, expect, it } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'

import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
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
})

