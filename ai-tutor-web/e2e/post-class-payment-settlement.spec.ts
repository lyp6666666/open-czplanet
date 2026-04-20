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
  lessonType?: string
  lessonPriceFen?: number
  trialPricePercent?: number
  payableAmountFen?: number
  paymentStatus?: string
  lessonPaymentOrderId?: number
  platformFeeRate?: number
  platformFeeAmountFen?: number
  teacherIncomeAmountFen?: number
}

const course = {
  courseId: 66,
  applicationId: 501,
  roomId: 88,
  teacherUid: 1001,
  studentUid: 2001,
  teachingMode: 'ONLINE',
  courseName: '线上一对一｜课后支付闭环',
  classTime: '每周三 19:00-20:00',
  frequencyPerWeek: 1,
  lessonPrice: '200 元/小时',
  status: 'TRIALING',
  trialStartAt: '2026-04-20T10:00:00',
  trialEndAt: '2026-04-27T10:00:00',
}

const application = {
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

const userBatch = [{ id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 }]
const userCard = {
  user: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
  teacherProfile: { subject: '数学', education: '硕士', experienceYears: 5, introduction: '擅长初中数学' },
  studentProfile: null,
  jobPosting: null,
}

async function seedAuth(page: import('@playwright/test').Page, user: { id: number; name: string; phone: string; userType: number; token: string }) {
  await page.addInitScript((payload) => {
    window.localStorage.setItem('ai_tutor_token', payload.token)
    window.localStorage.setItem('ai_tutor_user', JSON.stringify(payload))
    if (payload.userType === 1) {
      window.localStorage.setItem('ai_tutor_tutor_basic_completed', '1')
    }
  }, user)
}

async function switchAuth(
  page: import('@playwright/test').Page,
  user: { id: number; name: string; phone: string; userType: number; token: string },
  nextHash = '/',
) {
  await page.goto('/')
  await page.evaluate((payload) => {
    window.localStorage.setItem('ai_tutor_token', payload.token)
    window.localStorage.setItem('ai_tutor_user', JSON.stringify(payload))
    if (payload.userType === 1) {
      window.localStorage.setItem('ai_tutor_tutor_basic_completed', '1')
    } else {
      window.localStorage.removeItem('ai_tutor_tutor_basic_completed')
    }
  }, user)
  await page.goto(`/?auth_switch=${user.userType}_${user.id}#${nextHash.replace(/^#?\/?/, '/')}`)
}

async function routeCommonShellApis(
  page: import('@playwright/test').Page,
  currentUserRef: { current: { id: number; name: string; phone: string; userType: number; token?: string } },
) {
  await page.route('**/user/me', async (route) => {
    const user = currentUserRef.current
    await route.fulfill({
      json: {
        code: 0,
        data: {
          id: user.id,
          name: user.name,
          phone: user.phone,
          avatar: '',
          sex: null,
          userType: user.userType,
          teacherProfile: user.userType === 1 ? { realName: user.name, subject: '数学' } : null,
          studentProfile: user.userType === 2 ? { realName: user.name } : null,
        },
        message: 'ok',
      },
    })
  })
  await page.route('**/live/sessions/reminders**', async (route) => {
    await route.fulfill({ json: { code: 0, data: [], message: 'ok' } })
  })
  await page.route('**/chat/room/page**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { cursor: null, isLast: true, list: [] }, message: 'ok' } })
  })
  await page.route('**/chat/events/sync**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { cursor: null, isLast: true, latestEventId: null, list: [] }, message: 'ok' } })
  })
  await page.route('**/chat/stream/v2**', async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        'content-type': 'text/event-stream',
        'cache-control': 'no-cache',
      },
      body: 'event: ready\ndata: {"clientId":"mock-e2e-client","lastEventId":0}\n\n',
    })
  })
  await page.route('**/chat/stream', async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        'content-type': 'text/event-stream',
        'cache-control': 'no-cache',
      },
      body: '',
    })
  })
}

test('post-class payment settlement flow covers trial price, bill payment, and cashier success', async ({ page }) => {
  const lessons: LessonItem[] = [
    {
      id: 701,
      courseId: 66,
      title: '第 1 节｜试课',
      description: '第一节试课',
      startAt: 1771412400000,
      endAt: 1771416000000,
      status: 'ACCEPTED',
      creatorUserId: 2001,
      participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
      chatRoomId: 88,
      lessonType: 'TRIAL',
      lessonPriceFen: 20000,
      trialPricePercent: 50,
      payableAmountFen: 10000,
      paymentStatus: 'UNBILLED',
      platformFeeRate: 10,
      platformFeeAmountFen: 1000,
      teacherIncomeAmountFen: 9000,
    },
  ]

  let completed = false
  let prepayContext: any = null
  const currentUser = {
    current: { id: 1001, name: '王老师', phone: '13800138000', userType: 1, token: 'mock.teacher.token' },
  }

  await seedAuth(page, { id: 1001, name: '王老师', phone: '13800138000', userType: 1, token: 'mock.teacher.token' })
  await routeCommonShellApis(page, currentUser)

  await page.route('**/courses/66', async (route) => {
    await route.fulfill({ json: { code: 0, data: course, message: 'ok' } })
  })
  await page.route('**/chat/application/501', async (route) => {
    await route.fulfill({ json: { code: 0, data: application, message: 'ok' } })
  })
  await page.route('**/user/batch**', async (route) => {
    await route.fulfill({ json: { code: 0, data: userBatch, message: 'ok' } })
  })
  await page.route('**/user/card**', async (route) => {
    await route.fulfill({ json: { code: 0, data: userCard, message: 'ok' } })
  })
  await page.route('**/api/v1/schedule/courses/66/events', async (route) => {
    await route.fulfill({ json: { code: 0, data: lessons, message: 'ok' } })
  })
  await page.route('**/appointment/701/complete', async (route) => {
    completed = true
    lessons[0] = {
      ...lessons[0],
      status: 'COMPLETED',
      paymentStatus: 'PENDING',
      lessonPaymentOrderId: 8801,
    }
    await route.fulfill({ json: { code: 0, data: 'OK', message: 'ok' } })
  })
  await page.route('**/payment/prepay', async (route) => {
    prepayContext = await route.request().postDataJSON()
    lessons[0] = { ...lessons[0], paymentStatus: 'PAID' }
    await route.fulfill({
      json: {
        code: 0,
        data: {
          orderNo: 'PAY_LESSON_1',
          amountFen: 10000,
          channel: 'WECHAT',
          qrCodeUrl: 'https://pay.example.com/qr.png',
          expireTime: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
        },
        message: 'ok',
      },
    })
  })
  await page.route('**/payment/orders/PAY_LESSON_1', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: {
          orderNo: 'PAY_LESSON_1',
          status: 'SUCCESS',
          amountFen: 10000,
          channel: 'WECHAT',
          successTime: new Date().toISOString(),
        },
        message: 'ok',
      },
    })
  })

  await page.goto('/#/courses/66')

  await expect(page.getByText('首节试课')).toBeVisible()
  await expect(page.getByText('应付 ¥100.00')).toBeVisible()
  await expect(page.getByText('平台服务费 10%，预计到账 ¥90.00')).toBeVisible()

  page.once('dialog', async (dialog) => dialog.accept())
  await page.getByRole('button', { name: '结课' }).click()
  await expect.poll(() => completed).toBe(true)
  await expect(page.getByText('待支付')).toBeVisible()

  currentUser.current = { id: 2001, name: '学生2001', phone: '13800138001', userType: 2, token: 'mock.student.token' }
  await switchAuth(page, currentUser.current, '/courses/66')
  await page.getByRole('button', { name: '去支付' }).click()
  await expect(page).toHaveURL(/#\/pay\/cashier/)
  await expect.poll(() => prepayContext).not.toBeNull()
  expect(prepayContext).toMatchObject({ contextType: 'LESSON_PAYMENT_ORDER', contextId: 8801, channel: 'WECHAT' })
  await expect(page.getByText('支付完成')).toBeVisible()
})

test('offline failed trial refund requires video proof fields', async ({ page }) => {
  const offlineCourse = {
    ...course,
    teachingMode: 'OFFLINE',
    courseName: '线下试课退款审核',
  }
  let refundPayload: any = null
  const currentUser = {
    current: { id: 1001, name: '王老师', phone: '13800138000', userType: 1, token: 'mock.teacher.token' },
  }

  await seedAuth(page, { id: 1001, name: '王老师', phone: '13800138000', userType: 1, token: 'mock.teacher.token' })
  await routeCommonShellApis(page, currentUser)

  await page.route('**/courses/my**', async (route) => {
    await route.fulfill({ json: { code: 0, data: [offlineCourse], message: 'ok' } })
  })
  await page.route('**/chat/application/501', async (route) => {
    await route.fulfill({ json: { code: 0, data: { ...application, teachingMode: 'OFFLINE' }, message: 'ok' } })
  })
  await page.route('**/user/batch**', async (route) => {
    await route.fulfill({ json: { code: 0, data: [{ id: 2001, name: '李同学', realName: '李同学', avatar: '', userType: 2 }], message: 'ok' } })
  })
  await page.route('**/user/card**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { ...userCard, user: { id: 2001, name: '李同学', realName: '李同学', avatar: '', userType: 2 } }, message: 'ok' } })
  })
  await page.route('**/api/v1/schedule/events**', async (route) => {
    await route.fulfill({ json: { code: 0, data: [], message: 'ok' } })
  })
  await page.route('**/api/v1/assets/upload', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: { objectKey: 'trial/a.png', url: 'https://assets.example.com/trial/a.png', contentType: 'image/png', size: 10 },
        message: 'ok',
      },
    })
  })
  await page.route('**/courses/66/trial-refund/apply', async (route) => {
    refundPayload = await route.request().postDataJSON()
    await route.fulfill({ json: { code: 0, data: 9901, message: 'ok' } })
  })

  await page.goto('/#/courses/my')

  await page.getByRole('button', { name: '试课不通过' }).click()
  await page.getByPlaceholder('请填写试课不通过说明').fill('微信沟通后确认不合适')
  await page.setInputFiles('input[type="file"]', {
    name: 'proof.png',
    mimeType: 'image/png',
    buffer: Buffer.from('proof'),
  })
  await page.getByPlaceholder('微信录屏 URL（1 分钟内）').fill('https://assets.example.com/wechat-proof.mp4')
  await page.getByPlaceholder('录屏时长（秒）').fill('45')
  await page.getByRole('button', { name: '提交申请' }).click()

  await expect.poll(() => refundPayload).not.toBeNull()
  expect(refundPayload).toMatchObject({
    reason: '微信沟通后确认不合适',
    evidenceImageUrls: ['https://assets.example.com/trial/a.png'],
    evidenceVideoUrl: 'https://assets.example.com/wechat-proof.mp4',
    evidenceVideoDurationSeconds: 45,
  })
})
