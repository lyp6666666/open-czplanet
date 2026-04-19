import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'

import LivePreparePage from './LivePreparePage.vue'

const prepareMock = vi.fn()
const reportMock = vi.fn()

vi.mock('@/api/live', () => ({
  liveApi: {
    prepare: (...args: unknown[]) => prepareMock(...args),
    reportDevice: (...args: unknown[]) => reportMock(...args),
  },
}))

describe('LivePreparePage', () => {
  beforeEach(() => {
    prepareMock.mockReset()
    reportMock.mockReset()
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
      routes: [{ path: '/live/prepare/:courseId', component: LivePreparePage }],
    })
    router.push('/live/prepare/66')
    await router.isReady()

    const wrapper = mount(LivePreparePage, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('王同学')
    expect(wrapper.text()).toContain('进入课堂前')
  })
})
