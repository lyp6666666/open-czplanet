<template>
  <view class="page">
    <AppStateCard
      v-if="loading"
      title="正在接入课堂"
      description="正在整理会话令牌、课堂地址和当前登录身份。"
      variant="soft"
    />
    <AppStateCard
      v-else-if="error"
      title="进入课堂失败"
      :description="error"
      action-text="重试"
      variant="error"
      @action="openClassroom"
    />

    <template v-else-if="bridgeUrl">
      <view class="hero">
        <text class="eyebrow">课堂承接</text>
        <text class="title">{{ courseTitle || '线上课堂' }}</text>
        <text class="subtitle">{{ peerName || '对方' }} · 已准备跳转到真实课堂容器</text>
      </view>

      <view class="panel">
        <view class="row">
          <text class="k">课程 ID</text>
          <text class="v">{{ courseId }}</text>
        </view>
        <view class="row">
          <text class="k">会话 ID</text>
          <text class="v">{{ sessionId || '-' }}</text>
        </view>
        <view class="row">
          <text class="k">课堂地址</text>
          <text class="v mono">{{ shortUrl }}</text>
        </view>
      </view>

      <view class="hint-box">
        <text class="hint-copy">当前会通过小程序 `web-view` 打开现有 H5 课堂页，并把真实登录态与课堂上下文一并带过去。</text>
      </view>

      <view class="actions">
        <button class="action primary" @click="goWebView">立即进入课堂</button>
        <button class="action ghost" @click="copyUrl">复制课堂地址</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import AppStateCard from '@/components/AppStateCard.vue';

const loading = ref(true);
const error = ref('');
const bridgeUrl = ref('');
const courseId = ref<number | null>(null);
const sessionId = ref<number | null>(null);
const courseTitle = ref('');
const peerName = ref('');

const shortUrl = computed(() => {
  if (!bridgeUrl.value) return '';
  if (bridgeUrl.value.length <= 54) return bridgeUrl.value;
  return `${bridgeUrl.value.slice(0, 54)}...`;
});

function openClassroom() {
  loading.value = true;
  error.value = '';
  try {
    if (!bridgeUrl.value) throw new Error('缺少课堂地址');
    goWebView();
  } catch (e: any) {
    error.value = e?.message || '打开课堂失败';
  } finally {
    loading.value = false;
  }
}

function goWebView() {
  if (!bridgeUrl.value) return;
  uni.redirectTo({
    url:
      `/pages/live/webview?url=${encodeURIComponent(bridgeUrl.value)}` +
      `&title=${encodeURIComponent(courseTitle.value || '线上课堂')}` +
      `&courseId=${courseId.value || ''}` +
      `&sessionId=${sessionId.value || ''}`,
  });
}

function copyUrl() {
  if (!bridgeUrl.value) return;
  uni.setClipboardData({
    data: bridgeUrl.value,
    success: () => {
      uni.showToast({ title: '课堂地址已复制', icon: 'none' });
    },
  });
}

onLoad((options: any) => {
  bridgeUrl.value = String(options?.url || '').trim();
  courseTitle.value = String(options?.courseTitle || '').trim();
  peerName.value = String(options?.peerName || '').trim();
  const cid = Number(options?.courseId);
  const sid = Number(options?.sessionId);
  courseId.value = Number.isFinite(cid) ? cid : null;
  sessionId.value = Number.isFinite(sid) ? sid : null;

  if (!bridgeUrl.value) {
    error.value = '缺少课堂桥接地址';
    loading.value = false;
    return;
  }
  loading.value = false;
});
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 16px;
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #122529 0%, #2f665f 70%, #d49f52 140%);
  box-shadow: 0 18px 38px rgba(18, 37, 41, 0.2);
}

.eyebrow,
.title,
.subtitle,
.k,
.v,
.hint-copy {
  display: block;
}

.eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.72;
}

.title {
  font-size: 22px;
  line-height: 1.25;
  font-weight: 900;
}

.subtitle {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.82;
}

.panel,
.hint-box {
  margin-top: 12px;
  padding: 15px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 12px 28px rgba(18, 37, 41, 0.06);
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(18, 37, 41, 0.07);
}

.row:last-child {
  border-bottom: none;
}

.k {
  color: #82909a;
  font-size: 13px;
}

.v {
  color: #162326;
  font-size: 14px;
  font-weight: 800;
  text-align: right;
  word-break: break-all;
}

.mono {
  font-family: monospace;
}

.hint-copy {
  color: #65717a;
  font-size: 13px;
  line-height: 1.7;
}

.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  gap: 10px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(18, 37, 41, 0.08);
  box-sizing: border-box;
}

.action {
  height: 44px;
  line-height: 44px;
  border: 0;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 900;
}

.action::after {
  border: 0;
}

.action.primary {
  color: #fff;
  background: #0f766e;
}

.action.ghost {
  color: #33424a;
  background: #eef2f3;
}
</style>
