import type { LiveRoomClient } from '@/modules/live/livekit'

export type WhiteboardRoomClient = Pick<LiveRoomClient, 'sendData' | 'onDataReceived'>

export type WhiteboardSaveState = 'idle' | 'loading' | 'saving' | 'synced' | 'offline' | 'readonly' | 'error'

export type WhiteboardUser = {
  uid: number
  name: string
  color: string
}

export type WhiteboardScene = {
  elements: unknown[]
  appState: Record<string, unknown>
  files?: Record<string, unknown>
}

export type WhiteboardSnapshot = {
  whiteboardId: number
  sessionId: number
  courseId: number
  scheduleEventId?: number | null
  sceneVersion: number
  scene: WhiteboardScene
  finalized?: boolean | null
  updatedAt?: string | null
}

export type WhiteboardHostProps = {
  sessionId: number
  currentUser: WhiteboardUser
  readonly?: boolean
  roomClient?: WhiteboardRoomClient | null
  onStatusChange?: (state: WhiteboardSaveState, message?: string) => void
}
