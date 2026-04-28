import { expect, test } from '@playwright/test'

function makeAiSections() {
  return Array.from({ length: 8 }).map((_, index) => ({
    id: `section-${index + 1}`,
    title: `知识阶段 ${index + 1}`,
    summary: `第 ${index + 1} 段课堂纪要：老师围绕关键概念进行讲解，并通过例题检查学生理解。`,
    items: [
      { title: '核心概念', detail: '提炼本段中最重要的公式、定义或解题方法，并记录学生在这部分的理解情况与追问。' },
      { title: '课堂互动', detail: '记录学生提问、教师追问和即时反馈，确保后续复盘时能看出课堂推进节奏。' },
    ],
  }))
}

test('resizes the realtime summary panel and reflows text while dragging the left edge', async ({ page }) => {
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
          realtimeSummaryEnabled: true,
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
          realtimeSummaryEnabled: true,
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
          aiStatus: 'ACTIVE',
          realtimeEnabled: true,
          summaryStatus: 'GENERATING',
          asrEnabled: true,
          llmEnabled: true,
          segmentCount: 40,
          lastLlmSegmentCount: 36,
          currentTopic: '函数图像',
          latestStageSummary: '正在整理函数图像的性质。',
          studentQuestions: [],
          homeworkCandidates: [],
          keyPoints: [],
          minutesOutline: makeAiSections(),
          activeSectionTitle: '知识阶段 5',
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
        },
      },
    })
  })

  await page.goto('/#/live/classroom/66', { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.insight-panel')).toBeVisible({ timeout: 30_000 })

  const insightPanel = page.locator('.insight-panel')
  const firstSummary = page.locator('.minute-summary').first()
  const beforePanel = await insightPanel.boundingBox()
  const beforeSummary = await firstSummary.boundingBox()

  await page.getByTestId('insight-panel-resizer').hover()
  await page.mouse.down()
  await page.mouse.move((beforePanel?.x || 0) + 110, (beforePanel?.y || 0) + 30, { steps: 8 })
  await page.mouse.up()

  const afterPanel = await insightPanel.boundingBox()
  const afterSummary = await firstSummary.boundingBox()

  expect(afterPanel?.width || 0).toBeLessThan((beforePanel?.width || 0) - 60)
  expect(afterSummary?.height || 0).toBeGreaterThan((beforeSummary?.height || 0) + 8)
})
