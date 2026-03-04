<template>
  <div class="wrap">
    <div class="panel card">
      <div class="title">管理员登录</div>
      <div class="sub">使用后台管理员账号登录后处理审核与纠纷</div>

      <form class="form" @submit.prevent="onSubmit">
        <label class="label">
          <div class="label-text">用户名</div>
          <input v-model="username" class="input" autocomplete="username" />
        </label>

        <label class="label">
          <div class="label-text">密码</div>
          <input v-model="password" class="input" type="password" autocomplete="current-password" />
        </label>

        <div v-if="errorText" class="error">{{ errorText }}</div>

        <button class="btn btn-primary" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { adminLogin } from '@/api/adminAuth'
import { ApiError } from '@/api/http'
import { useAdminAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAdminAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorText = ref<string | null>(null)

async function onSubmit() {
  if (loading.value) return
  errorText.value = null
  loading.value = true
  try {
    const res = await adminLogin({ username: username.value.trim(), password: password.value })
    auth.setAuth(res)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch (e) {
    if (e instanceof ApiError) {
      errorText.value = e.message || '登录失败'
    } else if (e && typeof e === 'object' && 'message' in e) {
      errorText.value = String((e as { message?: unknown }).message || '登录失败')
    } else {
      errorText.value = '登录失败'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
}

.panel {
  width: 420px;
  max-width: 100%;
  padding: 18px;
}

.title {
  font-weight: 800;
  font-size: 20px;
}

.sub {
  color: var(--muted);
  font-size: 13px;
  margin-top: 6px;
}

.form {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.label-text {
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.error {
  color: var(--danger);
  font-size: 13px;
}
</style>
