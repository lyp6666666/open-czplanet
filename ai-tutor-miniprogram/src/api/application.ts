import { request } from '@/utils/request';

export type ApplicationContextType = 'DEMAND' | 'TUTOR' | 'ORG_POSTING';
export type ApplicationDecision = 'ACCEPT' | 'REJECT';

export interface CreateApplicationRequest {
  receiverUid: number;
  contextType: ApplicationContextType;
  contextId: number;
  content: string;
  teachingMode?: 'ONLINE' | 'OFFLINE';
  clientRequestId?: string;
}

export interface TutorApplication {
  id: number;
  senderUid: number;
  receiverUid: number;
  senderRole?: string;
  receiverRole?: string;
  contextType: ApplicationContextType;
  contextId: number;
  teachingMode?: string;
  content: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | string;
  chatAccessStatus?: 'PAYMENT_REQUIRED' | 'CHAT_ENABLED' | string;
  paymentPayerRole?: string;
  orderId?: number | null;
  roomId?: number | null;
  receiverRead?: boolean;
  decidedAt?: string;
  createTime?: string;
}

export interface CursorPageResp<T> {
  cursor?: number | null;
  isLast?: boolean;
  list?: T[];
}

export interface EnterChatResp {
  paymentRequired?: boolean;
  waitingForTeacherPayment?: boolean;
  orderId?: number | null;
  roomId?: number | null;
}

export const applicationApi = {
  create(data: CreateApplicationRequest) {
    return request({
      url: '/chat/application',
      method: 'POST',
      data,
      loading: true,
    });
  },

  startChat(data: CreateApplicationRequest) {
    return request({
      url: '/chat/application/start-chat',
      method: 'POST',
      data,
      loading: true,
    });
  },

  sent(params: { cursor?: number | string | null; pageSize?: number }): Promise<CursorPageResp<TutorApplication>> {
    return request({
      url: '/chat/application/sent/page',
      method: 'GET',
      data: params,
    });
  },

  received(params: { cursor?: number | string | null; pageSize?: number }): Promise<CursorPageResp<TutorApplication>> {
    return request({
      url: '/chat/application/received/page',
      method: 'GET',
      data: params,
    });
  },

  unread(): Promise<{ unreadCount?: number }> {
    return request({
      url: '/chat/application/unread',
      method: 'GET',
    });
  },

  detail(applicationId: number): Promise<TutorApplication> {
    return request({
      url: `/chat/application/${applicationId}`,
      method: 'GET',
    });
  },

  decide(applicationId: number, action: ApplicationDecision): Promise<TutorApplication> {
    return request({
      url: `/chat/application/${applicationId}/decision`,
      method: 'POST',
      data: { action },
      loading: true,
    });
  },

  enterChat(applicationId: number): Promise<EnterChatResp> {
    return request({
      url: `/chat/application/${applicationId}/enter-chat`,
      method: 'POST',
      loading: true,
    });
  },
};
