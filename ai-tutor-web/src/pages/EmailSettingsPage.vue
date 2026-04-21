<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import type { EmailType, UserEmailStatusVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

type FormState = {
  email: string
  code: string
  sending: boolean
  verifying: boolean
  cooldown: number
}

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)
const saved = ref<string | null>(null)
const status = ref<UserEmailStatusVO | null>(null)

const primary = ref<FormState>({ email: '', code: '', sending: false, verifying: false, cooldown: 0 })
const summary = ref<FormState>({ email: '', code: '', sending: false, verifying: false, cooldown: 0 })

const isStudent = computed(() => auth.user?.userType === 2)
const primaryBound = computed(() => status.value?.primaryEmail?.verifyStatus === 'VERIFIED')
const summaryBound = computed(() => status.value?.summaryEmail?.verifyStatus === 'VERIFIED')

function back() {
  router.back()
}

function itemText(type: EmailType) {
  const item = type === 'PRIMARY' ? status.value?.primaryEmail : status.value?.summaryEmail
  if (!item) return type === 'PRIMARY' ? '未绑定' : '未设置'
  if (item.verifyStatus === 'VERIFIED') return item.emailMasked
  if (item.verifyStatus === 'PENDING') return `${item.emailMasked || '邮箱'} 待验证`
  return '邮箱异常，请重新绑定'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    status.value = await userApi.emailStatus()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '邮箱状态加载失败'
  } finally {
    loading.value = false
  }
}

function startCooldown(form: FormState, seconds: number) {
  form.cooldown = seconds
  const timer = window.setInterval(() => {
    form.cooldown -= 1
    if (form.cooldown <= 0) {
      window.clearInterval(timer)
      form.cooldown = 0
    }
  }, 1000)
}

async function sendCode(type: EmailType) {
  const form = type === 'PRIMARY' ? primary.value : summary.value
  if (!form.email.trim()) {
    error.value = '请输入邮箱地址'
    return
  }
  form.sending = true
  error.value = null
  saved.value = null
  try {
    const res = await userApi.sendEmailCode({ email: form.email.trim(), emailType: type, scene: 'BIND' })
    startCooldown(form, res.cooldownSeconds || 60)
    saved.value = '验证码已发送，请前往邮箱查看'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '验证码发送失败'
  } finally {
    form.sending = false
  }
}

async function verify(type: EmailType) {
  const form = type === 'PRIMARY' ? primary.value : summary.value
  if (!form.email.trim() || !form.code.trim()) {
    error.value = '请输入邮箱地址和验证码'
    return
  }
  form.verifying = true
  error.value = null
  saved.value = null
  try {
    await userApi.verifyEmail({
      email: form.email.trim(),
      emailType: type,
      code: form.code.trim(),
      scene: 'BIND',
      bindSource: 'MY_PAGE',
    })
    form.email = ''
    form.code = ''
    await load()
    saved.value =
      type === 'PRIMARY'
        ? '主邮箱绑定成功，后续将通过邮箱接收消息、开课和课后总结提醒。'
        : '课后总结邮箱设置成功，后续每节课总结也会发送到这个邮箱。'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '邮箱验证失败'
  } finally {
    form.verifying = false
  }
}

async function deleteSummaryEmail() {
  if (!window.confirm('确认移除课后总结邮箱吗？移除后家长邮箱将不再收到课后总结。')) return
  error.value = null
  saved.value = null
  try {
    await userApi.deleteSummaryEmail()
    await load()
    saved.value = '已移除课后总结邮箱。'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '移除失败'
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="email-page">
    <div class="email-shell">
      <header class="email-hero">
        <button class="ghost" type="button" @click="back">返回</button>
        <div>
          <p class="eyebrow">Email reminders</p>
          <h1>邮箱提醒设置</h1>
          <p class="hero-copy">短信目前仅用于验证码。绑定邮箱后，消息、开课和课后总结都会通过邮件稳定提醒你。</p>
        </div>
      </header>

      <div v-if="error" class="notice error">{{ error }}</div>
      <div v-if="saved" class="notice ok">{{ saved }}</div>

      <section class="mail-card primary">
        <div class="card-head">
          <div>
            <span class="tag">主邮箱</span>
            <h2>{{ itemText('PRIMARY') }}</h2>
          </div>
          <span class="state" :class="{ on: primaryBound }">{{ primaryBound ? '已验证' : '未开启' }}</span>
        </div>
        <p class="card-copy">用于接收未读消息提醒、开课提醒和课后总结。建议填写你常用且能及时查看的邮箱。</p>
        <div class="form-grid">
          <input v-model="primary.email" class="input" type="email" placeholder="输入主邮箱" />
          <button class="btn" type="button" :disabled="primary.sending || primary.cooldown > 0" @click="sendCode('PRIMARY')">
            {{ primary.cooldown > 0 ? `${primary.cooldown}s` : '获取验证码' }}
          </button>
          <input v-model="primary.code" class="input" inputmode="numeric" placeholder="输入邮箱验证码" />
          <button class="btn solid" type="button" :disabled="primary.verifying" @click="verify('PRIMARY')">完成绑定</button>
        </div>
      </section>

      <section v-if="isStudent" class="mail-card summary">
        <div class="card-head">
          <div>
            <span class="tag warm">仅课后总结</span>
            <h2>{{ itemText('SUMMARY_ONLY') }}</h2>
          </div>
          <span class="state" :class="{ on: summaryBound }">{{ summaryBound ? '已设置' : '选填' }}</span>
        </div>
        <p class="card-copy">
          适合填写家长邮箱。这个邮箱只会接收每节课结束后的课后总结，不会收到聊天消息、开课提醒，也不能用于登录账号。
        </p>
        <div class="form-grid">
          <input v-model="summary.email" class="input" type="email" placeholder="输入家长邮箱或课后总结邮箱" />
          <button class="btn" type="button" :disabled="summary.sending || summary.cooldown > 0" @click="sendCode('SUMMARY_ONLY')">
            {{ summary.cooldown > 0 ? `${summary.cooldown}s` : '获取验证码' }}
          </button>
          <input v-model="summary.code" class="input" inputmode="numeric" placeholder="输入邮箱验证码" />
          <button class="btn solid warm-solid" type="button" :disabled="summary.verifying" @click="verify('SUMMARY_ONLY')">设置课后总结邮箱</button>
        </div>
        <button v-if="summaryBound" class="danger-link" type="button" @click="deleteSummaryEmail">移除课后总结邮箱</button>
      </section>

      <section class="mail-card explain">
        <h2>会在什么时候收到邮件？</h2>
        <div class="explain-grid">
          <div><strong>2 小时未读</strong><span>消息长时间未读时，邮件会提醒你回到站内查看。</span></div>
          <div><strong>开课提醒</strong><span>提醒时间与站内提醒保持一致，避免漏课。</span></div>
          <div><strong>课后总结</strong><span>每节课结束并生成总结后，会发送给教师、学生和已设置的家长邮箱。</span></div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.email-page {
  min-height: 100vh;
  padding: 76px 20px 32px;
  background:
    radial-gradient(circle at 12% 18%, rgba(0, 181, 120, 0.16), transparent 28%),
    linear-gradient(135deg, #f6fbf5 0%, #edf5f1 44%, #fff8ec 100%);
}

.email-shell {
  max-width: 880px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.email-hero,
.mail-card {
  border: 1px solid rgba(35, 84, 66, 0.12);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 55px rgba(40, 62, 54, 0.1);
  backdrop-filter: blur(18px);
  border-radius: 24px;
}

.email-hero {
  padding: 24px;
  display: grid;
  gap: 18px;
}

.ghost {
  width: fit-content;
  border: 0;
  background: rgba(255, 255, 255, 0.7);
  color: #315242;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: #5f8d70;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  font-size: clamp(30px, 5vw, 48px);
  color: #183328;
  letter-spacing: -0.05em;
}

.hero-copy,
.card-copy {
  margin-top: 10px;
  color: #5d6f66;
  line-height: 1.7;
}

.mail-card {
  padding: 22px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.tag,
.state {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.tag {
  color: #0d7a50;
  background: #e7f8ef;
}

.tag.warm {
  color: #a56100;
  background: #fff1d7;
}

.state {
  color: #7a827e;
  background: #f1f3f2;
}

.state.on {
  color: #0d7a50;
  background: #ddf6e8;
}

.form-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.input {
  min-height: 46px;
  border: 1px solid #d8e4dc;
  border-radius: 14px;
  padding: 0 14px;
  outline: none;
  background: #fff;
}

.input:focus {
  border-color: #00b578;
  box-shadow: 0 0 0 4px rgba(0, 181, 120, 0.1);
}

.btn {
  min-height: 46px;
  border: 0;
  border-radius: 14px;
  padding: 0 18px;
  font-weight: 800;
  color: #0d7a50;
  background: #e7f8ef;
  cursor: pointer;
}

.btn.solid {
  color: #fff;
  background: #00b578;
}

.btn.warm-solid {
  background: #e89017;
}

.btn:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.danger-link {
  margin-top: 14px;
  border: 0;
  background: transparent;
  color: #b33a2d;
  cursor: pointer;
}

.notice {
  border-radius: 16px;
  padding: 12px 16px;
}

.notice.error {
  background: #fff2f0;
  color: #b33a2d;
}

.notice.ok {
  background: #effaf2;
  color: #138a55;
}

.explain-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.explain-grid div {
  padding: 14px;
  background: #f8fbf7;
  border-radius: 16px;
}

.explain-grid strong,
.explain-grid span {
  display: block;
}

.explain-grid span {
  margin-top: 6px;
  color: #66736d;
  line-height: 1.55;
}

@media (max-width: 720px) {
  .form-grid,
  .explain-grid {
    grid-template-columns: 1fr;
  }
}
</style>
