<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Track } from 'livekit-client'

import { liveApi, type LiveAiStateResp, type LiveSessionResp } from '@/api/live'
import {
  attachTrackToElement,
  detachTrack,
  LiveRoomClient,
} from '@/modules/live/livekit'
import { readLiveMediaPreferences, saveLiveMediaPreferences } from '@/modules/live/mediaPreferences'
import LiveClassChatPanel from '@/ui/live/LiveClassChatPanel.vue'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const loading = ref(false)
const connecting = ref(false)
const error = ref<string | null>(null)
const session = ref<LiveSessionResp | null>(null)
const sidebar = ref<'ai' | 'chat' | 'info' | 'tech'>('ai')
const connectionState = ref<'idle' | 'connecting' | 'connected' | 'reconnecting' | 'disconnected'>('idle')
const remoteParticipantName = ref('对方')
const remoteParticipantJoined = ref(false)
const remoteAudioConnected = ref(false)
const remoteVideoConnected = ref(false)
const roomId = computed(() => session.value?.roomId ?? null)
const aiState = ref<LiveAiStateResp | null>(null)
const aiError = ref<string | null>(null)

const prefs = readLiveMediaPreferences()
const micOn = ref(prefs.micEnabled)
const camOn = ref(prefs.cameraEnabled)
const selectedCameraId = ref(prefs.cameraDeviceId ?? '')
const selectedMicId = ref(prefs.micDeviceId ?? '')

const localVideoRef = ref<HTMLVideoElement | null>(null)
const remoteVideoRef = ref<HTMLVideoElement | null>(null)
const remoteAudioRef = ref<HTMLAudioElement | null>(null)

let pollTimer: number | null = null
let roomClient: LiveRoomClient | null = null

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
  for (const publication of participant.trackPublications.values()) {
    const track = publication.track
    if (!track) continue
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
  if (!hasRemoteVideo && remoteVideoRef.value) remoteVideoRef.value.srcObject = null
  if (!hasRemoteAudio && remoteAudioRef.value) remoteAudioRef.value.srcObject = null
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

async function load() {
  loading.value = true
  error.value = null
  try {
    session.value = await liveApi.getByCourse(courseId.value)
    if (session.value?.sessionId) {
      aiState.value = await liveApi.aiState(session.value.sessionId)
      aiError.value = null
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
    aiState.value = await liveApi.aiState(session.value.sessionId)
    aiError.value = null
  } catch {
    // 保持当前课堂状态，避免轮询报错打断课堂。
  }
}

const aiStatusText = computed(() => {
  const status = String(aiState.value?.aiStatus || '').trim().toUpperCase()
  if (status === 'ACTIVE') return '实时转写中'
  if (status === 'ASR_DEGRADED') return '语音识别暂不可用'
  if (status === 'LLM_DEGRADED') return 'AI 正在稍后重试整理摘要'
  if (status === 'FAILED') return 'AI 暂不可用'
  if (status === 'OFF') return '本节课未开启课堂 AI'
  return 'AI 正在进入课堂...'
})

function bindRoomEvents(client: LiveRoomClient) {
  client.onLocalTrackPublished((publication) => {
    if (publication.track?.kind === Track.Kind.Video) {
      attachTrackToElement(publication.track, localVideoRef.value)
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
    if (track.kind === Track.Kind.Video) {
      attachTrackToElement(track, remoteVideoRef.value)
      remoteVideoConnected.value = true
      return
    }
    attachTrackToElement(track, remoteAudioRef.value)
    remoteAudioConnected.value = true
  })
  client.onTrackUnsubscribed((track) => {
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
  } catch (e) {
    error.value = e instanceof Error ? e.message : '连接课堂失败'
    connectionState.value = 'disconnected'
  } finally {
    connecting.value = false
  }
}

async function toggleMic() {
  if (!roomClient) {
    micOn.value = !micOn.value
    return
  }
  micOn.value = !micOn.value
  await roomClient.setMicrophoneEnabled(micOn.value, selectedMicId.value || null)
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
  await roomClient?.disconnect()
  await liveApi.leave(session.value.sessionId, { leaveReason: 'USER_BACK', connectionState: connectionState.value.toUpperCase() })
  await router.push({ name: 'myCourses' })
}

async function endClass() {
  if (!session.value?.sessionId) return
  await roomClient?.disconnect()
  await liveApi.end(session.value.sessionId, { reason: 'MANUAL_END', confirm: true })
  await router.push({ name: 'myCourses' })
}

onMounted(async () => {
  await load()
  await connectRoom()
  pollTimer = window.setInterval(() => {
    void refreshStatus()
  }, 8000)
})

onUnmounted(async () => {
  if (pollTimer != null) window.clearInterval(pollTimer)
  await roomClient?.disconnect()
})
</script>

<template>
  <div class="live-room">
    <header class="topbar card">
      <div>
        <div class="room-title">实时课堂</div>
        <div class="room-meta">
          <span>课程 #{{ courseId }}</span>
          <span v-if="session?.status">状态：{{ session.status }}</span>
          <span
            :data-state="connectionState"
            data-testid="classroom-connection-state"
          >{{ connectionState === 'connected' || remoteAudioConnected || remoteVideoConnected ? '音视频已连接' : connectionState === 'reconnecting' ? '连接恢复中' : connecting ? '正在加入课堂' : '等待连接' }}</span>
        </div>
      </div>
      <div class="top-actions">
        <button class="btn" type="button" @click="leaveClass">离开课堂</button>
        <button class="btn btn-danger" type="button" @click="endClass">结束课程</button>
      </div>
    </header>

    <div class="stage-grid">
      <section class="stage card">
        <div class="stage-screen" data-testid="remote-stage">
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
          <span class="sr-only" :data-connected="remoteVideoConnected" data-testid="remote-video-state">远端视频</span>
          <span class="sr-only" :data-connected="remoteAudioConnected" data-testid="remote-audio-state">远端音频</span>
          <audio ref="remoteAudioRef" autoplay data-testid="remote-audio" />
        </div>
        <div class="self-tile" data-testid="local-stage">
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
        </div>
      </section>

      <aside class="sidebar card">
        <div class="side-tabs">
          <button class="tab" :class="{ active: sidebar === 'ai' }" type="button" @click="sidebar = 'ai'">AI 纪要</button>
          <button class="tab" :class="{ active: sidebar === 'info' }" type="button" @click="sidebar = 'info'">课程信息</button>
          <button class="tab" :class="{ active: sidebar === 'tech' }" type="button" @click="sidebar = 'tech'">技术状态</button>
          <button class="tab" :class="{ active: sidebar === 'chat' }" type="button" @click="sidebar = 'chat'">课中聊天</button>
        </div>

        <div v-if="sidebar === 'ai'" class="side-section">
          <div class="row"><span>AI 状态</span><strong>{{ aiStatusText }}</strong></div>
          <div class="ai-block">
            <div class="ai-title">当前阶段摘要</div>
            <div class="ai-text">{{ aiState?.latestStageSummary || 'AI 正在整理本阶段重点，稍后会展示摘要' }}</div>
          </div>
          <div class="ai-block">
            <div class="ai-title">当前讲解主题</div>
            <div class="ai-text">{{ aiState?.currentTopic || '暂未提炼' }}</div>
          </div>
          <div class="ai-block">
            <div class="ai-title">学生提问</div>
            <div v-if="!aiState?.studentQuestions?.length" class="ai-empty">暂无内容</div>
            <ul v-else class="ai-list">
              <li v-for="item in aiState?.studentQuestions || []" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="ai-block">
            <div class="ai-title">课堂重点</div>
            <div v-if="!aiState?.keyPoints?.length" class="ai-empty">暂无内容</div>
            <ul v-else class="ai-list">
              <li v-for="item in aiState?.keyPoints || []" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="ai-block">
            <div class="ai-title">作业候选</div>
            <div v-if="!aiState?.homeworkCandidates?.length" class="ai-empty">暂无内容</div>
            <ul v-else class="ai-list">
              <li v-for="item in aiState?.homeworkCandidates || []" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div v-if="aiError" class="hint error">{{ aiError }}</div>
          <div class="hint">仅供课堂辅助参考，课后将自动生成完整总结。</div>
        </div>

        <div v-else-if="sidebar === 'info'" class="side-section">
          <div class="row"><span>课堂状态</span><strong>{{ session?.status || '—' }}</strong></div>
          <div class="row"><span>房间</span><strong>{{ session?.providerRoomName || '—' }}</strong></div>
          <div class="row"><span>对方状态</span><strong>{{ peerJoinedState ? '已入会' : '等待中' }}</strong></div>
          <div class="row"><span>AI 观察</span><strong>{{ session?.aiPolicy || 'OFF' }}</strong></div>
        </div>

        <div v-else-if="sidebar === 'tech'" class="side-section">
          <div class="row"><span>麦克风</span><strong>{{ micOn ? '开启' : '关闭' }}</strong></div>
          <div class="row"><span>摄像头</span><strong>{{ camOn ? '开启' : '关闭' }}</strong></div>
          <div class="row"><span>远端音频</span><strong>{{ remoteAudioConnected ? '已接收' : '未接收' }}</strong></div>
          <div class="row"><span>远端视频</span><strong>{{ remoteVideoConnected ? '已接收' : '未接收' }}</strong></div>
          <div class="row"><span>连接状态</span><strong>{{ connectionState }}</strong></div>
          <div v-if="error" class="hint error">{{ error }}</div>
        </div>

        <div v-else class="side-section chat-panel">
          <LiveClassChatPanel
            :room-id="roomId"
            :peer-name="remoteParticipantName"
          />
        </div>
      </aside>
    </div>

    <footer class="controls card">
      <button class="ctl" :class="{ off: !micOn }" type="button" data-testid="classroom-toggle-mic" @click="toggleMic">
        {{ micOn ? '关闭麦克风' : '打开麦克风' }}
      </button>
      <button class="ctl" :class="{ off: !camOn }" type="button" data-testid="classroom-toggle-camera" @click="toggleCamera">
        {{ camOn ? '关闭摄像头' : '打开摄像头' }}
      </button>
      <button class="ctl" type="button" @click="sidebar = 'ai'">AI 纪要</button>
      <button class="ctl" type="button" @click="sidebar = 'tech'">切换设备</button>
      <button class="ctl" type="button" @click="sidebar = 'chat'">课中聊天</button>
      <button class="ctl primary" type="button" @click="endClass">结束课程</button>
    </footer>
  </div>
</template>

<style scoped>
.live-room {
  display: grid;
  gap: 14px;
}

.topbar,
.controls,
.stage,
.sidebar {
  border-radius: 22px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.room-title {
  font-size: 22px;
  font-weight: 800;
}

.room-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.58);
}

.top-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 14px;
}

.stage {
  position: relative;
  min-height: 560px;
  padding: 16px;
  background: linear-gradient(160deg, #08191d 0%, #10262c 100%);
}

.stage-screen {
  height: 100%;
  min-height: 528px;
  overflow: hidden;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.stage-video,
.self-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.waiting {
  text-align: center;
  max-width: 420px;
  padding: 24px;
}

.waiting-title {
  font-size: 28px;
  font-weight: 800;
}

.waiting-desc {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.self-tile {
  position: absolute;
  right: 28px;
  bottom: 28px;
  width: 196px;
  height: 132px;
  overflow: hidden;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.self-placeholder {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.sidebar {
  padding: 14px;
}

.side-tabs {
  display: flex;
  gap: 8px;
}

.tab {
  flex: 1 1 0;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
}

.tab.active {
  background: rgba(0, 190, 189, 0.1);
  border-color: rgba(0, 190, 189, 0.24);
}

.side-section {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.ai-block {
  display: grid;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.ai-title {
  font-size: 13px;
  font-weight: 700;
}

.ai-text,
.ai-empty,
.ai-list li {
  color: rgba(15, 23, 42, 0.72);
  line-height: 1.6;
}

.ai-list {
  margin: 0;
  padding-left: 18px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.chat-empty {
  padding: 16px;
  border-radius: 16px;
  background: rgba(0, 190, 189, 0.08);
  color: rgba(0, 0, 0, 0.66);
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  padding: 14px;
}

.ctl {
  min-width: 120px;
  padding: 12px 16px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
}

.ctl.off {
  color: #b42318;
}

.ctl.primary,
.btn-danger {
  background: #0f766e;
  color: #fff;
  border-color: #0f766e;
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

@media (max-width: 1080px) {
  .stage-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: -1;
  }
}
</style>
