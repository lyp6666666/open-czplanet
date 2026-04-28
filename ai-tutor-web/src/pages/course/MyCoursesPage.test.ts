import { flushPromises, mount } from '@vue/test-utils'
import { HttpResponse, http } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MyCoursesPage from './MyCoursesPage.vue'
import { server } from '@/test/server'
import { useAuthStore } from '@/stores/auth'

const push = vi.fn()
const storage: Record<string, string> = {}

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

function ok<T>(data: T) {
  return HttpResponse.json({ code: 0, data, message: 'ok' })
}

function seedStudentAuth() {
  const auth = useAuthStore()
  auth.token = 'mock.student.token'
  auth.user = {
    id: 2001,
    name: '学生2001',
    phone: '13800138000',
    avatar: '',
    sex: null,
    userType: 2,
    token: 'mock.student.token',
  }
}

function seedTeacherAuth() {
  const auth = useAuthStore()
  auth.token = 'mock.teacher.token'
  auth.user = {
    id: 1001,
    name: '王老师',
    phone: '13800138001',
    avatar: '',
    sex: null,
    userType: 1,
    token: 'mock.teacher.token',
  }
}

function mockBaseCourseApis(courseStatus: string, extraCourse: Record<string, unknown> = {}, events: unknown[] = [], extraApplication: Record<string, unknown> = {}) {
  return [
    http.get('http://localhost/user/email/reminder-hints', () => ok({ show: false, title: '', description: '', actionText: '' })),
    http.get('http://localhost/courses/my', () =>
      ok([
        {
          courseId: 66,
          applicationId: 501,
          roomId: 88,
          teacherUid: 1001,
          studentUid: 2001,
          status: courseStatus,
          trialEndAt: '2026-04-27T10:00:00',
          ...extraCourse,
        },
      ]),
    ),
    http.get('http://localhost/chat/application/501', () =>
      ok({
        id: 501,
        senderUid: 2001,
        receiverUid: 1001,
        senderRole: 'STUDENT',
        receiverRole: 'TEACHER',
        contextType: 'TUTOR',
        contextId: 11,
        content: '想约数学课',
        status: 'ACCEPTED',
        chatAccessStatus: 'CHAT_ENABLED',
        paymentPayerRole: 'TEACHER',
        orderId: 9001,
        roomId: 88,
        receiverRead: true,
        decidedAt: null,
        createTime: '2026-04-20T10:00:00',
        ...extraApplication,
      }),
    ),
    http.get('http://localhost/user/batch', () =>
      ok([{ id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 }]),
    ),
    http.get('http://localhost/user/card', () =>
      ok({
        user: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
        teacherProfile: { subject: '数学', education: '硕士', experienceYears: 5, introduction: '擅长高中数学' },
        studentProfile: null,
        jobPosting: null,
      }),
    ),
    http.get('http://localhost/api/v1/schedule/events', () => ok(events)),
    http.get('http://localhost/live/sessions/reminders', () => ok([])),
    http.options('http://localhost/live/sessions/by-course/:courseId', () => new HttpResponse(null, { status: 200 })),
    http.options('http://localhost/live/sessions/:sessionId/ai/result', () => new HttpResponse(null, { status: 200 })),
  ]
}

describe('MyCoursesPage', () => {
  beforeEach(() => {
    Object.keys(storage).forEach((key) => delete storage[key])
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        getItem: vi.fn((key: string) => storage[key] ?? null),
        setItem: vi.fn((key: string, value: string) => {
          storage[key] = value
        }),
        removeItem: vi.fn((key: string) => {
          delete storage[key]
        }),
        clear: vi.fn(() => {
          Object.keys(storage).forEach((key) => delete storage[key])
        }),
      },
    })
    setActivePinia(createPinia())
    seedStudentAuth()
    push.mockReset()
  })

  it('opens cashier through the detail action when info fee is pending', async () => {
    const enterChat = vi.fn(() => ok({ paymentRequired: true, waitingForTeacherPayment: false, orderId: 9001, roomId: null }))

    server.use(
      ...mockBaseCourseApis('WAIT_PAY', { trialEndAt: null }, []),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
      http.post('http://localhost/chat/application/501/enter-chat', enterChat),
    )

    const pinia = createPinia()
    setActivePinia(pinia)
    seedTeacherAuth()
    const wrapper = mount(MyCoursesPage, { global: { plugins: [pinia] } })
    await flushPromises()

    expect(wrapper.text()).toContain('待支付信息费')
    const payBtn = wrapper.findAll('button').find((btn) => btn.text() === '支付信息费')
    expect(payBtn).toBeTruthy()
    await payBtn!.trigger('click')
    await flushPromises()

    expect(enterChat).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith({
      name: 'cashierPay',
      query: {
        contextType: 'BROKERAGE_ORDER',
        contextId: '9001',
        applicationId: '501',
      },
    })
  })

  it('does not render schedule shortcut buttons on my courses page', async () => {
    server.use(
      ...mockBaseCourseApis('TEACHING'),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).not.toContain('安排正式课')
    expect(wrapper.text()).not.toContain('查看日程表')
  })

  it('opens chat collaboration composer from communicating stage', async () => {
    server.use(
      ...mockBaseCourseApis('COMMUNICATING'),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    const startBtn = wrapper.findAll('button').find((btn) => btn.text() === '发起合作')
    expect(startBtn).toBeTruthy()
    await startBtn!.trigger('click')

    expect(push).toHaveBeenCalledWith({
      name: 'chatRoom',
      params: { roomId: '88' },
      query: { otherUid: '1001', action: 'collaboration' },
    })
  })

  it('shows trial lesson as directly enterable before the scheduled end time', async () => {
    server.use(
      ...mockBaseCourseApis('TRIALING', {}, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() + 22 * 60 * 60 * 1000,
          endAt: Date.now() + 24 * 60 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('试课阶段')
    expect(wrapper.text()).toContain('现在可以进入课堂')
    expect(wrapper.text()).toContain('预约时间只用于提醒，双方也可以提前进入课堂准备')
  })

  it('shows concise teacher copy while waiting for student trial decision', async () => {
    server.use(
      ...mockBaseCourseApis('TRIAL_WAIT_STUDENT_DECISION', {}, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() - 3 * 60 * 60 * 1000,
          endAt: Date.now() - 60 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 2001, name: 'syj', realName: 'syj', avatar: '', userType: 2 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          status: 'ENDED',
          actualStartAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
          actualEndAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
          joinableNow: false,
          canJoin: false,
        }),
      ),
      http.get('http://localhost/live/sessions/9001/ai/result', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          resultStatus: 'PENDING',
          preview: null,
        }),
      ),
    )

    const pinia = createPinia()
    setActivePinia(pinia)
    seedTeacherAuth()
    const wrapper = mount(MyCoursesPage, { global: { plugins: [pinia] } })
    await flushPromises()

    expect(wrapper.text()).toContain('等待学生确认试课结果')
    expect(wrapper.text()).toContain('24 小时内确认')
    expect(wrapper.text()).toContain('退还 60% 信息费')
    expect(wrapper.text()).toContain('微信或站内聊天')
    const chatButtons = wrapper.findAll('button').filter((btn) => btn.text() === '进入聊天沟通')
    expect(chatButtons).toHaveLength(1)
    expect(wrapper.findAll('button').filter((btn) => btn.text() === '进入聊天')).toHaveLength(0)
  })

  it('shows abnormal pending confirmation when lesson expired without realtime session', async () => {
    server.use(
      ...mockBaseCourseApis('TRIALING', {}, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() - 4 * 60 * 60 * 1000,
          endAt: Date.now() - 2 * 60 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        }),
      ),
      http.get('http://localhost/live/sessions/9001/ai/result', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          resultStatus: 'PENDING',
          preview: null,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('待确认未上课')
    expect(wrapper.text()).toContain('未检测到双方成功进入实时课堂')
    expect(wrapper.text()).toContain('未走正常课后流程')
  })

  it('does not mark lesson as abnormal after class already ended', async () => {
    server.use(
      ...mockBaseCourseApis('TRIALING', {}, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() - 4 * 60 * 60 * 1000,
          endAt: Date.now() - 2 * 60 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          status: 'ENDED',
          actualStartAt: '2026-04-25T10:00:00',
          actualEndAt: '2026-04-25T11:00:00',
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        }),
      ),
      http.get('http://localhost/live/sessions/9001/ai/result', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          resultStatus: 'READY',
          preview: '课后总结已生成',
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('课堂已结束')
    expect(wrapper.find('.metric-strip').text()).toContain('0有待上课程')
    expect(wrapper.text()).not.toContain('对方未到场 / 本节未正常开始，等待双方或客服确认。')
    expect(wrapper.text()).toContain('课后总结已生成')
  })

  it('allows student to confirm expired trial even when backend status is still TRIALING', async () => {
    const submitTrialResult = vi.fn(() => ok(true))

    server.use(
      ...mockBaseCourseApis('TRIALING', { trialEndAt: '2026-04-01T10:00:00' }, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() - 4 * 60 * 60 * 1000,
          endAt: Date.now() - 2 * 60 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        }),
      ),
      http.post('http://localhost/courses/66/trial-result', submitTrialResult),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    const passBtn = wrapper.findAll('button').find((btn) => btn.text() === '试课合适')
    expect(passBtn).toBeTruthy()
    await passBtn!.trigger('click')
    await flushPromises()

    expect(submitTrialResult).toHaveBeenCalledTimes(1)
  })

  it('routes to live prepare with courseId instead of latest lesson id', async () => {
    server.use(
      ...mockBaseCourseApis('TRIALING', {}, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() + 30 * 60 * 1000,
          endAt: Date.now() + 90 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          status: 'CREATED',
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: true,
          canJoin: true,
        }),
      ),
      http.get('http://localhost/live/sessions/by-course/701', () =>
        ok({
          sessionId: null,
          courseId: 701,
          status: 'CREATED',
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
      http.get('http://localhost/live/sessions/9001/ai/result', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          resultStatus: 'PENDING',
          preview: null,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    const enterBtn = wrapper.findAll('button').find((btn) => btn.text() === '进入课堂')
    expect(enterBtn).toBeTruthy()
    await enterBtn!.trigger('click')

    expect(push).toHaveBeenCalledWith({
      name: 'livePrepare',
      params: { courseId: '66' },
    })
  })

  it('does not mark lesson as abnormal when there is peer attendance evidence', async () => {
    server.use(
      ...mockBaseCourseApis('TRIALING', {}, [
        {
          id: 701,
          courseId: 66,
          lessonType: 'TRIAL',
          title: '试课｜线上一对一',
          description: null,
          startAt: Date.now() - 4 * 60 * 60 * 1000,
          endAt: Date.now() - 2 * 60 * 60 * 1000,
          status: 'ACCEPTED',
          creatorUserId: 1001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
        },
      ]),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          peerJoined: true,
          peerOnline: false,
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        }),
      ),
      http.get('http://localhost/live/sessions/9001/ai/result', () =>
        ok({
          sessionId: 9001,
          courseId: 66,
          resultStatus: 'PENDING',
          preview: null,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).not.toContain('待确认未上课')
    expect(wrapper.text()).not.toContain('本节未正常开始')
  })

  it('hides backend APPLYING courses from my courses', async () => {
    server.use(
      ...mockBaseCourseApis('APPLYING', { trialEndAt: null }, []),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无符合条件的合作')
    expect(wrapper.text()).not.toContain('申请中')
  })

  it('shows student-side wait-pay copy without a pay action', async () => {
    server.use(
      ...mockBaseCourseApis('WAIT_PAY', { trialEndAt: null }, []),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('等待教师支付信息费')
    expect(wrapper.text()).toContain('如果教师在48小时内没有支付信息费用，该项目进入归档状态（流程停止）')
    expect(wrapper.findAll('button').some((btn) => btn.text() === '支付信息费')).toBe(false)
  })

  it('lets the receiver accept a pending collaboration proposal from my courses', async () => {
    const respond = vi.fn(() => ok({ id: 900, roomId: 88, msgType: 1, body: { type: 'collaboration_status', proposalId: 77, status: 'ACCEPTED' } }))

    server.use(
      ...mockBaseCourseApis('COMMUNICATING', {
        latestProposal: {
          id: 77,
          fromUid: 1001,
          toUid: 2001,
          status: 'PENDING',
          pricePerHour: '200 元/小时',
          trialStartAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          trialEndAt: new Date(Date.now() + 26 * 60 * 60 * 1000).toISOString(),
          remark: '先试讲一次',
          expireAt: new Date(Date.now() + 48 * 60 * 60 * 1000).toISOString(),
        },
      }),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
      http.post('http://localhost/chat/collaboration/proposal/77/response', respond),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('待确认试课合作')
    expect(wrapper.text()).toContain('合作待你确认')
    const acceptBtn = wrapper.findAll('button').find((btn) => btn.text() === '同意安排')
    expect(acceptBtn).toBeTruthy()
    await acceptBtn!.trigger('click')
    await flushPromises()

    expect(respond).toHaveBeenCalledTimes(1)
  })

  it('submits teacher info-fee refund through the confirmation modal', async () => {
    const requestRefund = vi.fn(() => ok({ id: 901, roomId: 88, msgType: 1, body: { type: 'refund_status', status: 'PENDING' } }))
    const pinia = createPinia()
    setActivePinia(pinia)
    seedTeacherAuth()

    server.use(
      ...mockBaseCourseApis('COMMUNICATING'),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
      http.post('http://localhost/chat/refund/apply', requestRefund),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [pinia] } })
    await flushPromises()

    const openBtn = wrapper.findAll('button').find((btn) => btn.text() === '申请退信息费')
    expect(openBtn).toBeTruthy()
    await openBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('如申请退费之后聊天功能将立刻全部关闭')
    await wrapper.find('textarea[placeholder="请说明为什么当前沟通不合适，便于平台审核"]').setValue('沟通后发现需求不匹配')
    await wrapper.findAll('button').find((btn) => btn.text() === '确认申请退费')!.trigger('click')
    await flushPromises()

    expect(requestRefund).toHaveBeenCalledTimes(1)
  })

  it('shows student-facing demand reopen copy after teacher info-fee refund', async () => {
    server.use(
      ...mockBaseCourseApis(
        'REFUNDED',
        {
          latestRefund: {
            id: 94,
            type: 'BROKERAGE',
            status: 'APPROVED',
            reason: '沟通后发现需求不匹配',
            adminNote: '审核通过',
            refundPercent: 100,
            refundAmountFen: 19900,
            createTime: '2026-04-26T21:24:00',
            decidedAt: '2026-04-26T21:27:00',
          },
        },
        [],
        { contextType: 'DEMAND', contextId: 5001 },
      ),
      http.get('http://localhost/live/sessions/by-course/66', () =>
        ok({
          sessionId: null,
          courseId: 66,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          joinableNow: false,
          canJoin: false,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('本次沟通已结束，继续为你匹配老师')
    expect(wrapper.text()).toContain('你的需求已重新开放给其他老师可见')
    expect(wrapper.text()).not.toContain('退费信息')
    expect(wrapper.text()).not.toContain('退费比例')
    expect(wrapper.text()).not.toContain('审批通过，已退费')

    const editBtn = wrapper.findAll('button').find((btn) => btn.text() === '修改需求')
    expect(editBtn).toBeTruthy()
    await editBtn!.trigger('click')

    expect(push).toHaveBeenCalledWith({ name: 'studentEditJob', params: { id: '5001' } })
  })
})
