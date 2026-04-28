<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="empty-state">
      <text class="empty-title">登录后查看需求广场</text>
      <text class="empty-desc">登录并完成家教入驻后，可以浏览需求、收藏需求并发起沟通。</text>
      <button class="empty-btn" @click="goLogin">去登录</button>
    </view>

    <view v-else-if="userStore.tutorStatus !== 'APPROVED'" class="guard-card">
      <view class="guard-icon">{{ guardIcon }}</view>
      <text class="guard-title">{{ guardTitle }}</text>
      <text class="guard-desc">{{ guardDesc }}</text>
      <button class="guard-btn primary" @click="handleGuardAction">{{ guardActionText }}</button>
      <button class="guard-btn secondary" @click="switchStudent">先看老师</button>
    </view>

    <view v-else>
      <view class="hero-card">
        <text class="hero-title">需求广场</text>
        <text class="hero-desc">先筛出值得跟进的需求，再进入详情查看预算、时间安排和发单人信息。</text>
        <view class="search-box">
          <text class="search-icon">⌕</text>
          <input class="search-input" v-model="keyword" type="text" placeholder="搜索科目、城市或关键词" @confirm="refreshList" />
          <text v-if="keyword" class="clear-text" @click="clearKeyword">清空</text>
        </view>
      </view>

      <view class="filter-card">
        <scroll-view class="chip-row" scroll-x>
          <view class="chip-track">
            <view class="chip" :class="{ active: classModeFilter === '' }" @click="setClassMode('')">全部方式</view>
            <view class="chip" :class="{ active: classModeFilter === 'online' }" @click="setClassMode('online')">线上</view>
            <view class="chip" :class="{ active: classModeFilter === 'offline' }" @click="setClassMode('offline')">线下</view>
            <view class="chip" :class="{ active: classModeFilter === 'both' }" @click="setClassMode('both')">线上/线下</view>
          </view>
        </scroll-view>

        <scroll-view class="chip-row secondary" scroll-x>
          <view class="chip-track">
            <view class="chip secondary-chip" :class="{ active: selectedSubject === '' }" @click="selectSubject('')">全部科目</view>
            <view
              v-for="subject in SUBJECT_PRESETS"
              :key="subject"
              class="chip secondary-chip"
              :class="{ active: selectedSubject === subject }"
              @click="selectSubject(subject)"
            >
              {{ subject }}
            </view>
          </view>
        </scroll-view>

        <view class="budget-row">
          <view class="budget-box">
            <text class="budget-label">城市</text>
            <input v-model="cityInput" class="budget-field" placeholder="如：上海" />
          </view>
        </view>

        <view class="budget-row budget-range-row">
          <view class="budget-box">
            <text class="budget-label">最低预算</text>
            <input v-model="budgetMinInput" type="number" class="budget-field" placeholder="不限" />
          </view>
          <text class="budget-sep">-</text>
          <view class="budget-box">
            <text class="budget-label">最高预算</text>
            <input v-model="budgetMaxInput" type="number" class="budget-field" placeholder="不限" />
          </view>
        </view>

        <view class="filter-actions">
          <button class="filter-btn ghost" @click="resetFilters">重置</button>
          <button class="filter-btn primary" @click="applyFilters">应用筛选</button>
        </view>
      </view>

      <view class="section-head">
        <view>
          <text class="section-title">最新需求</text>
          <text class="section-sub">{{ resultSummary }}</text>
        </view>
        <text class="refresh-link" @click="refreshList">刷新</text>
      </view>

      <view v-if="loading && !jobList.length" class="skeleton-list">
        <view v-for="idx in 3" :key="idx" class="skeleton-card"></view>
      </view>

      <view v-else-if="jobList.length" class="job-list">
        <view v-for="job in jobList" :key="job.id" class="job-card" @click="goToDetail(job.id)">
          <view class="card-head">
            <view class="title-wrap">
              <text class="title">{{ job.title || job.subjectName || '需求' }}</text>
              <text v-if="job.publisherIdentity === 'ORGANIZATION'" class="org-badge">机构单</text>
            </view>
            <text class="price">{{ formatBudget(job) }}</text>
          </view>

          <view class="meta-row">
            <text v-if="formatPlace(job)" class="meta-pill">{{ formatPlace(job) }}</text>
            <text v-if="formatClassMode(job.classMode)" class="meta-pill">{{ formatClassMode(job.classMode) }}</text>
            <text v-if="job.frequencyPerWeek" class="meta-pill">每周 {{ job.frequencyPerWeek }} 次</text>
            <text v-if="formatEducation(job.educationRequirement)" class="meta-pill">{{ formatEducation(job.educationRequirement) }}</text>
          </view>

          <view v-if="displayTags(job).length" class="tag-row">
            <text v-for="tag in displayTags(job)" :key="tag" class="tag">{{ tag }}</text>
          </view>

          <text class="desc">{{ job.description || '暂无描述' }}</text>

          <view class="foot-row">
            <text class="publisher">{{ publisherText(job) }}</text>
            <text class="status-tip">{{ job.publisherIdentity === 'ORGANIZATION' ? '注意核对履约规则' : '可先发起申请再沟通细节' }}</text>
          </view>
        </view>
      </view>

      <AppStateCard
        v-else-if="listError"
        title="需求列表暂时加载失败"
        description="当前筛选已保留，可以稍后刷新，或者先调整城市、科目和预算范围。"
        action-text="重新加载"
        variant="error"
        @action="refreshList"
      />

      <AppStateCard
        v-else
        title="暂时没有匹配到需求"
        description="可以换关键词、放宽预算筛选，或者稍后刷新看看。"
        action-text="重新加载"
        variant="soft"
        @action="refreshList"
      />

      <view v-if="jobList.length" class="footer-state">
        <text v-if="loadingMore" class="footer-text">正在加载更多...</text>
        <text v-else-if="isLast" class="footer-text">已经到底了</text>
        <text v-else class="footer-link" @click="loadMore">查看更多</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { jobsApi } from '@/api/jobs';
import { useUserStore } from '@/stores/user';
import { tutorStatusUrl } from '@/utils/tutorGuard';
import AppStateCard from '@/components/AppStateCard.vue';

const SUBJECT_PRESETS = ['全科辅导', '语文', '数学', '英语', '科学', '物理', '化学', '生物', '历史', '地理', '政治'] as const;

const userStore = useUserStore();
const keyword = ref('');
const jobList = ref<any[]>([]);
const cursor = ref<number | null>(null);
const classModeFilter = ref<'online' | 'offline' | 'both' | ''>('');
const selectedSubject = ref('');
const cityInput = ref('');
const budgetMinInput = ref('');
const budgetMaxInput = ref('');
const appliedCity = ref('');
const appliedBudgetMin = ref<number | null>(null);
const appliedBudgetMax = ref<number | null>(null);
const loading = ref(false);
const loadingMore = ref(false);
const isLast = ref(false);
const listError = ref('');

const resultSummary = computed(() => {
  if (loading.value && !jobList.value.length) return '正在加载中';
  if (listError.value && !jobList.value.length) return '加载失败';
  if (!jobList.value.length) return '暂无结果';
  return `已加载 ${jobList.value.length} 条需求`;
});

const guardIcon = computed(() => {
  if (userStore.tutorStatus === 'PENDING') return '…';
  if (userStore.tutorStatus === 'REJECTED') return '!';
  return '+';
});

const guardTitle = computed(() => {
  if (userStore.tutorStatus === 'PENDING') return '资料审核中';
  if (userStore.tutorStatus === 'REJECTED') return '审核未通过';
  return '先完成家教入驻';
});

const guardDesc = computed(() => {
  if (userStore.tutorStatus === 'PENDING') return '审核通过后即可浏览需求广场、收藏需求并发起沟通。';
  if (userStore.tutorStatus === 'REJECTED') return userStore.tutorRejectReason;
  return '补齐基础资料、授课信息和认证材料后，平台会开放教师接单能力。';
});

const guardActionText = computed(() => {
  if (userStore.tutorStatus === 'PENDING') return '查看审核状态';
  if (userStore.tutorStatus === 'REJECTED') return '重新完善资料';
  return '去入驻';
});

function normalizeCursor(v: unknown): number | null {
  if (v == null) return null;
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v !== 'string') return null;
  const s = v.trim();
  if (!s || s === 'null' || s === 'undefined') return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

function parseNumber(v: string) {
  const s = String(v || '').trim();
  if (!s) return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

async function fetchJobs(reset = false) {
  if (loading.value || loadingMore.value) return;
  const stateRef = reset ? loading : loadingMore;
  stateRef.value = true;
  if (reset) listError.value = '';
  try {
    const res: any = await jobsApi.feedDemands({
      q: keyword.value.trim() || undefined,
      cursor: reset ? null : normalizeCursor(cursor.value),
      classMode: classModeFilter.value || undefined,
      city: appliedCity.value || undefined,
      subject: selectedSubject.value || undefined,
      pageSize: 10,
      budgetMin: appliedBudgetMin.value,
      budgetMax: appliedBudgetMax.value,
    });

    const nextList = Array.isArray(res?.list) ? res.list : [];
    jobList.value = reset ? nextList : [...jobList.value, ...nextList];
    cursor.value = normalizeCursor(res?.nextCursor);
    isLast.value = !!res?.isLast || !nextList.length;
    listError.value = '';
  } catch {
    if (reset) jobList.value = [];
    listError.value = '需求列表加载失败';
  } finally {
    stateRef.value = false;
  }
}

function refreshList() {
  cursor.value = null;
  isLast.value = false;
  void fetchJobs(true);
}

function loadMore() {
  if (loading.value || loadingMore.value || isLast.value) return;
  void fetchJobs(false);
}

function setClassMode(v: '' | 'online' | 'offline' | 'both') {
  classModeFilter.value = v;
  refreshList();
}

function selectSubject(v: string) {
  selectedSubject.value = v;
  refreshList();
}

function applyFilters() {
  appliedCity.value = String(cityInput.value || '').trim();
  appliedBudgetMin.value = parseNumber(budgetMinInput.value);
  appliedBudgetMax.value = parseNumber(budgetMaxInput.value);
  refreshList();
}

function resetFilters() {
  classModeFilter.value = '';
  selectedSubject.value = '';
  cityInput.value = '';
  appliedCity.value = '';
  budgetMinInput.value = '';
  budgetMaxInput.value = '';
  appliedBudgetMin.value = null;
  appliedBudgetMax.value = null;
  refreshList();
}

function clearKeyword() {
  keyword.value = '';
  refreshList();
}

function goToDetail(id: number) {
  uni.navigateTo({ url: `/pages/job/detail?id=${id}` });
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

function handleGuardAction() {
  if (userStore.tutorStatus === 'PENDING') {
    uni.navigateTo({ url: tutorStatusUrl('PENDING') });
    return;
  }
  if (userStore.tutorStatus === 'REJECTED') {
    uni.navigateTo({ url: tutorStatusUrl('REJECTED', userStore.tutorRejectReason) });
    return;
  }
  uni.navigateTo({ url: '/pages/tutor/onboarding/index' });
}

function switchStudent() {
  userStore.setCurrentRole('student');
  uni.reLaunch({ url: '/pages/home/index' });
}

const normalizeLower = (v: unknown) => String(v || '').trim().toLowerCase();

const formatClassMode = (v: unknown) => {
  const s = normalizeLower(v);
  if (!s) return '';
  if (s === 'online') return '线上';
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '';
};

const formatPlace = (job: any) => {
  const s = normalizeLower(job?.classMode);
  if (s === 'online') return '线上';
  const city = String(job?.city || '').trim();
  if (city) return city;
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '';
};

const formatBudget = (job: any) => {
  const min = job?.budgetMin;
  const max = job?.budgetMax;
  if (min != null && max != null && String(min) !== '' && String(max) !== '') return `¥${min}-${max}/小时`;
  if (min != null && String(min) !== '') return `¥${min}/小时起`;
  if (max != null && String(max) !== '') return `≤¥${max}/小时`;
  return '面议';
};

const formatStage = (v: unknown) => {
  const s = String(v || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'PRESCHOOL') return '幼小';
  if (s === 'PRIMARY') return '小学';
  if (s === 'JUNIOR') return '初中';
  if (s === 'SENIOR') return '高中';
  if (s === 'OTHER') return '其他';
  return s;
};

const formatGrade = (v: unknown) => {
  const s = String(v || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'PRESCHOOL') return '学前';
  if (s.startsWith('GRADE')) return `小学${s.replace('GRADE', '')}年级`;
  if (s.startsWith('JUNIOR')) return `初${s.replace('JUNIOR', '')}`;
  if (s.startsWith('SENIOR')) return `高${s.replace('SENIOR', '')}`;
  if (s === 'SELF_EXAM') return '自考';
  if (s.startsWith('COLLEGE')) return `大学${s.replace('COLLEGE', '')}`;
  if (s === 'ADULT') return '成人';
  return s;
};

const formatTeacherGender = (v: unknown) => {
  const s = normalizeLower(v);
  if (!s) return '';
  if (s === 'male') return '偏好男老师';
  if (s === 'female') return '偏好女老师';
  if (s === 'both') return '性别不限';
  return '';
};

const formatEducation = (v: unknown) => {
  const s = String(v || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'TOP2') return 'TOP2';
  if (s === 'C985') return '985';
  if (s === 'C211') return '211';
  if (s === 'DOUBLE_FIRST_CLASS') return '双一流';
  if (s === 'FIRST_TIER') return '一本';
  if (s === 'BACHELOR') return '本科及以上';
  if (s === 'OVERSEAS') return '海外';
  if (s === 'QS50') return 'QS50';
  return s;
};

function displayTags(job: any) {
  return [
    String(job?.subjectName || '').trim(),
    formatStage(job?.stageCode),
    formatGrade(job?.gradeCode),
    formatTeacherGender(job?.teacherGenderPreference),
  ].filter(Boolean).slice(0, 4);
}

function publisherText(job: any) {
  const name = String(job?.publisher?.displayName || '').trim();
  if (name) return `发布者：${name}`;
  return job?.publisherIdentity === 'ORGANIZATION' ? '发布者：机构' : '发布者：学生/家长';
}

onMounted(() => {
  if (userStore.isLoggedIn && userStore.tutorStatus === 'APPROVED') {
    refreshList();
  }
});

onReachBottom(() => {
  loadMore();
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
  background: #f6f7fb;
}

.empty-state,
.guard-card,
.hero-card,
.filter-card,
.job-card {
  background: #ffffff;
  border: 1px solid rgba(31, 35, 41, 0.08);
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.06);
}

.guard-card {
  min-height: calc(100vh - 32px);
  padding: 34px 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  gap: 12px;
  text-align: center;
}

.guard-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 4px;
  border-radius: 24px;
  background: rgba(0, 190, 189, 0.12);
  color: #00a7a6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 900;
}

.guard-title {
  font-size: 20px;
  font-weight: 900;
  color: #1f2329;
}

.guard-desc {
  font-size: 13px;
  line-height: 1.7;
  color: #646a73;
  margin-bottom: 8px;
}

.guard-btn {
  height: 48px;
  border-radius: 999px;
  border: 0;
  font-size: 15px;
  font-weight: 900;
  line-height: 48px;
}

.guard-btn::after {
  border: 0;
}

.guard-btn.primary {
  background: #00bebd;
  color: #ffffff;
}

.guard-btn.secondary {
  background: #f6f7fb;
  color: #1f2329;
}

.hero-card,
.filter-card {
  border-radius: 16px;
  padding: 13px 13px 11px;
}

.hero-title {
  display: block;
  font-size: 22px;
  font-weight: 900;
  color: #1f2329;
}

.hero-desc {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.65;
  color: #646a73;
}

.search-box {
  margin-top: 12px;
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  border-radius: 12px;
  background: #f6f7fb;
}

.search-icon {
  font-size: 18px;
  color: #646a73;
  line-height: 1;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #1f2329;
}

.clear-text {
  font-size: 12px;
  color: #00a7a6;
}

.filter-card {
  margin-top: 14px;
}

.chip-row {
  white-space: nowrap;
  width: 100%;
}

.chip-track {
  display: inline-flex;
  align-items: center;
  padding-right: 10px;
}

.chip-row.secondary {
  margin-top: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  height: 29px;
  padding: 0 9px;
  margin-right: 5px;
  border-radius: 999px;
  background: #f6f7fb;
  color: #646a73;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}

.chip.active {
  background: rgba(0, 190, 189, 0.14);
  color: #00a7a6;
}

.secondary-chip {
  min-width: 56px;
}

.budget-row {
  margin-top: 9px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
}

.budget-range-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 6px;
  align-items: end;
}

.budget-label {
  display: block;
  margin-bottom: 5px;
  font-size: 11px;
  color: #646a73;
}

.budget-field {
  height: 34px;
  border-radius: 10px;
  background: #f6f7fb;
  padding: 0 9px;
  font-size: 12px;
  color: #1f2329;
}

.budget-sep {
  padding-bottom: 10px;
  font-size: 16px;
  color: #8f959e;
}

.filter-actions {
  margin-top: 9px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.filter-btn {
  height: 34px;
  border-radius: 10px;
  border: 0;
  font-size: 12px;
  font-weight: 900;
  line-height: 34px;
}

.filter-btn::after {
  border: 0;
}

.filter-btn.ghost {
  background: #f1f3f5;
  color: #4f5660;
}

.filter-btn.primary {
  background: #00bebd;
  color: #ffffff;
}

.section-head {
  margin-top: 14px;
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.section-title {
  display: block;
  font-size: 17px;
  font-weight: 900;
  color: #1f2329;
}

.section-sub {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #8f959e;
}

.refresh-link {
  flex-shrink: 0;
  font-size: 12px;
  color: #00a7a6;
}

.skeleton-list,
.job-list {
  display: grid;
  gap: 7px;
}

.skeleton-card {
  height: 130px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f3f5 0%, #fafbfc 50%, #f1f3f5 100%);
}

.job-card {
  padding: 10px;
  border-radius: 15px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.title-wrap {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 14px;
  font-weight: 900;
  color: #1f2329;
  line-height: 1.35;
}

.org-badge {
  display: inline-flex;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.14);
  color: #b86a00;
  font-size: 11px;
  font-weight: 800;
}

.price {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 900;
  color: #ff4d4f;
  white-space: nowrap;
}

.meta-row,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 5px;
}

.meta-pill {
  padding: 2px 6px;
  border-radius: 999px;
  background: #f6f7fb;
  color: #646a73;
  font-size: 9px;
}

.tag {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(0, 190, 189, 0.08);
  color: #00a7a6;
  font-size: 9px;
  font-weight: 700;
}

.desc {
  display: block;
  margin-top: 6px;
  font-size: 10px;
  line-height: 1.6;
  color: #4f5660;
}

.foot-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-top: 7px;
}

.publisher,
.status-tip {
  font-size: 9px;
  color: #8f959e;
}

.status-tip {
  text-align: right;
}

.footer-state {
  padding: 12px 0 8px;
  text-align: center;
}

.footer-text,
.footer-link {
  font-size: 12px;
  color: #8f959e;
}

.footer-link {
  color: #00a7a6;
}
</style>
