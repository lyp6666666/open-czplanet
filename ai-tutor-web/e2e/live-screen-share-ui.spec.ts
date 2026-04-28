import { expect, test } from '@playwright/test'

async function installLiveRoutes(page: import('@playwright/test').Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem('ai_tutor_token', 'pw-token')
    window.localStorage.setItem('ai_tutor_tutor_basic_completed', '1')
    window.localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 1001, name: '王老师', phone: '18800000000', userType: 1, token: 'pw-token' }),
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
  })
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
          peerDisplayName: '李同学',
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
          peerDisplayName: '李同学',
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
          participantName: '王老师',
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
          peerDisplayName: '李同学',
        },
      },
    })
  })
}

test('shows screen share as the main stage with participant video dock', async ({ page }) => {
  await installLiveRoutes(page)

  await page.goto('/#/live/classroom/66', { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('local-stage')).toBeVisible({ timeout: 30_000 })

  await page.evaluate(() => {
    window.dispatchEvent(new CustomEvent('live-screen-share-e2e', { detail: { owner: 'remote', ownerName: '李同学' } }))
  })

  await expect(page.getByTestId('live-screen-share-stage')).toBeVisible()
  await expect(page.getByTestId('screen-share-video')).toBeVisible()
  await expect(page.locator('.stage-panel')).toHaveClass(/screen-share-active/)
  await expect(page.locator('.screen-share-topline')).toContainText('李同学 正在共享屏幕')

  const shareBox = await page.getByTestId('live-screen-share-stage').boundingBox()
  const dockBox = await page.getByTestId('remote-stage').boundingBox()
  const controlsBox = await page.locator('.meeting-controls').boundingBox()
  expect(shareBox?.width || 0).toBeGreaterThan(500)
  expect(dockBox?.width || 0).toBeGreaterThanOrEqual(280)
  expect(dockBox?.width || 0).toBeLessThanOrEqual(360)
  expect(controlsBox?.y || 0).toBeGreaterThan((shareBox?.y || 0) + (shareBox?.height || 0) - 2)

  await page.screenshot({ path: 'test-results/live-screen-share-ui.png', fullPage: false })

  await page.evaluate(() => {
    window.dispatchEvent(new CustomEvent('live-screen-share-e2e', { detail: { owner: 'stop' } }))
  })
  await expect(page.locator('.stage-panel')).not.toHaveClass(/screen-share-active/)
})
