import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import LivePreparePage from './LivePreparePage.vue'

const {
  prepareMock,
  reportMock,
  updateAiOptionsMock,
  requestUserMediaPreviewMock,
  stopMediaStreamMock,
  listLocalMediaDevicesMock,
  attachMediaStreamMock,
  inspectBrowserMediaSupportMock,
  queryBrowserMediaPermissionsMock,
  playSpeakerTestToneMock,
  MockBrowserMediaError,
} = vi.hoisted(() => {
  class MockBrowserMediaError extends Error {
    code: string

    constructor(code: string, message: string) {
      super(message)
      this.code = code
    }
  }
  return {
    prepareMock: vi.fn(),
    reportMock: vi.fn(),
    updateAiOptionsMock: vi.fn(),
    requestUserMediaPreviewMock: vi.fn(),
    stopMediaStreamMock: vi.fn(),
    listLocalMediaDevicesMock: vi.fn(),
    attachMediaStreamMock: vi.fn(),
    inspectBrowserMediaSupportMock: vi.fn(),
    queryBrowserMediaPermissionsMock: vi.fn(),
    playSpeakerTestToneMock: vi.fn(),
    MockBrowserMediaError,
  }
})

vi.mock('@/api/live', () => ({
  liveApi: {
    prepare: (...args: unknown[]) => prepareMock(...args),
    reportDevice: (...args: unknown[]) => reportMock(...args),
    updateAiOptions: (...args: unknown[]) => updateAiOptionsMock(...args),
  },
}))

vi.mock('@/modules/live/livekit', () => ({
  BrowserMediaError: MockBrowserMediaError,
  requestUserMediaPreview: (...args: unknown[]) => requestUserMediaPreviewMock(...args),
  stopMediaStream: (...args: unknown[]) => stopMediaStreamMock(...args),
  listLocalMediaDevices: (...args: unknown[]) => listLocalMediaDevicesMock(...args),
  attachMediaStream: (...args: unknown[]) => attachMediaStreamMock(...args),
  inspectBrowserMediaSupport: (...args: unknown[]) => inspectBrowserMediaSupportMock(...args),
  queryBrowserMediaPermissions: (...args: unknown[]) => queryBrowserMediaPermissionsMock(...args),
  playSpeakerTestTone: (...args: unknown[]) => playSpeakerTestToneMock(...args),
}))

function createMemoryStorage() {
  const store = new Map<string, string>()
  return {
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    setItem(key: string, value: string) {
      store.set(key, String(value))
    },
    removeItem(key: string) {
      store.delete(key)
    },
    clear() {
      store.clear()
    },
  }
}

function createPrepareData() {
  return {
    sessionId: 8,
    status: 'JOIN_OPEN',
    courseTitle: '实时课程',
    peerDisplayName: '王同学',
    canJoin: true,
    joinableNow: true,
    deviceCheckRequired: true,
  }
}

async function mountPage() {
  const router = createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/live/prepare/:courseId', component: LivePreparePage, name: 'livePrepare' },
      { path: '/live/classroom/:courseId', component: { template: '<div>classroom</div>' }, name: 'liveClassroom' },
      { path: '/live/permission-help', component: { template: '<div>permission help</div>' }, name: 'livePermissionHelp' },
    ],
  })
  router.push('/live/prepare/66')
  await router.isReady()

  const wrapper = mount(LivePreparePage, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('LivePreparePage', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'localStorage', { value: createMemoryStorage(), configurable: true })
    prepareMock.mockReset()
    reportMock.mockReset()
    updateAiOptionsMock.mockReset()
    requestUserMediaPreviewMock.mockReset()
    stopMediaStreamMock.mockReset()
    listLocalMediaDevicesMock.mockReset()
    attachMediaStreamMock.mockReset()
    inspectBrowserMediaSupportMock.mockReset()
    queryBrowserMediaPermissionsMock.mockReset()
    playSpeakerTestToneMock.mockReset()

    prepareMock.mockResolvedValue(createPrepareData())
    reportMock.mockResolvedValue(true)
    updateAiOptionsMock.mockResolvedValue({ sessionId: 8 })
    listLocalMediaDevicesMock.mockResolvedValue({
      cameras: [{ deviceId: 'cam-1', kind: 'videoinput', label: 'FaceTime 摄像头' }],
      microphones: [{ deviceId: 'mic-1', kind: 'audioinput', label: '内置麦克风' }],
      speakers: [{ deviceId: 'speaker-1', kind: 'audiooutput', label: '系统扬声器' }],
    })
    requestUserMediaPreviewMock.mockResolvedValue({} as MediaStream)
    inspectBrowserMediaSupportMock.mockReturnValue({
      secureContext: true,
      hasMediaDevices: true,
      canEnumerateDevices: true,
      canGetUserMedia: true,
      canSelectSpeaker: true,
      supported: true,
    })
    queryBrowserMediaPermissionsMock.mockResolvedValue({ camera: 'granted', microphone: 'granted' })
    playSpeakerTestToneMock.mockResolvedValue({ canSelectSpeaker: true })
  })

  it('loads prepare data and exposes one-click device authorization', async () => {
    const { wrapper } = await mountPage()

    expect(wrapper.text()).toContain('王同学')
    expect(wrapper.text()).toContain('一键检测设备权限')
    expect(wrapper.get('[data-testid="prepare-permission-state"]').attributes('data-state')).toBe('granted')
    expect(requestUserMediaPreviewMock).toHaveBeenCalled()

    await wrapper.get('[data-testid="check-action-speaker"]').trigger('click')
    await flushPromises()

    expect(playSpeakerTestToneMock).toHaveBeenCalledWith({ speakerDeviceId: 'speaker-1' })
    expect(wrapper.text()).toContain('试听成功')
  })

  it('opens permission modal when browser cannot request media in insecure context', async () => {
    inspectBrowserMediaSupportMock.mockReturnValue({
      secureContext: false,
      hasMediaDevices: false,
      canEnumerateDevices: false,
      canGetUserMedia: false,
      canSelectSpeaker: false,
      supported: false,
    })
    queryBrowserMediaPermissionsMock.mockResolvedValue({ camera: 'denied', microphone: 'denied' })
    requestUserMediaPreviewMock.mockRejectedValue(
      new MockBrowserMediaError('INSECURE_CONTEXT', '当前页面不是安全连接，浏览器不会开放摄像头或麦克风权限'),
    )

    const { wrapper } = await mountPage()
    await wrapper.get('[data-testid="prepare-check-all"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="prepare-permission-state"]').attributes('data-state')).toBe('unsupported')
    expect(wrapper.get('[data-testid="permission-modal"]').text()).toContain('当前页面无法直接申请设备权限')
    expect(wrapper.text()).toContain('https')
  })

  it('reports device status before entering classroom', async () => {
    const { wrapper, router } = await mountPage()

    await wrapper.get('[data-testid="enter-classroom-button"]').trigger('click')
    await flushPromises()

    expect(reportMock).toHaveBeenCalledTimes(1)
    expect(updateAiOptionsMock).toHaveBeenCalledWith(8, {
      realtimeSummaryEnabled: true,
      postClassSummaryEnabled: true,
    })
    const [, payload] = reportMock.mock.calls[0]!
    expect(payload).toMatchObject({
      cameraStatus: 'READY',
      micStatus: 'READY',
      speakerStatus: 'UNTESTED',
      deviceInfo: {
        secureContext: true,
        permissionState: 'granted',
      },
    })
    expect(router.currentRoute.value.name).toBe('liveClassroom')
  })

  it('shows payment block reason and disables enter button when previous lesson is unpaid', async () => {
    prepareMock.mockResolvedValue({
      ...createPrepareData(),
      canJoin: false,
      joinBlockedReason: '上一节课尚未支付，支付后才能进入本节课堂',
      blockingPaymentOrderId: 9001,
      blockingLessonId: 77,
    })

    const { wrapper } = await mountPage()

    expect(wrapper.get('[data-testid="prepare-join-blocked"]').text()).toContain('上一节课尚未支付')
    expect(wrapper.get('[data-testid="enter-classroom-button"]').attributes('disabled')).toBeDefined()
  })
})
