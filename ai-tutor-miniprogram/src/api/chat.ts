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
  }
};
