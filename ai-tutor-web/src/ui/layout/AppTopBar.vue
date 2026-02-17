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

function go(path: string) {
  void router.push(path)
}
</script>

<template>
  <header class="bar">
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

          <div class="user" @click="go('/me')">
            <img v-if="auth.user?.avatar" class="avatar" :src="auth.user.avatar" alt="avatar" />
            <div v-else class="avatar fallback">{{ userInitial }}</div>
            <div class="name">{{ displayName }}</div>
          </div>
        </template>
        <div v-else class="guest">
          <button class="btn" type="button" @click="go('/auth/student')">找家教登录</button>
          <button class="btn btn-primary" type="button" @click="go('/auth/tutor')">当家教登录</button>
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
</style>
