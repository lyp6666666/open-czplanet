import { expect, test } from '@playwright/test'

function makeAiSections() {
  return Array.from({ length: 14 }).map((_, index) => ({
    id: `section-${index + 1}`,
    title: index === 0 ? '课前作业回顾' : `知识阶段 ${index + 1}`,
    summary: `第 ${index + 1} 段课堂纪要：老师围绕关键概念进行讲解，并通过例题检查学生理解。`,
    items: [
      { title: '核心概念', detail: '提炼本段中最重要的公式、定义或解题方法。' },
      { title: '课堂互动', detail: '记录学生提问、教师追问和即时反馈。' },
      { title: '下一步', detail: '提示接下来应继续练习或复盘的内容。' },
    ],
  }))
}

test.describe('live classroom whiteboard UI', () => {
  test('opens Excalidraw whiteboard with top-right video dock and scrollable compact AI panel', async ({ page }) => {
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

    await page.route('**/live/sessions/reminders', async (route) => {
      await route.fulfill({
        json: {
          code: 0,
          message: 'ok',
          data: [],
        },
      })
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
            postClassSummaryEnabled: true,
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
            activeSectionTitle: '知识阶段 8',
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
            sceneVersion: 1,
            finalized: false,
            scene: {
              elements: [],
              appState: { viewBackgroundColor: '#fffaf0' },
              files: {},
            },
          },
        },
      })
    })
    await page.route('**/live/sessions/8/whiteboard/**', async (route) => {
      await route.fulfill({
        json: {
          code: 0,
          message: 'ok',
          data: {
            whiteboardId: 99,
            sessionId: 8,
            courseId: 66,
            sceneVersion: 2,
            finalized: false,
            scene: {
              elements: [],
              appState: { viewBackgroundColor: '#fffaf0' },
              files: {},
            },
          },
        },
      })
    })

    await page.goto('/#/live/classroom/66', { waitUntil: 'domcontentloaded' })
    await expect(page.getByTestId('local-stage')).toBeVisible({ timeout: 30_000 })
    await page.getByTestId('classroom-open-whiteboard').click()

    await expect(page.getByTestId('live-whiteboard-stage')).toBeVisible()
    await expect(page.getByTestId('live-whiteboard-panel')).toBeVisible()
    await expect(page.getByTestId('live-whiteboard-react-host')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('.stage-panel')).toHaveClass(/whiteboard-active/)
    await expect(page.locator('.insight-panel')).toHaveClass(/whiteboard-compact/)

    const dockBox = await page.getByTestId('remote-stage').boundingBox()
    const stageBox = await page.locator('.stage-panel').boundingBox()
    const controlsBox = await page.locator('.meeting-controls').boundingBox()
    const insightBox = await page.locator('.insight-panel').boundingBox()
    expect(dockBox?.width).toBeGreaterThanOrEqual(280)
    expect(dockBox?.width).toBeLessThanOrEqual(360)
    expect(dockBox?.x || 0).toBeGreaterThan((stageBox?.x || 0) + (stageBox?.width || 0) * 0.55)
    expect((dockBox?.x || 0) + (dockBox?.width || 0)).toBeLessThanOrEqual((insightBox?.x || 0) - 8)
    expect(controlsBox?.y || 0).toBeGreaterThan((stageBox?.y || 0) + (stageBox?.height || 0) - 2)
    expect(Math.abs((insightBox?.y || 0) - (stageBox?.y || 0))).toBeLessThanOrEqual(2)
    expect(insightBox?.height || 0).toBeGreaterThanOrEqual(stageBox?.height || 0)

    const scrollInfo = await page.locator('.summary-timeline').evaluate((el) => ({
      clientHeight: el.clientHeight,
      scrollHeight: el.scrollHeight,
      overflowY: window.getComputedStyle(el).overflowY,
    }))
    expect(scrollInfo.overflowY).toMatch(/auto|scroll/)
    expect(scrollInfo.scrollHeight).toBeGreaterThan(scrollInfo.clientHeight)

    await page.screenshot({ path: 'test-results/live-whiteboard-ui.png', fullPage: false })

    if (!dockBox) throw new Error('Remote video dock is not rendered')
    await page.mouse.move(dockBox.x + 40, dockBox.y + 32)
    await page.mouse.down()
    await page.mouse.move(dockBox.x - 70, dockBox.y + 54, { steps: 6 })
    await page.mouse.up()
    const draggedDockBox = await page.getByTestId('remote-stage').boundingBox()
    expect(draggedDockBox?.x || 0).toBeLessThan(dockBox.x - 80)
    expect(draggedDockBox?.y || 0).toBeGreaterThan(dockBox.y + 16)

    await page.getByTestId('remote-stage').dblclick()
    const resetDockBox = await page.getByTestId('remote-stage').boundingBox()
    expect(Math.abs((resetDockBox?.x || 0) - dockBox.x)).toBeLessThan(4)
    expect(Math.abs((resetDockBox?.y || 0) - dockBox.y)).toBeLessThan(4)
  })
})
