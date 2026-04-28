<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="eyebrow">我的需求</text>
        <text class="title">管理正在招募的老师</text>
        <text class="subtitle">修改预算、关闭招募，或继续补充孩子的学习情况。</text>
      </view>
      <button class="hero-btn" @click="goToPost">发布</button>
    </view>

    <AppStateCard
      v-if="error"
      title="需求加载失败"
      :description="error"
      action-text="重试"
      variant="error"
      @action="reload"
    />

    <AppStateCard
      v-else-if="jobList.length === 0 && !loading"
      title="还没有发布需求"
      description="发布后，老师可以先申请，确认后再进入支付与聊天流程。"
      action-text="发布第一个需求"
      variant="soft"
      @action="goToPost"
    />

    <view v-else class="list">
      <view v-for="job in jobList" :key="job.id" class="job-card" @click="goToDetail(job.id)">
        <view class="card-head">
          <view>
            <text class="job-title">{{ job.title || `${job.subjectName || ''}家教需求` }}</text>
            <text class="job-sub">{{ job.subjectName || '科目待定' }} · {{ modeText(job.classMode) }}</text>
          </view>
          <text class="status" :class="{ closed: isClosed(job) }">{{ isClosed(job) ? '已关闭' : '招募中' }}</text>
        </view>
        <view class="info-grid">
          <view>
            <text class="k">预算</text>
            <text class="v">{{ budgetText(job) }}</text>
          </view>
          <view>
            <text class="k">频次</text>
            <text class="v">{{ job.frequencyPerWeek || 1 }} 次/周</text>
          </view>
        </view>
        <text class="desc">{{ job.description || '暂无描述' }}</text>
        <view class="ops" @click.stop>
          <button class="op ghost" @click="goToEdit(job.id)">编辑</button>
          <button class="op" :class="{ danger: !isClosed(job) }" :disabled="busyId === job.id" @click="toggleStatus(job)">
            {{ busyId === job.id ? '处理中' : isClosed(job) ? '重新打开' : '关闭需求' }}
          </button>
        </view>
      </view>
    </view>

    <view class="load-more">
      <text v-if="loading">加载中...</text>
      <text v-else-if="isLast && jobList.length > 0">没有更多了</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app';
import { jobsApi } from '@/api/jobs';
import { ensureStudentMode } from '@/utils/studentGuard';
import AppStateCard from '@/components/AppStateCard.vue';

const jobList = ref<any[]>([]);
const cursor = ref<number | null>(null);
const isLast = ref(false);
const loading = ref(false);
const error = ref('');
const busyId = ref<number | null>(null);

async function fetchJobs(reset = false) {
  if (loading.value || (!reset && isLast.value)) return;
  loading.value = true;
  error.value = '';
  try {
    if (reset) {
      cursor.value = null;
      isLast.value = false;
      jobList.value = [];
    }
    const res: any = await jobsApi.mineDemands({ pageSize: 10, cursor: cursor.value });
    const rows = res?.items || res?.list || [];
    jobList.value = reset ? rows : [...jobList.value, ...rows];
    cursor.value = res?.cursor ?? res?.nextCursor ?? null;
    isLast.value = !!res?.isLast || rows.length === 0;
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载需求失败';
  } finally {
    loading.value = false;
  }
}

function isClosed(job: any) {
  return Number(job.status) !== 1 || String(job.bizStatus || '').toUpperCase().includes('CLOSED');
}

function modeText(mode?: string) {
  const s = String(mode || '').toLowerCase();
  if (s === 'online') return '线上';
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '授课方式待定';
}

function budgetText(job: any) {
  if (job.budgetMin != null && job.budgetMax != null) return `¥${job.budgetMin}-${job.budgetMax}/小时`;
  if (job.budgetMin != null) return `¥${job.budgetMin}/小时起`;
  if (job.budgetMax != null) return `≤¥${job.budgetMax}/小时`;
  return '面议';
}

async function toggleStatus(job: any) {
  if (!job?.id || busyId.value) return;
  busyId.value = job.id;
  try {
    const next = isClosed(job) ? 1 : 0;
    await jobsApi.updateDemand(job.id, { status: next });
    job.status = next;
    uni.showToast({ title: next === 1 ? '已重新打开' : '已关闭', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || e?.msg || '操作失败', icon: 'none' });
  } finally {
    busyId.value = null;
  }
}

function goToDetail(id: number) {
  uni.navigateTo({ url: `/pages/job/detail?id=${id}` });
}

function goToEdit(id: number) {
  uni.navigateTo({ url: `/pages/post/index?id=${id}` });
}

function goToPost() {
  uni.navigateTo({ url: '/pages/post/index' });
}

async function reload() {
  await fetchJobs(true);
}

onShow(() => {
  if (!ensureStudentMode('学生/家长身份可以管理已发布需求。')) return;
  void fetchJobs(true);
});

onReachBottom(() => {
  void fetchJobs(false);
});

onPullDownRefresh(async () => {
  await fetchJobs(true);
  uni.stopPullDownRefresh();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px;
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #102326 0%, #14524d 70%, #0f8f86 100%);
  box-shadow: 0 18px 38px rgba(16, 35, 38, 0.2);
}

.eyebrow,
.title,
.subtitle,
.job-title,
.job-sub,
.k,
.v,
.desc,
.state-title,
.state-desc {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.72;
  margin-bottom: 6px;
}

.title {
  font-size: 22px;
  font-weight: 900;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.82;
}

.hero-btn {
  width: 62px;
  height: 38px;
  line-height: 38px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  flex-shrink: 0;
}

.list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.job-card {
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 12px 28px rgba(18, 37, 41, 0.06);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.job-title {
  color: #142326;
  font-size: 17px;
  font-weight: 900;
  margin-bottom: 5px;
}

.job-sub {
  color: #7a838c;
  font-size: 12px;
}

.status {
  align-self: flex-start;
  padding: 5px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: #edf7f5;
  font-size: 12px;
  font-weight: 900;
  flex-shrink: 0;
}

.status.closed {
  color: #7a838c;
  background: #edf0f1;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 14px 0;
}

.info-grid > view {
  padding: 10px;
  border-radius: 12px;
  background: #f4f7f7;
}

.k {
  color: #87909a;
  font-size: 11px;
  margin-bottom: 4px;
}

.v {
  color: #172326;
  font-size: 14px;
  font-weight: 900;
}

.desc {
  color: #65717a;
  font-size: 13px;
  line-height: 1.6;
  max-height: 42px;
  overflow: hidden;
}

.ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(18, 37, 41, 0.08);
}

.op,
.mini-btn {
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 13px;
}

.op {
  height: 34px;
  line-height: 34px;
  padding: 0 14px;
}

.op.ghost {
  color: #3b4a51;
  background: #eef2f3;
}

.op.danger {
  background: #c24141;
}

.state {
  margin-top: 14px;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  text-align: center;
  color: #66727c;
}

.state-title {
  color: #142326;
  font-size: 17px;
  font-weight: 900;
  margin-bottom: 8px;
}

.state-desc {
  color: #7a838c;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 14px;
}

.mini-btn {
  display: inline-block;
  height: 34px;
  line-height: 34px;
  padding: 0 18px;
}

.error {
  color: #c24141;
  background: #fff0f0;
}

.load-more {
  padding: 18px 0;
  text-align: center;
  color: #9aa3aa;
  font-size: 12px;
}
</style>
