import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import type { AdminLoginResponse } from '@/api/types'

const STORAGE_TOKEN_KEY = 'ai_tutor_admin_token'
const STORAGE_USER_KEY = 'ai_tutor_admin_user'

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const token = ref<string | null>(null)
  const user = ref<AdminLoginResponse | null>(null)

  function loadFromStorage() {
    const rawToken = localStorage.getItem(STORAGE_TOKEN_KEY)
    token.value = typeof rawToken === 'string' && rawToken.trim() ? rawToken.trim() : null

    const rawUser = localStorage.getItem(STORAGE_USER_KEY)
    if (!rawUser) {
      user.value = null
      return
    }
    try {
      user.value = JSON.parse(rawUser) as AdminLoginResponse
    } catch {
      user.value = null
    }
  }

  function setAuth(payload: AdminLoginResponse) {
    token.value = payload.token
    user.value = payload
    localStorage.setItem(STORAGE_TOKEN_KEY, payload.token)
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(payload))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem(STORAGE_TOKEN_KEY)
    localStorage.removeItem(STORAGE_USER_KEY)
  }

  const isAuthed = computed(() => !!token.value)

  return {
    token,
    user,
    isAuthed,
    loadFromStorage,
    setAuth,
    logout,
  }
})

