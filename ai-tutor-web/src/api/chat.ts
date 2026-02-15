import { http } from './http'
import type { ChatMessageResp, ChatRoomItemResp, CursorPageBaseResp, CursorPageResp } from './types'

export const chatApi = {
  getOrCreateRoom(targetUid: number) {
    return http.post<unknown, number>('/chat/room', { targetUid })
  },

  listRooms(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResp<ChatRoomItemResp>>('/chat/room/page', { params })
  },

  listMessages(params: { roomId: number; pageSize?: number; cursor?: string | null }) {
    return http.get<unknown, CursorPageBaseResp<ChatMessageResp>>('/chat/public/msg/page', { params })
  },

  sendText(roomId: number, content: string) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', { roomId, msgType: 1, body: { content } })
  },
}

