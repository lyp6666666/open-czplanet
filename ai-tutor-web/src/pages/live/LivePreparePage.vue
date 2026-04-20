<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { liveApi, type PrepareLiveSessionResp } from '@/api/live'
import {
  attachMediaStream,
  BrowserMediaError,
  listLocalMediaDevices,
  requestUserMediaPreview,
  stopMediaStream,
} from '@/modules/live/livekit'
import { readLiveMediaPreferences, saveLiveMediaPreferences } from '@/modules/live/mediaPreferences'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const loading = ref(false)
const joining = ref(false)
const preparingDevices = ref(false)
const error = ref<string | null>(null)
const deviceError = ref<string | null>(null)
const prepareData = ref<PrepareLiveSessionResp | null>(null)

const previewRef = ref<HTMLVideoElement | null>(null)
const previewStream = ref<MediaStream | null>(null)
const permissionState = ref<'idle' | 'granted' | 'denied' | 'unsupported'>('idle')
const networkLevel = ref<'GOOD' | 'NORMAL' | 'POOR'>('GOOD')

const cameraOptions = ref<MediaDeviceInfo[]>([])
const microphoneOptions = ref<MediaDeviceInfo[]>([])
const speakerOptions = ref<MediaDeviceInfo[]>([])

const saved = readLiveMediaPreferences()
const cameraEnabled = ref(saved.cameraEnabled)
const micEnabled = ref(saved.micEnabled)
const speakerChecked = ref(saved.speakerChecked)
const selectedCameraId = ref(saved.cameraDeviceId ?? '')
const selectedMicId = ref(saved.micDeviceId ?? '')
const selectedSpeakerId = ref(saved.speakerDeviceId ?? '')

async function load() {
  loading.value = true
  error.value = null
  try {
    prepareData.value = await liveApi.prepare(courseId.value, { clientType: 'WEB', sourcePage: 'LIVE_PREPARE' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课堂失败'
  } finally {
    loading.value = false
  }
}

async function loadDevices() {
  const devices = await listLocalMediaDevices()
  cameraOptions.value = devices.cameras
  microphoneOptions.value = devices.microphones
  speakerOptions.value = devices.speakers
  if (!selectedCameraId.value && devices.cameras[0]?.deviceId) selectedCameraId.value = devices.cameras[0].deviceId
  if (!selectedMicId.value && devices.microphones[0]?.deviceId) selectedMicId.value = devices.microphones[0].deviceId
  if (!selectedSpeakerId.value && devices.speakers[0]?.deviceId) selectedSpeakerId.value = devices.speakers[0].deviceId
}

async function refreshPreview() {
  deviceError.value = null
  stopMediaStream(previewStream.value)
  previewStream.value = null
  attachMediaStream(previewRef.value, null, true)

  if (!cameraEnabled.value && !micEnabled.value) {
    permissionState.value = 'idle'
    return
  }

  preparingDevices.value = true
  try {
    const stream = await requestUserMediaPreview({
      cameraEnabled: cameraEnabled.value,
      micEnabled: micEnabled.value,
      cameraDeviceId: selectedCameraId.value || null,
      micDeviceId: selectedMicId.value || null,
    })
    previewStream.value = stream
    permissionState.value = 'granted'
    attachMediaStream(previewRef.value, stream, true)
    await loadDevices()
  } catch (e) {
    if (e instanceof BrowserMediaError) {
      permissionState.value = e.code === 'UNSUPPORTED' ? 'unsupported' : 'denied'
      deviceError.value = e.message
    } else {
      permissionState.value = 'denied'
      deviceError.value = e instanceof Error ? e.message : '设备检测失败'
    }
  } finally {
    preparingDevices.value = false
  }
}

async function enterClassroom() {
  if (!prepareData.value?.sessionId || joining.value) return
  joining.value = true
  error.value = null
  try {
    await liveApi.reportDevice(prepareData.value.sessionId, {
      reportStage: 'PREPARE',
      cameraStatus: cameraEnabled.value ? 'READY' : 'OFF',
      micStatus: micEnabled.value ? 'READY' : 'OFF',
      speakerStatus: speakerChecked.value ? 'READY' : 'UNTESTED',
      networkLevel: networkLevel.value,
      browserInfo: navigator.userAgent,
      osInfo: navigator.platform,
      deviceInfo: {
        cameraEnabled: cameraEnabled.value,
        micEnabled: micEnabled.value,
        cameraDeviceId: selectedCameraId.value || null,
        micDeviceId: selectedMicId.value || null,
        speakerDeviceId: selectedSpeakerId.value || null,
      },
    })
    saveLiveMediaPreferences({
      cameraEnabled: cameraEnabled.value,
      micEnabled: micEnabled.value,
      speakerChecked: speakerChecked.value,
      cameraDeviceId: selectedCameraId.value || null,
      micDeviceId: selectedMicId.value || null,
      speakerDeviceId: selectedSpeakerId.value || null,
    })
    await router.push({ name: 'liveClassroom', params: { courseId: String(courseId.value) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '进入课堂失败'
  } finally {
    joining.value = false
  }
}

watch(
  [cameraEnabled, micEnabled, selectedCameraId, selectedMicId],
  () => {
    void refreshPreview()
  },
  { immediate: false },
)

watch([cameraEnabled, micEnabled, speakerChecked, selectedCameraId, selectedMicId, selectedSpeakerId], () => {
  saveLiveMediaPreferences({
    cameraEnabled: cameraEnabled.value,
    micEnabled: micEnabled.value,
    speakerChecked: speakerChecked.value,
    cameraDeviceId: selectedCameraId.value || null,
    micDeviceId: selectedMicId.value || null,
    speakerDeviceId: selectedSpeakerId.value || null,
  })
})

onMounted(async () => {
  await load()
  await loadDevices()
  await refreshPreview()
})

onUnmounted(() => {
  stopMediaStream(previewStream.value)
})
</script>

<template>
  <div class="live-prepare">
    <section class="hero card">
      <div class="hero-copy">
        <div class="eyebrow">实时课程准备</div>
        <h1>进入课堂前，先确认设备和环境</h1>
        <p>
          <template v-if="prepareData">
            你即将和 {{ prepareData.peerDisplayName || '对方' }} 进入实时课堂，建议先完成摄像头、麦克风和网络自检。
          </template>
          <template v-else>正在加载课堂信息。</template>
        </p>
        <div class="hero-status">
          <span
            class="status-chip"
            :class="permissionState"
            :data-state="permissionState"
            data-testid="prepare-permission-state"
          >{{ permissionState === 'granted' ? '设备权限已就绪' : permissionState === 'denied' ? '设备权限待处理' : permissionState === 'unsupported' ? '浏览器不支持媒体采集' : '等待检测' }}</span>
          <span class="status-chip subtle">{{ preparingDevices ? '检测设备中...' : '支持进入前预览' }}</span>
        </div>
      </div>
      <div class="preview">
        <div class="preview-frame">
          <div class="preview-badge">本地预览</div>
          <div class="preview-surface" data-testid="prepare-preview">
            <video
              v-show="cameraEnabled && permissionState === 'granted'"
              ref="previewRef"
              class="preview-video"
              autoplay
              muted
              playsinline
              data-testid="prepare-video"
            />
            <div v-if="!cameraEnabled || permissionState !== 'granted'" class="preview-placeholder">
              <div class="avatar-orb">你</div>
              <p>{{ cameraEnabled ? deviceError || '等待摄像头授权' : '摄像头已关闭，可仅使用语音上课' }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel-grid">
      <div class="card device-panel">
        <div class="section-title">设备自检</div>
        <label class="device-row">
          <span>摄像头</span>
          <input
            v-model="cameraEnabled"
            type="checkbox"
            :data-enabled="cameraEnabled"
            data-testid="prepare-camera-state"
          />
        </label>
        <label class="device-row">
          <span>麦克风</span>
          <input
            v-model="micEnabled"
            type="checkbox"
            :data-enabled="micEnabled"
            data-testid="prepare-mic-state"
          />
        </label>
        <label class="device-row">
          <span>扬声器试听</span>
          <input v-model="speakerChecked" type="checkbox" />
        </label>
        <label class="device-row stacked">
          <span>摄像头设备</span>
          <select v-model="selectedCameraId" class="select" data-testid="camera-device-select">
            <option value="">系统默认</option>
            <option v-for="item in cameraOptions" :key="item.deviceId" :value="item.deviceId">{{ item.label || `摄像头 ${item.deviceId.slice(-4)}` }}</option>
          </select>
        </label>
        <label class="device-row stacked">
          <span>麦克风设备</span>
          <select v-model="selectedMicId" class="select" data-testid="mic-device-select">
            <option value="">系统默认</option>
            <option v-for="item in microphoneOptions" :key="item.deviceId" :value="item.deviceId">{{ item.label || `麦克风 ${item.deviceId.slice(-4)}` }}</option>
          </select>
        </label>
        <label class="device-row stacked">
          <span>扬声器设备</span>
          <select v-model="selectedSpeakerId" class="select">
            <option value="">系统默认</option>
            <option v-for="item in speakerOptions" :key="item.deviceId" :value="item.deviceId">{{ item.label || `扬声器 ${item.deviceId.slice(-4)}` }}</option>
          </select>
        </label>
        <label class="device-row">
          <span>网络状态</span>
          <select v-model="networkLevel" class="select">
            <option value="GOOD">良好</option>
            <option value="NORMAL">一般</option>
            <option value="POOR">较差</option>
          </select>
        </label>
        <div v-if="deviceError" class="hint error">{{ deviceError }}</div>
      </div>

      <div class="card summary-panel">
        <div class="section-title">课堂信息</div>
        <div class="summary-row">
          <span>课程状态</span>
          <strong>{{ prepareData?.status || '加载中' }}</strong>
        </div>
        <div class="summary-row">
          <span>对方</span>
          <strong>{{ prepareData?.peerDisplayName || '—' }}</strong>
        </div>
        <div class="summary-row">
          <span>允许入会</span>
          <strong>{{ prepareData?.joinableNow ? '现在可进入' : '暂未开放' }}</strong>
        </div>
        <div class="summary-row">
          <span>建议模式</span>
          <strong>{{ cameraEnabled && micEnabled ? '音视频课堂' : micEnabled ? '语音课堂' : '仅文本提醒' }}</strong>
        </div>
        <div v-if="error" class="hint error">{{ error }}</div>
        <div class="actions">
          <button class="btn" type="button" @click="$router.back()">稍后再说</button>
          <button
            class="btn btn-primary"
            type="button"
            data-testid="enter-classroom-button"
            :disabled="loading || joining || !prepareData?.canJoin"
            @click="enterClassroom"
          >
            {{ joining ? '进入中...' : '进入课堂' }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.live-prepare {
  display: grid;
  gap: 16px;
}

.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 18px;
  padding: 24px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(0, 190, 189, 0.18), transparent 35%),
    linear-gradient(135deg, #f8fcff 0%, #edf7f4 100%);
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.48);
}

h1 {
  margin: 8px 0 10px;
  font-size: 30px;
  line-height: 1.1;
}

p {
  margin: 0;
  color: rgba(0, 0, 0, 0.62);
}

.hero-status {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.status-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
}

.status-chip.denied,
.status-chip.unsupported {
  background: rgba(180, 35, 24, 0.12);
  color: #b42318;
}

.status-chip.subtle {
  background: rgba(255, 255, 255, 0.78);
  color: rgba(0, 0, 0, 0.58);
}

.preview-frame {
  border-radius: 22px;
  padding: 18px;
  background: linear-gradient(160deg, #0f2c2f 0%, #17393a 100%);
  color: #fff;
}

.preview-badge {
  font-size: 12px;
  opacity: 0.82;
}

.preview-surface {
  margin-top: 14px;
  min-height: 280px;
  border-radius: 18px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.06);
}

.preview-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-placeholder {
  display: grid;
  gap: 16px;
  justify-items: center;
  padding: 24px;
  text-align: center;
}

.avatar-orb {
  width: 88px;
  height: 88px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.14);
}

.panel-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
}

.device-panel,
.summary-panel {
  padding: 18px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.device-row,
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.device-row.stacked {
  align-items: flex-start;
  flex-direction: column;
}

.select {
  min-width: 160px;
  width: 100%;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.hint.error {
  margin-top: 14px;
  color: #b42318;
}

@media (max-width: 960px) {
  .hero,
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
