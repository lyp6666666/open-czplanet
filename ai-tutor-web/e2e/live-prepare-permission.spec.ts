import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const opsToken = process.env.OPS_VERIFY_TOKEN || 'DevOpsVerifyTokenForE2E'

function randPhone(prefix: string) {
  const n = Math.floor(Math.random() * 1e8)
  return `${prefix}${String(n).padStart(8, '0')}`.slice(0, 11)
}

async function devLogin() {
  const response = await fetch(new URL(`/api/v1/public/dev/sms/login?phone=${randPhone('188')}&role=TEACHER`, baseURL), {
    headers: { 'X-Ops-Token': opsToken },
  })
  const json = (await response.json()) as { code: number; data: { id: number; token: string; userType: number; name: string; phone: string }; message?: string }
  if (!response.ok || json.code !== 0) {
    throw new Error(`dev login failed: ${response.status} ${JSON.stringify(json)}`)
  }
  return json.data
}

async function seedAuth(page: Page) {
  const user = await devLogin()
  await page.addInitScript((authUser) => {
    localStorage.setItem('ai_tutor_token', authUser.token)
    localStorage.setItem('ai_tutor_user', JSON.stringify(authUser))
    localStorage.setItem('ai_tutor_tutor_basic_completed', '1')
  }, user)
}

test.describe('live prepare media permission UX', () => {
  test('shows authorization guidance when browser cannot grant media permission', async ({ page }) => {
    await page.route('**/live/sessions/by-course/92051/prepare', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          data: {
            sessionId: 92051,
            status: 'JOIN_OPEN',
            courseTitle: '权限验证课堂',
            peerDisplayName: '丁晨峰',
            canJoin: true,
            joinableNow: true,
            deviceCheckRequired: true,
          },
        }),
      })
    })

    await seedAuth(page)
    await page.goto('/#/live/prepare/92051')
    await expect(page.getByTestId('prepare-check-all')).toBeVisible()

    await page.evaluate(() => {
      Object.defineProperty(window, 'isSecureContext', { value: false, configurable: true })
      Object.defineProperty(navigator, 'mediaDevices', { value: undefined, configurable: true })
    })

    await page.getByTestId('prepare-check-all').click()

    await expect(page.getByTestId('permission-modal')).toBeVisible()
    await expect(page.getByTestId('permission-modal')).toContainText('当前页面无法直接申请设备权限')
    await expect(page.getByTestId('permission-modal')).toContainText('https')

    await page.getByTestId('permission-help-action').click()
    await expect(page.getByTestId('permission-guide-modal')).toBeVisible()
    await expect(page.getByTestId('permission-guide-modal')).toContainText('Chrome / Edge')
    await expect(page.getByTestId('permission-guide-modal')).toContainText('当前页面显示不安全')
  })

  test('opens standalone permission help page', async ({ page }) => {
    await seedAuth(page)
    await page.goto('/#/live/permission-help')
    await expect(page.getByText('开启摄像头、麦克风和扬声器权限')).toBeVisible()
    await expect(page.getByText('先确认是不是安全连接')).toBeVisible()
    await expect(page.getByText('在浏览器站点设置中允许权限')).toBeVisible()
  })
})
