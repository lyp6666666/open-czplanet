import { expect, test, type Page } from '@playwright/test'

const webBaseUrl = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const opsToken = process.env.OPS_VERIFY_TOKEN || 'DevOpsVerifyTokenForE2E'
const e2eLessonPriceFen = 2000

type LoginUser = {
  id: number
  token: string
  userType: number
  name: string | null
  phone: string
  avatar?: string | null
}

type ChatMessageResp = {
  message?: {
    roomId?: number
    body?: {
      applicationId?: number
      proposalId?: number
    }
  }
}

type TutorApplicationDetail = {
  id: number
  roomId: number
  status: string
  teachingMode?: 'ONLINE' | 'OFFLINE'
}

type CourseDetail = {
  courseId: number
  applicationId: number
  roomId: number
  teacherUid: number
  studentUid: number
  courseName: string
  status: string
}

type ScheduleEvent = {
  id: number
  courseId: number | null
  title: string
  startAt: number
  endAt: number
  status: string
  lessonType?: 'TRIAL' | 'NORMAL'
  paymentStatus?: string
  lessonPaymentOrderId?: number | null
}

type PaymentOrderStatus = {
  orderNo: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED'
  amountFen: number
  channel: 'WECHAT' | 'ALIPAY'
  expireTime?: string
  successTime?: string
}

function randPhone(prefix: string) {
  const n = Math.floor(Math.random() * 1e8)
  return `${prefix}${String(n).padStart(8, '0')}`.slice(0, 11)
}

async function api<T>(path: string, options: { method?: string; token?: string; body?: unknown; headers?: Record<string, string> } = {}) {
  const response = await fetch(new URL(path, apiBaseUrl), {
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...(options.headers || {}),
    },
    body: options.body == null ? undefined : JSON.stringify(options.body),
  })
  const text = await response.text()
  const json = text ? JSON.parse(text) : {}
  if (!response.ok || json.code !== 0) {
    throw new Error(`API failed ${options.method || 'GET'} ${path}: ${response.status} ${text}`)
  }
  return json.data as T
}

async function devLogin(role: 'TEACHER' | 'STUDENT', phone: string): Promise<LoginUser> {
  return api<LoginUser>(`/api/v1/public/dev/sms/login?phone=${phone}&role=${role}`, {
    headers: { 'X-Ops-Token': opsToken },
  })
}

async function switchAuth(page: Page, user: LoginUser, hash = '/') {
  await page.goto('/')
  await page.evaluate((authUser) => {
    localStorage.setItem('ai_tutor_token', authUser.token)
    localStorage.setItem('ai_tutor_user', JSON.stringify(authUser))
    if (authUser.userType === 1) {
      localStorage.setItem('ai_tutor_tutor_basic_completed', '1')
    } else {
      localStorage.removeItem('ai_tutor_tutor_basic_completed')
    }
  }, user)
  // 通过整页跳转刷新应用态，确保真实链路里的请求都带上当前用户 token。
  await page.goto(`/?auth_switch=${user.userType}_${user.id}#${hash.replace(/^#?\/?/, '/')}`)
}

async function createOnlineLongCourseFlow() {
  const teacher = await devLogin('TEACHER', randPhone('188'))
  const student = await devLogin('STUDENT', randPhone('186'))

  const startChat = await api<ChatMessageResp>('/chat/application/start-chat', {
    method: 'POST',
    token: student.token,
    body: {
      receiverUid: teacher.id,
      contextType: 'TUTOR',
      contextId: teacher.id,
      content: 'Playwright 线上课程全链路测试',
      teachingMode: 'ONLINE',
      clientRequestId: `pw-online-${Date.now()}`,
    },
  })

  const applicationId = startChat?.message?.body?.applicationId
  const roomId = startChat?.message?.roomId
  if (!applicationId || !roomId) {
    throw new Error(`missing applicationId/roomId: ${JSON.stringify(startChat)}`)
  }

  await api<unknown>(`/chat/application/${applicationId}/decision-message`, {
    method: 'POST',
    token: teacher.token,
    body: { action: 'ACCEPT' },
  })

  const enterChat = await api<{ paymentRequired?: boolean; orderId?: number; roomId?: number }>(`/chat/application/${applicationId}/enter-chat`, {
    method: 'POST',
    token: student.token,
    body: {},
  })
  if (!enterChat.orderId) {
    throw new Error(`missing brokerage order for application ${applicationId}`)
  }

  await api<unknown>(`/payment/dev/orders/${enterChat.orderId}/mock-success`, {
    method: 'POST',
    headers: { 'X-Ops-Token': opsToken },
  }).catch(() => {
    // 兼容旧环境：如果远端还没同步到新支付调试接口，这里先静默，后续由显式环境启动步骤解决。
  })

  const appDetail = await api<TutorApplicationDetail>(`/chat/application/${applicationId}`, {
    token: teacher.token,
  })

  const chatMessages = await api<{ list?: Array<{ message?: { body?: { type?: string; proposalId?: number } } }> }>(`/chat/public/msg/page?roomId=${roomId}&pageSize=100`, {
    token: teacher.token,
  })

  const proposalMsg = (chatMessages.list || []).find((item) => item?.message?.body?.type === 'collaboration_proposal')
  let proposalId = proposalMsg?.message?.body?.proposalId
  if (!proposalId) {
    const collab = await api<ChatMessageResp>('/chat/collaboration/proposal', {
      method: 'POST',
      token: teacher.token,
      body: {
        roomId,
        pricePerHour: '200',
        classTime: '每周三 19:00-20:00',
        frequencyPerWeek: 1,
      },
    })
    proposalId = collab?.message?.body?.proposalId
  }
  if (!proposalId) {
    throw new Error(`missing collaboration proposal in room ${roomId}`)
  }

  await api<unknown>(`/chat/collaboration/proposal/${proposalId}/response`, {
    method: 'POST',
    token: student.token,
    body: { action: 'ACCEPT' },
  })

  const courseList = await api<Array<{ courseId: number; applicationId: number }>>('/courses/my?role=TEACHER&page=1&size=20', {
    token: teacher.token,
  })
  const course = courseList.find((item) => item.applicationId === applicationId)
  if (!course) {
    throw new Error(`missing course for application ${applicationId}`)
  }

  const courseDetail = await api<CourseDetail>(`/courses/${course.courseId}`, { token: teacher.token })
  return { teacher, student, roomId, applicationId, courseId: course.courseId, courseDetail, appDetail }
}

async function listCourseEvents(courseId: number, token: string) {
  return api<ScheduleEvent[]>(`/api/v1/schedule/courses/${courseId}/events`, { token })
}

async function createCourseEvent(courseId: number, token: string, participantUserId: number, payload: Partial<ScheduleEvent> & { title: string; startAt: number; endAt: number }) {
  return api<ScheduleEvent>('/api/v1/schedule/events', {
    method: 'POST',
    token,
    body: {
      courseId,
      participantUserId,
      title: payload.title,
      startAt: payload.startAt,
      endAt: payload.endAt,
      lessonType: payload.lessonType,
      lessonPriceFen: e2eLessonPriceFen,
      trialPricePercent: payload.lessonType === 'TRIAL' ? 50 : undefined,
      description: 'Playwright E2E',
    },
  })
}

async function acceptEvent(eventId: number, token: string) {
  return api<ScheduleEvent>(`/api/v1/schedule/events/${eventId}/response`, {
    method: 'POST',
    token,
    body: { action: 'ACCEPT' },
  })
}

async function completeEvent(eventId: number, token: string) {
  return api<string>(`/appointment/${eventId}/complete`, {
    method: 'POST',
    token,
    body: {},
  })
}

test.describe('real online course payment flow', () => {
  test('blocks next lesson until previous lesson is paid, then unlocks live prepare after real payment success', async ({ page }) => {
    const flow = await createOnlineLongCourseFlow()

    const now = Date.now()
    const lessonOne = await createCourseEvent(flow.courseId, flow.teacher.token, flow.student.id, {
      title: '第 1 节｜试课',
      startAt: now + 60 * 60 * 1000,
      endAt: now + 2 * 60 * 60 * 1000,
      lessonType: 'TRIAL',
    })
    await acceptEvent(lessonOne.id, flow.student.token)

    const lessonTwo = await createCourseEvent(flow.courseId, flow.teacher.token, flow.student.id, {
      title: '第 2 节｜正式课',
      startAt: now + 3 * 60 * 60 * 1000,
      endAt: now + 4 * 60 * 60 * 1000,
      lessonType: 'NORMAL',
    })
    await acceptEvent(lessonTwo.id, flow.student.token)

    await switchAuth(page, flow.teacher, `/courses/${flow.courseId}`)
    await expect(page.getByText('长期课程详情')).toBeVisible()
    await expect(page.getByText('第 1 节｜试课')).toBeVisible()
    await expect(page.getByText('第 2 节｜正式课')).toBeVisible()
    await expect(page.getByText('应付 ¥10.00')).toBeVisible()

    page.once('dialog', async (dialog) => dialog.accept())
    const completeResponsePromise = page.waitForResponse((response) => response.url().includes(`/appointment/${lessonOne.id}/complete`) && response.request().method() === 'POST')
    await page.getByRole('button', { name: '结课' }).first().click()
    await completeResponsePromise
    await expect(page.getByText('待支付')).toBeVisible()

    const eventsAfterComplete = await expect
      .poll(async () => listCourseEvents(flow.courseId, flow.teacher.token), { timeout: 20_000 })
      .toBeTruthy()
      .then(() => listCourseEvents(flow.courseId, flow.teacher.token))
    const firstCompleted = eventsAfterComplete.find((item) => item.id === lessonOne.id)
    const secondLesson = eventsAfterComplete.find((item) => item.id === lessonTwo.id)
    expect(firstCompleted?.lessonPaymentOrderId).toBeTruthy()
    expect(secondLesson?.id).toBe(lessonTwo.id)

    const prepareBlockedResponsePromise = page.waitForResponse(
      (response) => response.url().includes(`/live/sessions/by-course/${lessonTwo.id}/prepare`) && response.request().method() === 'POST',
    )
    await page.goto(`/#/live/prepare/${lessonTwo.id}`)
    await prepareBlockedResponsePromise
    await expect(page.getByTestId('prepare-join-blocked')).toContainText('上一节课尚未支付')
    await expect(page.getByTestId('enter-classroom-button')).toBeDisabled()

    await switchAuth(page, flow.student, `/courses/${flow.courseId}`)
    await expect(page.getByText('长期课程详情')).toBeVisible()
    const prepayResponsePromise = page.waitForResponse((response) => response.url().includes('/payment/prepay') && response.request().method() === 'POST')
    await page.getByRole('button', { name: '去支付' }).first().click()
    await expect(page).toHaveURL(/#\/pay\/cashier/)
    const prepayResponse = await prepayResponsePromise
    const prepayJson = (await prepayResponse.json()) as { code: number; data: { orderNo: string } }
    expect(prepayJson.code).toBe(0)
    const paymentOrderNo = prepayJson.data.orderNo

    await expect(page.getByText('订单支付')).toBeVisible()
    // 真实链路里先走前端收银台，再用后端调试接口把订单推进到成功态，避免前端 route mock。
    const mockPayResponse = await page.request.post(`${apiBaseUrl}/payment/dev/orders/${paymentOrderNo}/mock-success`, {
      headers: {
        'X-Ops-Token': opsToken,
        Authorization: `Bearer ${flow.student.token}`,
      },
    })
    expect(mockPayResponse.ok()).toBeTruthy()

    await expect(page.getByText('支付完成')).toBeVisible({ timeout: 20_000 })
    const paidStatus = await page.request.get(`${apiBaseUrl}/payment/orders/${encodeURIComponent(paymentOrderNo)}`, {
      headers: { Authorization: `Bearer ${flow.student.token}` },
    })
    expect(paidStatus.ok()).toBeTruthy()
    const paidJson = (await paidStatus.json()) as { code: number; data: PaymentOrderStatus }
    expect(paidJson.code).toBe(0)
    expect(paidJson.data.status).toBe('SUCCESS')

    // 支付成功后再次进入直播准备页，验证“上一节未支付”拦截已经被解除。
    await switchAuth(page, flow.teacher, `/live/prepare/${lessonTwo.id}`)
    await expect(page.getByTestId('prepare-permission-state')).toBeVisible()
    await expect(page.getByTestId('prepare-join-blocked')).toHaveCount(0)
    await expect(page.getByTestId('enter-classroom-button')).toBeEnabled({ timeout: 20_000 })
  })
})
