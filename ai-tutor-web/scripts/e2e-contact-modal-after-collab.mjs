import { chromium } from '@playwright/test'
import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'

const BASE_API = (process.env.E2E_API_BASE || 'http://localhost:18080').replace(/\/$/, '')
const BASE_WEB = (process.env.E2E_WEB_BASE || 'http://localhost:5173').replace(/\/$/, '')
const OPS_TOKEN = process.env.OPS_VERIFY_TOKEN || 'DevOpsVerifyTokenForE2E'
const REMOTE_APP = process.env.E2E_REMOTE_APP || 'root@111.228.20.88'
const NACOS_TENANT = process.env.E2E_NACOS_TENANT || '481e4376-4576-4b18-ac19-f61e170ca3ae'
const NACOS_DATA_ID = process.env.E2E_NACOS_PAYMENT_DATA_ID || 'ai-tutor-payment-dev.yaml'
const NACOS_GROUP = process.env.E2E_NACOS_GROUP || 'DEFAULT_GROUP'

function randPhone(prefix = '18') {
  const n = Math.floor(Math.random() * 1e8)
  return `${prefix}${String(n).padStart(8, '0')}`.slice(0, 11)
}

function sh(cmd, args = []) {
  return execFileSync(cmd, args, { encoding: 'utf8' })
}

function md5Upper(text) {
  return createHash('md5').update(text).digest('hex').toUpperCase()
}

function createYungouosSign(params, appKey) {
  const entries = Object.entries(params)
    .filter(([key, value]) => key && key.toLowerCase() !== 'sign' && value != null && String(value).trim() !== '')
    .sort(([a], [b]) => a.localeCompare(b))
  const payload = entries.map(([key, value]) => `${key}=${String(value)}`).join('&')
  return md5Upper(`${payload}&key=${appKey}`)
}

async function api(path, { method = 'GET', token, body, query, headers } = {}) {
  const url = new URL(`${BASE_API}${path}`)
  if (query && typeof query === 'object') {
    for (const [k, v] of Object.entries(query)) {
      if (v == null) continue
      url.searchParams.set(k, String(v))
    }
  }
  const requestHeaders = {
    'Content-Type': 'application/json',
    ...(OPS_TOKEN ? { 'X-Ops-Token': OPS_TOKEN } : {}),
    ...(headers || {}),
  }
  if (token) requestHeaders.Authorization = `Bearer ${token}`
  const res = await fetch(url, {
    method,
    headers: requestHeaders,
    body: body == null ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`Non-JSON response for ${method} ${path}: ${text.slice(0, 300)}`)
  }
  if (!res.ok || json?.code !== 0) {
    throw new Error(`API ${method} ${path} failed: ${JSON.stringify(json)}`)
  }
  return json.data
}

async function postForm(path, form) {
  const body = new URLSearchParams()
  for (const [k, v] of Object.entries(form)) {
    if (v == null) continue
    body.set(k, String(v))
  }
  const res = await fetch(`${BASE_API}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body,
  })
  const text = await res.text()
  if (!res.ok) {
    throw new Error(`FORM POST ${path} failed: ${res.status} ${text}`)
  }
  return text
}

async function devLogin(role, phone) {
  return api('/api/v1/public/dev/sms/login', {
    query: { phone, role },
  })
}

function findFirstLeafSubjectId(nodes) {
  const stack = Array.isArray(nodes) ? [...nodes] : []
  while (stack.length) {
    const node = stack.shift()
    if (!node) continue
    const children = Array.isArray(node.children) ? node.children : []
    if (children.length === 0 && typeof node.id === 'number') return node.id
    stack.unshift(...children)
  }
  return 200
}

function parseYamlLikeValue(raw, key) {
  const escaped = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = raw.match(new RegExp(`(?:^|\\n)\\s*${escaped}:\\s*"?([^"\\n]+)"?`, 'i'))
  return match?.[1]?.trim() || ''
}

function loadRemotePaymentConfig() {
  const nacosCmd = `curl -s 'http://127.0.0.1:8848/nacos/v1/cs/configs?tenant=${NACOS_TENANT}&dataId=${NACOS_DATA_ID}&group=${NACOS_GROUP}'`
  const yaml = sh('ssh', [REMOTE_APP, nacosCmd])
  const appKey = process.env.E2E_YUNGOUOS_APP_KEY || parseYamlLikeValue(yaml, 'appKey')
  const wechatMchId = process.env.E2E_YUNGOUOS_WECHAT_MCH_ID || parseYamlLikeValue(yaml, 'wechat-mch-id') || parseYamlLikeValue(yaml, 'wechatMchId')
  if (!appKey) throw new Error('Cannot resolve YunGouOS appKey')
  if (!wechatMchId) throw new Error('Cannot resolve YunGouOS wechat mch id')
  return { appKey, wechatMchId }
}

async function injectAuth(page, user) {
  await page.addInitScript(
    ({ token, authUser }) => {
      localStorage.setItem('ai_tutor_token', token)
      localStorage.setItem('ai_tutor_user', JSON.stringify(authUser))
    },
    {
      token: user.token,
      authUser: {
        id: user.id,
        name: user.name || `用户${user.id}`,
        phone: user.phone,
        avatar: user.avatar || '',
        sex: user.sex,
        userType: user.userType,
        token: user.token,
      },
    },
  )
}

async function closeContactModalIfOpen(page) {
  const title = page.getByText('联系方式', { exact: true })
  try {
    await title.waitFor({ state: 'visible', timeout: 1500 })
  } catch {
    return false
  }
  const close = page.locator('.modal .icon-btn').first()
  if (await close.isVisible().catch(() => false)) {
    await close.click()
  }
  await title.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
  return true
}

async function waitForChatReady(page) {
  await page.getByRole('button', { name: '发起合作' }).waitFor({ state: 'visible', timeout: 20000 })
}

async function createCollaborationFromStudent(page) {
  await page.getByRole('button', { name: '发起合作' }).click()
  await page.getByPlaceholder('例如：200 元/小时').fill('220 元/小时')
  await page.getByPlaceholder('例如：周一、周三 19:00-21:00').fill('每周二、周四 19:30-21:00')
  await page.locator('input[type="number"]').fill('2')
  await page.getByRole('button', { name: '发送提案' }).click()
  await page.getByText('等待对方确认').waitFor({ state: 'visible', timeout: 10000 })
}

async function acceptCollaborationFromTeacher(page) {
  await page.getByRole('button', { name: '同意' }).waitFor({ state: 'visible', timeout: 20000 })
  await page.getByRole('button', { name: '同意' }).click()
  await page.getByText('合作提案：已同意').waitFor({ state: 'visible', timeout: 10000 })
}

async function waitForPostAcceptPaymentEntry(page, roomId) {
  const deadline = Date.now() + 20000
  while (Date.now() < deadline) {
    const hash = new URL(page.url()).hash || ''
    if (/#\/pay\/cashier/.test(hash)) return 'cashier'
    const payButtonVisible = await page.getByRole('button', { name: '去支付' }).isVisible().catch(() => false)
    if (payButtonVisible) return 'chat'
    const paymentCardVisible = await page.getByText('待教师支付信息费').isVisible().catch(() => false)
    if (paymentCardVisible) return 'chat'
    await page.waitForTimeout(500)
  }
  throw new Error(`payment entry not ready after accept, url=${page.url()}`)
}

async function waitForPaymentUnlocked(page, roomId) {
  const deadline = Date.now() + 30000
  while (Date.now() < deadline) {
    const hash = new URL(page.url()).hash || ''
    const onChatPage = new RegExp(`#\\/chat\\/${roomId}(?:\\?|$)`).test(hash)
    const unlockedVisible = await page.getByRole('button', { name: '查看联系方式' }).isVisible().catch(() => false)
    if (unlockedVisible && onChatPage) return
    const paidHintVisible = await page.getByText('支付成功').isVisible().catch(() => false)
    if (paidHintVisible && onChatPage) return
    if (!onChatPage && /#\/pay\/cashier/.test(hash)) {
      await page.goto(`${BASE_WEB}/#/chat/${roomId}`, { waitUntil: 'domcontentloaded' })
    }
    await page.waitForTimeout(800)
  }
  throw new Error(`payment unlock not reflected in chat within timeout, url=${page.url()}`)
}

async function triggerPaymentSuccess(orderNo, amountFen, mchId, appKey) {
  const form = {
    out_trade_no: orderNo,
    total_fee: (amountFen / 100).toFixed(2),
    pay_no: `E2E_PAY_${orderNo}`,
    order_no: `E2E_ORDER_${orderNo}`,
    mch_id: mchId,
  }
  form.sign = createYungouosSign(form, appKey)
  const resp = await postForm('/payment/notify/yungouos', form)
  if (!resp.trim().toUpperCase().includes('SUCCESS')) {
    throw new Error(`payment notify failed: ${resp}`)
  }
  return resp.trim()
}

async function prepareFlow() {
  const student = await devLogin('STUDENT', randPhone('186'))
  const teacher = await devLogin('TEACHER', randPhone('188'))
  const subjectTree = await api('/api/v1/public/subjects/tree')
  const subjectId = findFirstLeafSubjectId(subjectTree)

  await api('/user/updateUserInfo', {
    method: 'POST',
    token: teacher.token,
    body: {
      baseUserInfo: { avatar: '/avatars/avatar-1.svg' },
      teacherExtInfo: { realName: 'E2E王老师', defaultGreeting: '您好，方便确认一下长期补课安排吗？' },
    },
  })
  await api('/user/updateUserInfo', {
    method: 'POST',
    token: teacher.token,
    body: {
      teacherExtInfo: {
        education: '本科',
        city: '上海',
        highestEduSchool: '复旦大学',
        introduction: 'E2E 自动化测试老师',
        subject: '数学',
        teachingMode: 'ONLINE',
      },
    },
  })

  const demandId = await api('/api/v1/parent/jobs', {
    method: 'POST',
    token: student.token,
    body: {
      subjectId,
      subjectName: '数学',
      title: 'E2E 联系方式验证需求',
      description: '验证支付解锁后再次产生申请仍可查看联系方式',
      studentGender: 'male',
      teacherGenderPreference: 'both',
      teacherRequirementDetail: '希望老师耐心、善于鼓励',
      classMode: 'online',
      frequencyPerWeek: 2,
      budgetMin: 120,
      budgetMax: 180,
      stageCode: 'PRIMARY',
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
    },
  })

  const startMsg = await api('/chat/application/start-chat', {
    method: 'POST',
    token: student.token,
    body: {
      receiverUid: teacher.id,
      contextType: 'TUTOR',
      contextId: teacher.id,
      content: '您好，我想申请长期补课，方便沟通吗？',
      clientRequestId: `e2e-contact-modal-${Date.now()}`,
    },
  })
  const roomId = startMsg?.message?.roomId
  const applicationId = startMsg?.message?.body?.applicationId
  if (!roomId || !applicationId) {
    throw new Error(`missing room/application id: ${JSON.stringify(startMsg)}`)
  }
  return { student, teacher, demandId, roomId, applicationId }
}

async function main() {
  const paymentConfig = loadRemotePaymentConfig()
  const flow = await prepareFlow()
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const teacherContext = await browser.newContext()
  const studentContext = await browser.newContext()
  const teacherPage = await teacherContext.newPage()
  const studentPage = await studentContext.newPage()

  await injectAuth(teacherPage, flow.teacher)
  await injectAuth(studentPage, flow.student)

  const result = {
    flow: {
      roomId: flow.roomId,
      applicationId: flow.applicationId,
      teacherUid: flow.teacher.id,
      studentUid: flow.student.id,
      teacherPhone: flow.teacher.phone,
      studentPhone: flow.student.phone,
      demandId: flow.demandId,
    },
    payment: {},
    validation: {},
  }

  try {
    await teacherPage.goto(`${BASE_WEB}/#/chat/${flow.roomId}?otherUid=${flow.student.id}`, { waitUntil: 'domcontentloaded' })
    await teacherPage.getByRole('button', { name: '通过' }).waitFor({ state: 'visible', timeout: 15000 })
    await teacherPage.getByRole('button', { name: '通过' }).click()
    result.payment.entryMode = await waitForPostAcceptPaymentEntry(teacherPage, flow.roomId)

    const applicationDetail = await api(`/chat/application/${flow.applicationId}`, {
      token: flow.teacher.token,
    })
    const orderId = applicationDetail?.orderId
    if (!orderId) {
      throw new Error(`missing order id after accept: ${JSON.stringify(applicationDetail)}`)
    }
    const prepay = await api('/payment/prepay', {
      method: 'POST',
      token: flow.teacher.token,
      body: {
        contextType: 'BROKERAGE_ORDER',
        contextId: orderId,
        channel: 'WECHAT',
      },
    })
    result.payment = {
      orderId,
      orderNo: prepay.orderNo,
      amountFen: prepay.amountFen,
    }

    result.payment.notifyResp = await triggerPaymentSuccess(
      prepay.orderNo,
      prepay.amountFen,
      paymentConfig.wechatMchId,
      paymentConfig.appKey,
    )

    await waitForPaymentUnlocked(teacherPage, flow.roomId)
    await closeContactModalIfOpen(teacherPage)
    await waitForChatReady(teacherPage)

    const secondApplication = await api('/chat/application/start-chat', {
      method: 'POST',
      token: flow.teacher.token,
      body: {
        receiverUid: flow.student.id,
        contextType: 'DEMAND',
        contextId: flow.demandId,
        content: '我再补充一条需求确认消息，用于验证历史已解锁联系方式仍可查看。',
        clientRequestId: `e2e-contact-modal-second-${Date.now()}`,
      },
    })
    result.validation.secondApplicationId = secondApplication?.message?.body?.applicationId || null

    await studentPage.goto(`${BASE_WEB}/#/chat/${flow.roomId}?otherUid=${flow.teacher.id}`, { waitUntil: 'domcontentloaded' })
    await closeContactModalIfOpen(studentPage)
    await waitForChatReady(studentPage)
    await createCollaborationFromStudent(studentPage)

    const teacherReviewPage = await teacherContext.newPage()
    await injectAuth(teacherReviewPage, flow.teacher)
    await teacherReviewPage.goto(`${BASE_WEB}/#/chat/${flow.roomId}?otherUid=${flow.student.id}`, { waitUntil: 'domcontentloaded' })
    await closeContactModalIfOpen(teacherReviewPage)
    await acceptCollaborationFromTeacher(teacherReviewPage)
    await teacherReviewPage.getByRole('button', { name: '查看联系方式' }).click()
    await teacherReviewPage.getByText('联系方式', { exact: true }).waitFor({ state: 'visible', timeout: 10000 })
    await teacherReviewPage.getByText(flow.student.phone, { exact: true }).waitFor({ state: 'visible', timeout: 10000 })

    const modalText = await teacherReviewPage.locator('.modal').innerText()
    result.validation.contactModalText = modalText
    result.validation.contactPhoneVisible = modalText.includes(flow.student.phone)
    result.validation.contactModalHasError = modalText.includes('请求数据不存在')

    if (!result.validation.contactPhoneVisible) {
      throw new Error(`contact phone not visible: ${modalText}`)
    }
    if (result.validation.contactModalHasError) {
      throw new Error(`contact modal still shows error: ${modalText}`)
    }

    const contactApiResp = await api('/chat/contact/unlock', {
      token: flow.teacher.token,
      query: { roomId: flow.roomId, targetUid: flow.student.id },
    })
    result.validation.contactApiPhone = contactApiResp?.phone || ''
    if (contactApiResp?.phone !== flow.student.phone) {
      throw new Error(`contact api phone mismatch: ${JSON.stringify(contactApiResp)}`)
    }

    console.log(JSON.stringify(result, null, 2))
  } finally {
    await studentContext.close().catch(() => {})
    await teacherContext.close().catch(() => {})
    await browser.close().catch(() => {})
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
