import { http } from './http'
import type { ChatMessageResp, CursorPageResp, TutorApplicationEnterResp, TutorApplicationUnreadResp, TutorApplicationVO } from './types'

export const applicationApi = {
  create(params: { receiverUid: number; contextType: 'DEMAND' | 'TUTOR' | 'ORG_POSTING'; contextId: number; content: string; clientRequestId?: string | null }) {
    return http.post<unknown, TutorApplicationVO>('/chat/application', {
      receiverUid: params.receiverUid,
      contextType: params.contextType,
      contextId: params.contextId,
      content: params.content,
      clientRequestId: params.clientRequestId || undefined,
    })
  },

  startChat(params: { receiverUid: number; contextType: 'DEMAND' | 'TUTOR' | 'ORG_POSTING'; contextId: number; content: string; clientRequestId?: string | null }) {
    return http.post<unknown, ChatMessageResp>('/chat/application/start-chat', {
      receiverUid: params.receiverUid,
      contextType: params.contextType,
      contextId: params.contextId,
      content: params.content,
      clientRequestId: params.clientRequestId || undefined,
    })
  },

  listSent(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResp<TutorApplicationVO>>('/chat/application/sent/page', { params })
  },

  listReceived(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResp<TutorApplicationVO>>('/chat/application/received/page', { params })
  },

  unread() {
    return http.get<unknown, TutorApplicationUnreadResp>('/chat/application/unread')
  },

  detail(applicationId: number) {
    return http.get<unknown, TutorApplicationVO>(`/chat/application/${applicationId}`)
  },

  decide(applicationId: number, action: 'ACCEPT' | 'REJECT') {
    return http.post<unknown, TutorApplicationVO>(`/chat/application/${applicationId}/decision`, { action })
  },

  decideMessage(applicationId: number, action: 'ACCEPT' | 'REJECT') {
    return http.post<unknown, ChatMessageResp>(`/chat/application/${applicationId}/decision-message`, { action })
  },

  enterChat(applicationId: number) {
    return http.post<unknown, TutorApplicationEnterResp>(`/chat/application/${applicationId}/enter-chat`, {})
  },
}
