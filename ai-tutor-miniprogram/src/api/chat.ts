import { request } from '@/utils/request';

export type CollaborationProposalPayload = {
  roomId: number;
  pricePerHour: string;
  trialStartAt: number;
  trialEndAt: number;
  remark?: string;
  clientRequestId?: string;
};

export type ChatImagePayload = {
  url: string;
  objectKey?: string | null;
  contentType?: string | null;
  size: number;
  width?: number | null;
  height?: number | null;
};

export const chatApi = {
  // 房间列表
  listRooms(params: any) {
    return request({
      url: '/chat/room/page',
      method: 'GET',
      data: params
    });
  },
  // 消息列表
  listMessages(params: any) {
    return request({
      url: '/chat/public/msg/page',
      method: 'GET',
      data: params
    });
  },
  // 发送消息
  sendText(roomId: number, content: string) {
    return request({
      url: '/chat/msg',
      method: 'POST',
      data: { roomId, msgType: 1, body: { content } }
    });
  },
  sendImage(roomId: number, body: ChatImagePayload) {
    return request({
      url: '/chat/msg',
      method: 'POST',
      data: { roomId, msgType: 3, body }
    });
  },
  // 创建/获取房间
  getOrCreateRoom(targetUid: number) {
    return request({
      url: '/chat/room',
      method: 'POST',
      data: { targetUid }
    });
  },
  startChatByApplication(params: {
    receiverUid: number;
    contextType: 'DEMAND' | 'TUTOR' | 'ORG_POSTING';
    contextId: number;
    content: string;
    teachingMode?: 'ONLINE' | 'OFFLINE';
    clientRequestId?: string;
  }) {
    return request({
      url: '/chat/application/start-chat',
      method: 'POST',
      data: params
    });
  },
  decideApplication(applicationId: number, action: 'ACCEPT' | 'REJECT') {
    return request({
      url: `/chat/application/${applicationId}/decision-message`,
      method: 'POST',
      data: { action }
    });
  },
  createCollaborationProposal(params: CollaborationProposalPayload) {
    return request({
      url: '/chat/collaboration/proposal',
      method: 'POST',
      data: params
    });
  },
  respondCollaborationProposal(proposalId: number, action: 'ACCEPT' | 'REJECT') {
    return request({
      url: `/chat/collaboration/proposal/${proposalId}/response`,
      method: 'POST',
      data: { action }
    });
  },
  getChatRefundState(roomId: number) {
    return request({
      url: '/chat/refund/state',
      method: 'GET',
      data: { roomId }
    });
  },
  requestBrokerageRefund(roomId: number, reason?: string) {
    return request({
      url: '/chat/refund/apply',
      method: 'POST',
      data: { roomId, reason }
    });
  },
  ackRead(roomId: number, lastReadMsgId: number) {
    return request({
      url: '/chat/read/ack',
      method: 'POST',
      data: { roomId, lastReadMsgId }
    });
  },
  ackDelivered(roomId: number, lastDeliveredMsgId: number) {
    return request({
      url: '/chat/delivery/ack',
      method: 'POST',
      data: { roomId, lastDeliveredMsgId }
    });
  },
  reportTyping(roomId: number, typing: boolean) {
    return request({
      url: '/chat/typing',
      method: 'POST',
      data: { roomId, typing }
    });
  },
  syncRealtimeEvents(params: { lastEventId?: number | null; pageSize?: number }) {
    return request({
      url: '/chat/events/sync',
      method: 'GET',
      data: params
    });
  }
};
