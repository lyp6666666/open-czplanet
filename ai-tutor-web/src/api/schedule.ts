import { http } from './http'
import type { ScheduleEventVO, UserSimpleVO } from './types'

export interface ListScheduleEventsParams {
  startAt: number
  endAt: number
  includePending?: boolean
}

export interface CreateScheduleEventRequest {
  title: string
  participantUserId: number
  startAt: number
  endAt: number
  description?: string
  subjectId?: number
}

export type RespondScheduleAction = 'ACCEPT' | 'REJECT'

/**
 * 课程安排（日历）相关 API。
 *
 * 说明：后端返回统一 BaseResponse，http.ts 已做 code!=0 的错误抛出。
 */
export const scheduleApi = {
  listEvents(params: ListScheduleEventsParams) {
    return http.get<unknown, ScheduleEventVO[]>('/api/v1/schedule/events', { params })
  },

  createEvent(request: CreateScheduleEventRequest) {
    return http.post<unknown, ScheduleEventVO>('/api/v1/schedule/events', request)
  },

  respond(eventId: number, action: RespondScheduleAction) {
    return http.post<unknown, ScheduleEventVO>(`/api/v1/schedule/events/${eventId}/response`, { action })
  },

  cancel(eventId: number, remark?: string) {
    return http.post<unknown, ScheduleEventVO>(`/api/v1/schedule/events/${eventId}/cancel`, remark ? { remark } : {})
  },

  listRecentContacts(limit = 50) {
    return http.get<unknown, UserSimpleVO[]>('/api/v1/contacts/recent', { params: { limit } })
  },

  searchContacts(q: string, limit = 50) {
    return http.get<unknown, UserSimpleVO[]>('/api/v1/contacts/search', { params: { q, limit } })
  },
}

