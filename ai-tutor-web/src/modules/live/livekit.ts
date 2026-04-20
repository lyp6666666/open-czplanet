import {
  LogLevel,
  Room,
  RoomEvent,
  setLogLevel,
  Track,
  type LocalTrackPublication,
  type Participant,
  type RemoteTrack,
  type RemoteTrackPublication,
} from 'livekit-client'

const LIVEKIT_DEBUG_ENABLED =
  import.meta.env.DEV || import.meta.env.MODE === 'test' || import.meta.env.VITE_LIVEKIT_DEBUG === '1'
const LIVEKIT_FORCE_V0 =
  import.meta.env.VITE_LIVEKIT_FORCE_V0 == null
    ? import.meta.env.DEV || import.meta.env.MODE === 'test'
    : String(import.meta.env.VITE_LIVEKIT_FORCE_V0).trim() !== '0'

if (LIVEKIT_DEBUG_ENABLED) {
  setLogLevel(LogLevel.debug)
}

export type LiveRoomParticipantView = {
  identity: string
  name: string
  isSpeaking: boolean
}

export type LiveRoomTrackView = {
  participantIdentity: string
  kind: 'audio' | 'video'
  publicationSid?: string
}

export type LiveRoomConnectPayload = {
  serverUrl: string
  token: string
  cameraEnabled: boolean
  micEnabled: boolean
  cameraDeviceId?: string | null
  micDeviceId?: string | null
}

export class BrowserMediaError extends Error {
  code: 'UNSUPPORTED' | 'PERMISSION_DENIED' | 'DEVICE_NOT_FOUND' | 'DEVICE_BUSY' | 'UNKNOWN'

  constructor(code: BrowserMediaError['code'], message: string) {
    super(message)
    this.code = code
  }
}

function normalizeMediaError(error: unknown): BrowserMediaError {
  const name = error instanceof Error ? error.name : ''
  if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
    return new BrowserMediaError('PERMISSION_DENIED', '浏览器尚未授予摄像头或麦克风权限')
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return new BrowserMediaError('DEVICE_NOT_FOUND', '未检测到可用的摄像头或麦克风')
  }
  if (name === 'NotReadableError' || name === 'TrackStartError') {
    return new BrowserMediaError('DEVICE_BUSY', '设备正在被其他应用占用')
  }
  return new BrowserMediaError('UNKNOWN', error instanceof Error ? error.message : '媒体设备初始化失败')
}

export async function listLocalMediaDevices() {
  if (!navigator.mediaDevices?.enumerateDevices) {
    return { cameras: [] as MediaDeviceInfo[], microphones: [] as MediaDeviceInfo[], speakers: [] as MediaDeviceInfo[] }
  }
  const devices = await navigator.mediaDevices.enumerateDevices()
  return {
    cameras: devices.filter((item) => item.kind === 'videoinput'),
    microphones: devices.filter((item) => item.kind === 'audioinput'),
    speakers: devices.filter((item) => item.kind === 'audiooutput'),
  }
}

export async function requestUserMediaPreview(options: {
  cameraEnabled: boolean
  micEnabled: boolean
  cameraDeviceId?: string | null
  micDeviceId?: string | null
}) {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new BrowserMediaError('UNSUPPORTED', '当前浏览器不支持音视频采集')
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: options.cameraEnabled
        ? {
            deviceId: options.cameraDeviceId ? { exact: options.cameraDeviceId } : undefined,
            width: { ideal: 1280 },
            height: { ideal: 720 },
            facingMode: 'user',
          }
        : false,
      audio: options.micEnabled
        ? {
            deviceId: options.micDeviceId ? { exact: options.micDeviceId } : undefined,
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true,
          }
        : false,
    })
    return stream
  } catch (error) {
    throw normalizeMediaError(error)
  }
}

export function stopMediaStream(stream: MediaStream | null | undefined) {
  stream?.getTracks().forEach((track) => track.stop())
}

export function attachMediaStream(element: HTMLMediaElement | null, stream: MediaStream | null | undefined, muted = false) {
  if (!element) return
  element.srcObject = stream ?? null
  element.muted = muted
  element.autoplay = true
  if ('playsInline' in element) {
    ;(element as HTMLVideoElement).playsInline = true
  }
}

type TrackListener = (track: RemoteTrack, publication: RemoteTrackPublication, participant: Participant) => void

function redactLiveKitUrl(rawUrl: string) {
  try {
    const url = new URL(rawUrl)
    if (url.searchParams.has('access_token')) url.searchParams.set('access_token', '<redacted>')
    return url.toString()
  } catch {
    return rawUrl
  }
}

function logLiveKit(message: string, payload?: Record<string, unknown>) {
  if (!LIVEKIT_DEBUG_ENABLED) return
  console.info(`[livekit-classroom] ${message}`, payload || {})
}

function normalizeLiveKitServerUrl(rawUrl: string) {
  const fallback = rawUrl.trim()
  try {
    const url = new URL(fallback)
    url.hash = ''
    url.search = ''
    return url.toString().replace(/\/$/, '')
  } catch {
    return fallback
  }
}

export class LiveRoomClient {
  room: Room
  localCameraPublication: LocalTrackPublication | undefined
  localMicrophonePublication: LocalTrackPublication | undefined

  constructor() {
    this.room = new Room({
      adaptiveStream: true,
      dynacast: true,
      singlePeerConnection: !LIVEKIT_FORCE_V0,
      publishDefaults: {
        simulcast: false,
        backupCodec: false,
        videoCodec: 'vp8',
      },
    })
  }

  async connect(payload: LiveRoomConnectPayload) {
    const serverUrl = normalizeLiveKitServerUrl(payload.serverUrl)
    logLiveKit('connect:start', {
      serverUrl: redactLiveKitUrl(serverUrl),
      forceV0: LIVEKIT_FORCE_V0,
      cameraEnabled: payload.cameraEnabled,
      micEnabled: payload.micEnabled,
      hasCameraDeviceId: !!payload.cameraDeviceId,
      hasMicDeviceId: !!payload.micDeviceId,
    })
    await this.room.prepareConnection(serverUrl, payload.token)
    await this.room.connect(serverUrl, payload.token, {
      autoSubscribe: true,
      maxRetries: 2,
      websocketTimeout: 25_000,
      peerConnectionTimeout: 30_000,
    })
    logLiveKit('connect:signal-ready', {
      roomName: this.room.name,
      localIdentity: this.room.localParticipant.identity,
      remoteParticipants: this.room.remoteParticipants.size,
      state: this.room.state,
    })
    this.localCameraPublication = await this.room.localParticipant.setCameraEnabled(
      payload.cameraEnabled,
      payload.cameraDeviceId ? { deviceId: payload.cameraDeviceId } : undefined,
    )
    this.localMicrophonePublication = await this.room.localParticipant.setMicrophoneEnabled(
      payload.micEnabled,
      payload.micDeviceId ? { deviceId: payload.micDeviceId } : undefined,
    )
    logLiveKit('connect:local-published', {
      cameraPublished: !!this.localCameraPublication,
      microphonePublished: !!this.localMicrophonePublication,
      state: this.room.state,
    })
  }

  async setCameraEnabled(enabled: boolean, deviceId?: string | null) {
    const publication = await this.room.localParticipant.setCameraEnabled(enabled, deviceId ? { deviceId } : undefined)
    this.localCameraPublication = publication
    return publication as LocalTrackPublication | undefined
  }

  async setMicrophoneEnabled(enabled: boolean, deviceId?: string | null) {
    const publication = await this.room.localParticipant.setMicrophoneEnabled(enabled, deviceId ? { deviceId } : undefined)
    this.localMicrophonePublication = publication
    return publication as LocalTrackPublication | undefined
  }

  onTrackSubscribed(listener: TrackListener) {
    this.room.on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
      logLiveKit('track:subscribed', {
        kind: track.kind,
        source: publication.source,
        participant: participant.identity,
      })
      listener(track, publication, participant)
    })
  }

  onTrackUnsubscribed(listener: (track: RemoteTrack) => void) {
    this.room.on(RoomEvent.TrackUnsubscribed, listener)
  }

  onParticipantConnected(listener: (participant: Participant) => void) {
    this.room.on(RoomEvent.ParticipantConnected, (participant) => {
      logLiveKit('participant:connected', {
        identity: participant.identity,
        name: participant.name,
      })
      listener(participant)
    })
  }

  onParticipantDisconnected(listener: (participant: Participant) => void) {
    this.room.on(RoomEvent.ParticipantDisconnected, listener)
  }

  onDisconnected(listener: () => void) {
    this.room.on(RoomEvent.Disconnected, listener)
  }

  onConnectionStateChanged(listener: (state: string) => void) {
    this.room.on(RoomEvent.ConnectionStateChanged, (state) => {
      logLiveKit('connection:state', { state })
      listener(state)
    })
  }

  onMediaError(listener: (error: Error) => void) {
    this.room.on(RoomEvent.MediaDevicesError, listener)
  }

  async disconnect() {
    await this.room.disconnect(true)
  }
}

export function attachTrackToElement(track: RemoteTrack | LocalTrackPublication['track'], element: HTMLMediaElement | null) {
  if (!track || !element) return
  const attached = track.attach()
  if (attached === element) return
  element.srcObject = (attached as HTMLMediaElement).srcObject
  element.autoplay = true
  if ('playsInline' in element) {
    ;(element as HTMLVideoElement).playsInline = true
  }
  if (track.kind === Track.Kind.Audio) {
    element.muted = false
  }
}

export function detachTrack(track: RemoteTrack | LocalTrackPublication['track'] | undefined | null, element?: HTMLMediaElement | null) {
  if (!track) return
  if (element) {
    track.detach(element)
    if (element.srcObject) element.srcObject = null
    return
  }
  track.detach()
}
