import { beforeEach, describe, expect, it } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'
import { HttpResponse, http } from 'msw'

import { setAuthInvalidHandler } from '@/api/http'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { server } from '@/test/server'

describe('http auth invalid handling', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    setAuthInvalidHandler(null)
  })

  it('logs out when backend returns NOT_LOGIN_ERROR', async () => {
    localStorage.setItem('ai_tutor_token', 'mock.token')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 1001, name: '教师0000', phone: '13800138000', avatar: '', sex: null, userType: 1, token: 'mock.token' }),
    )
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(true)

    setAuthInvalidHandler(() => {
      auth.logout()
    })

    server.use(
      http.get('http://localhost/user/me', () => HttpResponse.json({ code: 40100, data: null, message: '未登录' })),
    )

    await expect(userApi.me()).rejects.toBeTruthy()

    expect(auth.isLoggedIn).toBe(false)
    expect(localStorage.getItem('ai_tutor_token')).toBeNull()
    expect(localStorage.getItem('ai_tutor_user')).toBeNull()
  })
})

