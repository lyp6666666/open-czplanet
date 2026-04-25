import { expect, test } from '@playwright/test'

const now = Date.now()

test('my courses page shows cooperation-first layout and abnormal lesson states', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('ai_tutor_token', 'playwright-student-token')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({
        id: 2001,
        name: '学生2001',
        phone: '13800138000',
        avatar: '',
        sex: null,
        userType: 2,
        token: 'playwright-student-token',
      }),
    )
  })

  await page.route('**/user/email/reminder-hints**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { show: false }, message: 'ok' } })
  })

  await page.route('**/api/v1/public/home/config', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: {
          defaultCity: '上海',
          citySelectable: true,
          search: null,
          nav: [],
          authEntry: null,
        },
        message: 'ok',
      },
    })
  })

  await page.route('**/api/v1/public/geo/locate', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: { ip: '127.0.0.1', city: '上海', province: '上海', cityCode: '310100', suggestCities: ['上海'] },
        message: 'ok',
      },
    })
  })

  await page.route('**/api/v1/public/home/hot-words**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { updatedAt: new Date(now).toISOString(), list: [] }, message: 'ok' } })
  })

  await page.route('**/api/v1/public/subjects/tree', async (route) => {
    await route.fulfill({ json: { code: 0, data: [], message: 'ok' } })
  })

  await page.route('**/api/v1/public/home/banners**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { carousel: [], cards: [] }, message: 'ok' } })
  })

  await page.route('**/api/v1/public/home/hot-tabs**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { type: 'service', tabs: [] }, message: 'ok' } })
  })

  await page.route('**/api/v1/public/home/footer-links', async (route) => {
    await route.fulfill({ json: { code: 0, data: { links: [] }, message: 'ok' } })
  })

  await page.route('**/chat/room/page**', async (route) => {
    await route.fulfill({ json: { code: 0, data: { cursor: null, isLast: true, list: [] }, message: 'ok' } })
  })

  await page.route('**/chat/stream/v2**', async (route) => {
    await route.fulfill({ status: 204, body: '' })
  })

  await page.route('**/api/v1/public/assets/brand/logo-icon.svg', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'image/svg+xml',
      body: '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"32\" height=\"32\"></svg>',
    })
  })

  await page.route('**/user/me', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: {
          id: 2001,
          name: '学生2001',
          phone: '13800138000',
          avatar: '',
          sex: null,
          userType: 2,
          studentProfile: null,
          teacherProfile: null,
        },
        message: 'ok',
      },
    })
  })

  await page.route('**/courses/my', async (route) => {
    await route.fulfill({ path: 'index.html', contentType: 'text/html; charset=utf-8' })
  })

  await page.route('**/courses/my?**', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: [
          {
            courseId: 66,
            applicationId: 501,
            roomId: 88,
            teacherUid: 1001,
            studentUid: 2001,
            status: 'TRIALING',
            trialEndAt: '2026-04-27T10:00:00',
          },
          {
            courseId: 77,
            applicationId: 502,
            roomId: 99,
            teacherUid: 1002,
            studentUid: 2001,
            status: 'TEACHING',
            trialEndAt: '2026-04-27T10:00:00',
            aiResultStatus: 'FAILED',
          },
        ],
        message: 'ok',
      },
    })
  })

  await page.route('**/chat/application/501', async (route) => {
    await route.fulfill({ json: { code: 0, data: { id: 501, roomId: 88 }, message: 'ok' } })
  })

  await page.route('**/chat/application/502', async (route) => {
    await route.fulfill({ json: { code: 0, data: { id: 502, roomId: 99 }, message: 'ok' } })
  })

  await page.route('**/user/batch**', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: [
          { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
          { id: 1002, name: 'Amy', realName: 'Amy', avatar: '', userType: 1 },
        ],
        message: 'ok',
      },
    })
  })

  await page.route('**/user/card**', async (route) => {
    const url = route.request().url()
    const id = Number(new URL(url).searchParams.get('userId') || 1001)
    const name = id === 1002 ? 'Amy' : '王老师'
    await route.fulfill({
      json: {
        code: 0,
        data: {
          user: { id, name, realName: name, avatar: '', userType: 1 },
          teacherProfile: { subject: '数学', education: '硕士', experienceYears: 5, introduction: '擅长线上一对一' },
          studentProfile: null,
          jobPosting: null,
        },
        message: 'ok',
      },
    })
  })

  await page.route('**/api/v1/schedule/events**', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: [
          {
            id: 701,
            courseId: 66,
            lessonType: 'TRIAL',
            title: '试课',
            description: null,
            startAt: now - 4 * 60 * 60 * 1000,
            endAt: now - 2 * 60 * 60 * 1000,
            status: 'ACCEPTED',
            creatorUserId: 1001,
            participant: { id: 1001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
            chatRoomId: 88,
          },
          {
            id: 702,
            courseId: 77,
            lessonType: 'NORMAL',
            title: '正式课',
            description: null,
            startAt: now + 18 * 60 * 1000,
            endAt: now + 2 * 60 * 60 * 1000,
            status: 'ACCEPTED',
            creatorUserId: 1002,
            participant: { id: 1002, name: 'Amy', realName: 'Amy', avatar: '', userType: 1 },
            chatRoomId: 99,
          },
        ],
        message: 'ok',
      },
    })
  })

  await page.route('**/live/sessions/by-course/701', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: {
          sessionId: 9001,
          courseId: 701,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        },
        message: 'ok',
      },
    })
  })

  await page.route('**/live/sessions/by-course/702', async (route) => {
    await route.fulfill({
      json: {
        code: 0,
        data: {
          sessionId: 9002,
          courseId: 702,
          status: 'CREATED',
          actualStartAt: null,
          actualEndAt: null,
          scheduledStartAt: null,
          scheduledEndAt: null,
          joinableNow: false,
        },
        message: 'ok',
      },
    })
  })

  await page.route('**/live/sessions/9001/ai/result', async (route) => {
    await route.fulfill({ json: { code: 0, data: { sessionId: 9001, courseId: 701, resultStatus: 'PENDING', preview: null }, message: 'ok' } })
  })

  await page.route('**/live/sessions/9002/ai/result', async (route) => {
    await route.fulfill({ json: { code: 0, data: { sessionId: 9002, courseId: 702, resultStatus: 'FAILED', preview: '课后总结生成失败，可重试。' }, message: 'ok' } })
  })

  await page.route('**/live/sessions/reminders**', async (route) => {
    await route.fulfill({ json: { code: 0, data: [], message: 'ok' } })
  })

  await page.goto('/#/courses/my')

  await expect(page.getByText('我的合作')).toBeVisible()
  await expect(page.getByText('合作内全部课程')).toBeVisible()
  await expect(page.getByText('待确认未上课').first()).toBeVisible()
  await page.locator('.course-card').nth(1).click()
  await expect(page.getByText('课后生成失败', { exact: false })).toBeVisible()
  await expect(page.getByRole('button', { name: '重试生成' })).toBeVisible()
  await page.screenshot({ path: '../tmp/mockups/my-courses-page-e2e.png', fullPage: true })
})
