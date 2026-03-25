import { request } from '@/utils/request';

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
  // 创建/获取房间
  getOrCreateRoom(targetUid: number) {
    return request({
      url: '/chat/room',
      method: 'POST',
      data: { targetUid }
    });
  },
  startChatByApplication(params: { receiverUid: number; contextType: 'DEMAND' | 'TUTOR' | 'ORG_POSTING'; contextId: number; content: string; clientRequestId?: string }) {
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
  }
};
