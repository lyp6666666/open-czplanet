<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { liveApi, type PrepareLiveSessionResp } from '@/api/live'
import {
  attachMediaStream,
  BrowserMediaError,
  inspectBrowserMediaSupport,
  listLocalMediaDevices,
  playSpeakerTestTone,
  queryBrowserMediaPermissions,
  requestUserMediaPreview,
  stopMediaStream,
  type BrowserMediaPermissionState,
} from '@/modules/live/livekit'
import { readLiveMediaPreferences, saveLiveMediaPreferences } from '@/modules/live/mediaPreferences'

type PermissionState = 'idle' | 'checking' | 'granted' | 'partial' | 'denied' | 'unsupported'
type DeviceCheckState = 'ready' | 'pending' | 'warning'
type DevicePermissionIssueCode =
  | 'INSECURE_CONTEXT'
  | 'UNSUPPORTED'
  | 'PERMISSION_DENIED'
  | 'DEVICE_NOT_FOUND'
  | 'DEVICE_BUSY'
  | 'UNKNOWN'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const loading = ref(false)
const joining = ref(false)
const preparingDevices = ref(false)
const speakerTesting = ref(false)
const error = ref<string | null>(null)
const deviceError = ref<string | null>(null)
const prepareData = ref<PrepareLiveSessionResp | null>(null)

const previewRef = ref<HTMLVideoElement | null>(null)
const previewStream = ref<MediaStream | null>(null)
const permissionState = ref<PermissionState>('idle')
const browserSupport = ref(inspectBrowserMediaSupport())
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

const cameraPermission = ref<BrowserMediaPermissionState>('unknown')
const micPermission = ref<BrowserMediaPermissionState>('unknown')

const permissionModalOpen = ref(false)
const permissionModalReason = ref<DevicePermissionIssueCode>('UNKNOWN')
const permissionModalTitle = ref('无法完成设备授权')
const permissionModalDesc = ref('请先完成浏览器权限设置后再继续。')
const permissionModalActions = ref<string[]>([])

const securityHelpOpen = ref(false)

const speakerTestMessage = ref('')

const deviceCheckItems = computed(() => {
  const cameraStatus: DeviceCheckState =
    !cameraEnabled.value ? 'pending' : permissionState.value === 'granted' || permissionState.value === 'partial' ? 'ready' : 'warning'
  const micStatus: DeviceCheckState =
    !micEnabled.value ? 'pending' : permissionState.value === 'granted' || permissionState.value === 'partial' ? 'ready' : 'warning'
  const speakerStatus: DeviceCheckState = speakerChecked.value ? 'ready' : speakerTestMessage.value ? 'warning' : 'pending'
  return [
    {
      key: 'camera',
      title: '摄像头',
      status: cameraStatus,
      detail:
        !cameraEnabled.value
          ? '当前关闭，可切换为语音上课'
          : cameraPermission.value === 'granted'
            ? '已取得浏览器授权并可预览画面'
            : browserSupport.value.secureContext
              ? '点击检测后触发浏览器授权'
              : '当前页面不是安全连接，浏览器不会开放摄像头权限',
      actionText: cameraEnabled.value ? '检测并授权' : '开启摄像头',
      disabled: preparingDevices.value,
    },
    {
      key: 'mic',
      title: '麦克风',
      status: micStatus,
      detail:
        !micEnabled.value
          ? '当前关闭，可稍后在课堂内打开'
          : micPermission.value === 'granted'
            ? '已取得浏览器授权并可采集声音'
            : browserSupport.value.secureContext
              ? '点击检测后触发浏览器授权'
              : '当前页面不是安全连接，浏览器不会开放麦克风权限',
      actionText: micEnabled.value ? '检测并授权' : '开启麦克风',
      disabled: preparingDevices.value,
    },
    {
      key: 'speaker',
      title: '扬声器试听',
      status: speakerStatus,
      detail:
        speakerChecked.value
          ? '试听成功，可正常听到课堂声音'
          : speakerTestMessage.value || '点击按钮播放提示音，确认当前扬声器输出正常',
      actionText: speakerTesting.value ? '试听中...' : '开始试听',
      disabled: speakerTesting.value,
    },
  ] as const
})

const permissionHeadline = computed(() => {
  if (!browserSupport.value.secureContext) return '当前页面不是安全连接'
  if (!browserSupport.value.canGetUserMedia) return '当前浏览器不支持音视频采集'
  if (permissionState.value === 'granted') return '设备权限已就绪'
  if (permissionState.value === 'partial') return '部分设备已就绪'
  if (permissionState.value === 'checking') return '正在检测设备权限'
  if (permissionState.value === 'denied') return '设备权限待处理'
  if (permissionState.value === 'unsupported') return '当前环境不支持设备授权'
  return '等待设备检测'
})

const permissionSummary = computed(() => {
  if (!browserSupport.value.secureContext) return '浏览器只会在 https、localhost 或受信任环境下开放摄像头和麦克风。'
  if (!browserSupport.value.canGetUserMedia) return '请切换到最新版 Chrome、Edge 或 Safari，再重新进入课堂准备页。'
  if (permissionState.value === 'granted') return '摄像头和麦克风都已就绪，可以继续完成设备自检。'
  if (permissionState.value === 'partial') return '至少有一项设备已授权，建议继续检查未通过的项目。'
  if (permissionState.value === 'denied') return '请点击下方按钮重新申请浏览器授权，若仍失败可查看设置指引。'
  return '点击“一键检测设备权限”后，页面会主动向浏览器请求摄像头和麦克风权限。'
})

const canEnterClassroom = computed(
  () => !!prepareData.value?.canJoin && !!prepareData.value?.joinableNow && !loading.value && !joining.value,
)
const paymentBlocked = computed(() => !!prepareData.value?.blockingPaymentOrderId)

function setPermissionModal(reason: DevicePermissionIssueCode, message: string) {
  permissionModalReason.value = reason
  permissionModalOpen.value = true
  if (reason === 'INSECURE_CONTEXT') {
    permissionModalTitle.value = '当前页面无法直接申请设备权限'
    permissionModalDesc.value = '你正在非安全连接页面中打开课堂准备页。浏览器会直接拦截摄像头和麦克风授权请求，需要先切到安全连接后再试。'
    permissionModalActions.value = [
      '优先使用 https 域名重新打开课堂准备页，或联系运维为当前域名补齐证书。',
      '如果是本地调试，可改用 localhost 访问；浏览器通常信任 localhost。',
      '切换到安全连接后，回到当前页面再次点击“一键检测设备权限”。',
    ]
    return
  }
  if (reason === 'UNSUPPORTED') {
    permissionModalTitle.value = '当前浏览器不支持音视频采集'
    permissionModalDesc.value = '这通常出现在过旧浏览器、内嵌浏览器或受限 WebView 中。'
    permissionModalActions.value = [
      '请改用最新版 Chrome、Edge 或 Safari 打开课堂准备页。',
      '避免在微信、QQ、企业应用内置浏览器中直接进入课堂。',
      '切换浏览器后重新点击“一键检测设备权限”。',
    ]
    return
  }
  if (reason === 'DEVICE_BUSY') {
    permissionModalTitle.value = '设备正在被其他应用占用'
    permissionModalDesc.value = '浏览器已经具备能力，但摄像头或麦克风暂时被会议软件、录屏工具等程序占用。'
    permissionModalActions.value = [
      '先关闭 Zoom、腾讯会议、飞书会议、录屏工具等可能占用设备的应用。',
      '回到当前页面再次点击“一键检测设备权限”。',
      '如仍失败，可尝试重新插拔外接设备或重启浏览器。',
    ]
    return
  }
  if (reason === 'DEVICE_NOT_FOUND') {
    permissionModalTitle.value = '没有检测到可用设备'
    permissionModalDesc.value = '浏览器没有找到可用的摄像头或麦克风，请确认系统层面设备是否连接成功。'
    permissionModalActions.value = [
      '检查摄像头、耳机、麦克风是否已正确插入并被系统识别。',
      '返回当前页后重新点击“一键检测设备权限”，再从设备下拉框中选择正确设备。',
      '如果只需要语音上课，可以先关闭摄像头后继续进入课堂。',
    ]
    return
  }
  permissionModalTitle.value = '浏览器尚未允许访问设备'
  permissionModalDesc.value = message || '请先完成浏览器授权后再继续。'
  permissionModalActions.value = [
    '点击地址栏左侧的站点图标，将摄像头和麦克风权限改为“允许”。',
    '刷新页面后，再点击“一键检测设备权限”重新申请。',
    '如果浏览器没有再次弹出授权框，可点击下方“查看浏览器设置指引”。',
  ]
}

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
  browserSupport.value = inspectBrowserMediaSupport()
  const devices = await listLocalMediaDevices()
  cameraOptions.value = devices.cameras
  microphoneOptions.value = devices.microphones
  speakerOptions.value = devices.speakers
  if (!selectedCameraId.value && devices.cameras[0]?.deviceId) selectedCameraId.value = devices.cameras[0].deviceId
  if (!selectedMicId.value && devices.microphones[0]?.deviceId) selectedMicId.value = devices.microphones[0].deviceId
  if (!selectedSpeakerId.value && devices.speakers[0]?.deviceId) selectedSpeakerId.value = devices.speakers[0].deviceId
}

async function refreshPermissionSnapshot() {
  const permission = await queryBrowserMediaPermissions()
  cameraPermission.value = permission.camera
  micPermission.value = permission.microphone
  if (!browserSupport.value.secureContext || !browserSupport.value.canGetUserMedia) {
    permissionState.value = 'unsupported'
    return
  }
  const relevant = [
    cameraEnabled.value ? cameraPermission.value : 'granted',
    micEnabled.value ? micPermission.value : 'granted',
  ]
  const grantedCount = relevant.filter((item) => item === 'granted').length
  const deniedCount = relevant.filter((item) => item === 'denied').length
  if (grantedCount === relevant.length) {
    permissionState.value = 'granted'
  } else if (grantedCount > 0 && deniedCount < relevant.length) {
    permissionState.value = 'partial'
  } else if (deniedCount > 0) {
    permissionState.value = 'denied'
  } else {
    permissionState.value = 'idle'
  }
}

function stopPreview() {
  stopMediaStream(previewStream.value)
  previewStream.value = null
  attachMediaStream(previewRef.value, null, true)
}

async function refreshPreview(options?: { silent?: boolean }) {
  deviceError.value = null
  browserSupport.value = inspectBrowserMediaSupport()
  stopPreview()
  if (!cameraEnabled.value && !micEnabled.value) {
    permissionState.value = 'idle'
    await refreshPermissionSnapshot()
    return
  }

  preparingDevices.value = true
  permissionState.value = 'checking'
  try {
    const stream = await requestUserMediaPreview({
      cameraEnabled: cameraEnabled.value,
      micEnabled: micEnabled.value,
      cameraDeviceId: selectedCameraId.value || null,
      micDeviceId: selectedMicId.value || null,
    })
    previewStream.value = stream
    attachMediaStream(previewRef.value, stream, true)
    await loadDevices()
    await refreshPermissionSnapshot()
    if (cameraEnabled.value) cameraPermission.value = 'granted'
    if (micEnabled.value) micPermission.value = 'granted'
    permissionState.value = 'granted'
    permissionModalOpen.value = false
  } catch (e) {
    const mediaError =
      e instanceof BrowserMediaError ? e : new BrowserMediaError('UNKNOWN', e instanceof Error ? e.message : '设备检测失败')
    deviceError.value = mediaError.message
    permissionState.value = mediaError.code === 'UNSUPPORTED' || mediaError.code === 'INSECURE_CONTEXT' ? 'unsupported' : 'denied'
    await refreshPermissionSnapshot()
    if (!options?.silent) {
      setPermissionModal(mediaError.code, mediaError.message)
    }
  } finally {
    preparingDevices.value = false
  }
}

async function runDeviceCheck(target?: 'camera' | 'mic' | 'all') {
  if (target === 'camera' && !cameraEnabled.value) cameraEnabled.value = true
  if (target === 'mic' && !micEnabled.value) micEnabled.value = true
  await refreshPreview()
}

async function runSpeakerTest() {
  speakerTesting.value = true
  speakerTestMessage.value = ''
  try {
    const result = await playSpeakerTestTone({ speakerDeviceId: selectedSpeakerId.value || null })
    speakerChecked.value = true
    speakerTestMessage.value = result.canSelectSpeaker
      ? '试听成功，当前扬声器输出正常。'
      : '试听成功，当前浏览器不支持切换扬声器设备，但系统默认输出可用。'
  } catch (e) {
    const mediaError =
      e instanceof BrowserMediaError ? e : new BrowserMediaError('UNKNOWN', e instanceof Error ? e.message : '扬声器试听失败')
    speakerChecked.value = false
    speakerTestMessage.value = mediaError.message
    setPermissionModal(mediaError.code, mediaError.message)
  } finally {
    speakerTesting.value = false
  }
}

function openSecurityGuide() {
  securityHelpOpen.value = true
}

function openPermissionGuidePage() {
  void router.push({ name: 'livePermissionHelp' })
}

function closePermissionModal() {
  permissionModalOpen.value = false
}

function closeSecurityGuide() {
  securityHelpOpen.value = false
}

async function enterClassroom() {
  if (!prepareData.value?.sessionId || joining.value) return
  joining.value = true
  error.value = null
  try {
    await liveApi.reportDevice(prepareData.value.sessionId, {
      reportStage: 'PREPARE',
      cameraStatus: cameraEnabled.value ? (cameraPermission.value === 'granted' ? 'READY' : 'WARNING') : 'OFF',
      micStatus: micEnabled.value ? (micPermission.value === 'granted' ? 'READY' : 'WARNING') : 'OFF',
      speakerStatus: speakerChecked.value ? 'READY' : 'UNTESTED',
      networkLevel: networkLevel.value,
      browserInfo: navigator.userAgent,
      osInfo: navigator.platform,
      deviceInfo: {
        cameraEnabled: cameraEnabled.value,
        micEnabled: micEnabled.value,
        speakerChecked: speakerChecked.value,
        cameraDeviceId: selectedCameraId.value || null,
        micDeviceId: selectedMicId.value || null,
        speakerDeviceId: selectedSpeakerId.value || null,
        secureContext: browserSupport.value.secureContext,
        permissionState: permissionState.value,
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

watch([selectedCameraId, selectedMicId], () => {
  if (!cameraEnabled.value && !micEnabled.value) return
  void refreshPreview({ silent: true })
})

onMounted(async () => {
  await load()
  await loadDevices()
  await refreshPermissionSnapshot()
  await refreshPreview({ silent: true })
})

onUnmounted(() => {
  stopPreview()
})
</script>

<template>
  <div class="live-prepare">
    <section class="hero card">
      <div class="hero-copy">
        <div class="eyebrow">实时课程准备</div>
        <h1>进入课堂前，先完成设备授权与环境自检</h1>
        <p>
          <template v-if="prepareData">
            你即将和 {{ prepareData.peerDisplayName || '对方' }} 进入实时课堂。现在先确认摄像头、麦克风和扬声器权限，避免开课时临时掉线或无法发声。
          </template>
          <template v-else>正在加载课堂信息。</template>
        </p>
        <div class="hero-status">
          <span
            class="status-chip"
            :class="permissionState"
            :data-state="permissionState"
            data-testid="prepare-permission-state"
          >{{ permissionHeadline }}</span>
          <span class="status-chip subtle">{{ permissionSummary }}</span>
        </div>
        <div
          v-if="prepareData?.joinBlockedReason && (!prepareData.joinableNow || !prepareData.canJoin || paymentBlocked)"
          class="blocking-tip"
          data-testid="prepare-join-blocked"
        >
          {{ prepareData.joinBlockedReason }}
        </div>
        <div class="hero-actions">
          <button
            class="btn btn-primary"
            type="button"
            data-testid="prepare-check-all"
            :disabled="preparingDevices"
            @click="runDeviceCheck('all')"
          >
            {{ preparingDevices ? '检测中...' : '一键检测设备权限' }}
          </button>
          <button class="btn btn-light" type="button" data-testid="prepare-open-help" @click="openSecurityGuide">
            查看浏览器设置指引
          </button>
        </div>
      </div>
      <div class="preview">
        <div class="preview-frame">
          <div class="preview-badge">本地预览</div>
          <div class="preview-surface" data-testid="prepare-preview">
            <video
              v-show="cameraEnabled && (permissionState === 'granted' || permissionState === 'partial')"
              ref="previewRef"
              class="preview-video"
              autoplay
              muted
              playsinline
              data-testid="prepare-video"
            />
            <div v-if="!cameraEnabled || (permissionState !== 'granted' && permissionState !== 'partial')" class="preview-placeholder">
              <div class="avatar-orb">你</div>
              <p v-if="!browserSupport.secureContext">当前页面不是安全连接，浏览器不会开放摄像头和麦克风。</p>
              <p v-else-if="cameraEnabled">{{ deviceError || '点击“一键检测设备权限”后，浏览器会弹出授权提示。' }}</p>
              <p v-else>摄像头已关闭，可仅使用语音上课。</p>
            </div>
          </div>
          <div class="preview-foot">
            <span>摄像头权限：{{ cameraPermission }}</span>
            <span>麦克风权限：{{ micPermission }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="panel-grid">
      <div class="card device-panel">
        <div class="section-head">
          <div>
            <div class="section-title">设备自检</div>
            <div class="section-subtitle">点击每一项按钮时，网站会主动向浏览器请求对应权限，并把结果展示在当前页。</div>
          </div>
        </div>

        <div class="check-list">
          <div v-for="item in deviceCheckItems" :key="item.key" class="check-card" :data-testid="`check-card-${item.key}`">
            <div class="check-main">
              <div class="check-title-row">
                <strong>{{ item.title }}</strong>
                <span class="mini-state" :class="item.status">{{ item.status === 'ready' ? '已完成' : item.status === 'warning' ? '待处理' : '未检测' }}</span>
              </div>
              <p>{{ item.detail }}</p>
            </div>
            <button
              v-if="item.key !== 'speaker'"
              class="btn btn-inline"
              type="button"
              :data-testid="`check-action-${item.key}`"
              :disabled="item.disabled"
              @click="runDeviceCheck(item.key as 'camera' | 'mic')"
            >
              {{ item.actionText }}
            </button>
            <button
              v-else
              class="btn btn-inline"
              type="button"
              data-testid="check-action-speaker"
              :disabled="item.disabled"
              @click="runSpeakerTest"
            >
              {{ item.actionText }}
            </button>
          </div>
        </div>

        <div class="device-toggles">
          <label class="toggle-row">
            <span>启用摄像头</span>
            <input
              v-model="cameraEnabled"
              type="checkbox"
              :data-enabled="cameraEnabled"
              data-testid="prepare-camera-state"
            />
          </label>
          <label class="toggle-row">
            <span>启用麦克风</span>
            <input
              v-model="micEnabled"
              type="checkbox"
              :data-enabled="micEnabled"
              data-testid="prepare-mic-state"
            />
          </label>
        </div>

        <div class="device-form">
          <label class="device-row stacked">
            <span>摄像头设备</span>
            <select v-model="selectedCameraId" class="select" data-testid="camera-device-select">
              <option value="">系统默认</option>
              <option v-for="item in cameraOptions" :key="item.deviceId" :value="item.deviceId">
                {{ item.label || `摄像头 ${item.deviceId.slice(-4)}` }}
              </option>
            </select>
          </label>
          <label class="device-row stacked">
            <span>麦克风设备</span>
            <select v-model="selectedMicId" class="select" data-testid="mic-device-select">
              <option value="">系统默认</option>
              <option v-for="item in microphoneOptions" :key="item.deviceId" :value="item.deviceId">
                {{ item.label || `麦克风 ${item.deviceId.slice(-4)}` }}
              </option>
            </select>
          </label>
          <label class="device-row stacked">
            <span>扬声器设备</span>
            <select v-model="selectedSpeakerId" class="select" data-testid="speaker-device-select">
              <option value="">系统默认</option>
              <option v-for="item in speakerOptions" :key="item.deviceId" :value="item.deviceId">
                {{ item.label || `扬声器 ${item.deviceId.slice(-4)}` }}
              </option>
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
        </div>
        <div v-if="deviceError" class="hint error" data-testid="prepare-device-error">{{ deviceError }}</div>
        <div v-if="speakerTestMessage" class="hint" data-testid="prepare-speaker-message">{{ speakerTestMessage }}</div>
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
        <div v-if="paymentBlocked" class="summary-row">
          <span>支付拦截</span>
          <strong>需先支付上一节课</strong>
        </div>
        <div class="summary-row">
          <span>建议模式</span>
          <strong>{{ cameraEnabled && micEnabled ? '音视频课堂' : micEnabled ? '语音课堂' : '低设备占用模式' }}</strong>
        </div>
        <div class="summary-row">
          <span>浏览器环境</span>
          <strong>{{ browserSupport.secureContext ? '安全连接' : '非安全连接' }}</strong>
        </div>
        <div v-if="error" class="hint error">{{ error }}</div>
        <div class="actions">
          <button class="btn" type="button" @click="$router.back()">稍后再说</button>
          <button
            class="btn btn-primary"
            type="button"
            data-testid="enter-classroom-button"
            :disabled="!canEnterClassroom"
            @click="enterClassroom"
          >
            {{ joining ? '进入中...' : '进入课堂' }}
          </button>
        </div>
      </div>
    </section>

    <div v-if="permissionModalOpen" class="mask" data-testid="permission-modal" @click.self="closePermissionModal">
      <div class="modal card">
        <div class="modal-header">
          <div>
            <div class="modal-eyebrow">设备授权提示</div>
            <h3>{{ permissionModalTitle }}</h3>
          </div>
          <button class="icon-btn" type="button" @click="closePermissionModal">×</button>
        </div>
        <p class="modal-desc">{{ permissionModalDesc }}</p>
        <ul class="modal-steps">
          <li v-for="item in permissionModalActions" :key="item">{{ item }}</li>
        </ul>
        <div class="modal-actions">
          <button class="btn" type="button" data-testid="permission-help-action" @click="openSecurityGuide">查看浏览器设置指引</button>
          <button class="btn" type="button" data-testid="permission-help-page-action" @click="openPermissionGuidePage">打开完整帮助页</button>
          <button class="btn btn-primary" type="button" data-testid="permission-retry-action" @click="runDeviceCheck('all')">重新检测</button>
        </div>
      </div>
    </div>

    <div v-if="securityHelpOpen" class="mask" data-testid="permission-guide-modal" @click.self="closeSecurityGuide">
      <div class="modal card guide-modal">
        <div class="modal-header">
          <div>
            <div class="modal-eyebrow">浏览器设置指引</div>
            <h3>如何在浏览器里开启摄像头和麦克风权限</h3>
          </div>
          <button class="icon-btn" type="button" @click="closeSecurityGuide">×</button>
        </div>
        <div class="guide-grid">
          <div class="guide-card">
            <strong>Chrome / Edge</strong>
            <p>点击地址栏左侧的站点图标，在“摄像头”“麦克风”里改成“允许”，然后刷新页面。</p>
          </div>
          <div class="guide-card">
            <strong>Safari</strong>
            <p>在菜单栏打开“Safari - 设置 - 网站 - 摄像头 / 麦克风”，把当前站点权限改成“允许”。</p>
          </div>
          <div class="guide-card">
            <strong>当前页面显示不安全</strong>
            <p>这说明当前域名没有被浏览器识别为安全连接。请优先改用 https 域名或受信任的 localhost 入口。</p>
          </div>
          <div class="guide-card">
            <strong>浏览器不再弹窗</strong>
            <p>通常是你之前已经拒绝过权限，需要先在站点设置里改成“允许”，浏览器才会再次放行。</p>
          </div>
        </div>
        <div class="guide-footer">
          <button class="btn btn-primary" type="button" data-testid="close-guide-action" @click="closeSecurityGuide">我知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.live-prepare {
  --ink: #17313d;
  --muted: rgba(17, 24, 39, 0.62);
  --line: rgba(15, 23, 42, 0.08);
  --mint: #0f766e;
  --mint-soft: rgba(15, 118, 110, 0.12);
  --danger: #c2410c;
  --danger-soft: rgba(194, 65, 12, 0.12);
  --card-bg: rgba(255, 255, 255, 0.9);
  display: grid;
  gap: 18px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(320px, 0.82fr);
  gap: 20px;
  padding: 24px;
  border-radius: 30px;
  border: 1px solid rgba(15, 118, 110, 0.12);
  background:
    radial-gradient(circle at 0% 0%, rgba(94, 234, 212, 0.2), transparent 32%),
    radial-gradient(circle at 100% 0%, rgba(45, 212, 191, 0.14), transparent 28%),
    linear-gradient(135deg, #f8fcff 0%, #eef8f4 52%, #f6fbfa 100%);
}

.eyebrow,
.modal-eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.5);
}

h1 {
  margin: 8px 0 12px;
  color: var(--ink);
  font-size: 34px;
  line-height: 1.12;
}

p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.hero-status {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  padding: 9px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--mint);
  background: rgba(15, 118, 110, 0.14);
}

.status-chip.partial,
.mini-state.pending {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.status-chip.denied,
.status-chip.unsupported,
.mini-state.warning {
  background: var(--danger-soft);
  color: var(--danger);
}

.status-chip.subtle {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  color: rgba(0, 0, 0, 0.65);
  font-weight: 600;
  white-space: normal;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.blocking-tip {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 236, 214, 0.9);
  color: #9a4b00;
  font-size: 13px;
  line-height: 1.6;
}

.preview-frame {
  height: 100%;
  border-radius: 24px;
  padding: 18px;
  color: #fff;
  background:
    radial-gradient(circle at top, rgba(45, 212, 191, 0.12), transparent 28%),
    linear-gradient(160deg, #0f2c2f 0%, #17393a 100%);
}

.preview-badge {
  font-size: 12px;
  opacity: 0.84;
}

.preview-surface {
  margin-top: 14px;
  min-height: 280px;
  border-radius: 20px;
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
  padding: 28px;
  text-align: center;
}

.avatar-orb {
  width: 92px;
  height: 92px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-size: 20px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.14);
}

.preview-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 18px;
}

.device-panel,
.summary-panel {
  padding: 20px;
  background: var(--card-bg);
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--ink);
}

.section-subtitle {
  margin-top: 6px;
  color: var(--muted);
  line-height: 1.6;
}

.check-list {
  display: grid;
  gap: 12px;
}

.check-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 252, 0.92));
}

.check-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.check-main p {
  margin-top: 8px;
}

.mini-state {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #047857;
  background: rgba(4, 120, 87, 0.12);
}

.device-toggles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.toggle-row,
.device-row,
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}

.toggle-row {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
}

.device-form {
  margin-top: 10px;
}

.device-row.stacked {
  align-items: flex-start;
  flex-direction: column;
}

.select {
  width: 100%;
  min-width: 180px;
}

.summary-row strong {
  color: var(--ink);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.hint {
  margin-top: 14px;
  color: var(--muted);
  line-height: 1.6;
}

.hint.error {
  color: var(--danger);
}

.mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(10px);
}

.modal {
  width: min(100%, 720px);
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.guide-modal {
  width: min(100%, 860px);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.modal-header h3 {
  margin: 6px 0 0;
  color: var(--ink);
  font-size: 28px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  font-size: 22px;
  cursor: pointer;
}

.modal-desc {
  margin-top: 14px;
}

.modal-steps {
  margin: 16px 0 0;
  padding-left: 20px;
  color: var(--ink);
  line-height: 1.8;
}

.modal-actions,
.guide-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.guide-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(249, 250, 251, 0.95), rgba(255, 255, 255, 0.98));
}

.guide-card strong {
  display: block;
  margin-bottom: 8px;
  color: var(--ink);
}

.btn-inline {
  white-space: nowrap;
}

@media (max-width: 980px) {
  .hero,
  .panel-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }

  .check-card {
    grid-template-columns: 1fr;
  }

  .device-toggles {
    grid-template-columns: 1fr;
  }
}
</style>
