import { request } from '@/utils/request';

export type ScheduleEventStatus = 'PENDING' | 'ACCEPTED' | 'RESCHEDULE_PENDING' | 'REJECTED' | 'CANCELED' | 'COMPLETED' | 'UNKNOWN' | string;

export type ScheduleEvent = {
  id: number;
  courseId?: number | null;
  title: string;
  lessonType?: 'TRIAL' | 'NORMAL' | string | null;
  lessonPriceFen?: number | null;
  description?: string | null;
  startAt: number;
  endAt: number;
  status: ScheduleEventStatus;
  creatorUserId: number;
  participant?: any;
  chatRoomId?: number | null;
  durationMinutes?: number | null;
  proposedStartAt?: number | null;
  proposedEndAt?: number | null;
  proposedBy?: number | null;
  cancelBy?: number | null;
};

export type SubmitWeeklyScheduleRequest = {
  participantUserId: number;
  roomId?: number | null;
  title?: string;
  description?: string;
  lessonPriceFen?: number;
  weeks?: number;
  slots: Array<{
    dayOfWeek: number;
    startMinute: number;
    endMinute: number;
  }>;
};

export const scheduleApi = {
  listCourseEvents(courseId: number) {
    return request({
      url: `/api/v1/schedule/courses/${courseId}/events`,
      method: 'GET'
    }) as Promise<ScheduleEvent[]>;
  },
  respond(eventId: number, action: 'ACCEPT' | 'REJECT') {
    return request({
      url: `/api/v1/schedule/events/${eventId}/response`,
      method: 'POST',
      data: { action }
    }) as Promise<ScheduleEvent>;
  },
  cancel(eventId: number, remark?: string) {
    return request({
      url: `/api/v1/schedule/events/${eventId}/cancel`,
      method: 'POST',
      data: remark ? { remark } : {}
    }) as Promise<ScheduleEvent>;
  },
  submitWeeklySchedule(courseId: number, data: SubmitWeeklyScheduleRequest) {
    return request({
      url: `/api/v1/schedule/courses/${courseId}/weekly-schedule`,
      method: 'POST',
      data
    }) as Promise<ScheduleEvent[]>;
  }
};
