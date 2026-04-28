<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Track } from 'livekit-client'

import { liveApi, type LiveAiMinuteSection, type LiveAiStateResp, type LiveSessionResp } from '@/api/live'
import {
  attachTrackToElement,
  detachTrack,
  LiveRoomClient,
} from '@/modules/live/livekit'
import { startLiveAiAudioUplink } from '@/modules/live/liveAiAudioUplink'
import { readLiveMediaPreferences, saveLiveMediaPreferences } from '@/modules/live/mediaPreferences'
import LiveClassChatPanel from '@/ui/live/LiveClassChatPanel.vue'
import LiveWhiteboardPanel from '@/ui/live/LiveWhiteboardPanel.vue'
import type { WhiteboardSaveState } from '@/modules/whiteboard/whiteboardTypes'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const loading = ref(false)
const connecting = ref(false)
const error = ref<string | null>(null)
const session = ref<LiveSessionResp | null>(null)
const sidebar = ref<'ai' | 'chat' | 'info' | 'tech'>('ai')
const classroomMode = ref<'video' | 'whiteboard'>('video')
const connectionState = ref<'idle' | 'connecting' | 'connected' | 'reconnecting' | 'disconnected'>('idle')
const remoteParticipantName = ref('对方')
const remoteParticipantJoined = ref(false)
const remoteAudioConnected = ref(false)
const remoteVideoConnected = ref(false)
const localScreenShareOn = ref(false)
const remoteScreenShareConnected = ref(false)
const screenShareOwnerName = ref('')
const screenShareBusy = ref(false)
const screenShareError = ref<string | null>(null)
const screenShareVideoReady = ref(false)
const roomId = computed(() => session.value?.roomId ?? null)
const aiState = ref<LiveAiStateResp | null>(null)
const aiError = ref<string | null>(null)
const endConfirmOpen = ref(false)
const endConfirmCountdown = ref(3)
const endConfirmBusy = ref(false)
const endConfirmError = ref<string | null>(null)
const whiteboardStatus = ref<WhiteboardSaveState>('idle')
const whiteboardStatusMessage = ref('白板未打开')
const videoDockX = ref(0)
const videoDockY = ref(0)
const videoDockDragging = ref(false)
const insightPanelWidth = ref(392)
const insightPanelDragging = ref(false)
const joinedAt = ref(Date.now())
const now = ref(Date.now())

const prefs = readLiveMediaPreferences()
const micOn = ref(prefs.micEnabled)
const camOn = ref(prefs.cameraEnabled)
const selectedCameraId = ref(prefs.cameraDeviceId ?? '')
const selectedMicId = ref(prefs.micDeviceId ?? '')

const localVideoRef = ref<HTMLVideoElement | null>(null)
const remoteVideoRef = ref<HTMLVideoElement | null>(null)
const remoteAudioRef = ref<HTMLAudioElement | null>(null)
const screenShareVideoRef = ref<HTMLVideoElement | null>(null)
const roomClientView = ref<LiveRoomClient | null>(null)

let pollTimer: number | null = null
let clockTimer: number | null = null
let endConfirmTimer: number | null = null
let roomClient: LiveRoomClient | null = null
let aiAudioUplink: { stop: () => void } | null = null
let videoDockDragOrigin: { pointerX: number; pointerY: number; dockX: number; dockY: number } | null = null
let activeScreenShareTrack: Parameters<typeof attachTrackToElement>[0] | null = null
let insightPanelDragOrigin: { pointerX: number; width: number } | null = null

const toolItems = [
  { key: 'whiteboard', label: '白板', icon: '✎' },
  { key: 'docs', label: '文档', icon: '▤' },
  { key: 'screen', label: '屏幕', icon: '▣' },
  { key: 'interactive', label: '互动', icon: '◎' },
  { key: 'tools', label: '工具', icon: '◇' },
] as const

const utilityItems = [
  { key: 'members', label: '成员', icon: '◌' },
  { key: 'chat', label: '聊天', icon: '□' },
] as const

type DisplayMinuteItem = {
  title: string
  detail: string
}

type DisplayMinuteSection = {
  id: string
  title: string
  summary: string
  items: DisplayMinuteItem[]
}

function currentUserFromStorage() {
  try {
    const raw = window.localStorage.getItem('ai_tutor_user')
    if (!raw) return null
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const uid = Number(parsed.id || parsed.uid || 0)
    const name = String(parsed.realName || parsed.name || parsed.nickname || '我')
    return uid > 0 ? { uid, name } : null
  } catch {
    return null
  }
}

function clearRemoteMediaState() {
  remoteParticipantJoined.value = false
  remoteVideoConnected.value = false
  remoteAudioConnected.value = false
  if (remoteVideoRef.value) remoteVideoRef.value.srcObject = null
  if (remoteAudioRef.value) remoteAudioRef.value.srcObject = null
}

function syncRemoteStateFromRoom(client: LiveRoomClient) {
  const remoteParticipants = Array.from(client.room.remoteParticipants.values())
  const participant = remoteParticipants[0]
  if (!participant) {
    remoteParticipantName.value = '对方'
    clearRemoteMediaState()
    return
  }

  remoteParticipantJoined.value = true
  remoteParticipantName.value = participant.name || participant.identity || '对方'
  let hasRemoteAudio = false
  let hasRemoteVideo = false
  let hasRemoteScreenShare = false
  for (const publication of participant.trackPublications.values()) {
    const track = publication.track
    if (!track) continue
    if (publication.source === Track.Source.ScreenShare && track.kind === Track.Kind.Video) {
      void attachScreenShareTrack(track)
      hasRemoteScreenShare = true
      continue
    }
    if (track.kind === Track.Kind.Video) {
      attachTrackToElement(track, remoteVideoRef.value)
      hasRemoteVideo = true
      continue
    }
    attachTrackToElement(track, remoteAudioRef.value)
    hasRemoteAudio = true
  }
  remoteVideoConnected.value = hasRemoteVideo
  remoteAudioConnected.value = hasRemoteAudio
  remoteScreenShareConnected.value = hasRemoteScreenShare
  if (hasRemoteScreenShare && !localScreenShareOn.value) {
    screenShareOwnerName.value = participant.name || participant.identity || '对方'
  }
  if (!hasRemoteVideo && remoteVideoRef.value) remoteVideoRef.value.srcObject = null
  if (!hasRemoteAudio && remoteAudioRef.value) remoteAudioRef.value.srcObject = null
  if (!hasRemoteScreenShare && !localScreenShareOn.value) {
    clearScreenShareTrack()
    screenShareOwnerName.value = ''
  }
}

async function attachLocalPreview(client: LiveRoomClient, retries = 4) {
  for (let i = 0; i <= retries; i += 1) {
    const localCameraPublication = client.localCameraPublication ?? client.room.localParticipant.getTrackPublication(Track.Source.Camera)
    if (localCameraPublication?.track) {
      attachTrackToElement(localCameraPublication.track, localVideoRef.value)
      return
    }
    if (i < retries) {
      await new Promise<void>((resolve) => window.setTimeout(resolve, 180))
    }
  }
  if (localVideoRef.value) {
    localVideoRef.value.srcObject = null
  }
}

const peerJoinedState = computed(() => !!(session.value?.peerJoined || remoteParticipantJoined.value || remoteAudioConnected.value || remoteVideoConnected.value))
const memberCount = computed(() => (peerJoinedState.value ? 2 : 1))
const isWhiteboardMode = computed(() => classroomMode.value === 'whiteboard')
const isScreenShareMode = computed(() => localScreenShareOn.value || remoteScreenShareConnected.value)
const stageMode = computed<'video' | 'whiteboard' | 'screen'>(() => {
  if (isScreenShareMode.value) return 'screen'
  if (isWhiteboardMode.value) return 'whiteboard'
  return 'video'
})
const isVideoDockMode = computed(() => isWhiteboardMode.value || isScreenShareMode.value)
const screenShareLabel = computed(() => {
  if (localScreenShareOn.value) return '你正在共享屏幕'
  if (remoteScreenShareConnected.value) return `${screenShareOwnerName.value || remoteParticipantName.value} 正在共享屏幕`
  return '共享屏幕'
})

const currentWhiteboardUser = computed(() => {
  const stored = currentUserFromStorage()
  const uid = stored?.uid || session.value?.teacherUid || 0
  return {
    uid,
    name: stored?.name || '我',
    color: '#2d62f2',
  }
})

const classroomTitle = computed(() => {
  const peer = session.value?.peerDisplayName || remoteParticipantName.value || '对方'
  const subject = session.value?.subjectLabel || '课程'
  const kind = session.value?.courseKindLabel || '正式课'
  return `${peer} · ${subject} · ${kind}`
})

const classroomStatusLabel = computed(() => {
  if (connecting.value || connectionState.value === 'connecting') return '加入中'
  if (connectionState.value === 'reconnecting') return '重连中'
  if (connectionState.value === 'disconnected') return '已断开'
  const status = String(session.value?.status || '').trim().toUpperCase()
  if (status === 'IN_PROGRESS' || connectionState.value === 'connected') return '进行中'
  if (status === 'ENDED') return '已结束'
  return '待开始'
})

const classroomElapsed = computed(() => {
  const base = Date.parse(String(session.value?.actualStartAt || session.value?.scheduledStartAt || ''))
  const start = Number.isFinite(base) ? base : joinedAt.value
  const totalSeconds = Math.max(0, Math.floor((now.value - start) / 1000))
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const connectionLabel = computed(() => {
  if (connectionState.value === 'connected' || remoteAudioConnected.value || remoteVideoConnected.value) return '音视频已连接'
  if (connectionState.value === 'reconnecting') return '连接恢复中'
  if (connecting.value) return '正在加入课堂'
  return '等待连接'
})

const whiteboardStatusText = computed(() => {
  if (whiteboardStatusMessage.value) return whiteboardStatusMessage.value
  if (whiteboardStatus.value === 'loading') return '正在打开白板'
  if (whiteboardStatus.value === 'saving') return '正在同步白板'
  if (whiteboardStatus.value === 'synced') return '白板已同步'
  if (whiteboardStatus.value === 'offline') return '网络较慢，白板稍后同步'
  if (whiteboardStatus.value === 'readonly') return '白板只读查看'
  if (whiteboardStatus.value === 'error') return '白板暂不可用'
  return '白板未打开'
})

const videoDockStyle = computed(() => (
  isVideoDockMode.value
    ? {
        '--video-dock-x': `${videoDockX.value}px`,
        '--video-dock-y': `${videoDockY.value}px`,
      }
    : {}
))

const classroomGridStyle = computed(() => ({
  '--insight-panel-width': `${insightPanelWidth.value}px`,
}))

async function handleToolClick(key: string) {
  if (key === 'whiteboard') {
    classroomMode.value = isWhiteboardMode.value ? 'video' : 'whiteboard'
    if (!isWhiteboardMode.value) return
    sidebar.value = 'ai'
    return
  }
  if (key === 'screen') {
    await toggleScreenShare()
    return
  }
  if (key === 'docs') {
    classroomMode.value = 'video'
  }
}

function handleWhiteboardStatusChange(state: WhiteboardSaveState, message?: string) {
  whiteboardStatus.value = state
  whiteboardStatusMessage.value = message || ''
}

function clampVideoDockOffset(x: number, y: number) {
  const viewportWidth = window.innerWidth || 1280
  const viewportHeight = window.innerHeight || 720
  return {
    x: Math.min(24, Math.max(x, -Math.max(120, viewportWidth - 440))),
    y: Math.min(Math.max(120, viewportHeight - 360), Math.max(-48, y)),
  }
}

function stopVideoDockDrag() {
  videoDockDragging.value = false
  videoDockDragOrigin = null
  window.removeEventListener('pointermove', handleVideoDockDrag)
  window.removeEventListener('pointerup', stopVideoDockDrag)
  window.removeEventListener('pointercancel', stopVideoDockDrag)
}

function handleVideoDockDrag(event: PointerEvent) {
  if (!videoDockDragOrigin) return
  const next = clampVideoDockOffset(
    videoDockDragOrigin.dockX + event.clientX - videoDockDragOrigin.pointerX,
    videoDockDragOrigin.dockY + event.clientY - videoDockDragOrigin.pointerY,
  )
  videoDockX.value = next.x
  videoDockY.value = next.y
}

function startVideoDockDrag(event: PointerEvent) {
  if (!isVideoDockMode.value) return
  const target = event.target as HTMLElement | null
  if (target?.closest('button')) return
  videoDockDragging.value = true
  videoDockDragOrigin = {
    pointerX: event.clientX,
    pointerY: event.clientY,
    dockX: videoDockX.value,
    dockY: videoDockY.value,
  }
  window.addEventListener('pointermove', handleVideoDockDrag)
  window.addEventListener('pointerup', stopVideoDockDrag)
  window.addEventListener('pointercancel', stopVideoDockDrag)
}

function resetVideoDockPosition() {
  videoDockX.value = 0
  videoDockY.value = 0
}

function clampInsightPanelWidth(width: number) {
  const viewportWidth = window.innerWidth || 1280
  const minWidth = 300
  const maxWidth = Math.max(minWidth, Math.min(560, viewportWidth - 620))
  return Math.min(maxWidth, Math.max(minWidth, width))
}

function handleInsightPanelDrag(event: PointerEvent) {
  if (!insightPanelDragOrigin) return
  const deltaX = event.clientX - insightPanelDragOrigin.pointerX
  insightPanelWidth.value = clampInsightPanelWidth(insightPanelDragOrigin.width - deltaX)
}

function stopInsightPanelDrag() {
  if (!insightPanelDragging.value) return
  insightPanelDragging.value = false
  insightPanelDragOrigin = null
  window.removeEventListener('pointermove', handleInsightPanelDrag)
  window.removeEventListener('pointerup', stopInsightPanelDrag)
  window.removeEventListener('pointercancel', stopInsightPanelDrag)
  try {
    window.localStorage.setItem('ai_tutor_live_insight_panel_width', String(insightPanelWidth.value))
  } catch {
    // 忽略本地偏好保存失败，面板本次仍可正常拖动。
  }
}

function startInsightPanelDrag(event: PointerEvent) {
  if (window.innerWidth <= 1180) return
  event.preventDefault()
  insightPanelDragging.value = true
  insightPanelDragOrigin = {
    pointerX: event.clientX,
    width: insightPanelWidth.value,
  }
  window.addEventListener('pointermove', handleInsightPanelDrag)
  window.addEventListener('pointerup', stopInsightPanelDrag)
  window.addEventListener('pointercancel', stopInsightPanelDrag)
}

function restoreInsightPanelWidth() {
  try {
    const raw = Number(window.localStorage.getItem('ai_tutor_live_insight_panel_width') || 0)
    if (Number.isFinite(raw) && raw > 0) insightPanelWidth.value = clampInsightPanelWidth(raw)
  } catch {
    // 使用默认宽度。
  }
}

async function attachScreenShareTrack(track: typeof activeScreenShareTrack) {
  if (!track) return
  activeScreenShareTrack = track
  screenShareVideoReady.value = !!track
  await nextTick()
  attachTrackToElement(track, screenShareVideoRef.value)
}

function clearScreenShareTrack(track?: typeof activeScreenShareTrack) {
  if (track) {
    detachTrack(track, screenShareVideoRef.value)
  } else if (activeScreenShareTrack) {
    detachTrack(activeScreenShareTrack, screenShareVideoRef.value)
  }
  activeScreenShareTrack = null
  screenShareVideoReady.value = false
  if (screenShareVideoRef.value) screenShareVideoRef.value.srcObject = null
}

async function startLocalScreenShare() {
  if (!roomClient || screenShareBusy.value) return
  screenShareBusy.value = true
  screenShareError.value = null
  try {
    const publication = await roomClient.setScreenShareEnabled(true)
    localScreenShareOn.value = true
    remoteScreenShareConnected.value = false
    screenShareOwnerName.value = '我'
    if (publication?.track) {
      await attachScreenShareTrack(publication.track)
    }
  } catch (e) {
    localScreenShareOn.value = false
    screenShareError.value = e instanceof Error ? e.message : '共享屏幕启动失败'
  } finally {
    screenShareBusy.value = false
  }
}

async function stopLocalScreenShare() {
  if (!roomClient || screenShareBusy.value) return
  screenShareBusy.value = true
  screenShareError.value = null
  try {
    await roomClient.setScreenShareEnabled(false)
  } catch (e) {
    screenShareError.value = e instanceof Error ? e.message : '停止共享屏幕失败'
  } finally {
    localScreenShareOn.value = false
    clearScreenShareTrack()
    screenShareOwnerName.value = remoteScreenShareConnected.value ? screenShareOwnerName.value : ''
    screenShareBusy.value = false
  }
}

async function toggleScreenShare() {
  if (localScreenShareOn.value) {
    await stopLocalScreenShare()
    return
  }
  await startLocalScreenShare()
}

function installScreenShareE2eHook() {
  if (!import.meta.env.DEV && import.meta.env.MODE !== 'test') return
  window.addEventListener('live-screen-share-e2e', async (event) => {
    const detail = event instanceof CustomEvent ? event.detail as { owner?: 'local' | 'remote' | 'stop'; ownerName?: string } : {}
    if (detail.owner === 'stop') {
      localScreenShareOn.value = false
      remoteScreenShareConnected.value = false
      screenShareOwnerName.value = ''
      clearScreenShareTrack()
      return
    }
    const fakeTrack = {
      kind: Track.Kind.Video,
      attach(element: HTMLMediaElement) {
        element.srcObject = new MediaStream()
        return element
      },
      detach(element?: HTMLMediaElement) {
        if (element) element.srcObject = null
        return []
      },
    } as unknown as typeof activeScreenShareTrack
    localScreenShareOn.value = detail.owner === 'local'
    remoteScreenShareConnected.value = detail.owner === 'remote'
    screenShareOwnerName.value = detail.owner === 'local' ? '我' : detail.ownerName || '对方'
    await attachScreenShareTrack(fakeTrack)
  })
}

async function flushWhiteboard(finalize = false) {
  const promises: Promise<unknown>[] = []
  window.dispatchEvent(new CustomEvent('live-whiteboard-flush', { detail: { promises, finalize } }))
  if (promises.length) {
    await Promise.allSettled(promises)
  }
}

function readAiRawNumber(key: string) {
  const value = aiState.value?.rawState?.[key]
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

const aiSegmentCount = computed(() => Number(aiState.value?.segmentCount ?? readAiRawNumber('segmentCount') ?? 0))
const aiLastLlmSegmentCount = computed(() => Number(aiState.value?.lastLlmSegmentCount ?? readAiRawNumber('lastLlmSegmentCount') ?? 0))
const aiNewSegmentCount = computed(() => Math.max(0, aiSegmentCount.value - aiLastLlmSegmentCount.value))

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}

function cleanText(value: unknown, fallback = '') {
  const text = typeof value === 'string' || typeof value === 'number' ? String(value).trim() : ''
  return text || fallback
}

function normalizeMinuteSection(raw: LiveAiMinuteSection | Record<string, unknown>, index: number): DisplayMinuteSection | null {
  if (!isRecord(raw)) return null
  const title = cleanText(raw.title, index === 0 ? '课堂开始' : `课堂阶段 ${index + 1}`)
  const summary = cleanText(raw.summary, title)
  const rawItems = Array.isArray(raw.items) ? raw.items : []
  const items = rawItems
    .filter(isRecord)
    .map((item) => ({
      title: cleanText(item.title || item.heading, '阶段重点'),
      detail: cleanText(item.detail || item.content, ''),
    }))
    .filter((item) => item.detail)
    .slice(0, 6)
  return {
    id: cleanText(raw.id, `section-${index + 1}`),
    title,
    summary,
    items,
  }
}

const aiMinutesSections = computed<DisplayMinuteSection[]>(() => {
  const typed = Array.isArray(aiState.value?.minutesOutline) ? aiState.value?.minutesOutline || [] : []
  const raw = Array.isArray(aiState.value?.rawState?.minutesOutline) ? aiState.value?.rawState?.minutesOutline || [] : []
  const source = typed.length ? typed : raw
  const sections = source
    .map((item, index) => normalizeMinuteSection(item as LiveAiMinuteSection | Record<string, unknown>, index))
    .filter((item): item is DisplayMinuteSection => !!item)
  if (sections.length) return sections
  if (aiState.value?.latestStageSummary) {
    return [
      {
        id: 'latest-stage',
        title: aiState.value.currentTopic || '课堂阶段总结',
        summary: aiState.value.latestStageSummary,
        items: (aiState.value.keyPoints || []).slice(0, 4).map((item, index) => ({
          title: index === 0 ? '本段重点' : `重点 ${index + 1}`,
          detail: item,
        })),
      },
    ]
  }
  return []
})

const aiHasMinutes = computed(() => aiMinutesSections.value.length > 0)
const aiActiveSectionTitle = computed(() => {
  const last = aiMinutesSections.value[aiMinutesSections.value.length - 1]
  return aiState.value?.activeSectionTitle || last?.title || '课堂内容'
})
const aiIsComposing = computed(() => !!aiState.value?.realtimeEnabled && aiNewSegmentCount.value > 0)
const aiComposerText = computed(() => {
  if (!aiState.value?.realtimeEnabled) return '本节课未开启 AI 实时纪要'
  if (!aiHasMinutes.value) return 'AI 正在聆听课堂，形成第一段纪要'
  return `AI 正在整理「${aiActiveSectionTitle.value}」的后续内容`
})

async function loadAiState(sessionId: number) {
  try {
    aiState.value = await liveApi.aiState(sessionId)
    aiError.value = null
  } catch (e) {
    aiError.value = e instanceof Error ? e.message : 'AI 状态暂时不可用'
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    session.value = await liveApi.getByCourse(courseId.value)
    if (session.value?.sessionId) {
      await loadAiState(session.value.sessionId)
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课堂失败'
  } finally {
    loading.value = false
  }
}

async function refreshStatus() {
  if (!session.value?.sessionId) return
  try {
    session.value = await liveApi.status(session.value.sessionId)
    await loadAiState(session.value.sessionId)
  } catch {
    // 保持当前课堂状态，避免轮询报错打断课堂。
  }
}

const aiStatusText = computed(() => {
  const status = String(aiState.value?.aiStatus || '').trim().toUpperCase()
  if (status === 'ACTIVE') return '实时纪要生成中'
  if (status === 'ASR_DEGRADED' || status === 'LLM_DEGRADED') return 'AI 正在等待更多课堂内容'
  if (status === 'FAILED') return 'AI 纪要暂不可用'
  if (status === 'OFF') return '本节课未开启课堂 AI'
  return 'AI 正在准备课堂纪要...'
})

function bindRoomEvents(client: LiveRoomClient) {
  client.onLocalTrackPublished((publication) => {
    if (publication.source === Track.Source.ScreenShare && publication.track?.kind === Track.Kind.Video) {
      localScreenShareOn.value = true
      remoteScreenShareConnected.value = false
      screenShareOwnerName.value = '我'
      void attachScreenShareTrack(publication.track)
      return
    }
    if (publication.track?.kind === Track.Kind.Video) {
      attachTrackToElement(publication.track, localVideoRef.value)
    }
  })
  client.onLocalTrackUnpublished((publication) => {
    if (publication.source === Track.Source.ScreenShare) {
      localScreenShareOn.value = false
      clearScreenShareTrack(publication.track)
      screenShareOwnerName.value = remoteScreenShareConnected.value ? screenShareOwnerName.value : ''
    }
  })
  client.onParticipantConnected((participant) => {
    remoteParticipantJoined.value = true
    remoteParticipantName.value = participant.name || participant.identity || '对方'
    syncRemoteStateFromRoom(client)
  })
  client.onParticipantDisconnected(() => {
    syncRemoteStateFromRoom(client)
  })
  client.onTrackSubscribed((track, _publication, participant) => {
    remoteParticipantName.value = participant.name || participant.identity || '对方'
    if (_publication.source === Track.Source.ScreenShare && track.kind === Track.Kind.Video) {
      remoteScreenShareConnected.value = true
      if (!localScreenShareOn.value) {
        screenShareOwnerName.value = participant.name || participant.identity || '对方'
        void attachScreenShareTrack(track)
      }
      return
    }
    if (_publication.source === Track.Source.ScreenShareAudio) {
      attachTrackToElement(track, remoteAudioRef.value)
      remoteAudioConnected.value = true
      return
    }
    if (track.kind === Track.Kind.Video) {
      attachTrackToElement(track, remoteVideoRef.value)
      remoteVideoConnected.value = true
      return
    }
    attachTrackToElement(track, remoteAudioRef.value)
    remoteAudioConnected.value = true
  })
  client.onTrackUnsubscribed((track, publication) => {
    if (publication.source === Track.Source.ScreenShare) {
      if (!localScreenShareOn.value) {
        clearScreenShareTrack(track)
        screenShareOwnerName.value = ''
      }
      remoteScreenShareConnected.value = false
      syncRemoteStateFromRoom(client)
      return
    }
    if (track.kind === Track.Kind.Video) {
      detachTrack(track, remoteVideoRef.value)
      remoteVideoConnected.value = false
      syncRemoteStateFromRoom(client)
      return
    }
    detachTrack(track, remoteAudioRef.value)
    remoteAudioConnected.value = false
    syncRemoteStateFromRoom(client)
  })
  client.onTrackUnpublished((publication) => {
    if (publication.source === Track.Source.ScreenShare) {
      remoteScreenShareConnected.value = false
      if (!localScreenShareOn.value) {
        clearScreenShareTrack(publication.track)
        screenShareOwnerName.value = ''
      }
    }
  })
  client.onConnectionStateChanged((state) => {
    if (state === 'connected' || state === 'reconnecting' || state === 'disconnected') {
      connectionState.value = state
      return
    }
    connectionState.value = 'connecting'
  })
  client.onDisconnected(() => {
    connectionState.value = 'disconnected'
  })
  client.onMediaError((mediaError) => {
    error.value = mediaError.message
  })
}

function buildMediaWarning(result?: { cameraError?: Error | null; micError?: Error | null } | null) {
  if (!result) return null
  const messages: string[] = []
  if (result.cameraError) {
    messages.push(`摄像头不可用：${result.cameraError.message}`)
  }
  if (result.micError) {
    messages.push(`麦克风不可用：${result.micError.message}`)
  }
  return messages.length ? messages.join('；') : null
}

async function connectRoom() {
  if (!session.value?.sessionId || connecting.value) return
  connecting.value = true
  error.value = null
  connectionState.value = 'connecting'
  try {
    const tokenResp = await liveApi.joinToken(session.value.sessionId, {
      clientType: 'WEB',
      deviceFingerprint: `web-${Date.now()}`,
      joinMode: 'CLASSROOM',
    })
    const client = new LiveRoomClient()
    roomClient = client
    roomClientView.value = client
    bindRoomEvents(client)
    const connectResult = await client.connect({
      serverUrl: tokenResp.serverUrl,
      token: tokenResp.accessToken,
      cameraEnabled: camOn.value,
      micEnabled: micOn.value,
      cameraDeviceId: selectedCameraId.value || null,
      micDeviceId: selectedMicId.value || null,
    })
    const mediaWarning = buildMediaWarning(connectResult)
    if (mediaWarning) {
      error.value = mediaWarning
    }
    await attachLocalPreview(client)
    syncRemoteStateFromRoom(client)
    try {
      session.value = await liveApi.joinAck(session.value.sessionId, {
        clientType: 'WEB',
        joinMode: 'CLASSROOM',
        connectionState: 'CONNECTED',
        cameraEnabled: camOn.value,
        micEnabled: micOn.value,
        cameraDeviceId: selectedCameraId.value || null,
        micDeviceId: selectedMicId.value || null,
      })
    } catch {
      // webhook 未及时回流时，前端至少依靠房间内远端参与者状态保持课堂可用。
    }
    connectionState.value = 'connected'
    await refreshStatus()
    await startAiAudioIfNeeded()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '连接课堂失败'
    connectionState.value = 'disconnected'
  } finally {
    connecting.value = false
  }
}

async function startAiAudioIfNeeded() {
  if (!session.value?.sessionId || aiAudioUplink || !micOn.value) return
  if (!session.value.realtimeSummaryEnabled) return
  try {
    aiAudioUplink = await startLiveAiAudioUplink({
      sessionId: session.value.sessionId,
      micDeviceId: selectedMicId.value || null,
    })
  } catch (e) {
    aiError.value = e instanceof Error ? e.message : 'AI 音频采集启动失败'
  }
}

function stopAiAudioUplink() {
  aiAudioUplink?.stop()
  aiAudioUplink = null
}

async function toggleMic() {
  if (!roomClient) {
    micOn.value = !micOn.value
    return
  }
  micOn.value = !micOn.value
  await roomClient.setMicrophoneEnabled(micOn.value, selectedMicId.value || null)
  if (micOn.value) {
    await startAiAudioIfNeeded()
  } else {
    stopAiAudioUplink()
  }
  saveLiveMediaPreferences({
    ...readLiveMediaPreferences(),
    micEnabled: micOn.value,
  })
}

async function toggleCamera() {
  if (!roomClient) {
    camOn.value = !camOn.value
    return
  }
  camOn.value = !camOn.value
  const publication = await roomClient.setCameraEnabled(camOn.value, selectedCameraId.value || null)
  if (publication?.track) {
    attachTrackToElement(publication.track, localVideoRef.value)
  } else if (localVideoRef.value) {
    localVideoRef.value.srcObject = null
  }
  saveLiveMediaPreferences({
    ...readLiveMediaPreferences(),
    cameraEnabled: camOn.value,
  })
}

async function leaveClass() {
  if (!session.value?.sessionId) return
  await flushWhiteboard(false)
  stopAiAudioUplink()
  if (localScreenShareOn.value) await stopLocalScreenShare()
  await roomClient?.disconnect()
  roomClientView.value = null
  await liveApi.leave(session.value.sessionId, { leaveReason: 'USER_BACK', connectionState: connectionState.value.toUpperCase() })
  await router.push({ name: 'myCourses' })
}

function clearEndConfirmTimer() {
  if (endConfirmTimer != null) {
    window.clearInterval(endConfirmTimer)
    endConfirmTimer = null
  }
}

function openEndClassConfirm() {
  if (!session.value?.sessionId || endConfirmBusy.value) return
  clearEndConfirmTimer()
  endConfirmOpen.value = true
  endConfirmCountdown.value = 3
  endConfirmError.value = null
  endConfirmTimer = window.setInterval(() => {
    endConfirmCountdown.value = Math.max(0, endConfirmCountdown.value - 1)
    if (endConfirmCountdown.value <= 0) clearEndConfirmTimer()
  }, 1000)
}

function closeEndClassConfirm() {
  if (endConfirmBusy.value) return
  endConfirmOpen.value = false
  endConfirmError.value = null
  endConfirmCountdown.value = 3
  clearEndConfirmTimer()
}

async function confirmEndClass() {
  if (!session.value?.sessionId) return
  if (endConfirmBusy.value || endConfirmCountdown.value > 0) return
  endConfirmBusy.value = true
  endConfirmError.value = null
  try {
    await flushWhiteboard(true)
    stopAiAudioUplink()
    if (localScreenShareOn.value) await stopLocalScreenShare()
    await roomClient?.disconnect()
    roomClientView.value = null
    await liveApi.end(session.value.sessionId, { reason: 'MANUAL_END', confirm: true })
    endConfirmOpen.value = false
    await router.push({ name: 'myCourses' })
  } catch (e) {
    endConfirmError.value = e instanceof Error ? e.message : '结束课堂失败，请稍后重试'
  } finally {
    endConfirmBusy.value = false
  }
}

onMounted(async () => {
  installScreenShareE2eHook()
  restoreInsightPanelWidth()
  joinedAt.value = Date.now()
  await load()
  await connectRoom()
  pollTimer = window.setInterval(() => {
    void refreshStatus()
  }, 8000)
  clockTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(async () => {
  if (pollTimer != null) window.clearInterval(pollTimer)
  if (clockTimer != null) window.clearInterval(clockTimer)
  clearEndConfirmTimer()
  stopVideoDockDrag()
  stopInsightPanelDrag()
  stopAiAudioUplink()
  if (localScreenShareOn.value) {
    await roomClient?.setScreenShareEnabled(false).catch(() => undefined)
  }
  clearScreenShareTrack()
  await roomClient?.disconnect()
  roomClientView.value = null
})
</script>

<template>
  <div class="live-room workbench-shell">
    <header class="room-bar">
      <div class="room-brand">
        <div class="brand-dot"></div>
        <button class="course-selector" type="button">
          {{ classroomTitle }}
          <span>⌄</span>
        </button>
        <span class="state-pill" :class="connectionState">{{ classroomStatusLabel }}</span>
        <span class="elapsed">{{ classroomElapsed }}</span>
        <span
          class="connection-copy"
          :data-state="connectionState"
          data-testid="classroom-connection-state"
        >{{ connectionLabel }}</span>
      </div>

      <div class="room-actions">
        <button class="top-icon-btn" type="button" title="宫格视图">
          <span class="btn-icon">▦</span>
          <span>宫格视图</span>
        </button>
        <button class="top-icon-btn compact" type="button" title="成员">
          <span class="btn-icon">◌</span>
          <span>{{ memberCount }}</span>
        </button>
        <button class="top-icon-btn compact" type="button" title="设置" @click="sidebar = 'tech'">⚙</button>
        <button class="leave-room-btn" type="button" @click="leaveClass">离开课堂</button>
        <button class="end-room-btn" type="button" data-testid="classroom-end-button" @click="openEndClassConfirm">结束课堂</button>
      </div>
    </header>

    <div
      class="classroom-grid"
      :class="{ 'whiteboard-layout': isWhiteboardMode, 'resizing-insight': insightPanelDragging }"
      :style="classroomGridStyle"
    >
      <nav class="tool-rail" aria-label="课堂工具">
        <div class="tool-stack">
          <button
            v-for="item in toolItems"
            :key="item.key"
            class="rail-tool"
            :class="{ active: item.key === 'whiteboard' && isWhiteboardMode }"
            type="button"
            :title="item.label"
            :data-testid="item.key === 'whiteboard' ? 'classroom-open-whiteboard' : undefined"
            @click="handleToolClick(item.key)"
          >
            <span>{{ item.icon }}</span>
            <strong>{{ item.label }}</strong>
          </button>
        </div>
        <div class="tool-stack utility">
          <button
            v-for="item in utilityItems"
            :key="item.key"
            class="rail-tool"
            type="button"
            :title="item.label"
            @click="sidebar = item.key === 'chat' ? 'chat' : 'info'"
          >
            <span>{{ item.icon }}</span>
            <strong>{{ item.label }}</strong>
          </button>
        </div>
      </nav>

      <main class="stage-column">
        <section
          class="stage-panel"
          :class="{ 'whiteboard-active': stageMode === 'whiteboard', 'screen-share-active': stageMode === 'screen' }"
        >
          <section v-if="stageMode === 'screen'" class="screen-share-stage" data-testid="live-screen-share-stage">
            <div class="screen-share-topline">
              <div>
                <span class="screen-share-kicker">屏幕共享</span>
                <strong>{{ screenShareLabel }}</strong>
              </div>
              <button
                v-if="localScreenShareOn"
                class="stop-share-btn"
                type="button"
                data-testid="classroom-stop-screen-share"
                :disabled="screenShareBusy"
                @click="stopLocalScreenShare"
              >
                停止共享
              </button>
            </div>
            <div class="screen-share-canvas">
              <video
                ref="screenShareVideoRef"
                class="screen-share-video"
                autoplay
                muted
                playsinline
                data-testid="screen-share-video"
              />
              <div v-if="!screenShareVideoReady" class="screen-share-placeholder">
                <strong>正在等待共享画面</strong>
                <span>画面建立后会自动铺满主舞台。</span>
              </div>
            </div>
          </section>
          <section v-if="stageMode === 'whiteboard'" class="whiteboard-stage" data-testid="live-whiteboard-stage">
            <div class="whiteboard-topline">
              <div>
                <span class="whiteboard-kicker">课堂白板</span>
                <strong>本节课专属白板</strong>
              </div>
              <div class="whiteboard-sync" :data-state="whiteboardStatus">
                <span></span>
                {{ whiteboardStatusText }}
              </div>
            </div>
            <LiveWhiteboardPanel
              v-if="session?.sessionId"
              :session-id="session.sessionId"
              :current-user="currentWhiteboardUser"
              :room-client="roomClientView"
              @status-change="handleWhiteboardStatusChange"
            />
          </section>
          <div
            class="stage-screen"
            :class="{ dragging: videoDockDragging }"
            :style="videoDockStyle"
            data-testid="remote-stage"
            @pointerdown="startVideoDockDrag"
            @dblclick="resetVideoDockPosition"
          >
            <div v-if="isVideoDockMode" class="video-dock-grip">
              <span></span>
              <strong>拖动视频窗</strong>
            </div>
            <div v-if="!remoteVideoConnected" class="waiting">
              <div class="waiting-title">{{ peerJoinedState ? `${remoteParticipantName} 已加入，等待视频画面` : '已进入课堂，正在等待对方加入' }}</div>
              <div class="waiting-desc">
                {{ peerJoinedState ? '对方已经进入同一课堂，当前正在等待远端视频轨道或画面恢复。' : '你可以先确认本地设备已开启，或回到聊天提醒对方入会。' }}
              </div>
            </div>
            <video
              v-show="remoteVideoConnected"
              ref="remoteVideoRef"
              class="stage-video"
              autoplay
              playsinline
              data-testid="remote-video"
            />
            <div class="speaker-badge">
              <span>{{ remoteParticipantName }}</span>
              <span :class="{ muted: !remoteAudioConnected }">{{ remoteAudioConnected ? '●' : '×' }}</span>
            </div>
            <span class="sr-only" :data-connected="remoteVideoConnected" data-testid="remote-video-state">远端视频</span>
            <span class="sr-only" :data-connected="remoteAudioConnected" data-testid="remote-audio-state">远端音频</span>
            <audio ref="remoteAudioRef" autoplay data-testid="remote-audio" />
          </div>
          <div
            class="self-tile"
            :class="{ dragging: videoDockDragging, 'camera-off': !camOn }"
            :style="videoDockStyle"
            data-testid="local-stage"
            @pointerdown="startVideoDockDrag"
            @dblclick="resetVideoDockPosition"
          >
            <video
              v-show="camOn"
              ref="localVideoRef"
              class="self-video"
              autoplay
              muted
              playsinline
              data-testid="local-video"
            />
            <div v-if="!camOn" class="self-placeholder">摄像头关闭</div>
            <div class="self-badge">
              <span>我</span>
              <span :class="{ muted: !micOn }">{{ micOn ? '●' : '×' }}</span>
            </div>
          </div>
        </section>

        <footer class="meeting-controls">
          <button class="ctl" :class="{ off: !micOn }" type="button" data-testid="classroom-toggle-mic" @click="toggleMic">
            <span>{{ micOn ? '●' : '×' }}</span>
            <strong>{{ micOn ? '静音' : '取消静音' }}</strong>
          </button>
          <button class="ctl" :class="{ off: !camOn }" type="button" data-testid="classroom-toggle-camera" @click="toggleCamera">
            <span>{{ camOn ? '▣' : '▧' }}</span>
            <strong>{{ camOn ? '关闭视频' : '开启视频' }}</strong>
          </button>
          <button
            class="ctl"
            :class="{ active: localScreenShareOn }"
            type="button"
            data-testid="classroom-toggle-screen-share"
            :disabled="screenShareBusy || (!roomClientView && !localScreenShareOn)"
            @click="toggleScreenShare"
          >
            <span>{{ localScreenShareOn ? '■' : '▤' }}</span>
            <strong>{{ localScreenShareOn ? '停止共享' : '共享屏幕' }}</strong>
          </button>
          <button class="ctl" type="button" @click="sidebar = 'info'">
            <span>＋</span>
            <strong>邀请</strong>
          </button>
          <button class="ctl" type="button" @click="sidebar = 'info'">
            <span>◌</span>
            <strong>成员({{ memberCount }})</strong>
          </button>
          <button class="ctl" type="button" data-testid="classroom-open-chat" @click="sidebar = 'chat'">
            <span>□</span>
            <strong>聊天</strong>
          </button>
          <button class="ctl" type="button">
            <span>◎</span>
            <strong>录制</strong>
          </button>
          <button class="ctl" type="button">
            <span>...</span>
            <strong>更多</strong>
          </button>
        </footer>
      </main>

      <aside class="insight-panel" :class="{ 'whiteboard-compact': isWhiteboardMode, resizing: insightPanelDragging }">
        <button
          class="insight-resizer"
          type="button"
          aria-label="调整实时总结面板宽度"
          title="拖动调整面板宽度"
          data-testid="insight-panel-resizer"
          @pointerdown="startInsightPanelDrag"
        >
          <span></span>
        </button>
        <div class="panel-head">
          <div class="panel-title">
            <span class="panel-mark">✦</span>
            <strong>{{ sidebar === 'chat' ? '课中聊天' : sidebar === 'tech' ? '技术状态' : sidebar === 'info' ? '课程信息' : '实时总结' }}</strong>
          </div>
          <div class="panel-actions">
            <button type="button">...</button>
            <button type="button" @click="sidebar = 'ai'">×</button>
          </div>
        </div>

        <div v-if="sidebar === 'ai' || sidebar === 'info'" class="panel-tabs">
          <button class="tab" :class="{ active: sidebar === 'ai' }" type="button" @click="sidebar = 'ai'">AI 总结</button>
          <button class="tab" :class="{ active: sidebar === 'info' }" type="button" @click="sidebar = 'info'">课堂纪要</button>
        </div>

        <div v-if="sidebar === 'ai'" class="summary-timeline">
          <section class="minutes-hero" data-testid="ai-live-summary-orbit">
            <div class="minutes-hero-copy">
              <div>
                <div class="minutes-kicker">AI 实时纪要</div>
                <strong>{{ aiStatusText }}</strong>
              </div>
              <p>{{ aiComposerText }}</p>
            </div>
            <div class="composer-orb" :class="{ active: aiIsComposing }" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </section>

          <div v-if="aiHasMinutes" class="minutes-tree">
            <article
              v-for="(section, index) in aiMinutesSections"
              :key="section.id"
              class="minute-section"
              :class="{ current: index === aiMinutesSections.length - 1 }"
              :style="{ '--delay': `${index * 70}ms` }"
            >
              <div class="minute-spine">
                <span class="minute-node"></span>
              </div>
              <div class="minute-content">
                <h3>{{ section.title }}</h3>
                <p class="minute-summary">{{ section.summary }}</p>
                <div v-if="section.items.length" class="minute-items">
                  <section
                    v-for="(item, itemIndex) in section.items"
                    :key="`${section.id}-${item.title}-${itemIndex}`"
                    class="minute-item"
                    :style="{ '--delay': `${index * 70 + itemIndex * 45}ms` }"
                  >
                    <h4>{{ item.title }}</h4>
                    <p>{{ item.detail }}</p>
                  </section>
                </div>
              </div>
            </article>
          </div>

          <section v-else class="minute-empty">
            <div class="typing-line">
              <span>正在生成第一段课堂纪要</span>
              <i></i>
            </div>
            <p>课堂开始后，AI 会按讲解主题自动拆分大标题，并在每个主题下补充小标题和说明。</p>
          </section>

          <section class="minute-composer" :class="{ active: aiIsComposing }">
            <span class="typing-cursor"></span>
            <p>{{ aiComposerText }}</p>
          </section>

          <div v-if="aiError" class="hint error">{{ aiError }}</div>
          <div class="ai-disclaimer">AI 生成内容仅供参考</div>
        </div>

        <div v-else-if="sidebar === 'info'" class="side-section">
          <article class="note-card">
            <div class="note-label">课堂状态</div>
            <div class="note-value">{{ classroomStatusLabel }} · {{ memberCount }} 人在线</div>
            <p>{{ peerJoinedState ? `${remoteParticipantName} 已进入课堂，可以继续推进授课。` : '正在等待对方进入课堂，建议先完成设备检查。' }}</p>
          </article>
          <article class="note-card">
            <div class="note-label">课程房间</div>
            <div class="note-value">{{ session?.providerRoomName || '—' }}</div>
            <p>系统会持续同步音视频与课中消息，退出前无需手动保存课堂状态。</p>
          </article>
          <article class="note-card">
            <div class="note-label">待跟进</div>
            <ul v-if="aiState?.homeworkCandidates?.length" class="ai-list">
              <li v-for="item in aiState?.homeworkCandidates || []" :key="item">{{ item }}</li>
            </ul>
            <p v-else>暂无作业或课后跟进建议。</p>
          </article>
        </div>

        <div v-else-if="sidebar === 'tech'" class="side-section">
          <div class="row"><span>麦克风</span><strong>{{ micOn ? '开启' : '关闭' }}</strong></div>
          <div class="row"><span>摄像头</span><strong>{{ camOn ? '开启' : '关闭' }}</strong></div>
          <div class="row"><span>远端音频</span><strong>{{ remoteAudioConnected ? '已接收' : '未接收' }}</strong></div>
          <div class="row"><span>远端视频</span><strong>{{ remoteVideoConnected ? '已接收' : '未接收' }}</strong></div>
          <div class="row"><span>屏幕共享</span><strong>{{ localScreenShareOn ? '我正在共享' : remoteScreenShareConnected ? `${screenShareOwnerName || '对方'} 正在共享` : '未共享' }}</strong></div>
          <div class="row"><span>连接状态</span><strong>{{ connectionState }}</strong></div>
          <div v-if="error" class="hint error">{{ error }}</div>
          <div v-if="screenShareError" class="hint error">{{ screenShareError }}</div>
        </div>

        <div v-else class="chat-panel">
          <LiveClassChatPanel
            :room-id="roomId"
            :peer-name="remoteParticipantName"
          />
        </div>

      </aside>
    </div>

    <div v-if="endConfirmOpen" class="end-confirm-mask" data-testid="end-classroom-modal" @click.self="closeEndClassConfirm">
      <section class="end-confirm-card" role="dialog" aria-modal="true" aria-labelledby="end-confirm-title">
        <div class="end-confirm-signal" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="end-confirm-copy">
          <div class="end-confirm-kicker">结束课堂确认</div>
          <h2 id="end-confirm-title">确认结束当前课堂吗？</h2>
          <p>结束后双方会退出实时课堂，系统将进入课后总结与课堂记录整理流程。为避免误触，确认按钮需等待 3 秒后才能点击。</p>
        </div>
        <div class="end-confirm-warning">
          <strong>请确认：</strong>
          <span>本节课已完成，且不需要继续等待对方重新连线。</span>
        </div>
        <div v-if="endConfirmError" class="end-confirm-error">{{ endConfirmError }}</div>
        <div class="end-confirm-actions">
          <button class="end-confirm-cancel" type="button" :disabled="endConfirmBusy" @click="closeEndClassConfirm">取消</button>
          <button
            class="end-confirm-submit"
            type="button"
            data-testid="end-classroom-confirm"
            :disabled="endConfirmBusy || endConfirmCountdown > 0"
            @click="confirmEndClass"
          >
            {{ endConfirmBusy ? '正在结束...' : endConfirmCountdown > 0 ? `确认结束（${endConfirmCountdown}s）` : '确认结束课堂' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.workbench-shell {
  height: 100vh;
  height: 100dvh;
  min-height: 680px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  padding: 18px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.94), rgba(241, 246, 252, 0.96)),
    #f4f7fb;
  color: #151b2d;
}

.room-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.room-brand,
.room-actions,
.classroom-grid,
.meeting-controls,
.panel-head,
.panel-title,
.panel-actions,
.ai-status-row {
  display: flex;
  align-items: center;
}

.room-brand,
.room-actions {
  gap: 12px;
}

.brand-dot {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background:
    radial-gradient(circle at 35% 35%, #ffffff 0 18%, transparent 19%),
    linear-gradient(135deg, #7267ff, #31d0b5);
  box-shadow: 0 10px 24px rgba(49, 102, 220, 0.16);
}

.course-selector,
.top-icon-btn,
.leave-room-btn,
.end-room-btn,
.ctl,
.rail-tool,
.panel-actions button {
  border: 0;
  cursor: pointer;
  font-weight: 800;
}

.course-selector {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  color: #121827;
  font-size: 18px;
}

.state-pill {
  height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #16803a;
  font-size: 13px;
}

.state-pill.reconnecting,
.state-pill.connecting {
  background: rgba(245, 158, 11, 0.14);
  color: #a16207;
}

.state-pill.disconnected {
  background: rgba(239, 68, 68, 0.12);
  color: #b42318;
}

.elapsed,
.connection-copy {
  font-size: 14px;
  color: rgba(21, 27, 45, 0.72);
}

.connection-copy {
  padding-left: 4px;
}

.top-icon-btn {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border-radius: 8px;
  background: #fff;
  color: #1f2937;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.top-icon-btn.compact {
  min-width: 44px;
  justify-content: center;
}

.btn-icon {
  font-size: 16px;
}

.leave-room-btn,
.end-room-btn {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 8px;
}

.leave-room-btn {
  background: #ffffff;
  color: #273244;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.end-room-btn {
  background: #fff1ee;
  color: #ef3b2d;
}

.end-confirm-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 50% 42%, rgba(255, 255, 255, 0.22), transparent 32%),
    rgba(13, 19, 30, 0.56);
  backdrop-filter: blur(14px);
  animation: modalFade 180ms ease both;
}

.end-confirm-card {
  position: relative;
  width: min(460px, 100%);
  overflow: hidden;
  padding: 28px;
  border-radius: 20px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96)),
    #fff;
  box-shadow: 0 34px 90px rgba(15, 23, 42, 0.28);
  animation: modalRise 220ms ease both;
}

.end-confirm-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 5px;
  background: linear-gradient(90deg, #ef3b2d, #f59e0b, #42d3a9);
}

.end-confirm-signal {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: #fff1ee;
}

.end-confirm-signal span {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #ef3b2d;
  animation: signalPulse 1.5s ease-out infinite;
}

.end-confirm-signal span:nth-child(2) {
  animation-delay: 0.28s;
}

.end-confirm-signal span:nth-child(3) {
  animation-delay: 0.56s;
}

.end-confirm-copy {
  max-width: 330px;
}

.end-confirm-kicker {
  color: #ef3b2d;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.end-confirm-copy h2 {
  margin: 10px 0 0;
  color: #121827;
  font-size: 26px;
  line-height: 1.25;
}

.end-confirm-copy p,
.end-confirm-warning {
  color: rgba(31, 41, 55, 0.72);
  line-height: 1.75;
}

.end-confirm-copy p {
  margin: 14px 0 0;
}

.end-confirm-warning {
  display: grid;
  gap: 4px;
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 241, 238, 0.9), rgba(255, 251, 235, 0.88));
  border: 1px solid rgba(239, 59, 45, 0.12);
}

.end-confirm-warning strong {
  color: #9f241a;
}

.end-confirm-error {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.1);
  color: #b42318;
  font-weight: 800;
}

.end-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

.end-confirm-cancel,
.end-confirm-submit {
  min-height: 42px;
  border: 0;
  border-radius: 12px;
  padding: 0 18px;
  font-weight: 900;
  cursor: pointer;
}

.end-confirm-cancel {
  background: #f3f5f8;
  color: #263244;
}

.end-confirm-submit {
  min-width: 150px;
  background: linear-gradient(135deg, #ef3b2d, #f97316);
  color: #fff;
  box-shadow: 0 14px 32px rgba(239, 59, 45, 0.24);
}

.end-confirm-cancel:disabled,
.end-confirm-submit:disabled {
  cursor: not-allowed;
  opacity: 0.58;
  box-shadow: none;
}

.classroom-grid {
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr) minmax(300px, var(--insight-panel-width, 392px));
  gap: 16px;
  align-items: stretch;
}

.classroom-grid.resizing-insight,
.classroom-grid.resizing-insight * {
  cursor: col-resize;
  user-select: none;
}

.tool-rail,
.insight-panel {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.tool-rail {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 14px 10px;
}

.tool-stack {
  display: grid;
  gap: 12px;
}

.rail-tool {
  min-height: 58px;
  display: grid;
  place-items: center;
  gap: 4px;
  border-radius: 8px;
  background: transparent;
  color: #293244;
}

.rail-tool:hover {
  background: rgba(45, 98, 242, 0.08);
}

.rail-tool.active {
  color: #0f5f52;
  background:
    radial-gradient(circle at 76% 18%, rgba(66, 211, 169, 0.24), transparent 30%),
    linear-gradient(135deg, rgba(237, 252, 244, 0.96), rgba(239, 246, 255, 0.94));
  box-shadow: inset 0 0 0 1px rgba(34, 197, 94, 0.18), 0 12px 26px rgba(15, 118, 110, 0.1);
}

.rail-tool span {
  font-size: 22px;
  line-height: 1;
}

.rail-tool strong {
  font-size: 12px;
  line-height: 1;
}

.stage-column {
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-rows: minmax(420px, 1fr) auto;
  gap: 14px;
}

.stage-panel {
  position: relative;
  min-height: 420px;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #101822;
  box-shadow: 0 22px 54px rgba(15, 23, 42, 0.12);
}

.stage-panel.whiteboard-active {
  background:
    radial-gradient(circle at 18% 14%, rgba(66, 211, 169, 0.16), transparent 30%),
    linear-gradient(145deg, #f7f0df, #fffaf0 48%, #f6fbff);
  box-shadow: 0 24px 58px rgba(70, 55, 23, 0.12);
}

.whiteboard-stage {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  padding: 14px;
}

.screen-share-stage {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  background:
    radial-gradient(circle at 18% 12%, rgba(45, 98, 242, 0.16), transparent 30%),
    linear-gradient(145deg, #111827, #172033 54%, #0f1722);
}

.screen-share-topline {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
  backdrop-filter: blur(12px);
}

.screen-share-kicker {
  display: block;
  margin-bottom: 2px;
  color: #67e8f9;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.screen-share-topline strong {
  font-size: 18px;
}

.stop-share-btn {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.stop-share-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.screen-share-canvas {
  position: relative;
  min-height: 0;
  overflow: hidden;
  border-radius: 16px;
  background: #050816;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.screen-share-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #050816;
}

.screen-share-placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.82);
  text-align: center;
}

.screen-share-placeholder strong {
  font-size: 22px;
}

.whiteboard-topline {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(120, 96, 52, 0.12);
  backdrop-filter: blur(12px);
}

.whiteboard-kicker {
  display: block;
  margin-bottom: 2px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.whiteboard-topline strong {
  color: #172033;
  font-size: 18px;
}

.whiteboard-sync {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(236, 253, 245, 0.92);
  font-size: 13px;
  font-weight: 900;
}

.whiteboard-sync span,
.whiteboard-sync-chip span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #10b981;
  box-shadow: 0 0 0 5px rgba(16, 185, 129, 0.14);
}

.whiteboard-sync[data-state='saving'] span,
.whiteboard-sync-chip[data-state='saving'] span,
.whiteboard-sync[data-state='loading'] span,
.whiteboard-sync-chip[data-state='loading'] span {
  background: #f59e0b;
  box-shadow: 0 0 0 5px rgba(245, 158, 11, 0.14);
  animation: whiteboardPulse 1s ease-in-out infinite;
}

.whiteboard-sync[data-state='offline'] span,
.whiteboard-sync-chip[data-state='offline'] span,
.whiteboard-sync[data-state='error'] span,
.whiteboard-sync-chip[data-state='error'] span {
  background: #ef4444;
  box-shadow: 0 0 0 5px rgba(239, 68, 68, 0.14);
}

.whiteboard-react-host,
:deep(.whiteboard-react-host) {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border-radius: 16px;
  background: #fffaf0;
  border: 1px solid rgba(120, 96, 52, 0.14);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.58);
}

:deep(.excalidraw) {
  --color-primary: #0f766e;
  --color-primary-darker: #0b5f59;
  --color-primary-darkest: #064e49;
  --color-primary-light: #ccfbf1;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

:deep(.whiteboard-sync-chip) {
  position: absolute;
  top: 12px;
  right: 14px;
  z-index: 10;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(15, 118, 110, 0.12);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
  font-size: 12px;
}

:deep(.whiteboard-loading) {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  color: #172033;
  background:
    radial-gradient(circle at 50% 42%, rgba(66, 211, 169, 0.16), transparent 28%),
    #fffaf0;
}

:deep(.whiteboard-loading-orb) {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 34% 30%, #fff 0 18%, transparent 20%),
    linear-gradient(135deg, #0f766e, #42d3a9);
  animation: whiteboardFloat 1.5s ease-in-out infinite;
}

.stage-screen {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at center, rgba(255, 255, 255, 0.08), transparent 32%),
    linear-gradient(145deg, #172333, #0f1722);
}

.stage-video,
.self-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.waiting {
  max-width: 460px;
  padding: 28px;
  text-align: center;
}

.waiting-title {
  font-size: 24px;
  font-weight: 900;
}

.waiting-desc {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.7;
}

.speaker-badge,
.self-badge {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 8px;
  color: #fff;
  background: rgba(17, 24, 39, 0.72);
  backdrop-filter: blur(10px);
}

.speaker-badge {
  left: 18px;
  bottom: 18px;
  padding: 10px 12px;
}

.self-tile {
  position: absolute;
  z-index: 3;
  right: 18px;
  bottom: 18px;
  width: min(230px, 26%);
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 16px 44px rgba(15, 23, 42, 0.28);
}

.self-badge {
  left: 10px;
  bottom: 10px;
  padding: 7px 9px;
  font-size: 12px;
}

.muted {
  color: #ef4444;
}

.self-placeholder {
  color: rgba(255, 255, 255, 0.82);
}

.meeting-controls {
  min-height: 56px;
  align-self: end;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.ctl {
  width: 66px;
  min-height: 44px;
  display: grid;
  place-items: center;
  gap: 3px;
  border-radius: 8px;
  background: transparent;
  color: #1f2937;
}

.ctl:hover {
  background: rgba(45, 98, 242, 0.08);
}

.ctl span {
  font-size: 17px;
  line-height: 1;
}

.ctl strong {
  font-size: 10.5px;
  white-space: nowrap;
}

.ctl.off {
  color: #e11d48;
}

.ctl.active {
  color: #0f766e;
  background: rgba(16, 185, 129, 0.1);
}

.ctl:disabled {
  cursor: not-allowed;
  opacity: 0.52;
}

.insight-panel {
  position: relative;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 18px;
  overflow-wrap: anywhere;
  transition: box-shadow 160ms ease;
}

.insight-panel.resizing {
  box-shadow: 0 22px 48px rgba(45, 98, 242, 0.16);
}

.insight-resizer {
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: -10px;
  z-index: 20;
  width: 18px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  cursor: col-resize;
}

.insight-resizer span {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 54px;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.2);
  transform: translate(-50%, -50%);
  transition: background 160ms ease, box-shadow 160ms ease, height 160ms ease;
}

.insight-resizer:hover span,
.insight-panel.resizing .insight-resizer span {
  height: 86px;
  background: #4f46e5;
  box-shadow: 0 0 0 6px rgba(79, 70, 229, 0.12);
}

.insight-panel.whiteboard-compact {
  height: 100%;
  max-height: 100%;
  min-height: 0;
}

.panel-head {
  min-width: 0;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  min-width: 0;
  gap: 8px;
  color: #172033;
  font-size: 17px;
}

.panel-title strong {
  min-width: 0;
}

.panel-mark {
  color: #4f46e5;
}

.panel-actions {
  gap: 8px;
}

.panel-actions button {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: transparent;
  color: #202938;
}

.panel-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  margin: 18px 0;
  padding: 4px;
  border-radius: 8px;
  background: #f1f3f7;
}

.tab {
  min-height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: rgba(31, 41, 55, 0.78);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.tab.active {
  color: #4f46e5;
  background: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.summary-timeline,
.side-section,
.chat-panel {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.summary-timeline {
  display: grid;
  grid-auto-rows: max-content;
  gap: 16px;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(79, 70, 229, 0.36) transparent;
}

.summary-timeline::-webkit-scrollbar,
.side-section::-webkit-scrollbar,
.chat-panel::-webkit-scrollbar {
  width: 8px;
}

.summary-timeline::-webkit-scrollbar-thumb,
.side-section::-webkit-scrollbar-thumb,
.chat-panel::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.26);
}

.whiteboard-compact .summary-timeline {
  max-height: 100%;
}

.minutes-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 14px;
  background:
    radial-gradient(circle at 86% 10%, rgba(66, 211, 169, 0.2), transparent 34%),
    linear-gradient(135deg, #f7fbf8 0%, #ffffff 48%, #f5f8ff 100%);
  border: 1px solid rgba(63, 167, 120, 0.12);
}

.minutes-hero::before {
  content: '';
  position: absolute;
  inset: auto 18px -28px auto;
  width: 118px;
  height: 118px;
  border-radius: 999px;
  background: rgba(214, 245, 222, 0.72);
  filter: blur(10px);
}

.minutes-hero-copy,
.composer-orb {
  position: relative;
}

.minutes-hero-copy {
  min-width: 0;
}

.minutes-hero strong,
.minutes-hero p,
.minute-summary,
.minute-item p,
.note-card p,
.row strong {
  overflow-wrap: anywhere;
}

.minutes-kicker {
  margin-bottom: 8px;
  color: #4f46e5;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.minutes-hero strong {
  color: #142036;
  font-size: 20px;
}

.minutes-hero p {
  margin: 10px 0 0;
  color: rgba(31, 41, 55, 0.66);
  line-height: 1.7;
}

.composer-orb {
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(79, 70, 229, 0.08), 0 12px 28px rgba(34, 88, 155, 0.08);
}

.composer-orb span {
  width: 7px;
  height: 7px;
  margin: 0 3px;
  border-radius: 50%;
  background: linear-gradient(180deg, #4f46e5, #42d3a9);
  animation: typingDot 1.2s ease-in-out infinite;
}

.composer-orb span:nth-child(2) {
  animation-delay: 0.16s;
}

.composer-orb span:nth-child(3) {
  animation-delay: 0.32s;
}

.minutes-tree {
  position: relative;
  display: grid;
  gap: 20px;
}

.minutes-tree::before {
  content: '';
  position: absolute;
  top: 6px;
  bottom: 6px;
  left: 22px;
  width: 2px;
  background: linear-gradient(180deg, rgba(76, 175, 109, 0.24), rgba(222, 112, 172, 0.26));
}

.minute-section {
  position: relative;
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 8px;
  animation: summaryRise 420ms ease both;
  animation-delay: var(--delay);
}

.minute-spine {
  position: relative;
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.minute-node {
  position: relative;
  z-index: 1;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #4f46e5;
  box-shadow: 0 0 0 7px rgba(79, 70, 229, 0.1);
}

.minute-section:nth-child(3n + 1) .minute-node {
  background: #16a34a;
  box-shadow: 0 0 0 7px rgba(22, 163, 74, 0.1);
}

.minute-section:nth-child(3n + 2) .minute-node {
  background: #be3c7a;
  box-shadow: 0 0 0 7px rgba(190, 60, 122, 0.1);
}

.minute-content {
  display: grid;
  gap: 10px;
}

.minute-content h3 {
  width: fit-content;
  max-width: 100%;
  margin: 0;
  padding: 8px 14px;
  border-radius: 6px;
  color: #16723c;
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.96), rgba(241, 253, 244, 0.8));
  box-shadow: 0 10px 26px rgba(22, 163, 74, 0.08);
  font-size: 18px;
  line-height: 1.35;
}

.minute-section:nth-child(3n + 2) h3 {
  color: #a82164;
  background: linear-gradient(180deg, rgba(253, 242, 248, 0.96), rgba(255, 247, 251, 0.8));
}

.minute-section:nth-child(3n) h3 {
  color: #3653bb;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.96), rgba(247, 250, 255, 0.86));
}

.minute-summary,
.minute-item {
  border-radius: 10px;
  background: rgba(245, 253, 246, 0.86);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.minute-summary {
  margin: 0;
  padding: 14px 16px;
  color: rgba(31, 41, 55, 0.82);
  line-height: 1.85;
}

.minute-items {
  display: grid;
  gap: 10px;
}

.minute-item {
  padding: 13px 15px;
  animation: summaryRise 360ms ease both;
  animation-delay: var(--delay);
}

.minute-item h4 {
  margin: 0 0 7px;
  color: #182034;
  font-size: 15px;
}

.minute-item p {
  margin: 0;
  color: rgba(31, 41, 55, 0.72);
  line-height: 1.75;
}

.minute-empty,
.minute-composer {
  border-radius: 12px;
  background: #f8fafc;
}

.minute-empty {
  padding: 22px 18px;
  border: 1px dashed rgba(79, 70, 229, 0.18);
}

.typing-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #182034;
  font-size: 18px;
  font-weight: 900;
}

.typing-line i {
  width: 34px;
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(79, 70, 229, 0.18), rgba(66, 211, 169, 0.34), rgba(79, 70, 229, 0.18));
  background-size: 200% 100%;
  animation: shimmer 1.3s linear infinite;
}

.minute-empty p {
  margin: 12px 0 0;
  color: rgba(31, 41, 55, 0.62);
  line-height: 1.8;
}

.minute-composer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  color: rgba(31, 41, 55, 0.58);
}

.minute-composer.active {
  color: #3653bb;
  background: linear-gradient(90deg, rgba(239, 246, 255, 0.9), rgba(240, 253, 244, 0.85));
}

.typing-cursor {
  width: 8px;
  height: 18px;
  border-radius: 999px;
  background: #4f46e5;
  animation: cursorBlink 0.9s steps(2, start) infinite;
}

.minute-composer p {
  margin: 0;
  line-height: 1.6;
}

.ai-status-row {
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(79, 70, 229, 0.08);
  color: #4f46e5;
}

.summary-card {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  padding: 14px;
  border-radius: 8px;
  background: #f8fafc;
  animation: summaryRise 360ms ease both;
}

.summary-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #4f46e5;
}

.summary-card.orange .summary-dot {
  background: #f97316;
}

.summary-card.green .summary-dot {
  background: #16a34a;
}

.summary-card.blue {
  background: #f3f6ff;
}

.summary-card.orange {
  background: #fff7ed;
}

.summary-card.green {
  background: #f0fdf4;
}

.summary-title {
  font-weight: 900;
}

.summary-card p,
.ai-list li {
  margin: 8px 0 0;
  color: rgba(31, 41, 55, 0.74);
  line-height: 1.75;
}

.ai-list {
  margin: 8px 0 0;
  padding-left: 18px;
}

.ai-disclaimer {
  position: sticky;
  bottom: 0;
  padding: 12px;
  text-align: center;
  color: rgba(31, 41, 55, 0.42);
  background: linear-gradient(180deg, transparent, #fff 38%);
}

.side-section {
  display: grid;
  gap: 10px;
}

.note-card {
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.note-label {
  color: #4f46e5;
  font-size: 13px;
  font-weight: 900;
}

.note-value {
  margin-top: 8px;
  color: #182034;
  font-size: 18px;
  font-weight: 900;
}

.note-card p {
  margin: 8px 0 0;
  color: rgba(31, 41, 55, 0.68);
  line-height: 1.7;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.row span {
  color: rgba(31, 41, 55, 0.58);
}

.hint.error {
  color: #b42318;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@keyframes typingDot {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.42;
  }

  50% {
    transform: translateY(-7px);
    opacity: 1;
  }
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}

@keyframes cursorBlink {
  50% {
    opacity: 0.18;
  }
}

@keyframes summaryRise {
  from {
    transform: translateY(8px);
    opacity: 0;
  }

  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes modalFade {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes modalRise {
  from {
    transform: translateY(12px) scale(0.98);
    opacity: 0;
  }

  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

@keyframes signalPulse {
  0% {
    transform: scale(0.7);
    opacity: 0.92;
  }

  72% {
    transform: scale(2.5);
    opacity: 0;
  }

  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

@keyframes whiteboardPulse {
  50% {
    transform: scale(0.82);
    opacity: 0.62;
  }
}

@keyframes whiteboardFloat {
  50% {
    transform: translateY(-6px) rotate(-2deg);
  }
}

.video-dock-grip {
  position: absolute;
  top: 10px;
  left: 12px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.74);
  background: rgba(15, 23, 42, 0.52);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
  font-size: 12px;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 160ms ease, transform 160ms ease;
  pointer-events: none;
}

.stage-panel.whiteboard-active .stage-screen:hover .video-dock-grip,
.stage-panel.whiteboard-active .stage-screen.dragging .video-dock-grip,
.stage-panel.screen-share-active .stage-screen:hover .video-dock-grip,
.stage-panel.screen-share-active .stage-screen.dragging .video-dock-grip {
  opacity: 1;
  transform: translateY(0);
}

.video-dock-grip span {
  width: 14px;
  height: 10px;
  border-top: 2px dotted rgba(255, 255, 255, 0.7);
  border-bottom: 2px dotted rgba(255, 255, 255, 0.7);
}

.stage-panel.whiteboard-active .self-tile.camera-off {
  aspect-ratio: auto;
  height: 42px;
  padding: 0 10px;
  place-items: center;
  background: rgba(15, 23, 42, 0.68);
  backdrop-filter: blur(12px);
}

.stage-panel.screen-share-active .self-tile.camera-off {
  aspect-ratio: auto;
  height: 42px;
  padding: 0 10px;
  place-items: center;
  background: rgba(15, 23, 42, 0.68);
  backdrop-filter: blur(12px);
}

.stage-panel.whiteboard-active .self-tile.camera-off .self-placeholder {
  display: none;
}

.stage-panel.screen-share-active .self-tile.camera-off .self-placeholder {
  display: none;
}

.stage-panel.whiteboard-active .self-tile.camera-off .self-badge,
.stage-panel.screen-share-active .self-tile.camera-off .self-badge {
  position: static;
  padding: 0;
  background: transparent;
  backdrop-filter: none;
}

@media (min-width: 1181px) {
  .classroom-grid.whiteboard-layout {
    align-items: start;
  }

  .stage-panel.whiteboard-active .stage-screen {
    position: fixed;
    inset: auto calc(var(--insight-panel-width, 392px) + 34px) 210px auto;
    width: min(320px, calc(100vw - 56px));
    height: 180px;
    z-index: 30;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 22px;
    border: 1px solid rgba(255, 255, 255, 0.42);
    cursor: grab;
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
    background:
      radial-gradient(circle at center, rgba(255, 255, 255, 0.1), transparent 36%),
      linear-gradient(145deg, #1e293b, #0f1722);
    transition: width 160ms ease, height 160ms ease, box-shadow 160ms ease;
    touch-action: none;
  }

  .stage-panel.screen-share-active .stage-screen {
    position: fixed;
    inset: auto calc(var(--insight-panel-width, 392px) + 34px) 210px auto;
    width: min(320px, calc(100vw - 56px));
    height: 180px;
    z-index: 30;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 22px;
    border: 1px solid rgba(255, 255, 255, 0.42);
    cursor: grab;
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
    background:
      radial-gradient(circle at center, rgba(255, 255, 255, 0.1), transparent 36%),
      linear-gradient(145deg, #1e293b, #0f1722);
    transition: width 160ms ease, height 160ms ease, box-shadow 160ms ease;
    touch-action: none;
  }

  .stage-panel.whiteboard-active .stage-screen.dragging {
    cursor: grabbing;
    box-shadow: 0 30px 78px rgba(15, 23, 42, 0.3);
    transition: none;
  }

  .stage-panel.screen-share-active .stage-screen.dragging {
    cursor: grabbing;
    box-shadow: 0 30px 78px rgba(15, 23, 42, 0.3);
    transition: none;
  }

  .stage-panel.whiteboard-active .stage-screen .waiting,
  .stage-panel.screen-share-active .stage-screen .waiting {
    padding: 18px;
  }

  .stage-panel.whiteboard-active .stage-screen .waiting-title,
  .stage-panel.screen-share-active .stage-screen .waiting-title {
    font-size: 17px;
  }

  .stage-panel.whiteboard-active .stage-screen .waiting-desc,
  .stage-panel.screen-share-active .stage-screen .waiting-desc {
    display: none;
  }

  .stage-panel.whiteboard-active .speaker-badge,
  .stage-panel.screen-share-active .speaker-badge {
    left: 12px;
    bottom: 12px;
    padding: 7px 9px;
    font-size: 12px;
  }

  .stage-panel.whiteboard-active .self-tile {
    position: fixed;
    top: auto;
    right: calc(var(--insight-panel-width, 392px) + 48px);
    bottom: 224px;
    width: 112px;
    z-index: 31;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 15px;
    border: 1px solid rgba(255, 255, 255, 0.34);
    cursor: grab;
    box-shadow: 0 16px 42px rgba(15, 23, 42, 0.2);
    touch-action: none;
  }

  .stage-panel.screen-share-active .self-tile {
    position: fixed;
    top: auto;
    right: calc(var(--insight-panel-width, 392px) + 48px);
    bottom: 224px;
    width: 112px;
    z-index: 31;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 15px;
    border: 1px solid rgba(255, 255, 255, 0.34);
    cursor: grab;
    box-shadow: 0 16px 42px rgba(15, 23, 42, 0.2);
    touch-action: none;
  }

  .stage-panel.whiteboard-active .self-tile.dragging,
  .stage-panel.screen-share-active .self-tile.dragging {
    cursor: grabbing;
  }

  .stage-panel.whiteboard-active .self-tile.camera-off {
    top: auto;
    bottom: 238px;
    width: 74px;
  }

  .stage-panel.screen-share-active .self-tile.camera-off {
    top: auto;
    bottom: 238px;
    width: 74px;
  }
}

@media (max-width: 1180px) {
  .insight-resizer {
    display: none;
  }

  .classroom-grid {
    grid-template-columns: 68px minmax(0, 1fr);
    overflow: auto;
  }

  .insight-panel {
    grid-column: 2;
  }

  .insight-panel.whiteboard-compact {
    margin-top: 0;
    height: min(520px, calc(100dvh - 180px));
    max-height: min(520px, calc(100dvh - 180px));
  }
}

@media (min-width: 901px) and (max-width: 1180px) {
  .classroom-grid.whiteboard-layout {
    grid-template-columns: 64px minmax(0, 1fr) minmax(300px, min(var(--insight-panel-width, 330px), 330px));
    align-items: start;
  }

  .classroom-grid.whiteboard-layout .insight-panel {
    grid-column: auto;
  }

  .stage-panel.whiteboard-active .stage-screen {
    position: fixed;
    inset: auto calc(330px + 34px) 206px auto;
    width: min(280px, calc(100vw - 36px));
    height: 158px;
    z-index: 30;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.42);
    cursor: grab;
    box-shadow: 0 22px 52px rgba(15, 23, 42, 0.2);
    background:
      radial-gradient(circle at center, rgba(255, 255, 255, 0.1), transparent 36%),
      linear-gradient(145deg, #1e293b, #0f1722);
    touch-action: none;
  }

  .stage-panel.screen-share-active .stage-screen {
    position: fixed;
    inset: auto calc(330px + 34px) 206px auto;
    width: min(280px, calc(100vw - 36px));
    height: 158px;
    z-index: 30;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.42);
    cursor: grab;
    box-shadow: 0 22px 52px rgba(15, 23, 42, 0.2);
    background:
      radial-gradient(circle at center, rgba(255, 255, 255, 0.1), transparent 36%),
      linear-gradient(145deg, #1e293b, #0f1722);
    touch-action: none;
  }

  .stage-panel.whiteboard-active .stage-screen.dragging,
  .stage-panel.screen-share-active .stage-screen.dragging {
    cursor: grabbing;
    box-shadow: 0 28px 68px rgba(15, 23, 42, 0.3);
  }

  .stage-panel.whiteboard-active .stage-screen .waiting,
  .stage-panel.screen-share-active .stage-screen .waiting {
    padding: 16px;
  }

  .stage-panel.whiteboard-active .stage-screen .waiting-title,
  .stage-panel.screen-share-active .stage-screen .waiting-title {
    font-size: 16px;
  }

  .stage-panel.whiteboard-active .stage-screen .waiting-desc,
  .stage-panel.screen-share-active .stage-screen .waiting-desc {
    display: none;
  }

  .stage-panel.whiteboard-active .speaker-badge,
  .stage-panel.screen-share-active .speaker-badge {
    left: 10px;
    bottom: 10px;
    padding: 6px 8px;
    font-size: 12px;
  }

  .stage-panel.whiteboard-active .self-tile {
    position: fixed;
    top: auto;
    right: calc(330px + 48px);
    bottom: 220px;
    width: 98px;
    z-index: 31;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 14px;
    border: 1px solid rgba(255, 255, 255, 0.34);
    cursor: grab;
    box-shadow: 0 14px 34px rgba(15, 23, 42, 0.2);
    touch-action: none;
  }

  .stage-panel.screen-share-active .self-tile {
    position: fixed;
    top: auto;
    right: calc(330px + 48px);
    bottom: 220px;
    width: 98px;
    z-index: 31;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    border-radius: 14px;
    border: 1px solid rgba(255, 255, 255, 0.34);
    cursor: grab;
    box-shadow: 0 14px 34px rgba(15, 23, 42, 0.2);
    touch-action: none;
  }

  .stage-panel.whiteboard-active .self-tile.dragging,
  .stage-panel.screen-share-active .self-tile.dragging {
    cursor: grabbing;
  }

  .stage-panel.whiteboard-active .self-tile.camera-off {
    top: auto;
    bottom: 232px;
    width: 70px;
  }

  .stage-panel.screen-share-active .self-tile.camera-off {
    top: auto;
    bottom: 232px;
    width: 70px;
  }

  .insight-panel.whiteboard-compact {
    margin-top: 0;
    height: 100%;
    max-height: 100%;
    min-height: 0;
  }
}

@media (max-width: 900px) {
  .workbench-shell {
    height: auto;
    min-height: 100dvh;
    overflow: auto;
    padding: 12px;
  }

  .room-bar,
  .room-brand,
  .room-actions {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .classroom-grid {
    grid-template-columns: 1fr;
  }

  .tool-rail {
    flex-direction: row;
    overflow-x: auto;
  }

  .tool-stack {
    grid-auto-flow: column;
    grid-auto-columns: 64px;
    display: grid;
  }

  .stage-panel {
    min-height: 420px;
    height: auto;
  }

  .stage-panel.whiteboard-active .stage-screen {
    top: auto;
    left: 14px;
    right: auto;
    bottom: 14px;
    width: min(204px, 46vw);
    height: 115px;
    border-radius: 16px;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    cursor: grab;
    touch-action: none;
  }

  .stage-panel.screen-share-active .stage-screen {
    top: auto;
    left: 14px;
    right: auto;
    bottom: 14px;
    width: min(204px, 46vw);
    height: 115px;
    border-radius: 16px;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    cursor: grab;
    touch-action: none;
  }

  .stage-panel.whiteboard-active .self-tile {
    top: auto;
    right: 14px;
    bottom: 14px;
    width: min(118px, 28vw);
    border-radius: 14px;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    cursor: grab;
    touch-action: none;
  }

  .stage-panel.screen-share-active .self-tile {
    top: auto;
    right: 14px;
    bottom: 14px;
    width: min(118px, 28vw);
    border-radius: 14px;
    transform: translate(var(--video-dock-x, 0), var(--video-dock-y, 0));
    cursor: grab;
    touch-action: none;
  }

  .stage-panel.whiteboard-active .self-tile.camera-off,
  .stage-panel.screen-share-active .self-tile.camera-off {
    width: 68px;
  }

  .insight-panel {
    grid-column: auto;
  }

  .insight-panel.whiteboard-compact {
    margin-top: 0;
    max-height: 430px;
    min-height: 320px;
  }

  .meeting-controls {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .ctl {
    flex: 0 0 66px;
  }

  .self-tile {
    width: 156px;
  }
}
</style>
