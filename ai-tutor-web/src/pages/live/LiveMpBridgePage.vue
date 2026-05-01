<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const error = ref<string | null>(null)

const STORAGE_TOKEN_KEY = 'ai_tutor_token'
const STORAGE_USER_KEY = 'ai_tutor_user'

function cleanText(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function parseUser(raw: string) {
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const id = Number(parsed.id || 0)
    const userType = Number(parsed.userType || 0)
    const token = cleanText(parsed.token)
    if (!(id > 0) || !(userType > 0) || !token) return null
    return {
      id,
      name: cleanText(parsed.name) || '用户',
      phone: cleanText(parsed.phone),
      avatar: cleanText(parsed.avatar) || null,
      sex: parsed.sex == null ? null : Number(parsed.sex),
      userType,
      token,
    }
  } catch {
    return null
  }
}

function persistBridgeAuth() {
  const token = cleanText(route.query.token)
  const user = parseUser(cleanText(route.query.user))
  if (!token || !user) {
    error.value = '缺少小程序传入的登录态，无法进入课堂。'
    return null
  }
  localStorage.setItem(STORAGE_TOKEN_KEY, token)
  localStorage.setItem(STORAGE_USER_KEY, JSON.stringify({ ...user, token }))
  return user
}

async function bootstrap() {
  const user = persistBridgeAuth()
  const courseId = Number(route.query.courseId || 0)
  if (!user || !(courseId > 0)) {
    if (!error.value) error.value = '缺少课程参数，无法进入课堂。'
    return
  }
  await router.replace({
    name: 'liveClassroom',
    params: { courseId: String(courseId) },
    query: {
      sessionId: cleanText(route.query.sessionId),
      serverUrl: cleanText(route.query.serverUrl),
      roomName: cleanText(route.query.roomName),
      participantIdentity: cleanText(route.query.participantIdentity),
      participantName: cleanText(route.query.participantName),
      accessToken: cleanText(route.query.accessToken),
      expireAt: cleanText(route.query.expireAt),
      source: cleanText(route.query.source) || 'mp_weixin',
    },
  })
}

onMounted(() => {
  void bootstrap()
})
</script>

<template>
  <section class="bridge-page">
    <div class="card">
      <div class="eyebrow">小程序课堂桥接</div>
      <h1>正在进入课堂</h1>
      <p v-if="!error">正在接收小程序登录态并跳转到现有课堂页。</p>
      <p v-else class="error">{{ error }}</p>
    </div>
  </section>
</template>

<style scoped>
.bridge-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(16, 185, 129, 0.16), transparent 26%),
    linear-gradient(180deg, #f4fbfa 0%, #eef5ff 100%);
}

.card {
  width: min(520px, 100%);
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.eyebrow {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.55);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 10px 0 12px;
  font-size: 30px;
  line-height: 1.15;
  color: #0f172a;
}

p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.error {
  color: #b42318;
}
</style>
