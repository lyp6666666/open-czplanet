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

export type ScheduleAvailabilitySlot = {
  startAt: number;
  endAt: number;
  available?: boolean;
};

export type ScheduleAvailabilityResp = {
  date?: string;
  slots?: ScheduleAvailabilitySlot[];
  availableSlots?: ScheduleAvailabilitySlot[];
};

export type ListScheduleEventsParams = {
  startAt: number;
  endAt: number;
  includePending?: boolean;
};

export type CreateScheduleEventRequest = {
  courseId?: number;
  lessonType?: 'TRIAL' | 'NORMAL';
  lessonPriceFen?: number;
  trialPricePercent?: number;
  title: string;
  participantUserId: number;
  startAt: number;
  endAt: number;
  description?: string;
  subjectId?: number;
};

export const scheduleApi = {
  listEvents(params: ListScheduleEventsParams) {
    return request({
      url: '/api/v1/schedule/events',
      method: 'GET',
      data: params,
      silentError: true,
    }) as Promise<ScheduleEvent[]>;
  },
  listCourseEvents(courseId: number) {
    return request({
      url: `/api/v1/schedule/courses/${courseId}/events`,
      method: 'GET',
      silentError: true,
    }) as Promise<ScheduleEvent[]>;
  },
  createEvent(data: CreateScheduleEventRequest) {
    return request({
      url: '/api/v1/schedule/events',
      method: 'POST',
      data,
    }) as Promise<ScheduleEvent>;
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
  },
  availabilityDay(params: { participantUserId: number; date: string; roomId?: number | null; courseId?: number | null }) {
    return request({
      url: '/api/v1/schedule/availability/day',
      method: 'GET',
      data: params,
      silentError: true,
    }) as Promise<ScheduleAvailabilityResp>;
  }
};
