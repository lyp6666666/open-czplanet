import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import LiveClassroomPage from './LiveClassroomPage.vue'

const getByCourseMock = vi.fn()
const statusMock = vi.fn()

vi.mock('@/api/live', () => ({
  liveApi: {
    getByCourse: (...args: unknown[]) => getByCourseMock(...args),
    status: (...args: unknown[]) => statusMock(...args),
    leave: vi.fn(),
    end: vi.fn(),
  },
}))

describe('LiveClassroomPage', () => {
  beforeEach(() => {
    getByCourseMock.mockReset()
    statusMock.mockReset()
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
  })
})
