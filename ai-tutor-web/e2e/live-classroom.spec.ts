import { expect, test, type Browser, type BrowserContext, type Page, type StorageState } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const opsToken = process.env.OPS_VERIFY_TOKEN || 'DevOpsVerifyTokenForE2E'

function randPhone(prefix: string) {
  const n = Math.floor(Math.random() * 1e8)
  return `${prefix}${String(n).padStart(8, '0')}`.slice(0, 11)
}

async function api(path: string, options: { method?: string; token?: string; body?: unknown; headers?: Record<string, string> } = {}) {
  const response = await fetch(new URL(path, apiBaseUrl), {
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...(options.headers || {}),
    },
    body: options.body == null ? undefined : JSON.stringify(options.body),
  })
  const json = (await response.json()) as { code: number; data: any; message: string }
  if (!response.ok || json.code !== 0) {
    throw new Error(`API failed: ${path} ${response.status} ${JSON.stringify(json)}`)
  }
  return json.data
}

async function login(role: 'TEACHER' | 'STUDENT', phone: string) {
  await api('/user/sendcode', { method: 'POST', body: { phone } })
  const code = await api(`/api/v1/public/dev/sms/code?phone=${phone}`, {
    headers: { 'X-Ops-Token': opsToken },
  })
  const user = await api('/user/loginOrRegister', {
    method: 'POST',
    body: { phone, code, userRoleEnum: role },
  })
  return user as { id: number; token: string; userType: number; name: string; phone: string }
}

async function createLiveCourse() {
  const teacherPhone = randPhone('188')
  const studentPhone = randPhone('186')
  const teacher = await login('TEACHER', teacherPhone)
  const student = await login('STUDENT', studentPhone)

  const now = Date.now()
  const startAt = now + 3 * 60_000
  const endAt = startAt + 60 * 60_000

  const event = await api('/api/v1/schedule/events', {
    method: 'POST',
    token: teacher.token,
    body: {
      title: 'Playwright 实时课堂',
      participantUserId: student.id,
      startAt,
      endAt,
      description: 'Playwright 浏览器音视频联调',
    },
  })

  await api(`/api/v1/schedule/events/${event.id}/response`, {
    method: 'POST',
    token: student.token,
    body: { action: 'ACCEPT' },
  })

  return {
    courseId: event.id as number,
    teacher,
    student,
  }
}

type E2EUser = { token: string; userType: number; id: number; phone: string; name: string }

function originOf(rawUrl: string) {
  return new URL(rawUrl).origin
}

function buildAuthStorageState(user: E2EUser): StorageState {
  return {
    cookies: [],
    origins: [
      {
        origin: originOf(apiBaseUrl),
        localStorage: [
          { name: 'ai_tutor_token', value: user.token },
          {
            name: 'ai_tutor_user',
            value: JSON.stringify({
              id: user.id,
              name: user.name,
              phone: user.phone,
              userType: user.userType,
              token: user.token,
            }),
          },
          {
            name: 'ai_tutor_live_media_preferences',
            value: JSON.stringify({
              cameraEnabled: true,
              micEnabled: true,
              speakerChecked: true,
              cameraDeviceId: null,
              micDeviceId: null,
              speakerDeviceId: null,
            }),
          },
        ],
      },
    ],
  }
}

async function newAuthedMediaPage(browser: Browser, browserName: string, user: E2EUser) {
  const context = await browser.newContext({
    baseURL: apiBaseUrl,
    permissions: browserName === 'chromium' ? ['camera', 'microphone'] : undefined,
    storageState: buildAuthStorageState(user),
  })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') {
      console.log(`[browser:${user.userType}:${user.id}] ${message.text()}`)
    }
  })
  return { context, page }
}

async function closeAll(contexts: BrowserContext[]) {
  await Promise.allSettled(contexts.map((context) => context.close()))
}

async function expectMediaReady(page: Page) {
  await expect(page.getByTestId('prepare-preview')).toBeVisible()
  await expect(page.getByTestId('prepare-camera-state')).toHaveAttribute('data-enabled', 'true')
  await expect(page.getByTestId('prepare-mic-state')).toHaveAttribute('data-enabled', 'true')
  await Promise.race([
    expect(page.getByTestId('prepare-permission-state')).toHaveAttribute('data-state', 'granted', { timeout: 20_000 }),
    expect(page.getByTestId('prepare-video')).toBeVisible({ timeout: 20_000 }),
  ])
}

async function expectClassroomConnected(page: Page) {
  await expect(page.getByTestId('local-stage')).toBeVisible()
  await expect(page.getByTestId('local-video')).toBeVisible()
  await expect(page.getByTestId('classroom-connection-state')).toHaveAttribute('data-state', 'connected', { timeout: 30_000 })
  await expect(page.getByText('音视频已连接')).toBeVisible()
}

async function expectRemoteMediaReceived(page: Page) {
  await expect(page.getByTestId('remote-stage')).toBeVisible()
  await expect(page.getByTestId('remote-video')).toBeVisible({ timeout: 45_000 })
  await expect(page.getByTestId('remote-video-state')).toHaveAttribute('data-connected', 'true')
  await expect(page.getByTestId('remote-audio-state')).toHaveAttribute('data-connected', 'true')
}

test.describe('live classroom real media', () => {
  test('teacher and student can join same livekit room with media permissions', async ({ browser, browserName }) => {
    test.skip(browserName === 'webkit', 'WebKit 在 CI/dev 远程环境下 getUserMedia 与 LiveKit WebRTC 链路不稳定，保留项目配置但不纳入本轮自动化门禁')

    const data = await createLiveCourse()
    const { context: teacherContext, page: teacherPage } = await newAuthedMediaPage(browser, browserName, data.teacher)
    const { context: studentContext, page: studentPage } = await newAuthedMediaPage(browser, browserName, data.student)

    try {
      await teacherPage.goto(`/#/live/prepare/${data.courseId}`)
      await studentPage.goto(`/#/live/prepare/${data.courseId}`)

      await expectMediaReady(teacherPage)
      await expectMediaReady(studentPage)

      await teacherPage.getByTestId('enter-classroom-button').click()
      await studentPage.getByTestId('enter-classroom-button').click()

      await expectClassroomConnected(teacherPage)
      await expectClassroomConnected(studentPage)

      await expectRemoteMediaReceived(teacherPage)
      await expectRemoteMediaReceived(studentPage)

      await teacherPage.getByTestId('classroom-toggle-mic').click()
      await studentPage.getByTestId('classroom-toggle-camera').click()

      await expect(teacherPage.getByTestId('classroom-toggle-mic')).toContainText('打开麦克风')
      await expect(studentPage.getByTestId('classroom-toggle-camera')).toContainText('打开摄像头')
    } finally {
      await closeAll([teacherContext, studentContext])
    }
  })
})
