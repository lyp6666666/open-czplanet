import axios from 'axios'
import type { AxiosError, AxiosResponse } from 'axios'

import type { BaseResponse } from './types'

const STORAGE_TOKEN_KEY = 'ai_tutor_token'

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
        throw new ApiError(body.code, body.message || 'Request failed')
      }
      return body.data
    }
    return response.data
  },
  (error: AxiosError) => Promise.reject(error),
)
