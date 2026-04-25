import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const getByCourseMock = vi.fn()
const statusMock = vi.fn()
const joinTokenMock = vi.fn()
const joinAckMock = vi.fn()
const leaveMock = vi.fn()
const endMock = vi.fn()
const listMessagesMock = vi.fn()
const sendTextMock = vi.fn()
const connectMock = vi.fn()
const disconnectMock = vi.fn()
const setMicrophoneEnabledMock = vi.fn()
const setCameraEnabledMock = vi.fn()
const getTrackPublicationMock = vi.fn()
const attachTrackToElementMock = vi.fn()

const remoteParticipantsMap = new Map<string, { identity: string; name: string; trackPublications: Map<string, { track?: unknown }> }>()

vi.mock('@/api/live', () => ({
  liveApi: {
    getByCourse: (...args: unknown[]) => getByCourseMock(...args),
    status: (...args: unknown[]) => statusMock(...args),
    joinToken: (...args: unknown[]) => joinTokenMock(...args),
    joinAck: (...args: unknown[]) => joinAckMock(...args),
    leave: (...args: unknown[]) => leaveMock(...args),
    end: (...args: unknown[]) => endMock(...args),
  },
}))

vi.mock('@/api/chat', () => ({
  chatApi: {
    listMessages: (...args: unknown[]) => listMessagesMock(...args),
    sendText: (...args: unknown[]) => sendTextMock(...args),
  },
}))

vi.mock('@/modules/live/livekit', () => ({
  LiveRoomClient: vi.fn().mockImplementation(() => ({
    room: {
      remoteParticipants: remoteParticipantsMap,
      localParticipant: {
        getTrackPublication: getTrackPublicationMock.mockImplementation(() => ({
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
    onLocalTrackPublished: vi.fn(),
    onTrackSubscribed: vi.fn(),
    onTrackUnsubscribed: vi.fn(),
    onParticipantConnected: vi.fn(),
    onParticipantDisconnected: vi.fn(),
    onDisconnected: vi.fn(),
    onConnectionStateChanged: vi.fn(),
    onMediaError: vi.fn(),
  })),
  attachTrackToElement: (...args: unknown[]) => attachTrackToElementMock(...args),
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
    const pinia = createPinia()
    setActivePinia(pinia)
    Object.defineProperty(window, 'localStorage', { value: createMemoryStorage(), configurable: true })
    getByCourseMock.mockReset()
    statusMock.mockReset()
    joinTokenMock.mockReset()
    joinAckMock.mockReset()
    leaveMock.mockReset()
    endMock.mockReset()
    listMessagesMock.mockReset()
    sendTextMock.mockReset()
    connectMock.mockReset()
    disconnectMock.mockReset()
    setMicrophoneEnabledMock.mockReset()
    setCameraEnabledMock.mockReset()
    getTrackPublicationMock.mockReset()
    attachTrackToElementMock.mockReset()
    remoteParticipantsMap.clear()
    listMessagesMock.mockResolvedValue({ list: [], cursor: null, isLast: true })
    joinAckMock.mockImplementation(async (_sessionId: number, _payload: unknown) => ({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
      roomId: 7001,
    }))
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
      roomId: 7001,
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
      roomId: 7001,
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
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('正在等待对方加入')
    expect(joinTokenMock).toHaveBeenCalledTimes(1)
    expect(joinAckMock).toHaveBeenCalledTimes(1)
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
      roomId: 7001,
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
      roomId: 7001,
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
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.get('[data-testid="classroom-toggle-mic"]').trigger('click')
    await flushPromises()

    expect(setMicrophoneEnabledMock).toHaveBeenCalledWith(false, null)
  })

  it('shows live classroom chat panel and sends class message', async () => {
    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: true,
      roomId: 7001,
    })
    statusMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: true,
      roomId: 7001,
    })
    joinTokenMock.mockResolvedValue({
      serverUrl: 'ws://127.0.0.1:7880',
      accessToken: 'token',
    })
    connectMock.mockResolvedValue(undefined)
    sendTextMock.mockResolvedValue({
      fromUser: { uid: 1001 },
      toUser: { uid: 1002 },
      message: {
        id: 9001,
        roomId: 7001,
        sendTime: new Date().toISOString(),
        body: { type: 'text', content: '我们开始上课' },
      },
    })

    window.localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 1001, name: '老师', phone: '18800000000', userType: 1, token: 'token' }),
    )
    window.localStorage.setItem('ai_tutor_token', 'token')

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [{ path: '/live/classroom/:courseId', component: LiveClassroomPage }],
    })
    router.push('/live/classroom/66')
    await router.isReady()

    const wrapper = mount(LiveClassroomPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.findAll('.tab').find((button: { text: () => string }) => button.text() === '课中聊天')?.trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="live-chat-list"]').exists()).toBe(true)
    expect(listMessagesMock).toHaveBeenCalledWith({ roomId: 7001, pageSize: 20, cursor: null })

    await wrapper.get('[data-testid="live-chat-input"]').setValue('我们开始上课')
    await wrapper.get('[data-testid="live-chat-send"]').trigger('click')
    await flushPromises()

    expect(sendTextMock).toHaveBeenCalledWith(7001, '我们开始上课')
    expect(wrapper.text()).toContain('我们开始上课')
  })

  it('hydrates existing remote participant tracks right after room connection', async () => {
    const remoteVideoTrack = {
      kind: 'video',
      attach: vi.fn(() => {
        const media = document.createElement('video')
        media.srcObject = new MediaStream()
        return media
      }),
      detach: vi.fn(),
    }
    remoteParticipantsMap.set('remote-user', {
      identity: '1002',
      name: '王同学',
      trackPublications: new Map([
        [
          'video-track',
          {
            track: remoteVideoTrack,
          },
        ],
      ]),
    })

    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
      roomId: 7001,
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
      roomId: 7001,
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
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('已进入课堂，正在等待对方加入')
    expect(wrapper.get('[data-testid="remote-video-state"]').attributes('data-connected')).toBe('true')
    expect(attachTrackToElementMock).toHaveBeenCalled()
  })

  it('shows peer joined copy when remote participant is connected even before backend status catches up', async () => {
    remoteParticipantsMap.set('remote-user', {
      identity: '1002',
      name: '王同学',
      trackPublications: new Map(),
    })

    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'CREATED',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
      roomId: 7001,
    })
    statusMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'CREATED',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
      roomId: 7001,
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
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('王同学 已加入，等待视频画面')
    expect(wrapper.text()).not.toContain('已进入课堂，正在等待对方加入')
  })

  it('still sends join ack when local media publish partially fails', async () => {
    getByCourseMock.mockResolvedValue({
      sessionId: 8,
      courseId: 66,
      status: 'IN_PROGRESS',
      providerRoomName: 'class-66',
      provider: 'LIVEKIT',
      teacherUid: 1001,
      studentUid: 1002,
      peerJoined: false,
      roomId: 7001,
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
      roomId: 7001,
    })
    joinTokenMock.mockResolvedValue({
      serverUrl: 'ws://127.0.0.1:7880',
      accessToken: 'token',
    })
    connectMock.mockResolvedValue({
      cameraError: new Error('camera blocked'),
      micError: null,
    })

    const router = createRouter({
      history: createWebHashHistory(),
      routes: [{ path: '/live/classroom/:courseId', component: LiveClassroomPage }],
    })
    router.push('/live/classroom/66')
    await router.isReady()

    mount(LiveClassroomPage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    expect(connectMock).toHaveBeenCalledTimes(1)
    expect(joinAckMock).toHaveBeenCalledTimes(1)
  })
})
