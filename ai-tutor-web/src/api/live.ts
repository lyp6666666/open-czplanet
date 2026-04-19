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

export const liveApi = {
  getByCourse(courseId: number) {
    return http.get<unknown, LiveSessionResp>(`/live/sessions/by-course/${courseId}`)
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
}
