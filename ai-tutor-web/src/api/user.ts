import { http } from './http'
import type { LoginUserVO, UserRoleEnum } from './types'

export interface SendCodeRequest {
  phone: string
}

export interface LoginOrRegisterRequest {
  phone: string
  code: string
  userRoleEnum: UserRoleEnum
}

export const userApi = {
  sendCode(phone: string) {
    return http.post<unknown, string>('/user/sendcode', { phone } satisfies SendCodeRequest)
  },

  loginOrRegister(request: LoginOrRegisterRequest) {
    return http.post<unknown, LoginUserVO>('/user/loginOrRegister', request)
  },
}

