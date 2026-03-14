import { defineStore } from 'pinia'

import { userApi } from '@/api/user'
import type { LoginUserVO, UserMeVO, UserRoleEnum } from '@/api/types'

const STORAGE_TOKEN_KEY = 'ai_tutor_token'
const STORAGE_USER_KEY = 'ai_tutor_user'
const STORAGE_TUTOR_BASIC_COMPLETED_KEY = 'ai_tutor_tutor_basic_completed'

function readTokenFromStorage(): string | null {
  const raw = localStorage.getItem(STORAGE_TOKEN_KEY)
  const token = typeof raw === 'string' ? raw.trim() : ''
  return token.length > 0 ? token : null
}

function readUserFromStorage(): LoginUserVO | null {
  const raw = localStorage.getItem(STORAGE_USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as LoginUserVO
  } catch {
    return null
  }
}

function persistAuth(user: LoginUserVO) {
  localStorage.setItem(STORAGE_TOKEN_KEY, user.token)
  localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(user))
}

function clearPersistedAuth() {
  localStorage.removeItem(STORAGE_TOKEN_KEY)
  localStorage.removeItem(STORAGE_USER_KEY)
  localStorage.removeItem(STORAGE_TUTOR_BASIC_COMPLETED_KEY)
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const token = readTokenFromStorage()
    const user = readUserFromStorage()
    return {
      token: token ?? user?.token ?? null,
      user: user as LoginUserVO | null,
      me: null as UserMeVO | null,
    }
  },
  getters: {
    isLoggedIn: (s) => typeof s.token === 'string' && s.token.length > 0,
    role: (s) => (s.user?.userType === 1 ? 'TEACHER' : s.user?.userType === 2 ? 'STUDENT' : s.user?.userType === 3 ? 'ORG' : null),
  },
  actions: {
    async sendCode(phone: string) {
      return userApi.sendCode(phone)
    },

    async loginOrRegister(role: UserRoleEnum, phone: string, code: string) {
      const user = await userApi.loginOrRegister({ userRoleEnum: role, phone, code })
      this.token = user.token
      this.user = user
      this.me = null
      persistAuth(user)
      return user
    },

    async loginOrg(username: string, password: string) {
      const org = await userApi.orgLogin({ username, password })
      const user: LoginUserVO = {
        id: org.id,
        name: org.name,
        phone: org.organizationProfile?.contactPhone ?? '',
        avatar: null,
        sex: null,
        userType: org.userType,
        token: org.token,
      }
      this.token = user.token
      this.user = user
      this.me = null
      persistAuth(user)
      return org
    },

    async refreshMe() {
      if (!this.isLoggedIn) return null
      const me = await userApi.me()
      this.me = me
      if (this.user) {
        const merged = { ...this.user, name: me.name, phone: me.phone, avatar: me.avatar, sex: me.sex, userType: me.userType }
        this.user = merged
        persistAuth(merged)
      }
      if (me?.userType === 1) {
        const completed = !!(me.avatar && me.teacherProfile?.realName?.trim())
        localStorage.setItem(STORAGE_TUTOR_BASIC_COMPLETED_KEY, completed ? '1' : '0')
      }
      return me
    },

    logout() {
      this.token = null
      this.user = null
      this.me = null
      clearPersistedAuth()
    },
  },
})

export function getStoredToken(): string | null {
  return readTokenFromStorage()
}
