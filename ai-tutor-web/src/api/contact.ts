import { http } from './http'

export interface UnlockedContactVO {
  uid: number
  phone: string
}

export const contactApi = {
  unlock(roomId: number, targetUid: number) {
    return http.get<unknown, UnlockedContactVO>('/chat/contact/unlock', { params: { roomId, targetUid } })
  },
}

