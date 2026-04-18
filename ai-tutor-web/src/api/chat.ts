import { http } from './http'
import type { ChatMessageResp, ChatReadAckResp, ChatRoomItemResp, CursorPageBaseResp, CursorPageResp } from './types'

export type ChatRefundStateResp = {
  canApply: boolean
  disableReasonCode?: string
  hoverText?: string
}

export type RealtimeEnvelopeResp = {
  eventId?: number
  eventType?: string
  bizType?: string
  targetUid?: number
  roomId?: number | null
  msgId?: number | null
  occurredAt?: string | number | Date | null
  clientId?: string | null
  payload?: unknown
}

export type RealtimeEventSyncResp = {
  cursor?: number | null
  isLast?: boolean
  latestEventId?: number | null
  list?: RealtimeEnvelopeResp[]
}

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

  getChatRefundState(roomId: number) {
    return http.get<unknown, ChatRefundStateResp>('/chat/refund/state', { params: { roomId } })
  },

  requestBrokerageRefund(roomId: number, reason?: string) {
    return http.post<unknown, ChatMessageResp>('/chat/refund/apply', { roomId, reason })
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
    return http.post<unknown, ChatReadAckResp>('/chat/read/ack', { roomId, lastReadMsgId })
  },

  ackDelivered(roomId: number, lastDeliveredMsgId: number) {
    return http.post<unknown, boolean>('/chat/delivery/ack', { roomId, lastDeliveredMsgId })
  },

  reportTyping(roomId: number, typing: boolean) {
    return http.post<unknown, boolean>('/chat/typing', { roomId, typing })
  },

  syncRealtimeEvents(params: { lastEventId?: number | null; pageSize?: number }) {
    return http.get<unknown, RealtimeEventSyncResp>('/chat/events/sync', { params })
  },
}
