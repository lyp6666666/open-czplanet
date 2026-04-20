import { flushPromises, mount } from '@vue/test-utils'
import { HttpResponse, http } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import CourseDetailPage from './CourseDetailPage.vue'
import { server } from '@/test/server'
import { useAuthStore } from '@/stores/auth'

const push = vi.fn()
const storage: Record<string, string> = {}

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ params: { courseId: '66' } }),
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

describe('CourseDetailPage', () => {
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

  it('loads course detail and lesson list', async () => {
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
      http.get('http://localhost/api/v1/schedule/courses/66/events', () =>
        ok([
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
    )

    const wrapper = mount(CourseDetailPage, {
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
    seedStudentAuth()
    await flushPromises()

    expect(wrapper.text()).toContain('线上一对一')
    expect(wrapper.text()).toContain('第 1 节｜试课')
    expect(wrapper.text()).toContain('每周三 19:00-21:00')
  })
})
