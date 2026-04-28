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
  peerDisplayName?: string | null
  subjectLabel?: string | null
  courseKindLabel?: string | null
  realtimeSummaryEnabled?: boolean | null
  postClassSummaryEnabled?: boolean | null
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
  realtimeSummaryEnabled?: boolean | null
  postClassSummaryEnabled?: boolean | null
  subjectLabel?: string | null
  courseKindLabel?: string | null
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
  asrEnabled?: boolean | null
  llmEnabled?: boolean | null
  segmentCount?: number | null
  lastLlmSummaryTs?: number | null
  lastLlmSegmentCount?: number | null
  currentTopic?: string | null
  latestStageSummary?: string | null
  studentQuestions: string[]
  homeworkCandidates: string[]
  keyPoints: string[]
  minutesOutline?: LiveAiMinuteSection[] | null
  activeSectionTitle?: string | null
  updatedAt?: string | null
  rawState?: Record<string, unknown> | null
}

export type LiveAiMinuteItem = {
  title: string
  detail: string
}

export type LiveAiMinuteSection = {
  id: string
  title: string
  summary: string
  startSegment?: number | null
  endSegment?: number | null
  updatedAt?: number | null
  items?: LiveAiMinuteItem[] | null
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

export type LiveWhiteboardScene = {
  elements: unknown[]
  appState: Record<string, unknown>
  files?: Record<string, unknown>
}

export type LiveWhiteboardSnapshotResp = {
  whiteboardId: number
  sessionId: number
  courseId: number
  scheduleEventId?: number | null
  sceneVersion: number
  scene: LiveWhiteboardScene
  finalized?: boolean | null
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

  prepare(courseId: number, payload: { clientType: string; sourcePage?: string; realtimeSummaryEnabled?: boolean; postClassSummaryEnabled?: boolean }) {
    return http.post<unknown, PrepareLiveSessionResp>(`/live/sessions/by-course/${courseId}/prepare`, payload)
  },

  updateAiOptions(sessionId: number, payload: { realtimeSummaryEnabled: boolean; postClassSummaryEnabled: boolean }) {
    return http.post<unknown, LiveSessionResp>(`/live/sessions/${sessionId}/ai/options`, payload)
  },

  uploadAiAudioChunk(sessionId: number, payload: {
    sequence: number
    sampleRate: number
    channelCount: number
    durationMs: number
    rms: number
    format: string
    audioBase64: string
  }) {
    return http.post<unknown, LiveAiStateResp>(`/live/sessions/${sessionId}/ai/audio-chunks`, payload)
  },

  joinToken(sessionId: number, payload: { clientType: string; deviceFingerprint?: string; joinMode?: string }) {
    return http.post<unknown, IssueJoinTokenResp>(`/live/sessions/${sessionId}/join-token`, payload)
  },

  joinAck(sessionId: number, payload: {
    clientType: string
    joinMode?: string
    connectionState?: string
    cameraEnabled?: boolean
    micEnabled?: boolean
    cameraDeviceId?: string | null
    micDeviceId?: string | null
  }) {
    return http.post<unknown, LiveSessionResp>(`/live/sessions/${sessionId}/join-ack`, payload)
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

  getWhiteboard(sessionId: number) {
    return http.get<unknown, LiveWhiteboardSnapshotResp>(`/live/sessions/${sessionId}/whiteboard`)
  },

  saveWhiteboardSnapshot(sessionId: number, payload: { sceneVersion: number; scene: LiveWhiteboardScene }) {
    return http.put<unknown, LiveWhiteboardSnapshotResp>(`/live/sessions/${sessionId}/whiteboard/snapshot`, payload)
  },

  finalizeWhiteboard(sessionId: number, payload: { sceneVersion: number; scene: LiveWhiteboardScene }) {
    return http.post<unknown, LiveWhiteboardSnapshotResp>(`/live/sessions/${sessionId}/whiteboard/finalize`, payload)
  },
}
