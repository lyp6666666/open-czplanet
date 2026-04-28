import { request } from '@/utils/request';

export type CourseRefundInfo = {
  id: number;
  type?: string | null;
  status?: string | null;
  reason?: string | null;
  adminNote?: string | null;
  refundPercent?: number | null;
  refundAmountFen?: number | null;
  createTime?: string | null;
  decidedAt?: string | null;
};

export type CourseProposalInfo = {
  id: number;
  fromUid?: number | null;
  toUid?: number | null;
  status?: string | null;
  pricePerHour?: string | null;
  classTime?: string | null;
  frequencyPerWeek?: number | null;
  trialStartAt?: string | null;
  trialEndAt?: string | null;
  remark?: string | null;
  expireAt?: string | null;
};

export type CourseItem = {
  courseId: number;
  applicationId: number;
  roomId?: number | null;
  liveSessionId?: number | null;
  teacherUid: number;
  studentUid: number;
  teachingMode?: 'ONLINE' | 'OFFLINE' | null;
  courseName?: string | null;
  classTime?: string | null;
  frequencyPerWeek?: number | null;
  lessonPrice?: string | null;
  status: string;
  trialStartAt?: string | null;
  trialEndAt?: string | null;
  weeklyScheduleDeadlineAt?: string | null;
  weeklyScheduleSubmittedAt?: string | null;
  aiResultStatus?: string | null;
  aiPreview?: string | null;
  payDeadlineAt?: string | null;
  payExpired?: boolean | null;
  archiveReason?: string | null;
  latestRefund?: CourseRefundInfo | null;
  latestProposal?: CourseProposalInfo | null;
};

export type CourseDetail = CourseItem & {
  roomId: number | null;
};

export const courseApi = {
  myCourses(params: { page?: number; size?: number; role?: 'TEACHER' | 'STUDENT' } = {}) {
    return request({
      url: '/courses/my',
      method: 'GET',
      data: params,
      silentError: true,
    }) as Promise<CourseItem[]>;
  },
  detail(courseId: number) {
    return request({
      url: `/courses/${courseId}`,
      method: 'GET',
      silentError: true,
    }) as Promise<CourseDetail>;
  },
  byRoom(roomId: number) {
    return request({
      url: `/courses/by-room/${roomId}`,
      method: 'GET'
    }) as Promise<CourseDetail>;
  },
  submitTrialResult(courseId: number, payload: {
    result: 'PASS' | 'FAIL';
    reason?: string;
    evidenceImageUrls?: string[];
    evidenceVideoUrl?: string;
    evidenceVideoDurationSeconds?: number;
  }) {
    return request({
      url: `/courses/${courseId}/trial-result`,
      method: 'POST',
      data: payload
    });
  },
  applyTrialRefund(courseId: number, payload: {
    reason: string;
    evidenceImageUrls?: string[];
    evidenceVideoUrl?: string;
    evidenceVideoDurationSeconds?: number;
  }) {
    return request({
      url: `/courses/${courseId}/trial-refund/apply`,
      method: 'POST',
      data: payload
    });
  }
};
