<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const busy = ref(false)
const error = ref<string | null>(null)
const ok = ref<string | null>(null)

const oldCode = ref('')
const newPhone = ref('')
const newCode = ref('')

const mePhoneMasked = computed(() => {
  const p = auth.user?.phone || ''
  const s = String(p)
  if (s.length < 7) return s
  return `${s.slice(0, 3)}****${s.slice(-4)}`
})

async function sendOldCode() {
  if (busy.value) return
  busy.value = true
  error.value = null
  ok.value = null
  try {
    await userApi.sendUpdateUserPhoneCode()
    ok.value = '验证码已发送'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    busy.value = false
  }
}

async function sendNewCode() {
  if (busy.value) return
  busy.value = true
  error.value = null
  ok.value = null
  try {
    await userApi.sendUpdateUserNewPhoneCode(newPhone.value.trim())
    ok.value = '验证码已发送'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    busy.value = false
  }
}

async function submit() {
  if (busy.value) return
  busy.value = true
  error.value = null
  ok.value = null
  try {
    await userApi.updateUserPhoneV2({
      newPhone: newPhone.value.trim(),
      oldCode: oldCode.value.trim(),
      newCode: newCode.value.trim(),
    })
    ok.value = '手机号已更新，请重新登录'
    const role = auth.role
    auth.logout()
    await router.push(role === 'TEACHER' ? '/auth/tutor' : '/auth/student')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '更新失败'
  } finally {
    busy.value = false
  }
}

function back() {
  router.back()
}
</script>

<template>
  <div class="wrap">
    <div class="head">
      <button class="btn" type="button" @click="back">返回</button>
      <div class="title">修改手机号</div>
      <div />
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="ok" class="hint ok">{{ ok }}</div>

    <div class="card form">
      <div class="sec">
        <div class="sec-title">验证当前手机号</div>
        <div class="row">
          <div class="label">当前手机号</div>
          <div class="val">{{ mePhoneMasked }}</div>
        </div>
        <div class="row">
          <input v-model="oldCode" class="inp" placeholder="输入验证码" />
          <button class="btn" type="button" :disabled="busy" @click="sendOldCode">发送验证码</button>
        </div>
      </div>

      <div class="sep" />

      <div class="sec">
        <div class="sec-title">绑定新手机号</div>
        <div class="row">
          <input v-model="newPhone" class="inp" placeholder="输入新手机号" />
        </div>
        <div class="row">
          <input v-model="newCode" class="inp" placeholder="输入新手机号验证码" />
          <button class="btn" type="button" :disabled="busy" @click="sendNewCode">发送验证码</button>
        </div>
      </div>

      <div class="actions">
        <button class="btn btn-primary" type="button" :disabled="busy" @click="submit">确认修改</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}

.head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
}

.title {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}

.form {
  padding: 12px;
  display: grid;
  gap: 12px;
}

.sec {
  display: grid;
  gap: 10px;
}

.sec-title {
  font-weight: 900;
}

.row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.label {
  color: var(--muted);
  font-size: 12px;
}

.val {
  font-weight: 900;
}

.inp {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  font-size: 13px;
}

.sep {
  height: 1px;
  background: var(--border);
}

.actions {
  display: flex;
  justify-content: flex-end;
}
</style>
