<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="login-panel">
      <text class="login-title">登录后查看合作</text>
      <text class="login-desc">试课、正式课表和退费进度都会汇总在这里。</text>
      <button class="primary-btn" @click="goLogin">去登录</button>
    </view>

    <template v-else>
      <view class="hero">
        <view>
          <text class="eyebrow">我的合作</text>
          <text class="title">{{ roleLabel }}工作台</text>
          <text class="subtitle">从沟通到试课，再到正式课表，关键进展一屏掌握。</text>
        </view>
        <view class="hero-stat">
          <text class="num">{{ activeCount }}</text>
          <text class="cap">进行中</text>
        </view>
      </view>

      <view class="filters">
        <view v-for="it in filters" :key="it.key" class="filter" :class="{ active: filter === it.key }" @click="filter = it.key">
          {{ it.label }}
        </view>
      </view>

      <view v-if="loading" class="state">加载中...</view>
      <view v-else-if="error" class="state error">
        <text>{{ error }}</text>
        <button class="mini-btn" @click="load">重试</button>
      </view>
      <view v-else-if="visibleList.length === 0" class="state">
        <text class="state-title">暂无课程合作</text>
        <text class="state-desc">聊天中确认试课合作后，会自动进入课程流程。</text>
      </view>

      <view v-else class="list">
        <view v-for="it in visibleList" :key="it.courseId" class="course-card" @click="openCourse(it.courseId)">
          <view class="card-head">
            <view>
              <text class="course-title">{{ titleOf(it) }}</text>
              <text class="course-sub">{{ participantLabel(it) }} · {{ modeText(it.teachingMode) }}</text>
            </view>
            <text class="badge" :class="stageTone(it.status)">{{ stageText(it.status) }}</text>
          </view>

          <view class="progress">
            <view v-for="step in stepsOf(it.status)" :key="step.key" class="step" :class="step.state">
              <view class="dot"></view>
              <text>{{ step.label }}</text>
            </view>
          </view>

          <view class="meta-grid">
            <view>
              <text class="k">课时费</text>
              <text class="v">{{ it.lessonPrice || it.latestProposal?.pricePerHour || '待确认' }}</text>
            </view>
            <view>
              <text class="k">频次</text>
              <text class="v">{{ it.frequencyPerWeek ? `${it.frequencyPerWeek} 次/周` : '待确认' }}</text>
            </view>
          </view>

          <view v-if="it.latestRefund" class="notice">
            <text>退费进度：{{ refundStatusText(it.latestRefund.status) }}</text>
          </view>
          <view v-else-if="it.aiPreview" class="notice soft">
            <text>{{ it.aiPreview }}</text>
          </view>

          <view class="card-foot">
            <text>{{ nextHint(it) }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app';
import { courseApi, type CourseItem } from '@/api/course';
import { useUserStore } from '@/stores/user';

type FilterKey = 'ALL' | 'ACTIVE' | 'TRIAL' | 'TEACHING' | 'DONE';

const userStore = useUserStore();
const list = ref<CourseItem[]>([]);
const loading = ref(false);
const error = ref('');
const filter = ref<FilterKey>('ALL');

const filters: Array<{ key: FilterKey; label: string }> = [
  { key: 'ALL', label: '全部' },
  { key: 'ACTIVE', label: '进行中' },
  { key: 'TRIAL', label: '试课' },
  { key: 'TEACHING', label: '正式课' },
  { key: 'DONE', label: '已结束' }
];

const role = computed<'TEACHER' | 'STUDENT'>(() => (userStore.currentRole === 'tutor' ? 'TEACHER' : 'STUDENT'));
const roleLabel = computed(() => (role.value === 'TEACHER' ? '教师' : '学生'));
const activeCount = computed(() => list.value.filter((it) => isActiveStatus(it.status)).length);

const visibleList = computed(() => {
  if (filter.value === 'ALL') return list.value;
  return list.value.filter((it) => {
    const s = normalizeStatus(it.status);
    if (filter.value === 'ACTIVE') return isActiveStatus(s);
    if (filter.value === 'TRIAL') return s.includes('TRIAL');
    if (filter.value === 'TEACHING') return s === 'TEACHING';
    return ['FINISHED', 'REFUNDED', 'TRIAL_FAILED'].includes(s);
  });
});

function normalizeStatus(status?: string | null) {
  return String(status || '').trim().toUpperCase();
}

function isActiveStatus(status?: string | null) {
  const s = normalizeStatus(status);
  return !['FINISHED', 'REFUNDED', 'TRIAL_FAILED', 'ARCHIVED'].includes(s);
}

function stageText(status?: string | null) {
  const s = normalizeStatus(status);
  if (s === 'WAIT_PAY') return '待支付';
  if (s === 'COMMUNICATING') return '沟通中';
  if (s === 'TRIALING') return '试课中';
  if (s === 'TRIAL_WAIT_STUDENT_DECISION') return '待确认试课';
  if (s === 'TRIAL_WAIT_WEEKLY_SCHEDULE') return '待排正式课';
  if (s === 'TRIAL_FAILED') return '试课未通过';
  if (s === 'TEACHING') return '正式授课';
  if (s === 'FINISHED') return '已完成';
  if (s.includes('REFUND')) return '退费处理中';
  return s || '未知';
}

function stageTone(status?: string | null) {
  const s = normalizeStatus(status);
  return {
    amber: s === 'WAIT_PAY' || s.includes('WAIT'),
    blue: s === 'TRIALING' || s === 'COMMUNICATING',
    green: s === 'TEACHING' || s === 'FINISHED',
    red: s.includes('FAILED') || s.includes('REFUND')
  };
}

function titleOf(it: CourseItem) {
  return it.courseName || (it.teachingMode === 'ONLINE' ? '线上家教合作' : '家教课程合作');
}

function participantLabel(it: CourseItem) {
  const uid = role.value === 'TEACHER' ? it.studentUid : it.teacherUid;
  return role.value === 'TEACHER' ? `学生 ${uid}` : `老师 ${uid}`;
}

function modeText(mode?: string | null) {
  if (mode === 'ONLINE') return '线上';
  if (mode === 'OFFLINE') return '线下';
  return '授课方式待定';
}

function refundStatusText(status?: string | null) {
  const s = normalizeStatus(status);
  if (s === 'PENDING') return '审核中';
  if (s === 'APPROVED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  if (s === 'PAID') return '已退款';
  return s || '待确认';
}

function stepsOf(status?: string | null) {
  const s = normalizeStatus(status);
  const order = ['apply', 'pay', 'trial', 'schedule'];
  let index = 0;
  if (!['WAIT_PAY'].includes(s)) index = 1;
  if (s.includes('TRIAL') || s === 'TEACHING' || s === 'FINISHED') index = 2;
  if (s === 'TEACHING' || s === 'FINISHED') index = 3;
  return [
    { key: 'apply', label: '申请', state: stateOf(0, index, order.length, s) },
    { key: 'pay', label: '解锁', state: stateOf(1, index, order.length, s) },
    { key: 'trial', label: '试课', state: stateOf(2, index, order.length, s) },
    { key: 'schedule', label: '课表', state: stateOf(3, index, order.length, s) }
  ];
}

function stateOf(i: number, current: number, total: number, status: string) {
  if (status.includes('FAILED') || status.includes('REFUND')) return i <= current ? 'alert' : 'todo';
  if (i < current || (current === total - 1 && i === current)) return 'done';
  if (i === current) return 'current';
  return 'todo';
}

function nextHint(it: CourseItem) {
  const s = normalizeStatus(it.status);
  if (s === 'WAIT_PAY') return '进入详情完成信息费支付';
  if (s === 'COMMUNICATING') return '去聊天确认试课时间';
  if (s === 'TRIAL_WAIT_STUDENT_DECISION') return '学生需要确认试课结果';
  if (s === 'TRIAL_WAIT_WEEKLY_SCHEDULE') return '学生需要提交正式课表';
  if (s === 'TEACHING') return '查看课节和正式课表';
  if (s.includes('REFUND')) return '查看退费处理进度';
  return '查看课程详情';
}

async function load() {
  if (!userStore.isLoggedIn) return;
  loading.value = true;
  error.value = '';
  try {
    list.value = await courseApi.myCourses({ role: role.value, size: 50 });
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载课程失败';
  } finally {
    loading.value = false;
  }
}

function openCourse(courseId: number) {
  uni.navigateTo({ url: `/pages/course/detail?id=${courseId}` });
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

onShow(() => {
  void load();
});

onPullDownRefresh(async () => {
  await load();
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
  gap: 16px;
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #102326 0%, #14524d 62%, #18a199 100%);
  box-shadow: 0 18px 38px rgba(16, 35, 38, 0.2);
}

.eyebrow,
.title,
.subtitle,
.num,
.cap,
.course-title,
.course-sub,
.k,
.v,
.state-title,
.state-desc {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 6px;
}

.title {
  font-size: 23px;
  font-weight: 900;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.82;
}

.hero-stat {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.num {
  font-size: 24px;
  font-weight: 900;
}

.cap {
  font-size: 11px;
  opacity: 0.75;
}

.filters {
  display: flex;
  gap: 8px;
  margin: 14px 0;
  overflow-x: auto;
}

.filter {
  padding: 9px 13px;
  border-radius: 999px;
  background: #fff;
  color: #59616b;
  font-size: 13px;
  white-space: nowrap;
  border: 1px solid rgba(15, 35, 38, 0.08);
}

.filter.active {
  color: #fff;
  background: #0f766e;
  border-color: #0f766e;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-card {
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(15, 35, 38, 0.08);
  box-shadow: 0 12px 28px rgba(15, 35, 38, 0.06);
}

.card-head,
.card-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.course-title {
  font-size: 17px;
  font-weight: 900;
  color: #142326;
  margin-bottom: 5px;
}

.course-sub {
  font-size: 12px;
  color: #7a838c;
}

.badge {
  flex-shrink: 0;
  padding: 5px 9px;
  border-radius: 999px;
  background: #eef2f3;
  color: #51606b;
  font-size: 12px;
  font-weight: 800;
}

.badge.amber { background: #fff4dc; color: #b06b00; }
.badge.blue { background: #e8f4ff; color: #1469b8; }
.badge.green { background: #e8f7ee; color: #168447; }
.badge.red { background: #fff0f0; color: #c24141; }

.progress {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin: 16px 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  color: #a0a7ae;
  font-size: 11px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d8dee2;
}

.step.done,
.step.current {
  color: #0f766e;
  font-weight: 800;
}

.step.done .dot,
.step.current .dot {
  background: #0f766e;
}

.step.alert {
  color: #c24141;
  font-weight: 800;
}

.step.alert .dot {
  background: #c24141;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.meta-grid > view {
  padding: 10px;
  border-radius: 12px;
  background: #f6f8f8;
}

.k {
  color: #87909a;
  font-size: 11px;
  margin-bottom: 4px;
}

.v {
  color: #172326;
  font-size: 14px;
  font-weight: 800;
}

.notice {
  margin-top: 12px;
  padding: 10px;
  border-radius: 12px;
  background: #fff2ee;
  color: #a63f24;
  font-size: 12px;
  line-height: 1.5;
}

.notice.soft {
  background: #eef8f7;
  color: #236b66;
}

.card-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(15, 35, 38, 0.08);
  color: #65717a;
  font-size: 13px;
}

.arrow {
  color: #0f766e;
  font-size: 20px;
  line-height: 1;
}

.state,
.login-panel {
  margin-top: 18px;
  padding: 22px;
  border-radius: 18px;
  background: #fff;
  color: #707982;
  text-align: center;
  font-size: 14px;
}

.state-title,
.login-title {
  color: #152326;
  font-size: 17px;
  font-weight: 900;
  margin-bottom: 8px;
}

.state-desc,
.login-desc {
  display: block;
  color: #7c858f;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 14px;
}

.primary-btn,
.mini-btn {
  border: 0;
  color: #fff;
  background: #0f766e;
  border-radius: 999px;
  font-size: 14px;
}

.primary-btn {
  height: 42px;
  line-height: 42px;
}

.mini-btn {
  display: inline-block;
  margin-top: 12px;
  padding: 0 18px;
  height: 34px;
  line-height: 34px;
}
</style>
