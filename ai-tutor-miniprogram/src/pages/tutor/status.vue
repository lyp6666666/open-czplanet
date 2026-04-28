<template>
  <view class="page">
    <view class="hero">
      <view class="status-mark" :class="statusClass">{{ statusIcon }}</view>
      <text class="title">{{ titleText }}</text>
      <text class="desc">{{ descText }}</text>
    </view>

    <view class="panel">
      <view class="row">
        <text class="label">基础资料</text>
        <text class="value">{{ basicText }}</text>
      </view>
      <view class="row">
        <text class="label">教学履历</text>
        <text class="value">{{ resumeText }}</text>
      </view>
      <view class="row">
        <text class="label">身份认证</text>
        <text class="value">{{ verifyText(userStore.userInfo?.teacherProfile?.realnameVerifyStatus) }}</text>
      </view>
      <view class="row">
        <text class="label">学历认证</text>
        <text class="value">{{ verifyText(userStore.userInfo?.teacherProfile?.eduVerifyStatus) }}</text>
      </view>
    </view>

    <view v-if="currentStatus === 'REJECTED'" class="reason">
      <text class="reason-title">未通过原因</text>
      <text class="reason-desc">{{ reasonText }}</text>
    </view>

    <view class="actions">
      <button v-if="showEdit" class="action-btn primary" @click="reapply">{{ editText }}</button>
      <button v-if="currentStatus === 'APPROVED'" class="action-btn primary" @click="goTutorHome">去需求广场</button>
      <button class="action-btn secondary" @click="goHome">返回首页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import type { TutorStatus } from '@/types/domain';

const userStore = useUserStore();
const routeStatus = ref<TutorStatus | ''>('');
const routeReason = ref('');

onLoad((options: any) => {
  routeStatus.value = normalizeStatus(options.status);
  routeReason.value = decodeURIComponent(String(options.reason || ''));
});

onShow(() => {
  if (userStore.isLoggedIn) {
    void userStore.refreshUserInfo().catch(() => undefined);
  }
});

const currentStatus = computed<TutorStatus>(() => {
  if (userStore.tutorStatus && userStore.tutorStatus !== 'NONE') return userStore.tutorStatus;
  return routeStatus.value || 'NONE';
});

const statusClass = computed(() => {
  if (currentStatus.value === 'APPROVED') return 'ok';
  if (currentStatus.value === 'REJECTED') return 'bad';
  if (currentStatus.value === 'PENDING') return 'pending';
  return 'todo';
});

const statusIcon = computed(() => {
  if (currentStatus.value === 'APPROVED') return '✓';
  if (currentStatus.value === 'REJECTED') return '!';
  if (currentStatus.value === 'PENDING') return '…';
  return '+';
});

const titleText = computed(() => {
  if (currentStatus.value === 'APPROVED') return '审核通过';
  if (currentStatus.value === 'REJECTED') return '审核未通过';
  if (currentStatus.value === 'PENDING') return '资料审核中';
  if (currentStatus.value === 'INCOMPLETE') return '资料待完善';
  return '还未提交入驻';
});

const descText = computed(() => {
  if (currentStatus.value === 'APPROVED') return '教师功能已开放，可以浏览需求、收藏需求并发起沟通。';
  if (currentStatus.value === 'REJECTED') return '请根据审核意见调整资料后重新提交。';
  if (currentStatus.value === 'PENDING') return '平台正在审核你的身份与学历资料，审核通过后会开放接单能力。';
  return '完成基础资料、授课信息和认证材料后，即可提交审核。';
});

const reasonText = computed(() => {
  return routeReason.value || userStore.tutorRejectReason || '资料不完整或认证未通过，请检查后重新提交。';
});

const showEdit = computed(() => ['NONE', 'INCOMPLETE', 'REJECTED'].includes(currentStatus.value));
const editText = computed(() => (currentStatus.value === 'REJECTED' ? '重新完善资料' : '去入驻'));

const profile = computed(() => userStore.userInfo?.teacherProfile);
const basicText = computed(() => Number(profile.value?.basicCompleted || 0) === 1 ? '已完成' : '待完善');
const resumeText = computed(() => Number(profile.value?.resumeCompleted || 0) === 1 ? '已完成' : '待完善');

function normalizeStatus(v: unknown): TutorStatus | '' {
  const s = String(v || '').trim().toUpperCase();
  if (['NONE', 'INCOMPLETE', 'PENDING', 'APPROVED', 'REJECTED'].includes(s)) return s as TutorStatus;
  return '';
}

function verifyText(v: unknown) {
  const n = Number(v || 0);
  if (n === 2) return '已通过';
  if (n === 3) return '未通过';
  if (n === 1) return '审核中';
  return '未提交';
}

function goHome() {
  uni.reLaunch({ url: '/pages/home/index' });
}

function goTutorHome() {
  userStore.setCurrentRole('tutor');
  uni.reLaunch({ url: '/pages/home/index' });
}

function reapply() {
  uni.navigateTo({ url: '/pages/tutor/onboarding/index' });
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero {
  background: #ffffff;
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 16px;
  padding: 28px 18px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
}

.status-mark {
  width: 76px;
  height: 76px;
  border-radius: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 900;
  &.ok {
    background: rgba(0, 190, 189, 0.12);
    color: #00a7a6;
  }
  &.pending {
    background: rgba(245, 158, 11, 0.14);
    color: #b45309;
  }
  &.bad {
    background: rgba(255, 77, 79, 0.12);
    color: #d9363e;
  }
  &.todo {
    background: rgba(31, 35, 41, 0.06);
    color: var(--muted);
  }
}

.title {
  font-size: 21px;
  font-weight: 900;
  color: var(--text);
}

.desc {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.7;
}

.panel,
.reason {
  background: #ffffff;
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 16px;
  padding: 14px 16px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.06);
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid rgba(31, 35, 41, 0.08);
  &:first-child {
    border-top: 0;
  }
}

.label {
  font-size: 13px;
  color: var(--muted);
  font-weight: 800;
}

.value {
  font-size: 13px;
  color: var(--text);
  font-weight: 900;
}

.reason {
  background: rgba(255, 77, 79, 0.06);
  border-color: rgba(255, 77, 79, 0.16);
}

.reason-title {
  display: block;
  font-size: 14px;
  font-weight: 900;
  color: #b4232a;
  margin-bottom: 8px;
}

.reason-desc {
  font-size: 13px;
  line-height: 1.7;
  color: #7f1d1d;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.action-btn {
  height: 48px;
  border-radius: 999px;
  border: 0;
  font-size: 15px;
  font-weight: 900;
  line-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  &::after {
    border: 0;
  }
  &.primary {
    background: #00bebd;
    color: #ffffff;
    box-shadow: 0 10px 20px rgba(0, 190, 189, 0.20);
  }
  &.secondary {
    background: #ffffff;
    color: var(--text);
    border: 1px solid rgba(31, 35, 41, 0.12);
  }
}
</style>
