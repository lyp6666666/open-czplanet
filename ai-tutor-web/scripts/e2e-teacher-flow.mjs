const baseUrl = (process.env.VITE_API_BASE_URL || process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const opsToken = process.env.OPS_VERIFY_TOKEN || process.env.OPS_TOKEN || ''

function randPhone(prefix = '188') {
  const n = Math.floor(Math.random() * 1e8)
  return `${prefix}${String(n).padStart(8, '0')}`.slice(0, 11)
}

async function api(path, { method = 'GET', token, query, body } = {}) {
  const url = new URL(`${baseUrl}${path}`)
  if (query && typeof query === 'object') {
    Object.entries(query).forEach(([k, v]) => {
      if (v === undefined || v === null) return
      url.searchParams.set(k, String(v))
    })
  }
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  if (opsToken) headers['X-Ops-Token'] = opsToken

  const res = await fetch(url, {
    method,
    headers,
    body: body == null ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`Non-JSON response (${res.status}): ${text.slice(0, 200)}`)
  }
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${JSON.stringify(json)}`)
  }
  if (json && typeof json.code === 'number') {
    if (json.code !== 0) throw new Error(`API code=${json.code} msg=${json.message || ''}`)
    return json.data
  }
  return json
}

async function fetchSmsCode(phone) {
  if (!opsToken) throw new Error('Missing OPS_VERIFY_TOKEN/OPS_TOKEN for fetching SMS code')
  return api('/api/v1/public/dev/sms/code', { query: { phone } })
}

async function login(role, phone) {
  await api('/user/sendcode', { method: 'POST', body: { phone } })
  const code = await fetchSmsCode(phone)
  const user = await api('/user/loginOrRegister', {
    method: 'POST',
    body: { phone, code, userRoleEnum: role },
  })
  if (!user?.token) throw new Error('Missing token in login response')
  return user.token
}

function findFirstLeafSubjectId(nodes) {
  const stack = Array.isArray(nodes) ? [...nodes] : []
  while (stack.length) {
    const n = stack.shift()
    if (!n) continue
    const children = Array.isArray(n.children) ? n.children : []
    if (children.length === 0 && typeof n.id === 'number') return n.id
    stack.unshift(...children)
  }
  return null
}

async function main() {
  const teacherPhone = randPhone('188')
  const studentPhone = randPhone('186')

  console.log('[1/7] register/login student:', studentPhone)
  const studentToken = await login('STUDENT', studentPhone)

  console.log('[2/7] get subject tree')
  const tree = await api('/api/v1/public/subjects/tree')
  const subjectId = findFirstLeafSubjectId(tree) || 200

  console.log('[3/7] create demand')
  const demandId = await api('/api/v1/parent/jobs', {
    method: 'POST',
    token: studentToken,
    body: {
      subjectId,
      title: '初中数学一对一',
      description: '希望老师重点讲解函数与几何。',
      classMode: 'online',
      frequencyPerWeek: 2,
      stageCode: 'PRIMARY',
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
    },
  })

  console.log('[4/7] register/login teacher:', teacherPhone)
  const teacherToken = await login('TEACHER', teacherPhone)

  console.log('[5/7] complete teacher basic profile')
  await api('/user/updateUserInfo', {
    method: 'POST',
    token: teacherToken,
    body: {
      baseUserInfo: { avatar: '/avatars/avatar-1.svg' },
      teacherExtInfo: { realName: '张老师', education: '本科', defaultGreeting: '您好，我是张老师，方便聊下孩子情况吗？' },
    },
  })

  console.log('[6/7] teacher view demand detail')
  const demandView = await api(`/api/v1/parent/jobs/${demandId}/view`, { token: teacherToken })
  const targetUid = demandView?.publisher?.uid ?? demandView?.parentId ?? null
  if (!targetUid) throw new Error('Cannot find target uid in demand view response')

  console.log('[7/7] start chat + send message')
  const roomId = await api('/chat/room', {
    method: 'POST',
    token: teacherToken,
    body: { targetUid },
  })
  await api('/chat/msg', {
    method: 'POST',
    token: teacherToken,
    body: { roomId, msgType: 1, body: { content: '我可以先免费试听一节课，您看周末是否方便？' } },
  })
  const page = await api('/chat/public/msg/page', { token: teacherToken, query: { roomId, pageSize: 10 } })
  const count = Array.isArray(page?.list) ? page.list.length : 0
  console.log('ok:', { demandId, targetUid, roomId, messages: count })
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
