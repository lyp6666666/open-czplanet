import { chromium } from '@playwright/test'
import { execFileSync } from 'node:child_process'

const BASE_API = (process.env.E2E_API_BASE || 'http://localhost:18080').replace(/\/$/, '')
const BASE_WEB = (process.env.E2E_WEB_BASE || 'http://localhost:5173').replace(/\/$/, '')
const REMOTE_APP = process.env.E2E_REMOTE_APP || 'root@111.228.20.88'
const REMOTE_LOG = process.env.E2E_REMOTE_SMS_LOG || '/opt/ai-platform/.logs/tutor-appointment-service.log'

function randPhone(prefix = '18') {
  const n = Math.floor(Math.random() * 1e8)
  return `${prefix}${String(n).padStart(8, '0')}`.slice(0, 11)
}

function sh(cmd, args = []) {
  return execFileSync(cmd, args, { encoding: 'utf8' })
}

async function api(path, { method = 'GET', token, body, query } = {}) {
  const url = new URL(`${BASE_API}${path}`)
  if (query && typeof query === 'object') {
    for (const [k, v] of Object.entries(query)) {
      if (v == null) continue
      url.searchParams.set(k, String(v))
    }
  }
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(url, {
    method,
    headers,
    body: body == null ? undefined : JSON.stringify(body),
  })
  const json = await res.json()
  if (!res.ok || json?.code !== 0) {
    throw new Error(`API ${method} ${path} failed: ${JSON.stringify(json)}`)
  }
  return json.data
}

function lastSmsCode(phone) {
  const txt = sh('ssh', [REMOTE_APP, `tail -n 1000 ${REMOTE_LOG}`])
  const lines = txt.split('\n').reverse()
  for (const line of lines) {
    if (!line.includes(phone)) continue
    const match = line.match(/code[:=]\s*(\d{4,6})/)
    if (match) return match[1]
  }
  throw new Error(`sms code not found for ${phone}`)
}

async function login(role, phone) {
  await api('/user/sendcode', { method: 'POST', body: { phone } })
  await new Promise((resolve) => setTimeout(resolve, 700))
  const code = lastSmsCode(phone)
  return api('/user/loginOrRegister', {
    method: 'POST',
    body: { phone, code, userRoleEnum: role },
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

async function prepareFlow() {
  const student = await login('STUDENT', randPhone('186'))
  const teacher = await login('TEACHER', randPhone('188'))
  const subjectTree = await api('/api/v1/public/subjects/tree')
  const subjectId = findFirstLeafSubjectId(subjectTree)

  await api('/user/updateUserInfo', {
    method: 'POST',
    token: teacher.token,
    body: {
      baseUserInfo: { avatar: '/avatars/avatar-1.svg' },
      teacherExtInfo: { realName: 'E2E张老师', defaultGreeting: '您好，方便沟通一下学习情况吗？' },
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
      },
    },
  })

  await api('/api/v1/parent/jobs', {
    method: 'POST',
    token: student.token,
    body: {
      subjectId,
      subjectName: '数学',
      title: 'E2E 测试需求',
      description: '验证学生主动发申请后教师首次通过与支付提示链路',
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
      teachingMode: 'ONLINE',
      clientRequestId: `e2e-student-apply-${Date.now()}`,
    },
  })
  const roomId = startMsg?.message?.roomId
  const applicationId = startMsg?.message?.body?.applicationId
  if (!roomId || !applicationId) {
    throw new Error(`missing room/application id: ${JSON.stringify(startMsg)}`)
  }
  return { student, teacher, roomId, applicationId }
}

async function verifyUi(flow) {
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const page = await browser.newPage()
  page.setDefaultTimeout(15000)
  const pageErrors = []
  const responseErrors = []

  page.on('pageerror', (error) => pageErrors.push(String(error)))
  page.on('response', async (resp) => {
    if (!resp.url().includes(`/chat/application/${flow.applicationId}/decision-message`)) return
    if (resp.ok()) return
    responseErrors.push(`${resp.status()} ${await resp.text()}`)
  })

  await page.addInitScript(
    ({ token, user }) => {
      localStorage.setItem('ai_tutor_token', token)
      localStorage.setItem('ai_tutor_user', JSON.stringify(user))
    },
    {
      token: flow.teacher.token,
      user: {
        id: flow.teacher.id,
        name: flow.teacher.name || `用户${flow.teacher.id}`,
        phone: flow.teacher.phone,
        avatar: flow.teacher.avatar || '',
        sex: flow.teacher.sex,
        userType: flow.teacher.userType,
        token: flow.teacher.token,
      },
    },
  )

  await page.goto(`${BASE_WEB}/#/chat/${flow.roomId}?otherUid=${flow.student.id}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await page.getByRole('button', { name: '通过' }).waitFor({ state: 'visible' })
  await page.getByRole('button', { name: '通过' }).click()
  await page.waitForTimeout(2500)

  const currentUrl = page.url()
  const bodyText = await page.locator('body').innerText()
  await browser.close()
  return { currentUrl, bodyText, pageErrors, responseErrors }
}

async function verifyApi(flow) {
  const detail = await api(`/chat/application/${flow.applicationId}`, { token: flow.teacher.token })
  const enter = await api(`/chat/application/${flow.applicationId}/enter-chat`, { method: 'POST', token: flow.teacher.token, body: {} })
  const page = await api('/chat/public/msg/page', {
    token: flow.teacher.token,
    query: { roomId: flow.roomId, pageSize: 50 },
  })
  const list = page?.list || []
  const statusMsg = list.find((item) => item?.message?.body?.type === 'tutor_application_status' && item?.message?.body?.applicationId === flow.applicationId)
  const brokerageMsg = list.find((item) => item?.message?.body?.type === 'brokerage_required')
  return { detail, enter, statusMsg, brokerageMsg, messagesCount: list.length }
}

async function main() {
  const flow = await prepareFlow()
  const ui = await verifyUi(flow)
  const apiResult = await verifyApi(flow)
  const out = { flow, ui, api: apiResult }
  console.log(JSON.stringify(out, null, 2))

  if (ui.bodyText.includes('系统内部异常')) throw new Error('browser still shows 系统内部异常')
  if (ui.responseErrors.length > 0) throw new Error(`decision-message failed: ${ui.responseErrors.join('; ')}`)
  if (!ui.currentUrl.includes('/pay/cashier')) throw new Error(`did not navigate to cashier: ${ui.currentUrl}`)
  if (apiResult.detail.status !== 'ACCEPTED') throw new Error(`application not accepted: ${JSON.stringify(apiResult.detail)}`)
  if (!apiResult.enter.paymentRequired || !apiResult.enter.orderId) throw new Error(`enterChat not payment gated: ${JSON.stringify(apiResult.enter)}`)
  if (!apiResult.statusMsg) throw new Error('missing tutor_application_status message')
  if (!apiResult.brokerageMsg) throw new Error('missing brokerage_required message')
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
