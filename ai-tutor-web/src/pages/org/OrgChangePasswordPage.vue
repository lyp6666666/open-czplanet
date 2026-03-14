<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const toast = useToastStore()

const oldPassword = ref('')
const newPassword = ref('')
const loading = ref(false)

async function onSubmit() {
  const oldP = oldPassword.value
  const newP = newPassword.value.trim()
  if (!oldP || !newP) {
    toast.show('请输入旧密码与新密码', 'error')
    return
  }
  if (newP.length < 8) {
    toast.show('新密码至少 8 位', 'error')
    return
  }
  loading.value = true
  try {
    await userApi.orgChangePassword({ oldPassword: oldP, newPassword: newP })
    toast.show('修改成功', 'success')
    await router.replace({ name: 'orgMineJobs' })
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '修改失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <div class="card box">
      <div class="t">修改机构密码</div>
      <div class="tips">首次登录需要修改初始密码。</div>
      <label class="field">
        <div class="label">旧密码</div>
        <input v-model="oldPassword" class="input" type="password" autocomplete="current-password" />
      </label>
      <label class="field">
        <div class="label">新密码</div>
        <input v-model="newPassword" class="input" type="password" autocomplete="new-password" />
      </label>
      <button class="btn btn-primary" type="button" :disabled="loading" @click="onSubmit">{{ loading ? '提交中...' : '提交' }}</button>
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

.tips {
  font-size: 12px;
  color: var(--muted);
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

.hint.ok {
  border-color: rgba(0, 190, 189, 0.28);
  background: rgba(0, 190, 189, 0.08);
}
</style>
