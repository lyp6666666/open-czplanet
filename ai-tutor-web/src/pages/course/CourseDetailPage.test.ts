import { flushPromises, mount } from '@vue/test-utils'
import { HttpResponse, http } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import CourseDetailPage from './CourseDetailPage.vue'
import { server } from '@/test/server'
import { useAuthStore } from '@/stores/auth'

const push = vi.fn()
const toastShow = vi.fn()
const storage: Record<string, string> = {}

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ params: { courseId: '66' } }),
}))

vi.mock('@/stores/toast', () => ({
  useToastStore: () => ({ show: toastShow }),
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
    name: '老师1001',
    phone: '13800138001',
    avatar: '',
    sex: null,
    userType: 1,
    token: 'mock.teacher.token',
  }
}

function installLocalStorageMock() {
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
}

function buildCourseDetailHandlers(lessonListFactory?: () => any[]) {
  server.use(
    http.get('http://localhost/courses/66', () =>
      ok({
        courseId: 66,
        applicationId: 501,
        roomId: 88,
        teacherUid: 1001,
        studentUid: 2001,
        teachingMode: 'ONLINE',
        courseName: '线上一对一｜200 元/小时｜每周三 19:00-21:00',
        classTime: '每周三 19:00-21:00',
        frequencyPerWeek: 2,
        lessonPrice: '200 元/小时',
        status: 'TRIALING',
        trialStartAt: '2026-04-20T10:00:00',
        trialEndAt: '2026-04-27T10:00:00',
      }),
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
        teachingMode: 'ONLINE',
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
        teacherProfile: { subject: '数学', education: '硕士', experienceYears: 5, introduction: '擅长初中数学' },
        studentProfile: null,
        jobPosting: null,
      }),
    ),
    http.get('http://localhost/api/v1/schedule/courses/66/events', () =>
      ok(
        lessonListFactory
          ? lessonListFactory()
          : [
              {
                id: 701,
                courseId: 66,
                title: '第 1 节｜试课',
                description: '先做摸底',
                startAt: 1771412400000,
                endAt: 1771416000000,
                status: 'PENDING',
                creatorUserId: 2001,
                participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
                chatRoomId: 88,
              },
            ],
      ),
    ),
  )
}

function mountPage() {
  return mount(CourseDetailPage, {
    global: {
      plugins: [createPinia()],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a><slot /></a>',
        },
      },
    },
  })
}

describe('CourseDetailPage', () => {
  beforeEach(() => {
    installLocalStorageMock()
    setActivePinia(createPinia())
    seedStudentAuth()
    push.mockReset()
    toastShow.mockReset()
    vi.restoreAllMocks()
  })

  it('loads course detail and lesson list', async () => {
    buildCourseDetailHandlers()

    const wrapper = mountPage()
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('线上一对一')
    expect(wrapper.text()).toContain('第 1 节｜试课')
    expect(wrapper.text()).toContain('首节试课')
    expect(wrapper.text()).toContain('每周三 19:00-21:00')
  })

  it('cancels lesson from course detail page', async () => {
    buildCourseDetailHandlers(() => [
      {
        id: 701,
        courseId: 66,
        title: '第 1 节｜试课',
        description: '先做摸底',
        startAt: 1771412400000,
        endAt: 1771416000000,
        status: 'PENDING',
        creatorUserId: 2001,
        participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
        chatRoomId: 88,
      },
    ])
    server.use(
      http.post('http://localhost/api/v1/schedule/events/701/cancel', async () =>
        ok({
          id: 701,
          courseId: 66,
          title: '第 1 节｜试课',
          description: '从课程详情页取消课节',
          startAt: 1771412400000,
          endAt: 1771416000000,
          status: 'CANCELED',
          creatorUserId: 2001,
          participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          chatRoomId: 88,
          cancelBy: 2001,
        }),
      ),
    )
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountPage()
    seedStudentAuth()
    await flushPromises()

    const cancelButtons = wrapper.findAll('button').filter((node) => node.text() === '删课')
    expect(cancelButtons).toHaveLength(1)
    const cancelButton = cancelButtons[0]
    expect(cancelButton).toBeDefined()
    await cancelButton!.trigger('click')
    await flushPromises()

    expect(confirmSpy).toHaveBeenCalled()
    expect(wrapper.text()).toContain('已取消')
    expect(toastShow).toHaveBeenCalledWith('课节已取消。', 'success')
  })

  it('submits reschedule request from course detail page', async () => {
    buildCourseDetailHandlers(() => [
      {
        id: 702,
        courseId: 66,
        title: '第 2 节｜函数强化',
        description: '函数拔高',
        startAt: 1771498800000,
        endAt: 1771502400000,
        status: 'ACCEPTED',
        creatorUserId: 2001,
        participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
        chatRoomId: 88,
      },
    ])
    let reschedulePayload: any = null
    let listCount = 0
    server.use(
      http.post('http://localhost/appointment/702/reschedule', async ({ request }) => {
        reschedulePayload = await request.json()
        return ok('OK')
      }),
      http.get('http://localhost/api/v1/schedule/courses/66/events', () => {
        listCount += 1
        if (listCount <= 1) {
          return ok([
            {
              id: 702,
              courseId: 66,
              title: '第 2 节｜函数强化',
              description: '函数拔高',
              startAt: 1771498800000,
              endAt: 1771502400000,
              status: 'ACCEPTED',
              creatorUserId: 2001,
              participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
              chatRoomId: 88,
            },
          ])
        }
        return ok([
          {
            id: 702,
            courseId: 66,
            title: '第 2 节｜函数强化',
            description: '学校活动冲突，希望顺延',
            startAt: 1771498800000,
            endAt: 1771502400000,
            status: 'RESCHEDULE_PENDING',
            creatorUserId: 2001,
            participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
            chatRoomId: 88,
            proposedStartAt: 1771585200000,
            proposedEndAt: 1771588800000,
            proposedBy: 2001,
          },
        ])
      }),
    )

    const wrapper = mountPage()
    seedStudentAuth()
    await flushPromises()

    const rescheduleButtons = wrapper.findAll('button').filter((node) => node.text() === '调课')
    expect(rescheduleButtons).toHaveLength(1)
    const rescheduleButton = rescheduleButtons[0]
    expect(rescheduleButton).toBeDefined()
    await rescheduleButton!.trigger('click')
    await flushPromises()

    const textareas = wrapper.findAll('textarea')
    const rescheduleTextarea = textareas[textareas.length - 1]
    expect(rescheduleTextarea).toBeDefined()
    await rescheduleTextarea!.setValue('学校活动冲突，希望顺延')
    const primaryButtons = wrapper.findAll('button').filter((node) => node.text() === '发起调课')
    const submitButton = primaryButtons[0]
    expect(submitButton).toBeDefined()
    await submitButton!.trigger('click')
    await flushPromises()

    expect(reschedulePayload).toMatchObject({
      durationMinutes: 60,
      remark: '学校活动冲突，希望顺延',
    })
    expect(String(reschedulePayload.proposedStartTime)).toContain('T')
    expect(wrapper.text()).toContain('待确认调课')
    expect(wrapper.text()).toContain('由你发起调课')
    expect(toastShow).toHaveBeenCalledWith('调课申请已发出，等待对方确认。', 'success')
  })

  it('confirms reschedule when current user is counterparty', async () => {
    seedTeacherAuth()
    let confirmRescheduleCalled = false
    let listCount = 0
    buildCourseDetailHandlers(() => [
      {
        id: 703,
        courseId: 66,
        title: '第 3 节｜几何专项',
        description: '图形综合',
        startAt: 1771498800000,
        endAt: 1771502400000,
        status: 'RESCHEDULE_PENDING',
        creatorUserId: 2001,
        participant: { id: 2001, name: '学生2001', realName: '学生2001', avatar: '', userType: 2 },
        chatRoomId: 88,
        proposedStartAt: 1771585200000,
        proposedEndAt: 1771588800000,
        proposedBy: 2001,
      },
    ])
    server.use(
      http.get('http://localhost/user/batch', () =>
        ok([{ id: 2001, name: '学生2001', realName: '学生2001', avatar: '', userType: 2 }]),
      ),
      http.get('http://localhost/user/card', () =>
        ok({
          user: { id: 2001, name: '学生2001', realName: '学生2001', avatar: '', userType: 2 },
          teacherProfile: null,
          studentProfile: { demandDescription: '初中数学提升' },
          jobPosting: null,
        }),
      ),
      http.post('http://localhost/appointment/703/confirmReschedule', () => {
        confirmRescheduleCalled = true
        return ok('OK')
      }),
      http.get('http://localhost/api/v1/schedule/courses/66/events', () => {
        listCount += 1
        if (listCount <= 1) {
          return ok([
            {
              id: 703,
              courseId: 66,
              title: '第 3 节｜几何专项',
              description: '图形综合',
              startAt: 1771498800000,
              endAt: 1771502400000,
              status: 'RESCHEDULE_PENDING',
              creatorUserId: 2001,
              participant: { id: 2001, name: '学生2001', realName: '学生2001', avatar: '', userType: 2 },
              chatRoomId: 88,
              proposedStartAt: 1771585200000,
              proposedEndAt: 1771588800000,
              proposedBy: 2001,
            },
          ])
        }
        return ok([
          {
            id: 703,
            courseId: 66,
            title: '第 3 节｜几何专项',
            description: '图形综合',
            startAt: 1771585200000,
            endAt: 1771588800000,
            status: 'ACCEPTED',
            creatorUserId: 2001,
            participant: { id: 2001, name: '学生2001', realName: '学生2001', avatar: '', userType: 2 },
            chatRoomId: 88,
          },
        ])
      }),
    )
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountPage()
    seedTeacherAuth()
    await flushPromises()

    const confirmButtons = wrapper.findAll('button').filter((node) => node.text() === '确认改期')
    expect(confirmButtons).toHaveLength(1)
    const confirmButton = confirmButtons[0]
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')
    await flushPromises()

    expect(confirmSpy).toHaveBeenCalled()
    expect(confirmRescheduleCalled).toBe(true)
    expect(wrapper.text()).toContain('已确认')
    expect(toastShow).toHaveBeenCalledWith('调课已确认。', 'success')
  })
})
