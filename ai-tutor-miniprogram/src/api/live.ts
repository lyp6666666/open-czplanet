import { request } from '@/utils/request';

export type LiveSessionResp = {
  sessionId: number;
  courseId: number;
  status: string;
  scheduledStartAt?: string | null;
  scheduledEndAt?: string | null;
  actualStartAt?: string | null;
  actualEndAt?: string | null;
  teacherUid: number;
  studentUid: number;
  roomId?: number | null;
  provider?: string | null;
  providerRoomName?: string | null;
  canJoin?: boolean | null;
  joinableNow?: boolean | null;
  joinBlockedReason?: string | null;
  blockingPaymentOrderId?: number | null;
  peerDisplayName?: string | null;
  subjectLabel?: string | null;
  courseKindLabel?: string | null;
};

export type PrepareLiveSessionResp = {
  sessionId: number;
  status: string;
  courseTitle: string;
  peerDisplayName: string;
  canJoin: boolean;
  joinableNow: boolean;
  joinBlockedReason?: string | null;
  blockingPaymentOrderId?: number | null;
  blockingLessonId?: number | null;
  defaultMediaPolicy?: string | null;
  deviceCheckRequired: boolean;
  realtimeSummaryEnabled?: boolean | null;
  postClassSummaryEnabled?: boolean | null;
  subjectLabel?: string | null;
  courseKindLabel?: string | null;
};

export type IssueJoinTokenResp = {
  provider: string;
  serverUrl: string;
  roomName: string;
  participantName: string;
  participantIdentity: string;
  accessToken: string;
  expireAt: string;
};

export type LiveAiResultResp = {
  sessionId: number;
  courseId: number;
  resultStatus: string;
  reportStatus?: string | null;
  summary?: Record<string, unknown> | null;
  report?: Record<string, unknown> | null;
  preview?: string | null;
  updatedAt?: string | null;
};

export const liveApi = {
  getByCourse(courseId: number) {
    return request({
      url: `/live/sessions/by-course/${courseId}`,
      method: 'GET',
      silentError: true,
    }) as Promise<LiveSessionResp>;
  },
  prepare(courseId: number, data: { clientType: string; sourcePage?: string }) {
    return request({
      url: `/live/sessions/by-course/${courseId}/prepare`,
      method: 'POST',
      data,
      silentError: true,
    }) as Promise<PrepareLiveSessionResp>;
  },
  updateAiOptions(sessionId: number, data: { realtimeSummaryEnabled: boolean; postClassSummaryEnabled: boolean }) {
    return request({
      url: `/live/sessions/${sessionId}/ai/options`,
      method: 'POST',
      data,
      silentError: true,
    }) as Promise<LiveSessionResp>;
  },
  reportDevice(sessionId: number, data: Record<string, unknown>) {
    return request({
      url: `/live/sessions/${sessionId}/device-report`,
      method: 'POST',
      data,
      silentError: true,
    }) as Promise<boolean>;
  },
  joinToken(sessionId: number, data: { clientType: string; deviceFingerprint?: string; joinMode?: string }) {
    return request({
      url: `/live/sessions/${sessionId}/join-token`,
      method: 'POST',
      data,
      silentError: true,
    }) as Promise<IssueJoinTokenResp>;
  },
  aiResult(sessionId: number) {
    return request({
      url: `/live/sessions/${sessionId}/ai/result`,
      method: 'GET',
      silentError: true,
    }) as Promise<LiveAiResultResp>;
  },
  retryAiResult(sessionId: number) {
    return request({
      url: `/live/sessions/${sessionId}/ai/result/retry`,
      method: 'POST',
      silentError: true,
    }) as Promise<LiveAiResultResp>;
  },
};
