import axios from 'axios'
import type { AxiosError, AxiosResponse } from 'axios'

import type { BaseResponse } from './types'

const STORAGE_TOKEN_KEY = 'ai_tutor_token'
const STORAGE_USER_KEY = 'ai_tutor_user'

const AUTH_INVALID_CODES = new Set([40100])

let authInvalidHandler: ((reason: string) => void) | null = null
let lastAuthInvalidAt = 0

export function setAuthInvalidHandler(handler: ((reason: string) => void) | null) {
  authInvalidHandler = handler
  lastAuthInvalidAt = 0
}

export function notifyAuthInvalid(reason: string) {
  const now = Date.now()
  if (now - lastAuthInvalidAt < 800) return
  lastAuthInvalidAt = now

  if (authInvalidHandler) {
    authInvalidHandler(reason)
    return
  }

  let userType: number | null = null
  try {
    const rawUser = localStorage.getItem(STORAGE_USER_KEY)
    if (rawUser) {
      const u = JSON.parse(rawUser) as { userType?: unknown }
      const v = typeof u.userType === 'number' ? u.userType : Number(u.userType)
      userType = Number.isFinite(v) ? v : null
    }
  } catch {
    userType = null
  }

  localStorage.removeItem(STORAGE_TOKEN_KEY)
  localStorage.removeItem(STORAGE_USER_KEY)
  if (window.location.hash.startsWith('#/auth/')) return
  if (userType === 3) {
    window.location.hash = '#/auth/org'
    return
  }
  if (userType === 1) {
    window.location.hash = '#/auth/tutor'
    return
  }
  window.location.hash = '#/auth/student'
}

function getBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL
  if (typeof configured === 'string' && configured.trim().length > 0) {
    return configured.trim()
  }
  if (import.meta.env.MODE === 'test') {
    return 'http://localhost'
  }
  return ''
}

export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

export const http = axios.create({
  baseURL: getBaseUrl(),
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  // 统一在请求头补齐登录态，避免业务代码到处拼接 Authorization
  const token = localStorage.getItem(STORAGE_TOKEN_KEY)
  if (typeof token === 'string' && token.trim().length > 0) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token.trim()}`
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as BaseResponse<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        if (AUTH_INVALID_CODES.has(body.code)) {
          notifyAuthInvalid(`api_code_${body.code}`)
        }
        throw new ApiError(body.code, body.message || 'Request failed')
      }
      return body.data
    }
    return response.data
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const data = error.response?.data as BaseResponse<unknown> | undefined
    const code = data && typeof data.code === 'number' ? data.code : null
    if (status === 401) {
      notifyAuthInvalid(`http_${status}`)
    } else if (status === 403) {
      let userType: number | null = null
      try {
        const rawUser = localStorage.getItem(STORAGE_USER_KEY)
        if (rawUser) {
          const u = JSON.parse(rawUser) as { userType?: unknown }
          const v = typeof u.userType === 'number' ? u.userType : Number(u.userType)
          userType = Number.isFinite(v) ? v : null
        }
      } catch {
        userType = null
      }
      if (userType !== 3) {
        notifyAuthInvalid(`http_${status}`)
      }
    } else if (code != null && AUTH_INVALID_CODES.has(code)) {
      notifyAuthInvalid(`api_code_${code}`)
    }
    return Promise.reject(error)
  },
)
