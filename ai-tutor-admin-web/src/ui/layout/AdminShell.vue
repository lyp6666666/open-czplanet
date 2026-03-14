<template>
  <div class="shell">
    <aside class="sidebar card">
      <div class="brand">
        <div class="brand-dot" />
        <div class="brand-title">AI Tutor Admin</div>
      </div>

      <nav class="nav">
        <RouterLink class="nav-link" to="/dashboard">仪表盘</RouterLink>
        <RouterLink class="nav-link" to="/users">用户管理</RouterLink>
        <RouterLink class="nav-link" to="/organizations">机构账号</RouterLink>
        <RouterLink class="nav-link" to="/jobs">需求审核</RouterLink>
        <RouterLink class="nav-link" to="/verification">教师认证</RouterLink>
        <RouterLink class="nav-link" to="/refunds">退款纠纷</RouterLink>
        <RouterLink class="nav-link" to="/payments">付款记录</RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="user">
          <div class="user-name">{{ nicknameText }}</div>
          <div class="user-sub">ID: {{ userIdText }}</div>
        </div>
        <button class="btn btn-muted" type="button" @click="onLogout">退出登录</button>
      </div>
    </aside>

    <main class="main">
      <header class="topbar card">
        <div class="topbar-title">{{ title }}</div>
        <div class="topbar-right" />
      </header>

      <div class="content">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAdminAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAdminAuthStore()

const title = computed(() => {
  const name = String(route.name || '')
  if (name === 'dashboard') return '仪表盘'
  if (name === 'users') return '用户管理'
  if (name === 'organizations') return '机构账号'
  if (name === 'jobs') return '需求审核'
  if (name === 'verification') return '教师认证审核'
  if (name === 'verificationDetail') return '认证详情'
  if (name === 'refunds') return '退款纠纷'
  if (name === 'refundDetail') return '纠纷详情'
  if (name === 'paymentOrders') return '付款记录'
  if (name === 'paymentOrderDetail') return '付款记录详情'
  return '管理端'
})

const nicknameText = computed(() => auth.user?.nickname || auth.user?.token ? '管理员' : '未登录')
const userIdText = computed(() => (auth.user?.id != null ? String(auth.user.id) : '-'))

function onLogout() {
  auth.logout()
  router.replace({ name: 'login' })
}
</script>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  padding: 16px;
  min-height: 100vh;
}

.sidebar {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 2px;
}

.brand-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--primary);
}

.brand-title {
  font-weight: 700;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-link {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  color: var(--muted);
}

.nav-link.router-link-active {
  color: var(--text);
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.08);
}

.sidebar-footer {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding-top: 12px;
}

.user-name {
  font-weight: 600;
}

.user-sub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 2px;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topbar {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-title {
  font-weight: 700;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 900px) {
  .shell {
    grid-template-columns: 1fr;
  }
}
</style>
