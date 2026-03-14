<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function onSubmit() {
  const u = username.value.trim()
  const p = password.value
  if (!u || !p) {
    toast.show('请输入账号与密码', 'error')
    return
  }
  loading.value = true
  try {
    const res = await auth.loginOrg(u, p)
    if (res.mustChangePassword) {
      await router.replace({ name: 'orgChangePassword' })
      return
    }
    await router.replace({ name: 'orgMineJobs' })
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '登录失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <div class="card box">
      <div class="t">机构登录</div>
      <label class="field">
        <div class="label">账号</div>
        <input v-model="username" class="input" autocomplete="username" />
      </label>
      <label class="field">
        <div class="label">密码</div>
        <input v-model="password" class="input" type="password" autocomplete="current-password" />
      </label>
      <button class="btn btn-primary" type="button" :disabled="loading" @click="onSubmit">{{ loading ? '登录中...' : '登录' }}</button>
      <div class="tips">机构账号由平台客服发放，请勿自行注册。</div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  padding: 18px 0;
  display: grid;
  place-items: center;
}

.box {
  width: min(420px, 100%);
  padding: 16px;
  display: grid;
  gap: 10px;
}

.t {
  font-weight: 900;
  font-size: 18px;
}

.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 800;
}

.input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 14px;
}

.hint {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: #fff;
}

.hint.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
}

.tips {
  font-size: 12px;
  color: var(--muted);
}
</style>
