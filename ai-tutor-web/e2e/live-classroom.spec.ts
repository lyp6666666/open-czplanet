import { expect, test, type Browser, type BrowserContext, type ConsoleMessage, type Page, type StorageState } from '@playwright/test'

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
  try {
    const direct = await api(`/api/v1/public/dev/sms/login?phone=${phone}&role=${role}`, {
      headers: { 'X-Ops-Token': opsToken },
    })
    return direct as { id: number; token: string; userType: number; name: string; phone: string }
  } catch {
    // 兼容未部署 dev login 直登入口的环境，回退到短信验证码流程。
  }

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
type MediaPreferenceOptions = {
  cameraEnabled?: boolean
  micEnabled?: boolean
}
type MediaProbeResult = {
  supported: boolean
  ready: boolean
  audioTracks: number
  videoTracks: number
  error?: string
}

function originOf(rawUrl: string) {
  return new URL(rawUrl).origin
}

function buildAuthStorageState(user: E2EUser, mediaOptions: MediaPreferenceOptions = {}): StorageState {
  const cameraEnabled = mediaOptions.cameraEnabled ?? true
  const micEnabled = mediaOptions.micEnabled ?? true
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
              cameraEnabled,
              micEnabled,
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

async function formatConsoleMessage(message: ConsoleMessage) {
  const text = message.text()
  const args = message.args()
  if (args.length <= 0 || !text.includes('JSHandle@object')) return text
  try {
    const values = await Promise.all(
      args.map(async (arg) => {
        const value = await arg.jsonValue()
        if (typeof value === 'string') return value
        return JSON.stringify(value)
      }),
    )
    return values.filter(Boolean).join(' ') || text
  } catch {
    return text
  }
}

async function newAuthedMediaPage(
  browser: Browser,
  browserName: string,
  user: E2EUser,
  mediaOptions: MediaPreferenceOptions = {},
) {
  const context = await browser.newContext({
    baseURL: apiBaseUrl,
    permissions: browserName === 'chromium' ? ['camera', 'microphone'] : undefined,
    storageState: buildAuthStorageState(user, mediaOptions),
  })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error' || message.text().includes('[livekit-classroom]')) {
      void formatConsoleMessage(message).then((text) => {
        console.log(`[browser:${user.userType}:${user.id}] ${text}`)
      })
    }
  })
  page.on('pageerror', (error) => {
    console.log(`[pageerror:${user.userType}:${user.id}] ${error.message}`)
  })
  return { context, page }
}

async function closeAll(contexts: BrowserContext[]) {
  await Promise.allSettled(contexts.map((context) => context.close()))
}

async function probePageMedia(page: Page): Promise<MediaProbeResult> {
  return page.evaluate(async () => {
    if (!navigator.mediaDevices?.getUserMedia) {
      return {
        supported: false,
        ready: false,
        audioTracks: 0,
        videoTracks: 0,
        error: 'UNSUPPORTED',
      }
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
      const result = {
        supported: true,
        ready: stream.getAudioTracks().length > 0 && stream.getVideoTracks().length > 0,
        audioTracks: stream.getAudioTracks().length,
        videoTracks: stream.getVideoTracks().length,
      }
      stream.getTracks().forEach((track) => track.stop())
      return result
    } catch (error) {
      return {
        supported: true,
        ready: false,
        audioTracks: 0,
        videoTracks: 0,
        error: error instanceof Error ? error.name : String(error || ''),
      }
    }
  })
}

async function expectMediaReady(page: Page) {
  await expect(page.getByTestId('prepare-preview')).toBeVisible()
  await expect(page.getByTestId('prepare-camera-state')).toHaveAttribute('data-enabled', 'true')
  await expect(page.getByTestId('prepare-mic-state')).toHaveAttribute('data-enabled', 'true')
  await expect
    .poll(
      async () =>
        page.evaluate(async () => {
          try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
            const result = {
              audioTracks: stream.getAudioTracks().length,
              videoTracks: stream.getVideoTracks().length,
            }
            stream.getTracks().forEach((track) => track.stop())
            return result
          } catch (error) {
            return {
              audioTracks: 0,
              videoTracks: 0,
              error: error instanceof Error ? error.name : String(error || ''),
            }
          }
        }),
      { timeout: 20_000 },
    )
    .toMatchObject({ audioTracks: 1, videoTracks: 1 })
}

async function expectMediaReadyOrGracefulFallback(page: Page, browserName: string) {
  if (browserName !== 'webkit') {
    await expectMediaReady(page)
    return true
  }

  await expect(page.getByTestId('prepare-preview')).toBeVisible()
  await expect(page.getByTestId('prepare-camera-state')).toHaveAttribute('data-enabled', 'true')
  await expect(page.getByTestId('prepare-mic-state')).toHaveAttribute('data-enabled', 'true')

  const probe = await expect
    .poll(async () => probePageMedia(page), { timeout: 20_000 })
    .toMatchObject({ ready: expect.any(Boolean) })
    .then(async () => probePageMedia(page))

  if (probe.ready) return true

  await expect(page.getByTestId('prepare-permission-state')).toHaveAttribute('data-state', /denied|unsupported|idle/)
  await expect(page.getByTestId('enter-classroom-button')).toBeEnabled()
  console.log(`[webkit-media-probe] ${JSON.stringify(probe)}`)
  return false
}

async function expectClassroomConnected(page: Page) {
  await expect(page.getByTestId('local-stage')).toBeVisible()
  await expect(page.getByTestId('local-video')).toBeVisible()
  await expect(page.getByTestId('classroom-connection-state')).toHaveAttribute('data-state', 'connected', { timeout: 45_000 })
  await expect(page.getByText('音视频已连接')).toBeVisible()
}

async function expectRemoteMediaReceived(page: Page) {
  await expect(page.getByTestId('remote-stage')).toBeVisible()
  await expect(page.getByTestId('remote-video')).toBeVisible({ timeout: 60_000 })
  await expect(page.getByTestId('remote-video-state')).toHaveAttribute('data-connected', 'true')
  await expect(page.getByTestId('remote-audio-state')).toHaveAttribute('data-connected', 'true')
}

async function expectClassroomShellReady(page: Page) {
  await expect(page.getByTestId('local-stage')).toBeVisible({ timeout: 45_000 })
  await expect(page.getByTestId('classroom-connection-state')).toBeVisible()
}

test.describe('live classroom real media', () => {
  test('teacher and student can join same livekit room with media permissions', async ({ browser, browserName }) => {
    const data = await createLiveCourse()
    const { context: teacherContext, page: teacherPage } = await newAuthedMediaPage(browser, browserName, data.teacher)
    const { context: studentContext, page: studentPage } = await newAuthedMediaPage(browser, browserName, data.student)

    try {
      await teacherPage.goto(`/#/live/prepare/${data.courseId}`)
      await studentPage.goto(`/#/live/prepare/${data.courseId}`)

      const teacherMediaReady = await expectMediaReadyOrGracefulFallback(teacherPage, browserName)
      const studentMediaReady = await expectMediaReadyOrGracefulFallback(studentPage, browserName)

      if (!teacherMediaReady || !studentMediaReady) {
        return
      }

      await teacherPage.getByTestId('enter-classroom-button').click()
      await studentPage.getByTestId('enter-classroom-button').click()

      await expectClassroomConnected(teacherPage)
      await expectClassroomConnected(studentPage)

      await expectRemoteMediaReceived(teacherPage)
      await expectRemoteMediaReceived(studentPage)

      await teacherPage.locator('.side-tabs').getByRole('button', { name: '课中聊天' }).click()
      await studentPage.locator('.side-tabs').getByRole('button', { name: '课中聊天' }).click()
      await expect(teacherPage.getByTestId('live-chat-list')).toBeVisible()
      await expect(studentPage.getByTestId('live-chat-list')).toBeVisible()

      await teacherPage.getByTestId('classroom-toggle-mic').click()
      await studentPage.getByTestId('classroom-toggle-camera').click()

      await expect(teacherPage.getByTestId('classroom-toggle-mic')).toContainText('打开麦克风')
      await expect(studentPage.getByTestId('classroom-toggle-camera')).toContainText('打开摄像头')
    } finally {
      await closeAll([teacherContext, studentContext])
    }
  })

  test('in-class chat syncs across teacher and student', async ({ browser, browserName }) => {
    const data = await createLiveCourse()
    const chatOnlyMedia = { cameraEnabled: false, micEnabled: false }
    const { context: teacherContext, page: teacherPage } = await newAuthedMediaPage(browser, browserName, data.teacher, chatOnlyMedia)
    const { context: studentContext, page: studentPage } = await newAuthedMediaPage(browser, browserName, data.student, chatOnlyMedia)

    try {
      await teacherPage.goto(`/#/live/prepare/${data.courseId}`)
      await studentPage.goto(`/#/live/prepare/${data.courseId}`)

      await expect(teacherPage.getByTestId('prepare-preview')).toBeVisible()
      await expect(studentPage.getByTestId('prepare-preview')).toBeVisible()
      await expect(teacherPage.getByTestId('prepare-camera-state')).toHaveAttribute('data-enabled', 'false')
      await expect(studentPage.getByTestId('prepare-camera-state')).toHaveAttribute('data-enabled', 'false')
      await expect(teacherPage.getByTestId('prepare-mic-state')).toHaveAttribute('data-enabled', 'false')
      await expect(studentPage.getByTestId('prepare-mic-state')).toHaveAttribute('data-enabled', 'false')

      await teacherPage.getByTestId('enter-classroom-button').click()
      await studentPage.getByTestId('enter-classroom-button').click()

      await expectClassroomShellReady(teacherPage)
      await expectClassroomShellReady(studentPage)

      await teacherPage.locator('.side-tabs').getByRole('button', { name: '课中聊天' }).click()
      await studentPage.locator('.side-tabs').getByRole('button', { name: '课中聊天' }).click()

      const text = `老师已进入课堂-${Date.now()}`
      await teacherPage.getByTestId('live-chat-input').fill(text)
      await teacherPage.getByTestId('live-chat-send').click()

      await expect(teacherPage.getByTestId('live-chat-list')).toContainText(text, { timeout: 20_000 })
      await expect(studentPage.getByTestId('live-chat-list')).toContainText(text, { timeout: 20_000 })
    } finally {
      await closeAll([teacherContext, studentContext])
    }
  })
})
