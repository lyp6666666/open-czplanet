<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const isLoggedIn = computed(() => auth.isLoggedIn)
const isTeacher = computed(() => auth.user?.userType === 1)
const displayName = computed(() => auth.user?.name || '未登录')

function go(path: string) {
  void router.push(path)
}
</script>

<template>
  <header class="bar">
    <div class="container inner">
      <div class="left">
        <button class="logo" type="button" @click="go('/')">家教直聘</button>
        <nav v-if="isLoggedIn" class="nav">
          <button v-if="isTeacher" class="nav-item" type="button" @click="go('/tutor/jobs')">需求广场</button>
          <template v-else>
            <button class="nav-item" type="button" @click="go('/student/post')">发布需求</button>
            <button class="nav-item" type="button" @click="go('/student/jobs/mine')">我的需求</button>
          </template>
          <button class="nav-item" type="button" @click="go('/chat')">消息</button>
          <button class="nav-item" type="button" @click="go('/me')">我的</button>
        </nav>
      </div>

      <div class="right">
        <div v-if="isLoggedIn" class="user">
          <div class="name">{{ displayName }}</div>
        </div>
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
  background: #fff;
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
  gap: 18px;
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

.nav {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.nav-item {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 10px;
  font-weight: 700;
  color: var(--muted);
}

.nav-item:hover {
  background: rgba(31, 35, 41, 0.06);
  color: var(--text);
}

.right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.guest {
  display: flex;
  gap: 10px;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name {
  font-size: 13px;
  color: var(--muted);
  font-weight: 700;
}
</style>
