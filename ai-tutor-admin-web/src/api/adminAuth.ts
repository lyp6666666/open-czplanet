import { http } from './http'
import type { AdminLoginResponse } from './types'

export interface AdminLoginRequest {
  username: string
  password: string
}

export function adminLogin(payload: AdminLoginRequest): Promise<AdminLoginResponse> {
  return http.post('/api/admin/auth/login', payload)
}

