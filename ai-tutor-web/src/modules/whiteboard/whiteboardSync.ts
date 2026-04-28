import { liveApi, type LiveWhiteboardScene } from '@/api/live'
import type { LiveRoomDataMessage } from '@/modules/live/livekit'
import type { WhiteboardRoomClient, WhiteboardSaveState, WhiteboardScene, WhiteboardSnapshot } from './whiteboardTypes'

export const WHITEBOARD_SCENE_TOPIC = 'whiteboard.scene.snapshot'
export const WHITEBOARD_CURSOR_TOPIC = 'whiteboard.cursor'

export type WhiteboardRemoteSceneHandler = (scene: WhiteboardScene, version: number) => void

type SceneMessage = {
  kind: 'scene'
  sessionId: number
  version: number
  scene: WhiteboardScene
  senderUid: number
  sentAt: number
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}

function normalizeScene(scene: unknown): WhiteboardScene {
  if (!isRecord(scene)) return emptyWhiteboardScene()
  const elements = Array.isArray(scene.elements) ? scene.elements : []
  const appState = isRecord(scene.appState) ? scene.appState : {}
  const files = isRecord(scene.files) ? scene.files : undefined
  return { elements, appState, files }
}

export function isWhiteboardSceneEqual(left: WhiteboardScene, right: WhiteboardScene) {
  return JSON.stringify({
    elements: left.elements || [],
    appState: left.appState || {},
    files: left.files || {},
  }) === JSON.stringify({
    elements: right.elements || [],
    appState: right.appState || {},
    files: right.files || {},
  })
}

export function isWhiteboardContentEqual(left: WhiteboardScene, right: WhiteboardScene) {
  return JSON.stringify({
    elements: left.elements || [],
    files: left.files || {},
  }) === JSON.stringify({
    elements: right.elements || [],
    files: right.files || {},
  })
}

export function emptyWhiteboardScene(): WhiteboardScene {
  return {
    elements: [],
    appState: {
      viewBackgroundColor: '#fffaf0',
      currentItemStrokeColor: '#172033',
      currentItemBackgroundColor: 'transparent',
      currentItemRoughness: 1,
      currentItemStrokeWidth: 2,
    },
    files: {},
  }
}

export function toWhiteboardSnapshot(raw: unknown, sessionId: number): WhiteboardSnapshot {
  if (!isRecord(raw)) {
    return {
      whiteboardId: 0,
      sessionId,
      courseId: 0,
      sceneVersion: 0,
      scene: emptyWhiteboardScene(),
    }
  }
  return {
    whiteboardId: Number(raw.whiteboardId || 0),
    sessionId: Number(raw.sessionId || sessionId),
    courseId: Number(raw.courseId || 0),
    scheduleEventId: raw.scheduleEventId == null ? null : Number(raw.scheduleEventId),
    sceneVersion: Number(raw.sceneVersion || 0),
    scene: normalizeScene(raw.scene),
    finalized: Boolean(raw.finalized),
    updatedAt: typeof raw.updatedAt === 'string' ? raw.updatedAt : null,
  }
}

export async function loadWhiteboardSnapshot(sessionId: number) {
  const snapshot = await liveApi.getWhiteboard(sessionId)
  return toWhiteboardSnapshot(snapshot, sessionId)
}

export async function saveWhiteboardSnapshot(sessionId: number, sceneVersion: number, scene: WhiteboardScene, finalize = false) {
  const payload = {
    sceneVersion,
    scene: {
      elements: Array.isArray(scene.elements) ? scene.elements : [],
      appState: isRecord(scene.appState) ? scene.appState : {},
      files: isRecord(scene.files) ? scene.files : {},
    } satisfies LiveWhiteboardScene,
  }
  const snapshot = finalize
    ? await liveApi.finalizeWhiteboard(sessionId, payload)
    : await liveApi.saveWhiteboardSnapshot(sessionId, payload)
  return toWhiteboardSnapshot(snapshot, sessionId)
}

export function publishWhiteboardScene(
  roomClient: WhiteboardRoomClient | null | undefined,
  sessionId: number,
  senderUid: number,
  version: number,
  scene: WhiteboardScene,
) {
  if (!roomClient) return Promise.resolve()
  const message: SceneMessage = {
    kind: 'scene',
    sessionId,
    senderUid,
    version,
    scene,
    sentAt: Date.now(),
  }
  return roomClient.sendData(WHITEBOARD_SCENE_TOPIC, message, true)
}

export function subscribeWhiteboardScene(
  roomClient: WhiteboardRoomClient | null | undefined,
  sessionId: number,
  currentUid: number,
  onRemoteScene: WhiteboardRemoteSceneHandler,
  onStatus?: (state: WhiteboardSaveState, message?: string) => void,
) {
  if (!roomClient) return () => undefined
  return roomClient.onDataReceived((message: LiveRoomDataMessage) => {
    if (message.topic !== WHITEBOARD_SCENE_TOPIC || !isRecord(message.payload)) return
    const payload = message.payload
    if (payload.kind !== 'scene') return
    if (Number(payload.sessionId) !== sessionId) return
    if (Number(payload.senderUid) === currentUid) return
    const scene = normalizeScene(payload.scene)
    const version = Number(payload.version || 0)
    onRemoteScene(scene, version)
    onStatus?.('synced', '已接收对方白板更新')
  })
}
