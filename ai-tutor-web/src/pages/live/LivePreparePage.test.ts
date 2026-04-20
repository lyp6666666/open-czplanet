import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import LivePreparePage from './LivePreparePage.vue'

const prepareMock = vi.fn()
const reportMock = vi.fn()
const requestUserMediaPreviewMock = vi.fn()
const stopMediaStreamMock = vi.fn()
const listLocalMediaDevicesMock = vi.fn()
const attachMediaStreamMock = vi.fn()

vi.mock('@/api/live', () => ({
  liveApi: {
    prepare: (...args: unknown[]) => prepareMock(...args),
    reportDevice: (...args: unknown[]) => reportMock(...args),
  },
}))

vi.mock('@/modules/live/livekit', () => ({
  BrowserMediaError: class BrowserMediaError extends Error {
    code: string

    constructor(code: string, message: string) {
      super(message)
      this.code = code
    }
  },
  requestUserMediaPreview: (...args: unknown[]) => requestUserMediaPreviewMock(...args),
  stopMediaStream: (...args: unknown[]) => stopMediaStreamMock(...args),
  listLocalMediaDevices: (...args: unknown[]) => listLocalMediaDevicesMock(...args),
  attachMediaStream: (...args: unknown[]) => attachMediaStreamMock(...args),
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

describe('LivePreparePage', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'localStorage', { value: createMemoryStorage(), configurable: true })
    prepareMock.mockReset()
    reportMock.mockReset()
    requestUserMediaPreviewMock.mockReset()
    stopMediaStreamMock.mockReset()
    listLocalMediaDevicesMock.mockReset()
    attachMediaStreamMock.mockReset()
    listLocalMediaDevicesMock.mockResolvedValue({ cameras: [], microphones: [], speakers: [] })
    requestUserMediaPreviewMock.mockResolvedValue({} as MediaStream)
  })

  it('loads prepare data and shows peer display name', async () => {
    prepareMock.mockResolvedValue({
      sessionId: 8,
      status: 'JOIN_OPEN',
      courseTitle: '实时课程',
      peerDisplayName: '王同学',
      canJoin: true,
      joinableNow: true,
      deviceCheckRequired: true,
    })

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [
        { path: '/live/prepare/:courseId', component: LivePreparePage, name: 'livePrepare' },
        { path: '/live/classroom/:courseId', component: { template: '<div>classroom</div>' }, name: 'liveClassroom' },
      ],
    })
    router.push('/live/prepare/66')
    await router.isReady()

    const wrapper = mount(LivePreparePage, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('王同学')
    expect(wrapper.text()).toContain('进入课堂前')
    expect(requestUserMediaPreviewMock).toHaveBeenCalled()
  })

  it('reports device status before entering classroom', async () => {
    prepareMock.mockResolvedValue({
      sessionId: 8,
      status: 'JOIN_OPEN',
      courseTitle: '实时课程',
      peerDisplayName: '王同学',
      canJoin: true,
      joinableNow: true,
      deviceCheckRequired: true,
    })
    reportMock.mockResolvedValue(true)

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [
        { path: '/live/prepare/:courseId', component: LivePreparePage, name: 'livePrepare' },
        { path: '/live/classroom/:courseId', component: { template: '<div>classroom</div>' }, name: 'liveClassroom' },
      ],
    })
    router.push('/live/prepare/66')
    await router.isReady()

    const wrapper = mount(LivePreparePage, {
      global: { plugins: [router] },
    })
    await flushPromises()

    await wrapper.get('[data-testid="enter-classroom-button"]').trigger('click')
    await flushPromises()

    expect(reportMock).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.name).toBe('liveClassroom')
  })
})
