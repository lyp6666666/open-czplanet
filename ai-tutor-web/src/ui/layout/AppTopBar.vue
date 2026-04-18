<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'
import { useCityStore } from '@/stores/city'
import { BRAND_NAME } from '@/constants/brand'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chatRealtime = useChatRealtimeStore()
const cityStore = useCityStore()

const isLoggedIn = computed(() => auth.isLoggedIn)
const isTeacher = computed(() => auth.user?.userType === 1)
const isOrg = computed(() => auth.user?.userType === 3)
const canChat = computed(() => !isOrg.value)

function normalizedText(raw: string | null | undefined) {
  const text = String(raw || '').trim()
  return text || ''
}

const displayName = computed(() => {
  if (!auth.isLoggedIn) return '未登录'
  const teacherName = normalizedText(auth.me?.teacherProfile?.realName)
  if (teacherName) return teacherName
  const studentName = normalizedText(auth.me?.studentProfile?.realName)
  if (studentName) return studentName
  const userName = normalizedText(auth.user?.name)
  if (userName) return userName
  return '未填写姓名'
})
const unread = computed(() => chatRealtime.totalUnread)

const city = computed({
  get: () => cityStore.city,
  set: (v: string) => cityStore.setCity(v),
})

const cities = computed(() => {
  const base = [city.value, '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.filter(Boolean)))
})

const userInitial = computed(() => {
  const n = displayName.value.trim()
  return n && n !== '未填写姓名' && n !== '未登录' ? n.slice(0, 1) : 'U'
})

const menuOpen = ref(false)
const cityModalOpen = ref(false)
const switchModalOpen = ref(false)
const greetingModalOpen = ref(false)
const avatarLoadFailed = ref(false)
const greetingText = ref('')
const greetingBusy = ref(false)
const greetingError = ref<string | null>(null)

const switchLabel = computed(() => (isTeacher.value ? '切换为招聘者' : '切换为教师端'))
const switchTitle = computed(() => (isTeacher.value ? '是否将身份切换为招聘者' : '是否将身份切换为教师端'))
const switchDesc = computed(() =>
  isTeacher.value ? '点击【切换】后将退出当前教师端登录，并重新登录找家教端。' : '点击【切换】后将退出当前找家教端登录，并重新登录教师端。',
)

function go(path: string) {
  void router.push(path)
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

function onSwitchClick() {
  closeMenu()
  switchModalOpen.value = true
}

async function onGreetingClick() {
  closeMenu()
  greetingError.value = null
  if (!auth.me) {
    await auth.refreshMe()
  }
  greetingText.value = (auth.me?.teacherProfile?.defaultGreeting || '').trim()
  greetingModalOpen.value = true
}

async function saveGreeting() {
  if (greetingBusy.value) return
  greetingBusy.value = true
  greetingError.value = null
  try {
    await userApi.updateUserInfo({ teacherExtInfo: { defaultGreeting: greetingText.value.trim() } })
    await auth.refreshMe()
    greetingModalOpen.value = false
  } catch (e) {
    greetingError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    greetingBusy.value = false
  }
}

function confirmSwitch() {
  switchModalOpen.value = false
  auth.logout()
  void router.push(isTeacher.value ? '/auth/student' : '/auth/tutor')
}

function onLogout() {
  closeMenu()
  const t = auth.user?.userType
  auth.logout()
  void router.push(t === 3 ? '/auth/org' : '/')
}

watch(
  () => auth.user?.avatar,
  () => {
    avatarLoadFailed.value = false
  },
)

onMounted(() => {
  if (auth.isLoggedIn && !auth.me) {
    void auth.refreshMe()
  }
})
</script>

<template>
  <header class="bar" @click="closeMenu">
    <div class="container inner">
      <div class="left">
        <button class="logo" type="button" @click="go('/')">{{ BRAND_NAME }}</button>

        <div v-if="isLoggedIn" class="city">
          <button class="city-trigger" type="button" @click.stop="cityModalOpen = true">
            <svg class="city-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M12 2c3.9 0 7 3.1 7 7 0 5-7 13-7 13S5 14 5 9c0-3.9 3.1-7 7-7Zm0 4a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"
              />
            </svg>
            <span class="city-name">{{ city }}</span>
            <span class="city-switch">[切换]</span>
          </button>
          <CitySelectModal :open="cityModalOpen" v-model="city" :hot-cities="cities" @close="cityModalOpen = false" />
        </div>

        <nav v-if="isLoggedIn" class="tabs">
          <button class="tab" :class="{ active: route.path === '/' }" type="button" @click="go('/')">首页</button>
          <template v-if="isTeacher">
            <button class="tab" :class="{ active: route.path.startsWith('/tutor/jobs') }" type="button" @click="go('/tutor/jobs')">
              需求
            </button>
            <button
              class="tab"
              :class="{ active: route.path.startsWith('/tutor/favorites') }"
              type="button"
              @click="go('/tutor/favorites')"
            >
              收藏
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/schedule') }" type="button" @click="go('/schedule')">
              课程安排
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/courses') }" type="button" @click="go('/courses/my')">
              我的课程
            </button>
          </template>
          <template v-else-if="isOrg">
            <button class="tab" :class="{ active: route.path.startsWith('/org/tutors') }" type="button" @click="go('/org/tutors')">
              找教师
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/org/post') }" type="button" @click="go('/org/post')">
              发布需求
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/org/jobs') }" type="button" @click="go('/org/jobs/mine')">
              我的需求
            </button>
          </template>
          <template v-else>
            <button class="tab" :class="{ active: route.path.startsWith('/student/tutors') }" type="button" @click="go('/student/tutors')">
              找教师
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/student/post') }" type="button" @click="go('/student/post')">
              发布需求
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/student/jobs') }" type="button" @click="go('/student/jobs/mine')">
              我的需求
            </button>
            <button
              class="tab"
              :class="{ active: route.path.startsWith('/student/favorites') }"
              type="button"
              @click="go('/student/favorites')"
            >
              收藏
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/schedule') }" type="button" @click="go('/schedule')">
              课程安排
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/courses') }" type="button" @click="go('/courses/my')">
              我的课程
            </button>
          </template>
        </nav>
      </div>

      <div class="right">
        <template v-if="isLoggedIn">
          <button v-if="canChat" class="link link-msg" type="button" @click="go('/chat')">
            <span class="link-msg-text">消息</span>
            <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
          </button>
          <button v-if="isTeacher" class="link" type="button" @click="go('/me')">简历</button>
          <button v-else-if="isOrg" class="link" type="button" @click="go('/org/change-password')">修改密码</button>
          <button v-else class="link" type="button" @click="go('/me')">我的</button>

          <div class="user" @click.stop="toggleMenu">
            <img
              v-if="auth.user?.avatar && !avatarLoadFailed"
              class="avatar"
              :src="auth.user.avatar"
              alt="avatar"
              @error="avatarLoadFailed = true"
            />
            <div v-else class="avatar fallback">{{ userInitial }}</div>
            <div class="name">{{ displayName }}</div>
          </div>

          <div v-if="menuOpen" class="menu card" @click.stop>
            <button v-if="!isOrg" class="menu-item" type="button" @click="go('/me'); closeMenu()">{{ isTeacher ? '简历' : '我的' }}</button>
            <button v-else class="menu-item" type="button" @click="go('/org/change-password'); closeMenu()">修改密码</button>
            <button v-if="isTeacher" class="menu-item" type="button" @click="onGreetingClick">默认打招呼语</button>
            <button class="menu-item" type="button" @click="go('/settings'); closeMenu()">设置</button>
            <button v-if="!isOrg" class="menu-item" type="button" @click="onSwitchClick">{{ switchLabel }}</button>
            <button class="menu-item danger" type="button" @click="onLogout">退出登录</button>
          </div>
        </template>
        <div v-else class="guest">
          <button class="btn" type="button" @click="go('/auth/student')">找家教登录</button>
          <button class="btn btn-primary" type="button" @click="go('/auth/tutor')">当家教登录</button>
        </div>
      </div>
    </div>

    <div v-if="switchModalOpen" class="mask" @click.self="switchModalOpen = false">
      <div class="modal card">
        <div class="m-title">{{ switchTitle }}</div>
        <div class="m-desc">{{ switchDesc }}</div>
        <div class="m-ops">
          <button class="btn" type="button" @click="switchModalOpen = false">取消</button>
          <button class="btn btn-primary" type="button" @click="confirmSwitch">切换</button>
        </div>
      </div>
    </div>

    <div v-if="greetingModalOpen" class="mask" @click.self="greetingModalOpen = false">
      <div class="modal card">
        <div class="m-title">默认打招呼语</div>
        <div class="m-desc">首次发起沟通时自动发送</div>
        <div v-if="greetingError" class="m-error">{{ greetingError }}</div>
        <textarea v-model="greetingText" class="m-textarea" rows="4" placeholder="例如：您好，我是张老师，擅长数学提分，方便聊聊孩子情况吗？" />
        <div class="m-ops">
          <button class="btn" type="button" :disabled="greetingBusy" @click="greetingModalOpen = false">取消</button>
          <button class="btn btn-primary" type="button" :disabled="greetingBusy" @click="saveGreeting">
            {{ greetingBusy ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.bar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.link-msg {
  position: relative;
}

.link-msg-text {
  display: inline-flex;
  align-items: center;
  height: 20px;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 16px;
  min-width: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(255, 77, 79, 0.95);
  color: #fff;
  font-weight: 900;
  font-size: 11px;
  line-height: 16px;
  position: absolute;
  top: 2px;
  right: 2px;
  border: 2px solid rgba(255, 255, 255, 0.92);
}

.inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  gap: 16px;
}

.left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.logo {
  border: none;
  background: transparent;
  font-weight: 900;
  font-size: 16px;
  cursor: pointer;
  padding: 0;
}

.city {
  display: flex;
  align-items: center;
}

.city-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  font-weight: 900;
  color: var(--text);
}

.city-trigger:hover {
  border-color: var(--border);
  background: rgba(255, 255, 255, 0.7);
}

.city-icon {
  width: 18px;
  height: 18px;
  color: var(--primary);
}

.city-name {
  white-space: nowrap;
}

.city-switch {
  color: var(--muted);
  font-weight: 900;
  white-space: nowrap;
}

.tabs {
  display: flex;
  gap: 6px;
  align-items: center;
}

.tab {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 12px;
  font-weight: 900;
  color: var(--muted);
}

.tab.active {
  color: var(--text);
  background: rgba(0, 190, 189, 0.08);
}

.tab:hover {
  background: rgba(31, 35, 41, 0.06);
  color: var(--text);
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.name {
  font-size: 13px;
  color: var(--text);
  font-weight: 900;
  white-space: nowrap;
}

.link {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 8px 8px;
  border-radius: 12px;
  font-weight: 900;
  color: var(--muted);
}

.link:hover {
  background: rgba(31, 35, 41, 0.06);
  color: var(--text);
}

.guest {
  display: flex;
  gap: 10px;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 14px;
}

.user:hover {
  background: rgba(31, 35, 41, 0.06);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  background: #fff;
  border: 1px solid var(--border);
}

.avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  color: var(--text);
}

.menu {
  position: absolute;
  right: 0;
  top: 56px;
  width: 180px;
  padding: 8px;
  display: grid;
  gap: 4px;
  z-index: 30;
}

.menu-item {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 10px 10px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 900;
  color: var(--text);
}

.menu-item:hover {
  background: rgba(31, 35, 41, 0.06);
}

.menu-item.danger {
  color: #b42318;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 60;
}

.modal {
  width: min(520px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.m-title {
  font-weight: 900;
  font-size: 16px;
}

.m-desc {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.m-error {
  color: #d4380d;
  font-size: 13px;
}

.m-textarea {
  width: 100%;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 10px 12px;
  outline: none;
  resize: vertical;
  font-size: 13px;
  line-height: 1.6;
}

.m-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
