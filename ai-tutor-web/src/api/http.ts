import axios from 'axios'
import type { AxiosError, AxiosResponse } from 'axios'

import type { BaseResponse } from './types'

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
