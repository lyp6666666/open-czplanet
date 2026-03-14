<template>
  <view class="page">
    <view class="card">
      <view v-if="status === 'PENDING'" class="status">
        <icon type="waiting" size="64" color="#00bebd"></icon>
        <text class="title">审核中</text>
        <text class="desc">你的入驻资料已提交，正在审核中，请耐心等待。</text>
        <u-button type="primary" color="#00bebd" shape="circle" @click="goHome">返回首页</u-button>
      </view>

      <view v-else-if="status === 'REJECTED'" class="status">
        <icon type="warn" size="64" color="#ff4d4f"></icon>
        <text class="title">审核未通过</text>
        <text class="desc">{{ reasonText }}</text>
        <view class="btn-row">
          <u-button type="primary" color="#00bebd" shape="circle" @click="reapply">去完善资料</u-button>
          <u-button shape="circle" @click="goHome">返回首页</u-button>
        </view>
      </view>

      <view v-else-if="status === 'APPROVED'" class="status">
        <icon type="success" size="64" color="#00bebd"></icon>
        <text class="title">审核通过</text>
        <text class="desc">欢迎加入，快去浏览需求并发起沟通吧。</text>
        <u-button type="primary" color="#00bebd" shape="circle" @click="goHome">去接单</u-button>
      </view>

      <view v-else class="status">
        <icon type="info" size="64" color="#646a73"></icon>
        <text class="title">未提交入驻</text>
        <text class="desc">完善资料后即可开通教师功能。</text>
        <u-button type="primary" color="#00bebd" shape="circle" @click="reapply">去入驻</u-button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const status = ref('');
const reason = ref('');

onLoad((options: any) => {
    status.value = options.status || 'PENDING';
    reason.value = options.reason || '';
});

const reasonText = computed(() => {
    const s = String(reason.value || '').trim();
    if (s) return `原因：${s}`;
    return '原因：资料不完整或认证未通过，请检查后重新提交。';
});

const goHome = () => {
    uni.reLaunch({ url: '/pages/home/index' });
};

const reapply = () => {
    uni.navigateTo({ url: '/pages/tutor/onboarding/index' });
};
</script>

<style lang="scss" scoped>
.page {
    min-height: 100vh;
    background: var(--bg);
    padding: 16px;
    display: flex;
    align-items: center;
}

.card {
    width: 100%;
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 16px;
    box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
    padding: 24px 18px;
}

.status {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 10px;
}

.title {
    font-size: 18px;
    font-weight: 900;
    color: var(--text);
    margin-top: 4px;
}

.desc {
    font-size: 13px;
    color: var(--muted);
    line-height: 1.7;
    margin-bottom: 8px;
}

.btn-row {
    width: 100%;
    display: flex;
    gap: 10px;
}
</style>
