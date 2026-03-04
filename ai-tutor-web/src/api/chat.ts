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

  sendBody(roomId: number, body: unknown) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', { roomId, msgType: 1, body })
  },

  requestBrokerageRefund(roomId: number) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', {
      roomId,
      msgType: 8,
      body: { bizType: 'BROKERAGE_REFUND_REQUEST', eventId: Date.now(), title: '结束沟通', status: 'PENDING_REVIEW' },
    })
  },

  requestEndChat(roomId: number) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', {
      roomId,
      msgType: 8,
      body: { bizType: 'END_CHAT_REQUEST', eventId: Date.now(), title: '结束沟通', status: 'PENDING_CONFIRM' },
    })
  },

  respondEndChat(roomId: number, requestId: number, status: 'CONFIRMED' | 'REJECTED') {
    return http.post<unknown, ChatMessageResp>('/chat/msg', {
      roomId,
      msgType: 8,
      body: { bizType: 'END_CHAT_STATUS', eventId: requestId, title: '结束沟通', status },
    })
  },

  createCollaborationProposal(params: { roomId: number; pricePerHour: string; classTime: string; frequencyPerWeek: number }) {
    return http.post<unknown, ChatMessageResp>('/chat/collaboration/proposal', params)
  },

  updateCollaborationProposal(proposalId: number, params: { roomId: number; pricePerHour: string; classTime: string; frequencyPerWeek: number }) {
    return http.put<unknown, ChatMessageResp>(`/chat/collaboration/proposal/${proposalId}`, params)
  },

  respondCollaborationProposal(proposalId: number, action: 'ACCEPT' | 'REJECT') {
    return http.post<unknown, ChatMessageResp>(`/chat/collaboration/proposal/${proposalId}/response`, { action })
  },

  ackRead(roomId: number, lastReadMsgId: number) {
    return http.post<unknown, boolean>('/chat/read/ack', { roomId, lastReadMsgId })
  },
}
