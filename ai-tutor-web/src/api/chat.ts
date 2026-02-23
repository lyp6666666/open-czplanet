import { http } from './http'
import type { ChatMessageResp, ChatRoomItemResp, CursorPageBaseResp, CursorPageResp } from './types'

export const chatApi = {
  getOrCreateRoom(targetUid: number) {
    return http.post<unknown, number>('/chat/room', { targetUid })
  },

  startRoom(targetUid: number, greeting?: string | null) {
    return http.post<unknown, number>('/chat/room/start', { targetUid, greeting: greeting || undefined })
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

  createCollaborationProposal(params: { roomId: number; pricePerHour: string; classTime: string; frequencyPerWeek: number }) {
    return http.post<unknown, ChatMessageResp>('/chat/collaboration/proposal', params)
  },

  respondCollaborationProposal(proposalId: number, action: 'ACCEPT' | 'REJECT') {
    return http.post<unknown, ChatMessageResp>(`/chat/collaboration/proposal/${proposalId}/response`, { action })
  },

  ackRead(roomId: number, lastReadMsgId: number) {
    return http.post<unknown, boolean>('/chat/read/ack', { roomId, lastReadMsgId })
  },
}
