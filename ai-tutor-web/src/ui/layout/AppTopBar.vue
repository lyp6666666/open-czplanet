<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isLoggedIn = computed(() => auth.isLoggedIn)
const isTeacher = computed(() => auth.user?.userType === 1)
const displayName = computed(() => auth.user?.name || '未登录')

const city = ref(localStorage.getItem('ai_tutor_city') || '北京')
watch(city, (v) => localStorage.setItem('ai_tutor_city', v))

const cities = computed(() => {
  const base = [city.value, '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.filter(Boolean)))
})

const userInitial = computed(() => {
  const n = auth.user?.name?.trim()
  return n && n.length > 0 ? n.slice(0, 1) : 'U'
})

const menuOpen = ref(false)
const switchModalOpen = ref(false)

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

function confirmSwitch() {
  switchModalOpen.value = false
  auth.logout()
  void router.push(isTeacher.value ? '/auth/student' : '/auth/tutor')
}

function onLogout() {
  closeMenu()
  auth.logout()
  void router.push('/')
}
</script>

<template>
  <header class="bar" @click="closeMenu">
    <div class="container inner">
      <div class="left">
        <button class="logo" type="button" @click="go('/')">家教直聘</button>

        <label v-if="isLoggedIn" class="city">
          <select v-model="city" class="city-select">
            <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>

        <nav v-if="isLoggedIn" class="tabs">
          <button class="tab" :class="{ active: route.path === '/' }" type="button" @click="go('/')">首页</button>
          <template v-if="isTeacher">
            <button class="tab" :class="{ active: route.path.startsWith('/tutor/jobs') }" type="button" @click="go('/tutor/jobs')">
              需求
            </button>
          </template>
          <template v-else>
            <button class="tab" :class="{ active: route.path.startsWith('/student/post') }" type="button" @click="go('/student/post')">
              发布需求
            </button>
            <button class="tab" :class="{ active: route.path.startsWith('/student/jobs') }" type="button" @click="go('/student/jobs/mine')">
              我的需求
            </button>
          </template>
        </nav>
      </div>

      <div class="right">
        <template v-if="isLoggedIn">
          <button class="link" type="button" @click="go('/chat')">消息</button>
          <button v-if="isTeacher" class="link" type="button" @click="go('/me')">简历</button>
          <button v-else class="link" type="button" @click="go('/me')">我的</button>

          <div class="user" @click.stop="toggleMenu">
            <img v-if="auth.user?.avatar" class="avatar" :src="auth.user.avatar" alt="avatar" />
            <div v-else class="avatar fallback">{{ userInitial }}</div>
            <div class="name">{{ displayName }}</div>
          </div>

          <div v-if="menuOpen" class="menu card" @click.stop>
            <button class="menu-item" type="button" @click="go('/me')">{{ isTeacher ? '简历' : '我的' }}</button>
            <button class="menu-item" type="button" @click="onSwitchClick">{{ switchLabel }}</button>
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

.city-select {
  height: 34px;
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 0 10px;
  background: #fff;
  outline: none;
  font-weight: 800;
  color: var(--text);
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

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
