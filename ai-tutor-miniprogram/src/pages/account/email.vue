<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">Email reminders</text>
      <text class="title">邮箱提醒设置</text>
      <text class="desc">短信目前仅用于验证码。绑定邮箱后，可接收未读消息提醒、开课提醒和课后总结。</text>
    </view>

    <view v-if="tip" class="tip">{{ tip }}</view>

    <view class="card">
      <view class="head">
        <view>
          <text class="tag">主邮箱</text>
          <text class="mail">{{ primaryText }}</text>
        </view>
        <text class="state">{{ primaryVerified ? '已验证' : '未开启' }}</text>
      </view>
      <text class="copy">用于接收未读消息提醒、开课提醒和课后总结。</text>
      <input v-model="primaryEmail" class="input" placeholder="输入主邮箱" />
      <view class="row">
        <input v-model="primaryCode" class="input flex" placeholder="输入验证码" />
        <button class="btn" @click="sendCode('PRIMARY')">获取验证码</button>
      </view>
      <button class="btn solid" @click="verify('PRIMARY')">完成绑定</button>
    </view>

    <view v-if="isStudent" class="card warm">
      <view class="head">
        <view>
          <text class="tag warm-tag">仅课后总结</text>
          <text class="mail">{{ summaryText }}</text>
        </view>
        <text class="state">{{ summaryVerified ? '已设置' : '选填' }}</text>
      </view>
      <text class="copy">适合填写家长邮箱。这个邮箱只会接收课后总结，不会收到聊天消息、开课提醒，也不能用于登录账号。</text>
      <input v-model="summaryEmail" class="input" placeholder="输入家长邮箱" />
      <view class="row">
        <input v-model="summaryCode" class="input flex" placeholder="输入验证码" />
        <button class="btn warm-btn" @click="sendCode('SUMMARY_ONLY')">获取验证码</button>
      </view>
      <button class="btn solid warm-solid" @click="verify('SUMMARY_ONLY')">设置课后总结邮箱</button>
      <button v-if="summaryVerified" class="danger" @click="deleteSummary">移除课后总结邮箱</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { userApi } from '@/api/user';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const status = ref<any>(null);
const tip = ref('');
const primaryEmail = ref('');
const primaryCode = ref('');
const summaryEmail = ref('');
const summaryCode = ref('');

const isStudent = computed(() => userStore.currentRole === 'student');
const primaryVerified = computed(() => status.value?.primaryEmail?.verifyStatus === 'VERIFIED');
const summaryVerified = computed(() => status.value?.summaryEmail?.verifyStatus === 'VERIFIED');
const primaryText = computed(() => status.value?.primaryEmail?.emailMasked || '未绑定');
const summaryText = computed(() => status.value?.summaryEmail?.emailMasked || '未设置');

async function load() {
  try {
    status.value = await userApi.emailStatus();
  } catch (e: any) {
    tip.value = e.message || '加载失败';
  }
}

async function sendCode(type: 'PRIMARY' | 'SUMMARY_ONLY') {
  const email = type === 'PRIMARY' ? primaryEmail.value : summaryEmail.value;
  if (!email) {
    tip.value = '请输入邮箱地址';
    return;
  }
  try {
    await userApi.sendEmailCode({ email, emailType: type, scene: 'BIND' });
    tip.value = '验证码已发送，请前往邮箱查看';
  } catch (e: any) {
    tip.value = e.message || '发送失败';
  }
}

async function verify(type: 'PRIMARY' | 'SUMMARY_ONLY') {
  const email = type === 'PRIMARY' ? primaryEmail.value : summaryEmail.value;
  const code = type === 'PRIMARY' ? primaryCode.value : summaryCode.value;
  if (!email || !code) {
    tip.value = '请输入邮箱和验证码';
    return;
  }
  try {
    await userApi.verifyEmail({ email, emailType: type, code, scene: 'BIND', bindSource: 'MY_PAGE' });
    tip.value = type === 'PRIMARY' ? '主邮箱绑定成功' : '课后总结邮箱设置成功';
    await load();
  } catch (e: any) {
    tip.value = e.message || '验证失败';
  }
}

async function deleteSummary() {
  try {
    await userApi.deleteSummaryEmail();
    tip.value = '已移除课后总结邮箱';
    await load();
  } catch (e: any) {
    tip.value = e.message || '移除失败';
  }
}

onMounted(load);
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #f2fbf6, #fff7e8);
}

.hero,
.card {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 36px rgba(31, 35, 41, 0.08);
  padding: 20px;
  margin-bottom: 16px;
}

.eyebrow,
.title,
.desc,
.copy,
.mail,
.tag,
.state {
  display: block;
}

.eyebrow {
  color: #00a873;
  font-size: 12px;
  letter-spacing: 2px;
}

.title {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 900;
}

.desc,
.copy {
  margin-top: 8px;
  color: #68736d;
  line-height: 1.6;
  font-size: 13px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.tag {
  width: fit-content;
  padding: 4px 8px;
  border-radius: 999px;
  background: #e8f8ef;
  color: #008f61;
  font-size: 12px;
}

.warm-tag {
  background: #fff1d7;
  color: #a56100;
}

.mail {
  margin-top: 8px;
  font-weight: 900;
}

.state {
  color: #00a873;
  font-size: 12px;
}

.input {
  margin-top: 12px;
  height: 44px;
  border-radius: 12px;
  background: #f7faf8;
  padding: 0 12px;
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
  border-radius: 12px;
  background: #e8f8ef;
  color: #008f61;
  font-weight: 800;
}

.solid {
  width: 100%;
  background: #00b578;
  color: #fff;
}

.warm-btn {
  color: #a56100;
  background: #fff1d7;
}

.warm-solid {
  background: #e89017;
}

.danger {
  margin-top: 10px;
  background: transparent;
  color: #b33a2d;
}

.tip {
  margin-bottom: 12px;
  border-radius: 14px;
  padding: 12px;
  background: #fff;
  color: #00a873;
}
</style>
