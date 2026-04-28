import { expect, test, type BrowserContext, type Page } from '@playwright/test'

type SharedScene = {
  sceneVersion: number
  scene: {
    elements: unknown[]
    appState: Record<string, unknown>
    files: Record<string, unknown>
  }
}

function installAuth(page: Page, user: { id: number; name: string; userType: number }) {
  return page.addInitScript((currentUser) => {
    window.localStorage.setItem('ai_tutor_token', `pw-token-${currentUser.id}`)
    window.localStorage.setItem('ai_tutor_tutor_basic_completed', '1')
    window.localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({
        id: currentUser.id,
        name: currentUser.name,
        phone: currentUser.id === 1001 ? '18800000000' : '18600000000',
        userType: currentUser.userType,
        token: `pw-token-${currentUser.id}`,
      }),
    )
    window.localStorage.setItem(
      'ai_tutor_live_media_preferences',
      JSON.stringify({
        cameraEnabled: false,
        micEnabled: false,
        speakerChecked: true,
        cameraDeviceId: null,
        micDeviceId: null,
        speakerDeviceId: null,
      }),
    )
  }, user)
}

async function installRoutes(page: Page, shared: SharedScene, peerName: string) {
  await page.route('**/api/v1/public/assets/brand/logo-icon.svg', async (route) => {
    await route.fulfill({ status: 404, body: '' })
  })
  await page.route('**/user/me', async (route) => {
    await route.fulfill({ json: { code: 0, message: 'ok', data: null } })
  })
  await page.route('**/chat/**', async (route) => {
    await route.fulfill({ json: { code: 0, message: 'ok', data: { list: [], cursor: null, isLast: true } } })
  })
  await page.route('**/live/sessions/reminders', async (route) => {
    await route.fulfill({ json: { code: 0, message: 'ok', data: [] } })
  })
  await page.route('**/live/sessions/by-course/66', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          sessionId: 8,
          courseId: 66,
          status: 'IN_PROGRESS',
          providerRoomName: 'class-66',
          provider: 'LIVEKIT',
          teacherUid: 1001,
          studentUid: 1002,
          peerJoined: true,
          roomId: 7001,
          peerDisplayName: peerName,
          subjectLabel: '数学',
          courseKindLabel: '试课',
          realtimeSummaryEnabled: false,
        },
      },
    })
  })
  await page.route('**/live/sessions/8/status', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          sessionId: 8,
          courseId: 66,
          status: 'IN_PROGRESS',
          providerRoomName: 'class-66',
          provider: 'LIVEKIT',
          teacherUid: 1001,
          studentUid: 1002,
          peerJoined: true,
          roomId: 7001,
          peerDisplayName: peerName,
          subjectLabel: '数学',
          courseKindLabel: '试课',
          realtimeSummaryEnabled: false,
        },
      },
    })
  })
  await page.route('**/live/sessions/8/ai/state', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          sessionId: 8,
          courseId: 66,
          aiStatus: 'OFF',
          realtimeEnabled: false,
          summaryStatus: 'OFF',
          studentQuestions: [],
          homeworkCandidates: [],
          keyPoints: [],
          rawState: {},
        },
      },
    })
  })
  await page.route('**/live/sessions/8/join-token', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          provider: 'LIVEKIT',
          serverUrl: 'ws://127.0.0.1:9',
          roomName: 'class-66',
          participantName: 'E2E',
          participantIdentity: '1001',
          accessToken: 'mock-token',
          expireAt: '2026-04-27T12:00:00',
        },
      },
    })
  })
  await page.route('**/live/sessions/8/join-ack', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          sessionId: 8,
          courseId: 66,
          status: 'IN_PROGRESS',
          providerRoomName: 'class-66',
          provider: 'LIVEKIT',
          teacherUid: 1001,
          studentUid: 1002,
          peerJoined: true,
          roomId: 7001,
          peerDisplayName: peerName,
        },
      },
    })
  })
  await page.route('**/live/sessions/8/whiteboard', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          whiteboardId: 99,
          sessionId: 8,
          courseId: 66,
          scheduleEventId: 77,
          sceneVersion: shared.sceneVersion,
          finalized: false,
          scene: shared.scene,
        },
      },
    })
  })
  await page.route('**/live/sessions/8/whiteboard/snapshot', async (route) => {
    const body = await route.request().postDataJSON()
    shared.sceneVersion += 1
    shared.scene = body.scene
    await route.fulfill({
      json: {
        code: 0,
        message: 'ok',
        data: {
          whiteboardId: 99,
          sessionId: 8,
          courseId: 66,
          sceneVersion: shared.sceneVersion,
          finalized: false,
          scene: shared.scene,
        },
      },
    })
  })
}

async function openWhiteboard(context: BrowserContext, page: Page, shared: SharedScene, user: { id: number; name: string; userType: number }, peerName: string) {
  await installAuth(page, user)
  await installRoutes(page, shared, peerName)
  await page.goto('/#/live/classroom/66', { waitUntil: 'domcontentloaded' })
  await page.getByTestId('classroom-open-whiteboard').click()
  await expect(page.getByTestId('live-whiteboard-react-host')).toBeVisible({ timeout: 30_000 })
  return context
}

test('syncs a peer whiteboard snapshot between two browser clients in the same classroom', async ({ browser }) => {
  const shared: SharedScene = {
    sceneVersion: 1,
    scene: {
      elements: [],
      appState: { viewBackgroundColor: '#fffaf0' },
      files: {},
    },
  }

  const teacherContext = await browser.newContext()
  const studentContext = await browser.newContext()
  const teacherPage = await teacherContext.newPage()
  const studentPage = await studentContext.newPage()

  await openWhiteboard(teacherContext, teacherPage, shared, { id: 1001, name: '王老师', userType: 1 }, '李同学')
  await openWhiteboard(studentContext, studentPage, shared, { id: 1002, name: '李同学', userType: 0 }, '王老师')

  await teacherPage.evaluate(async () => {
    const element = {
      id: 'teacher-line-e2e',
      type: 'freedraw',
      x: 120,
      y: 120,
      width: 180,
      height: 80,
      angle: 0,
      strokeColor: '#2d62f2',
      backgroundColor: 'transparent',
      fillStyle: 'hachure',
      strokeWidth: 2,
      strokeStyle: 'solid',
      roughness: 1,
      opacity: 100,
      groupIds: [],
      frameId: null,
      roundness: null,
      seed: 20260427,
      version: 1,
      versionNonce: 20260427,
      isDeleted: false,
      boundElements: null,
      updated: Date.now(),
      link: null,
      locked: false,
      points: [[0, 0], [60, 20], [120, -10], [180, 80]],
      pressures: [],
      simulatePressure: true,
      lastCommittedPoint: null,
    }
    const response = await fetch('/live/sessions/8/whiteboard/snapshot', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sceneVersion: 2,
        scene: {
          elements: [element],
          appState: { viewBackgroundColor: '#fffaf0' },
          files: {},
        },
      }),
    })
    if (!response.ok) throw new Error(`snapshot save failed: ${response.status}`)
  })

  await expect(studentPage.locator('.whiteboard-sync-chip')).toContainText('已同步对方白板', { timeout: 8_000 })
  await expect.poll(() => studentPage.locator('canvas').count(), { timeout: 8_000 }).toBeGreaterThan(0)
  await expect.poll(() => Promise.resolve(shared.scene.elements.length), { timeout: 8_000 }).toBe(1)

  await teacherContext.close()
  await studentContext.close()
})
