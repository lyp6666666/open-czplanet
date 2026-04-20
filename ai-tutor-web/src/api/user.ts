import { http } from './http'
import type { LoginUserVO, OrgLoginVO, OrganizationProfile, UserCardVO, UserMeVO, UserRoleEnum, UserSettingsVO, UserSimpleVO } from './types'

export interface SendCodeRequest {
  phone: string
}

export interface LoginOrRegisterRequest {
  phone: string
  code: string
  userRoleEnum: UserRoleEnum
  /**
   * 邀请码为注册阶段的选填字段。
   * 仅在新用户注册成功时由后端建立邀请关系，老用户登录时忽略该字段。
   */
  inviteCode?: string
}

export interface OrgLoginRequest {
  username: string
  password: string
}

export interface UserUpdateRequest {
  baseUserInfo?: {
    name?: string
    avatar?: string
    sex?: number
  }
  teacherExtInfo?: {
    realName?: string
    education?: string
    subject?: string
    experienceYears?: number
    ratePerHour?: number
    introduction?: string
    city?: string
    highestEduSchool?: string
    defaultGreeting?: string
  }
  studentExtInfo?: {
    realName?: string
    childAge?: number
    address?: string
    demandDescription?: string
    budget?: number
  }
}

export const userApi = {
  sendCode(phone: string) {
    return http.post<unknown, string>('/user/sendcode', { phone } satisfies SendCodeRequest)
  },

  loginOrRegister(request: LoginOrRegisterRequest) {
    return http.post<unknown, LoginUserVO>('/user/loginOrRegister', request)
  },

  orgLogin(request: OrgLoginRequest) {
    return http.post<unknown, OrgLoginVO>('/org/auth/login', request)
  },

  orgChangePassword(request: { oldPassword: string; newPassword: string }) {
    return http.post<unknown, string>('/org/auth/changePassword', request)
  },

  me() {
    return http.get<unknown, UserMeVO>('/user/me')
  },

  orgPublicProfile(orgUserId: number) {
    return http.get<unknown, OrganizationProfile>(`/api/v1/public/organization/${orgUserId}`)
  },

  batch(ids: number[]) {
    return http.get<unknown, UserSimpleVO[]>('/user/batch', { params: { ids: ids.join(',') } })
  },

  card(uid: number) {
    return http.get<unknown, UserCardVO>('/user/card', { params: { uid } })
  },

  updateUserInfo(request: UserUpdateRequest) {
    return http.post<unknown, string>('/user/updateUserInfo', request)
  },

  sendUpdateUserPhoneCode() {
    return http.get<unknown, string>('/user/sendUpdateUserPhoneCode')
  },

  sendUpdateUserNewPhoneCode(newPhone: string) {
    return http.get<unknown, string>('/user/sendUpdateUserNewPhoneCode', { params: { newPhone } })
  },

  updateUserPhoneV2(request: { newPhone: string; oldCode: string; newCode: string }) {
    return http.post<unknown, string>('/user/updateUserPhoneV2', request)
  },

  settings() {
    return http.get<unknown, UserSettingsVO>('/user/settings')
  },

  updateSettings(request: { applicationGreeting?: string | null }) {
    return http.post<unknown, UserSettingsVO>('/user/settings', request)
  },
}
