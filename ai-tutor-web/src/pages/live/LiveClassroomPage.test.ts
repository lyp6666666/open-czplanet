import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const getByCourseMock = vi.fn()
const statusMock = vi.fn()
const joinTokenMock = vi.fn()
const leaveMock = vi.fn()
const endMock = vi.fn()
const connectMock = vi.fn()
const disconnectMock = vi.fn()
const setMicrophoneEnabledMock = vi.fn()
const setCameraEnabledMock = vi.fn()

vi.mock('@/api/live', () => ({
  liveApi: {
    getByCourse: (...args: unknown[]) => getByCourseMock(...args),
    status: (...args: unknown[]) => statusMock(...args),
    joinToken: (...args: unknown[]) => joinTokenMock(...args),
    leave: (...args: unknown[]) => leaveMock(...args),
    end: (...args: unknown[]) => endMock(...args),
  },
}))

vi.mock('@/modules/live/livekit', () => ({
  LiveRoomClient: vi.fn().mockImplementation(() => ({
    room: {
      localParticipant: {
        getTrackPublication: vi.fn(() => ({
          track: {
            attach: vi.fn(() => document.createElement('video')),
            detach: vi.fn(),
          },
        })),
      },
    },
    connect: (...args: unknown[]) => connectMock(...args),
    disconnect: (...args: unknown[]) => disconnectMock(...args),
    setMicrophoneEnabled: (...args: unknown[]) => setMicrophoneEnabledMock(...args),
    setCameraEnabled: (...args: unknown[]) => setCameraEnabledMock(...args),
    onTrackSubscribed: vi.fn(),
    onTrackUnsubscribed: vi.fn(),
    onParticipantConnected: vi.fn(),
    onParticipantDisconnected: vi.fn(),
    onDisconnected: vi.fn(),
    onConnectionStateChanged: vi.fn(),
    onMediaError: vi.fn(),
  })),
  attachTrackToElement: vi.fn(),
  detachTrack: vi.fn(),
}))

import LiveClassroomPage from './LiveClassroomPage.vue'

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

describe('LiveClassroomPage', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'localStorage', { value: createMemoryStorage(), configurable: true })
    getByCourseMock.mockReset()
    statusMock.mockReset()
    joinTokenMock.mockReset()
    leaveMock.mockReset()
    endMock.mockReset()
    connectMock.mockReset()
    disconnectMock.mockReset()
    setMicrophoneEnabledMock.mockReset()
    setCameraEnabledMock.mockReset()
  })

  it('shows waiting state when peer not joined', async () => {
    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
    })
    statusMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
    })
    joinTokenMock.mockResolvedValue({
      serverUrl: 'ws://127.0.0.1:7880',
      accessToken: 'token',
    })
    connectMock.mockResolvedValue(undefined)

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [{ path: '/live/classroom/:courseId', component: LiveClassroomPage }],
    })
    router.push('/live/classroom/66')
    await router.isReady()

    const wrapper = mount(LiveClassroomPage, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('正在等待对方加入')
    expect(joinTokenMock).toHaveBeenCalledTimes(1)
    expect(connectMock).toHaveBeenCalledTimes(1)
  })

  it('toggles microphone through live room client', async () => {
    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
    })
    statusMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
    })
    joinTokenMock.mockResolvedValue({
      serverUrl: 'ws://127.0.0.1:7880',
      accessToken: 'token',
    })
    connectMock.mockResolvedValue(undefined)
    setMicrophoneEnabledMock.mockResolvedValue(undefined)

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [{ path: '/live/classroom/:courseId', component: LiveClassroomPage }],
    })
    router.push('/live/classroom/66')
    await router.isReady()

    const wrapper = mount(LiveClassroomPage, {
      global: { plugins: [router] },
    })
    await flushPromises()

    await wrapper.get('[data-testid="classroom-toggle-mic"]').trigger('click')
    await flushPromises()

    expect(setMicrophoneEnabledMock).toHaveBeenCalledWith(false, null)
  })
})
