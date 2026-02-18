<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

import type { UserRoleEnum } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  role: UserRoleEnum
}>()

const router = useRouter()
const auth = useAuthStore()

const phone = ref('')
const code = ref('')
const agreed = ref(true)
const busy = ref(false)
const hint = ref<string | null>(null)

const sendBusy = ref(false)
const remainSec = ref(0)
let timer: number | null = null

const isTutor = computed(() => props.role === 'TEACHER')
const title = computed(() => (isTutor.value ? '验证码登录/注册（家教端）' : '验证码登录/注册（找家教）'))
const primaryAction = computed(() => (isTutor.value ? '我要当家教' : '我要找家教'))
const secondaryAction = computed(() => (isTutor.value ? '我要找家教' : '我要当家教'))

const needSwitchRole = computed(() => auth.isLoggedIn && auth.role != null && auth.role !== props.role)
const switchModalOpen = ref(false)

const switchTitle = computed(() => (isTutor.value ? '是否将身份切换为教师端' : '是否将身份切换为招聘者'))
const switchDesc = computed(() =>
  isTutor.value ? '若 APP 已登录家长身份，点击【切换】后将退出并重新登录教师端。' : '若 APP 已登录教师身份，点击【切换】后将退出并重新登录找家教端。',
)

const primarySubTitle = computed(() =>
  isTutor.value ? '用手机号快速入驻，匹配附近需求' : '用手机号快速登录，精准匹配老师',
)

const leftBullets = computed(() => {
  if (isTutor.value) {
    return [
      { title: '海量订单', desc: '覆盖热门学科，持续有单可接' },
      { title: '就近匹配', desc: '按城市与距离推荐更合适的需求' },
      { title: '安心保障', desc: '身份校验 + 评价体系，沟通更放心' },
    ]
  }
  return [
    { title: '快速找老师', desc: '按学科/年级/上课方式筛选' },
    { title: '透明报价', desc: '价格区间清晰，沟通成本更低' },
    { title: '高效沟通', desc: '确认邀约后自动建立 1v1 会话' },
  ]
})

const otherRolePath = computed(() => (isTutor.value ? '/auth/student' : '/auth/tutor'))

function normalizePhone(raw: string) {
  return raw.replace(/\s+/g, '')
}

function validatePhone(v: string): string | null {
  const p = normalizePhone(v)
  if (p.length === 0) return '请输入手机号'
  if (!/^\d{11}$/.test(p)) return '请输入 11 位手机号'
  return null
}

function validateCode(v: string): string | null {
  const c = v.replace(/\s+/g, '')
  if (c.length === 0) return '请输入验证码'
  if (!/^\d{4}$/.test(c)) return '请输入 4 位验证码'
  return null
}

function startCountdown(seconds: number) {
  remainSec.value = seconds
  if (timer != null) window.clearInterval(timer)
  timer = window.setInterval(() => {
    remainSec.value -= 1
    if (remainSec.value <= 0 && timer != null) {
      window.clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onBeforeUnmount(() => {
  if (timer != null) window.clearInterval(timer)
})

async function onSendCode() {
  hint.value = null
  if (needSwitchRole.value) {
    switchModalOpen.value = true
    return
  }
  const phoneErr = validatePhone(phone.value)
  if (phoneErr) {
    hint.value = phoneErr
    return
  }
  if (remainSec.value > 0) return

  sendBusy.value = true
  try {
    await auth.sendCode(normalizePhone(phone.value))
    hint.value = '验证码已发送（开发环境可在后端控制台查看模拟验证码）'
    startCountdown(60)
  } catch (e) {
    hint.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    sendBusy.value = false
  }
}

async function doSubmit() {
  hint.value = null
  const phoneErr = validatePhone(phone.value)
  const codeErr = validateCode(code.value)
  if (phoneErr || codeErr) {
    hint.value = phoneErr ?? codeErr
    return
  }
  if (!agreed.value) {
    hint.value = '请先勾选并同意用户协议与隐私政策'
    return
  }
  if (busy.value) return

  busy.value = true
  try {
    await auth.loginOrRegister(props.role, normalizePhone(phone.value), code.value.replace(/\s+/g, ''))
    if (props.role === 'TEACHER') {
      const me = await auth.refreshMe()
      const need = !(me?.avatar && me.teacherProfile?.realName?.trim() && me.teacherProfile?.education?.trim())
      await router.replace(need ? '/tutor/onboarding/basic' : '/tutor/jobs')
    } else {
      await router.replace('/student/post')
    }
  } catch (e) {
    hint.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    busy.value = false
  }
}

async function onSubmit() {
  if (needSwitchRole.value) {
    switchModalOpen.value = true
    return
  }
  await doSubmit()
}

async function confirmSwitch() {
  switchModalOpen.value = false
  auth.logout()
}
</script>

<template>
  <div class="page">
    <div class="shell">
      <div class="card board">
        <aside class="left">
          <div class="brand">
            <div class="logo">家教直聘</div>
            <div class="slogan">{{ primaryAction }}</div>
          </div>

          <div class="bullets">
            <div v-for="b in leftBullets" :key="b.title" class="bullet">
              <div class="bullet-title">{{ b.title }}</div>
              <div class="bullet-desc">{{ b.desc }}</div>
            </div>
          </div>
        </aside>

        <section class="right">
          <div class="right-head">
            <div class="r-title">{{ title }}</div>
            <div class="r-desc">{{ primarySubTitle }}</div>
          </div>

          <div class="toggle">
            <button class="toggle-btn active" type="button">{{ primaryAction }}</button>
            <button class="toggle-btn" type="button" @click="router.push(otherRolePath)">{{ secondaryAction }}</button>
          </div>

          <form class="form" @submit.prevent="onSubmit">
            <label class="field">
              <div class="label">手机号</div>
              <input v-model="phone" class="input" inputmode="numeric" autocomplete="tel" placeholder="请输入 11 位手机号" />
            </label>

            <label class="field">
              <div class="label">短信验证码</div>
              <div class="code-row">
                <input v-model="code" class="input" inputmode="numeric" autocomplete="one-time-code" placeholder="请输入验证码" />
                <button class="btn code-btn" type="button" :disabled="sendBusy || remainSec > 0" @click="onSendCode">
                  <span v-if="remainSec > 0">{{ remainSec }}s</span>
                  <span v-else>发送验证码</span>
                </button>
              </div>
            </label>

            <div class="agree">
              <label class="agree-row">
                <input v-model="agreed" type="checkbox" />
                <span>我已阅读并同意</span>
              </label>
              <a class="link" href="javascript:void(0)">《用户协议》</a>
              <a class="link" href="javascript:void(0)">《隐私政策》</a>
            </div>

            <div v-if="hint" class="hint">{{ hint }}</div>

            <button class="btn btn-primary submit" type="submit" :disabled="busy">
              {{ busy ? '提交中...' : '登录/注册' }}
            </button>
          </form>
        </section>
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
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #00bebd;
}

.shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 36px 16px;
}

.board {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 360px 1fr;
  border-radius: 18px;
  overflow: hidden;
}

.left {
  padding: 28px 26px;
  background: linear-gradient(180deg, rgba(0, 190, 189, 0.08), rgba(0, 190, 189, 0.02));
  border-right: 1px solid var(--border);
  display: grid;
  gap: 18px;
  align-content: start;
}

.brand {
  display: grid;
  gap: 6px;
}

.logo {
  font-weight: 900;
  letter-spacing: 0.5px;
  font-size: 18px;
}

.slogan {
  font-size: 13px;
  color: var(--muted);
}

.bullets {
  display: grid;
  gap: 14px;
}

.bullet {
  padding: 12px;
  border: 1px solid rgba(0, 190, 189, 0.2);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
}

.bullet-title {
  font-weight: 800;
  font-size: 13px;
}

.bullet-desc {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.right {
  padding: 28px 28px 30px;
}

.right-head {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
}

.r-title {
  font-size: 18px;
  font-weight: 900;
}

.r-desc {
  color: var(--muted);
  font-size: 12px;
}

.toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}

.toggle-btn {
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-weight: 700;
}

.toggle-btn.active {
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.form {
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.code-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 10px;
  align-items: center;
}

.code-btn {
  height: 40px;
}

.agree {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  color: var(--muted);
  font-size: 12px;
}

.agree-row {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.link {
  color: var(--primary);
}

.hint {
  padding: 10px 12px;
  border: 1px solid rgba(255, 125, 0, 0.35);
  background: rgba(255, 125, 0, 0.08);
  border-radius: 12px;
  font-size: 12px;
  line-height: 1.5;
}

.submit {
  height: 42px;
  border-radius: 12px;
  font-weight: 900;
}

@media (max-width: 920px) {
  .board {
    grid-template-columns: 1fr;
  }
  .left {
    border-right: none;
    border-bottom: 1px solid var(--border);
  }
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 50;
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
