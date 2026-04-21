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

export type ChatPresenceResp = {
  uid: number
  online: boolean
  lastOnlineAt?: string | number | Date | null
}

export type CollaborationProposalPayload = {
  roomId: number
  pricePerHour: string
  trialStartAt: number
  trialEndAt: number
  remark?: string
  clientRequestId?: string
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

  searchMessages(params: { roomId: number; keyword: string; pageSize?: number; cursor?: string | null }) {
    return http.get<unknown, CursorPageBaseResp<ChatMessageResp>>('/chat/public/msg/search', { params })
  },

  batchPresence(uids: number[]) {
    if (uids.length <= 0) return Promise.resolve([] satisfies ChatPresenceResp[])
    return http.get<unknown, ChatPresenceResp[]>('/chat/presence/batch', { params: { uids: uids.join(',') } })
  },

  sendText(roomId: number, content: string) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', { roomId, msgType: 1, body: { content } })
  },

  sendImage(
    roomId: number,
    payload: { url: string; objectKey?: string | null; contentType?: string | null; size: number; width?: number | null; height?: number | null },
  ) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', { roomId, msgType: 3, body: payload })
  },

  recallMessage(roomId: number, targetMsgId: number) {
    return http.post<unknown, ChatMessageResp>('/chat/msg', { roomId, msgType: 2, body: { targetMsgId } })
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

  createCollaborationProposal(params: CollaborationProposalPayload) {
    return http.post<unknown, ChatMessageResp>('/chat/collaboration/proposal', params)
  },

  updateCollaborationProposal(proposalId: number, params: CollaborationProposalPayload) {
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
