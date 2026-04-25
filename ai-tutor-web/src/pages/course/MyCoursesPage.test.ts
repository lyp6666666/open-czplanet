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

function mockBaseCourseApis(courseStatus: string, extraCourse: Record<string, unknown> = {}, events: unknown[] = []) {
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
      http.post('http://localhost/chat/application/501/enter-chat', enterChat),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('待支付信息费')
    const payBtn = wrapper.findAll('button').find((btn) => btn.text() === '去支付')
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

  it('opens schedule modal and creates a lesson from the cooperation detail', async () => {
    const createEvent = vi.fn(async ({ request }: { request: Request }) => {
      const body = (await request.json()) as { title: string; participantUserId: number; startAt: number; endAt: number }
      return ok({
        id: 701,
        courseId: 66,
        lessonType: 'NORMAL',
        title: body.title,
        description: null,
        startAt: body.startAt,
        endAt: body.endAt,
        status: 'PENDING',
        creatorUserId: 2001,
        participant: { id: body.participantUserId, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
        chatRoomId: 88,
      })
    })

    server.use(
      ...mockBaseCourseApis('TEACHING'),
      http.post('http://localhost/api/v1/schedule/events', createEvent),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    const scheduleBtn = wrapper.findAll('button').find((btn) => btn.text() === '安排正式课')
    expect(scheduleBtn).toBeTruthy()
    await scheduleBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('预约课程')
    await wrapper.get('.m-ops .btn-primary').trigger('click')
    await flushPromises()

    expect(createEvent).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('约课待确认')
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
      http.get('http://localhost/live/sessions/by-course/701', () =>
        ok({
          sessionId: null,
          courseId: 701,
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
    expect(wrapper.text()).toContain('在预约结束前均可进入课堂')
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
      http.get('http://localhost/live/sessions/by-course/701', () =>
        ok({
          sessionId: 9001,
          courseId: 701,
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
          courseId: 701,
          resultStatus: 'PENDING',
          preview: null,
        }),
      ),
    )

    const wrapper = mount(MyCoursesPage, { global: { plugins: [createPinia()] } })
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('待确认未上课')
    expect(wrapper.text()).toContain('本节未正常开始')
    expect(wrapper.text()).toContain('未走正常课后流程')
  })
})
