import { expect, test } from '@playwright/test'

type LessonItem = {
  id: number
  courseId: number
  title: string
  description: string | null
  startAt: number
  endAt: number
  status: string
  creatorUserId: number
  participant: { id: number; name: string; realName: string; avatar: string; userType: number }
  chatRoomId: number
  proposedStartAt?: number
  proposedEndAt?: number
  proposedBy?: number
  cancelBy?: number
}

const baseCourse = {
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
}

const baseApplication = {
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
}

const teacherBatch = [{ id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 }]
const teacherCard = {
  user: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
  teacherProfile: { subject: '数学', education: '硕士', experienceYears: 5, introduction: '擅长初中数学' },
  studentProfile: null,
  jobPosting: null,
}

test('course detail page supports cancel, reschedule, and confirm reschedule', async ({ page }) => {
  const lessons: LessonItem[] = [
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
    {
      id: 703,
      courseId: 66,
      title: '第 3 节｜几何专项',
      description: '图形综合',
      startAt: 1771585200000,
      endAt: 1771588800000,
      status: 'RESCHEDULE_PENDING',
      creatorUserId: 2001,
      participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
      chatRoomId: 88,
      proposedStartAt: 1771671600000,
      proposedEndAt: 1771675200000,
      proposedBy: 1001,
    },
  ]

  let reschedulePayload: any = null
  let confirmRescheduleCalled = false

  await page.route('**/courses/66', async (route) => {
    await route.fulfill({ json: { code: 0, data: baseCourse, message: 'ok' } })
  })
  await page.route('**/chat/application/501', async (route) => {
    await route.fulfill({ json: { code: 0, data: baseApplication, message: 'ok' } })
  })
  await page.route('**/user/batch**', async (route) => {
    await route.fulfill({ json: { code: 0, data: teacherBatch, message: 'ok' } })
  })
  await page.route('**/user/card**', async (route) => {
    await route.fulfill({ json: { code: 0, data: teacherCard, message: 'ok' } })
  })
  await page.route('**/api/v1/schedule/courses/66/events', async (route) => {
    await route.fulfill({ json: { code: 0, data: lessons, message: 'ok' } })
  })
  await page.route('**/api/v1/schedule/events/701/cancel', async (route) => {
    lessons[0] = { ...lessons[0], status: 'CANCELED', description: '从课程详情页取消课节', cancelBy: 2001 }
    await route.fulfill({ json: { code: 0, data: lessons[0], message: 'ok' } })
  })
  await page.route('**/appointment/702/reschedule', async (route) => {
    reschedulePayload = await route.request().postDataJSON()
    lessons[1] = {
      ...lessons[1],
      status: 'RESCHEDULE_PENDING',
      description: String(reschedulePayload.remark || '调课说明'),
      proposedBy: 2001,
      proposedStartAt: Date.parse(String(reschedulePayload.proposedStartTime)),
      proposedEndAt:
        Date.parse(String(reschedulePayload.proposedStartTime)) + Number(reschedulePayload.durationMinutes || 60) * 60 * 1000,
    }
    await route.fulfill({ json: { code: 0, data: 'OK', message: 'ok' } })
  })
  await page.route('**/appointment/703/confirmReschedule', async (route) => {
    confirmRescheduleCalled = true
    lessons[2] = {
      ...lessons[2],
      status: 'ACCEPTED',
      startAt: lessons[2].proposedStartAt || lessons[2].startAt,
      endAt: lessons[2].proposedEndAt || lessons[2].endAt,
      proposedStartAt: undefined,
      proposedEndAt: undefined,
      proposedBy: undefined,
    }
    await route.fulfill({ json: { code: 0, data: 'OK', message: 'ok' } })
  })

  await page.goto('/')
  await page.evaluate(() => {
    localStorage.setItem('ai_tutor_token', 'mock.student.token')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({
        id: 2001,
        name: '学生2001',
        phone: '13800138000',
        userType: 2,
        token: 'mock.student.token',
      }),
    )
  })
  await page.goto('/#/courses/66')
  await expect(page.getByRole('heading', { name: '线上一对一｜200 元/小时｜每周三 19:00-21:00' })).toBeVisible()
  await expect(page.getByText('首节试课')).toBeVisible()

  page.once('dialog', async (dialog) => dialog.accept())
  await page.locator('.lesson-item').filter({ hasText: '第 1 节｜试课' }).getByRole('button', { name: '删课' }).click()
  await expect(page.locator('.lesson-item').filter({ hasText: '第 1 节｜试课' }).getByText('已取消')).toBeVisible()

  await page.locator('.lesson-item').filter({ hasText: '第 2 节｜函数强化' }).getByRole('button', { name: '调课' }).click()
  await page.getByPlaceholder('例如：学校活动冲突，希望顺延到周四晚上').fill('学校活动冲突，希望顺延')
  await page.getByRole('button', { name: '发起调课' }).click()
  await expect.poll(() => reschedulePayload).not.toBeNull()
  await expect(page.locator('.lesson-item').filter({ hasText: '第 2 节｜函数强化' }).getByText('待确认调课')).toBeVisible()
  await expect(page.getByText('由你发起调课')).toBeVisible()

  page.once('dialog', async (dialog) => dialog.accept())
  await page.locator('.lesson-item').filter({ hasText: '第 3 节｜几何专项' }).getByRole('button', { name: '确认改期' }).click()
  await expect.poll(() => confirmRescheduleCalled).toBe(true)
  await expect(page.locator('.lesson-item').filter({ hasText: '第 3 节｜几何专项' }).getByText('已确认')).toBeVisible()
})
