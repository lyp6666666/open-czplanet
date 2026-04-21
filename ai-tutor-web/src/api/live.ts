import { http } from './http'

export type LiveSessionResp = {
  sessionId: number
  courseId: number
  status: string
  joinOpenAt?: string | null
  scheduledStartAt?: string | null
  scheduledEndAt?: string | null
  actualStartAt?: string | null
  actualEndAt?: string | null
  teacherUid: number
  studentUid: number
  roomId?: number | null
  provider: string
  providerRoomName: string
  canJoin?: boolean | null
  joinableNow?: boolean | null
  peerJoined?: boolean | null
  peerOnline?: boolean | null
  recordPolicy?: string | null
  aiPolicy?: string | null
}

export type PrepareLiveSessionResp = {
  sessionId: number
  status: string
  courseTitle: string
  peerDisplayName: string
  canJoin: boolean
  joinableNow: boolean
  joinBlockedReason?: string | null
  blockingPaymentOrderId?: number | null
  blockingLessonId?: number | null
  defaultMediaPolicy?: string | null
  deviceCheckRequired: boolean
}

export type IssueJoinTokenResp = {
  provider: string
  serverUrl: string
  roomName: string
  participantName: string
  participantIdentity: string
  accessToken: string
  expireAt: string
}

export type LiveTimelineItemResp = {
  eventType: string
  eventSource: string
  operatorUid?: number | null
  payloadJson?: string | null
  occurredAt: string
}

export type LiveAiStateResp = {
  sessionId: number
  courseId: number
  aiStatus: string
  realtimeEnabled: boolean
  summaryStatus: string
  currentTopic?: string | null
  latestStageSummary?: string | null
  studentQuestions: string[]
  homeworkCandidates: string[]
  keyPoints: string[]
  updatedAt?: string | null
  rawState?: Record<string, unknown> | null
}

export type LiveAiResultResp = {
  sessionId: number
  courseId: number
  resultStatus: string
  reportStatus?: string | null
  summary?: Record<string, unknown> | null
  report?: Record<string, unknown> | null
  preview?: string | null
  updatedAt?: string | null
}

export type LiveReminderItemResp = {
  sessionId: number
  courseId: number
  title: string
  status: string
  joinableNow: boolean
  canJoin: boolean
  scheduledStartAt?: string | null
  scheduledEndAt?: string | null
  joinOpenAt?: string | null
  peerDisplayName?: string | null
}

export const liveApi = {
  getByCourse(courseId: number) {
    return http.get<unknown, LiveSessionResp>(`/live/sessions/by-course/${courseId}`)
  },

  reminders() {
    return http.get<unknown, LiveReminderItemResp[]>('/live/sessions/reminders')
  },

  prepare(courseId: number, payload: { clientType: string; sourcePage?: string }) {
    return http.post<unknown, PrepareLiveSessionResp>(`/live/sessions/by-course/${courseId}/prepare`, payload)
  },

  joinToken(sessionId: number, payload: { clientType: string; deviceFingerprint?: string; joinMode?: string }) {
    return http.post<unknown, IssueJoinTokenResp>(`/live/sessions/${sessionId}/join-token`, payload)
  },

  status(sessionId: number) {
    return http.get<unknown, LiveSessionResp>(`/live/sessions/${sessionId}/status`)
  },

  reportDevice(sessionId: number, payload: Record<string, unknown>) {
    return http.post<unknown, boolean>(`/live/sessions/${sessionId}/device-report`, payload)
  },

  leave(sessionId: number, payload?: { leaveReason?: string; connectionState?: string; durationSeconds?: number }) {
    return http.post<unknown, LiveSessionResp>(`/live/sessions/${sessionId}/leave`, payload || {})
  },

  end(sessionId: number, payload?: { reason?: string; confirm?: boolean }) {
    return http.post<unknown, LiveSessionResp>(`/live/sessions/${sessionId}/end`, payload || {})
  },

  timeline(sessionId: number) {
    return http.get<unknown, LiveTimelineItemResp[]>(`/live/sessions/${sessionId}/timeline`)
  },

  aiState(sessionId: number) {
    return http.get<unknown, LiveAiStateResp>(`/live/sessions/${sessionId}/ai/state`)
  },

  aiResult(sessionId: number) {
    return http.get<unknown, LiveAiResultResp>(`/live/sessions/${sessionId}/ai/result`)
  },

  retryAiResult(sessionId: number) {
    return http.post<unknown, LiveAiResultResp>(`/live/sessions/${sessionId}/ai/result/retry`, {})
  },
}
