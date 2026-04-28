<template>
  <view class="page">
    <view class="hero-card">
      <text class="eyebrow">Email reminders</text>
      <text class="title">邮箱提醒设置</text>
      <text class="hero-desc">{{ heroCopy }}</text>
    </view>

    <AppStateCard
      v-if="!userStore.isLoggedIn"
      title="登录后配置邮箱提醒"
      description="绑定邮箱后，消息提醒、开课提醒和课后总结都能更稳定地送达。"
      action-text="去登录"
      variant="soft"
      @action="goLogin"
    />

    <template v-else>
      <AppStateCard
        v-if="loadError"
        title="邮箱状态加载失败"
        :description="loadError"
        action-text="重新加载"
        variant="error"
        @action="load"
      />

      <view v-else>
        <view v-if="tip" class="tip" :class="{ error: tipType === 'error' }">{{ tip }}</view>

        <view class="mail-card">
          <view class="card-head">
            <view>
              <text class="tag">主邮箱</text>
              <text class="mail">{{ primaryText }}</text>
            </view>
            <text class="state" :class="{ on: primaryVerified }">{{ primaryVerified ? '已验证' : '未开启' }}</text>
          </view>
          <text class="copy">{{ primaryCopy }}</text>

          <input v-model="primary.email" class="input" type="text" placeholder="输入主邮箱" />
          <view class="row">
            <input v-model="primary.code" class="input flex" type="text" placeholder="输入邮箱验证码" />
            <button class="btn ghost" :disabled="primary.sending || primary.cooldown > 0" @click="sendCode('PRIMARY')">
              {{ primary.cooldown > 0 ? `${primary.cooldown}s` : '获取验证码' }}
            </button>
          </view>
          <button class="btn solid" :disabled="primary.verifying" @click="verify('PRIMARY')">
            {{ primary.verifying ? '绑定中...' : '完成绑定' }}
          </button>
        </view>

        <view v-if="isStudent" class="mail-card warm">
          <view class="card-head">
            <view>
              <text class="tag warm-tag">仅课后总结</text>
              <text class="mail">{{ summaryText }}</text>
            </view>
            <text class="state" :class="{ on: summaryVerified }">{{ summaryVerified ? '已设置' : '选填' }}</text>
          </view>
          <text class="copy">适合填写家长邮箱。这个邮箱只接收课后总结，不接收聊天消息、开课提醒，也不能用于登录。</text>

          <input v-model="summary.email" class="input" type="text" placeholder="输入家长邮箱或总结邮箱" />
          <view class="row">
            <input v-model="summary.code" class="input flex" type="text" placeholder="输入邮箱验证码" />
            <button class="btn warm-ghost" :disabled="summary.sending || summary.cooldown > 0" @click="sendCode('SUMMARY_ONLY')">
              {{ summary.cooldown > 0 ? `${summary.cooldown}s` : '获取验证码' }}
            </button>
          </view>
          <button class="btn warm-solid" :disabled="summary.verifying" @click="verify('SUMMARY_ONLY')">
            {{ summary.verifying ? '设置中...' : '设置课后总结邮箱' }}
          </button>
          <button v-if="summaryVerified" class="text-btn" @click="deleteSummary">移除课后总结邮箱</button>
        </view>

        <view class="mail-card note-card">
          <text class="note-title">什么时候会收到邮件？</text>
          <view class="note-list">
            <view class="note-item">
              <text class="note-name">未读消息提醒</text>
              <text class="note-desc">当站内消息较长时间未读时，系统会用邮件提醒你回来查看。</text>
            </view>
            <view class="note-item">
              <text class="note-name">开课提醒</text>
              <text class="note-desc">开课前的关键提醒会同步发到邮箱，降低漏课风险。</text>
            </view>
            <view class="note-item">
              <text class="note-name">课后总结</text>
              <text class="note-desc">每节课结束后的总结，会发送给主邮箱和已设置的家长总结邮箱。</text>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { userApi } from '@/api/user';
import { useUserStore } from '@/stores/user';
import AppStateCard from '@/components/AppStateCard.vue';

type EmailType = 'PRIMARY' | 'SUMMARY_ONLY';
type FormState = {
  email: string;
  code: string;
  sending: boolean;
  verifying: boolean;
  cooldown: number;
};

const userStore = useUserStore();
const status = ref<any>(null);
const loadError = ref('');
const tip = ref('');
const tipType = ref<'ok' | 'error'>('ok');
const primary = reactive<FormState>({ email: '', code: '', sending: false, verifying: false, cooldown: 0 });
const summary = reactive<FormState>({ email: '', code: '', sending: false, verifying: false, cooldown: 0 });

const isStudent = computed(() => userStore.currentRole === 'student');
const primaryVerified = computed(() => status.value?.primaryEmail?.verifyStatus === 'VERIFIED');
const summaryVerified = computed(() => status.value?.summaryEmail?.verifyStatus === 'VERIFIED');
const primaryText = computed(() => status.value?.primaryEmail?.emailMasked || '未绑定');
const summaryText = computed(() => status.value?.summaryEmail?.emailMasked || '未设置');
const heroCopy = computed(() =>
  primaryVerified.value
    ? '主邮箱已开启，未读消息、开课提醒和课后总结会继续通过邮件稳定提醒你。'
    : '短信目前仅用于验证码。绑定邮箱后，消息提醒、开课提醒和课后总结都能更稳定送达。',
);
const primaryCopy = computed(() =>
  primaryVerified.value
    ? '当前主邮箱已用于接收未读消息提醒、开课提醒和课后总结；如需更换，可重新获取验证码完成改绑。'
    : '用于接收未读消息提醒、开课提醒和课后总结。建议填写你常用且能及时查看的邮箱。',
);

function setTip(message: string, type: 'ok' | 'error' = 'ok') {
  tip.value = message;
  tipType.value = type;
}

async function load() {
  if (!userStore.isLoggedIn) return;
  loadError.value = '';
  try {
    status.value = await userApi.emailStatus();
  } catch (e: any) {
    loadError.value = e?.message || e?.msg || '邮箱状态加载失败';
  }
}

function startCooldown(form: FormState, seconds = 60) {
  form.cooldown = seconds;
  const timer = setInterval(() => {
    form.cooldown -= 1;
    if (form.cooldown <= 0) {
      form.cooldown = 0;
      clearInterval(timer);
    }
  }, 1000);
}

function getForm(type: EmailType) {
  return type === 'PRIMARY' ? primary : summary;
}

async function sendCode(type: EmailType) {
  const form = getForm(type);
  const email = String(form.email || '').trim();
  if (!email) {
    setTip('请输入邮箱地址', 'error');
    return;
  }
  form.sending = true;
  try {
    const res: any = await userApi.sendEmailCode({ email, emailType: type, scene: 'BIND' });
    startCooldown(form, Number(res?.cooldownSeconds) || 60);
    setTip('验证码已发送，请前往邮箱查看');
  } catch (e: any) {
    setTip(e?.message || e?.msg || '验证码发送失败', 'error');
  } finally {
    form.sending = false;
  }
}

async function verify(type: EmailType) {
  const form = getForm(type);
  const email = String(form.email || '').trim();
  const code = String(form.code || '').trim();
  if (!email || !code) {
    setTip('请输入邮箱地址和验证码', 'error');
    return;
  }
  form.verifying = true;
  try {
    await userApi.verifyEmail({ email, emailType: type, code, scene: 'BIND', bindSource: 'MY_PAGE' });
    form.email = '';
    form.code = '';
    await load();
    setTip(type === 'PRIMARY' ? '主邮箱绑定成功' : '课后总结邮箱设置成功');
  } catch (e: any) {
    setTip(e?.message || e?.msg || '邮箱验证失败', 'error');
  } finally {
    form.verifying = false;
  }
}

async function deleteSummary() {
  try {
    await userApi.deleteSummaryEmail();
    await load();
    setTip('已移除课后总结邮箱');
  } catch (e: any) {
    setTip(e?.message || e?.msg || '移除失败', 'error');
  }
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

onMounted(() => {
  void load();
});
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 18px 16px 28px;
  background: linear-gradient(135deg, #f6fbf5 0%, #eef6f2 46%, #fff8ec 100%);
}

.hero-card,
.mail-card {
  border-radius: 20px;
  border: 1px solid rgba(35, 84, 66, 0.1);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 36px rgba(31, 35, 41, 0.08);
}

.hero-card {
  padding: 20px;
  margin-bottom: 14px;
}

.eyebrow,
.title,
.hero-desc,
.copy,
.mail,
.tag,
.state,
.note-title,
.note-name,
.note-desc {
  display: block;
}

.eyebrow {
  font-size: 12px;
  color: #5f8d70;
  letter-spacing: 1px;
}

.title {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 900;
  color: #183328;
}

.hero-desc,
.copy,
.note-desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #5d6f66;
}

.tip {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #ebf9f1;
  color: #0d7a50;
  font-size: 13px;
}

.tip.error {
  background: #fff3f2;
  color: #b33a2d;
}

.mail-card {
  padding: 18px;
  margin-bottom: 14px;
}

.warm {
  background: rgba(255, 250, 241, 0.94);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.tag {
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e8f8ef;
  color: #008f61;
  font-size: 12px;
  font-weight: 800;
}

.warm-tag {
  background: #fff1d7;
  color: #a56100;
}

.mail {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 900;
  color: #183328;
}

.state {
  font-size: 12px;
  color: #6f7f77;
}

.state.on {
  color: #0d7a50;
}

.input {
  margin-top: 12px;
  height: 44px;
  border-radius: 12px;
  background: #f7faf8;
  padding: 0 12px;
  font-size: 14px;
  color: #1f2329;
}

.row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.flex {
  flex: 1;
}

.btn {
  margin-top: 12px;
  height: 44px;
  border-radius: 12px;
  border: 0;
  font-size: 14px;
  font-weight: 800;
  line-height: 44px;
}

.btn::after {
  border: 0;
}

.ghost {
  min-width: 104px;
  background: #e8f8ef;
  color: #008f61;
}

.solid {
  width: 100%;
  background: #00b578;
  color: #fff;
}

.warm-ghost {
  min-width: 104px;
  background: #fff1d7;
  color: #a56100;
}

.warm-solid {
  width: 100%;
  background: #e89017;
  color: #fff;
}

.text-btn {
  margin-top: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #b33a2d;
  font-size: 13px;
  text-align: left;
}

.note-card {
  background: rgba(255, 255, 255, 0.86);
}

.note-title {
  font-size: 16px;
  font-weight: 900;
  color: #183328;
}

.note-list {
  margin-top: 12px;
  display: grid;
  gap: 12px;
}

.note-item {
  padding-top: 12px;
  border-top: 1px solid rgba(24, 51, 40, 0.08);
}

.note-item:first-child {
  padding-top: 0;
  border-top: 0;
}

.note-name {
  font-size: 13px;
  font-weight: 900;
  color: #183328;
}
</style>
