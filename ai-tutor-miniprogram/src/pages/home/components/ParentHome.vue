<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="guest-banner">
      <view>
        <text class="guest-title">先浏览老师，再决定要不要联系</text>
        <text class="guest-desc">未登录也能看老师资料，发起申请或收藏时再登录即可。</text>
      </view>
      <button class="guest-btn" @click="goLogin">登录</button>
    </view>

    <view class="hero-card">
      <text class="hero-title">找老师</text>
      <text class="hero-desc">按科目、课时费和关键词快速筛选，优先看适合你当前需求的老师。</text>
      <view class="search-box">
        <text class="search-icon">⌕</text>
        <input class="search-input" v-model="keyword" type="text" placeholder="搜索科目、学校、老师关键词" @confirm="handleSearch" />
        <text v-if="keyword" class="clear-text" @click="clearKeyword">清空</text>
      </view>
    </view>

    <view class="filter-section">
      <scroll-view class="chip-row" scroll-x>
        <view class="chip-track">
          <view
            class="chip"
            :class="{ active: selectedSubject === '' }"
            @click="selectSubject('')"
          >
            全部科目
          </view>
          <view
            v-for="subject in SUBJECT_PRESETS"
            :key="subject"
            class="chip"
            :class="{ active: selectedSubject === subject }"
            @click="selectSubject(subject)"
          >
            {{ subject }}
          </view>
        </view>
      </scroll-view>

      <view class="rate-row">
        <view class="rate-input">
          <text class="rate-label">最低课时费</text>
          <input v-model="rateMinInput" type="number" class="rate-field" placeholder="不限" />
        </view>
        <text class="rate-sep">-</text>
        <view class="rate-input">
          <text class="rate-label">最高课时费</text>
          <input v-model="rateMaxInput" type="number" class="rate-field" placeholder="不限" />
        </view>
      </view>

      <view class="filter-actions">
        <button class="filter-btn ghost" @click="resetFilters">重置</button>
        <button class="filter-btn primary" @click="applyFilters">应用筛选</button>
      </view>
    </view>

    <view class="section-head">
      <view>
        <text class="section-title">老师列表</text>
        <text class="section-sub">{{ resultSummary }}</text>
      </view>
      <text class="refresh-link" @click="refreshList">刷新</text>
    </view>

    <view v-if="loading && !tutorList.length" class="skeleton-list">
      <view v-for="idx in 3" :key="idx" class="skeleton-card"></view>
    </view>

    <view v-else-if="tutorList.length" class="tutor-list">
      <view v-for="tutor in tutorList" :key="tutor.userId" class="tutor-card" @click="goToDetail(tutor.userId)">
        <image class="avatar" :src="resolveImageUrl(tutor.avatar || undefined)" mode="aspectFill"></image>
        <view class="card-body">
          <view class="card-head">
            <view class="name-row">
              <text class="name">{{ tutor.displayName || `老师${tutor.userId}` }}</text>
              <text v-if="verifiedLabel(tutor)" class="verify-badge">{{ verifiedLabel(tutor) }}</text>
            </view>
            <text class="price">{{ priceText(tutor) }}</text>
          </view>

          <text class="meta">{{ metaText(tutor) }}</text>

          <view v-if="displaySubjectTags(tutor).length" class="tag-row">
            <text v-for="tag in displaySubjectTags(tutor)" :key="tag" class="tag">{{ tag }}</text>
          </view>

          <text class="intro">{{ tutor.introduction || '暂未填写个人简介' }}</text>

          <view v-if="displayHighlights(tutor).length" class="highlight-row">
            <text v-for="item in displayHighlights(tutor)" :key="item" class="highlight">{{ item }}</text>
          </view>
        </view>
      </view>
    </view>

    <AppStateCard
      v-else-if="listError"
      title="老师列表暂时加载失败"
      description="已保留当前筛选条件，可以稍后刷新，或者先换个科目和课时费范围。"
      action-text="重新加载"
      variant="error"
      @action="refreshList"
    />

    <AppStateCard
      v-else
      title="暂时没有匹配到老师"
      description="可以换个关键词、放宽课时费范围，或者稍后再来看看。"
      action-text="重新加载"
      variant="soft"
      @action="refreshList"
    />

    <view v-if="tutorList.length" class="footer-state">
      <text v-if="loadingMore" class="footer-text">正在加载更多...</text>
      <text v-else-if="isLast" class="footer-text">已经到底了</text>
      <text v-else class="footer-link" @click="loadMore">查看更多</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app';
import { resolveImageUrl } from '@/utils/request';
import { useUserStore } from '@/stores/user';
import { parentTutorsApi, type ParentTutorCard } from '@/api/parentTutors';
import AppStateCard from '@/components/AppStateCard.vue';

const SUBJECT_PRESETS = ['全科辅导', '语文', '数学', '英语', '科学', '物理', '化学', '生物', '历史', '地理', '政治'] as const;

const userStore = useUserStore();
const keyword = ref('');
const selectedSubject = ref('');
const rateMinInput = ref('');
const rateMaxInput = ref('');
const appliedRateMin = ref<number | null>(null);
const appliedRateMax = ref<number | null>(null);
const tutorList = ref<ParentTutorCard[]>([]);
const cursor = ref<number | null>(null);
const isLast = ref(false);
const loading = ref(false);
const loadingMore = ref(false);
const listError = ref('');

const resultSummary = computed(() => {
  if (loading.value && !tutorList.value.length) return '正在加载中';
  if (listError.value && !tutorList.value.length) return '加载失败';
  if (!tutorList.value.length) return '暂无结果';
  return `已加载 ${tutorList.value.length} 位老师`;
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

function parseRate(v: string) {
  const s = String(v || '').trim();
  if (!s) return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

function displaySubjectTags(tutor: ParentTutorCard) {
  return Array.isArray(tutor.subjectTags) ? tutor.subjectTags.filter(Boolean).slice(0, 4) : [];
}

function displayHighlights(tutor: ParentTutorCard) {
  return Array.isArray(tutor.highlights) ? tutor.highlights.filter(Boolean).slice(0, 3) : [];
}

function metaText(tutor: ParentTutorCard) {
  const parts: string[] = [];
  if (tutor.city) parts.push(tutor.city);
  if (tutor.highestEduSchool) parts.push(tutor.highestEduSchool);
  if (tutor.education) parts.push(tutor.education);
  if (tutor.experienceYears != null) parts.push(`${tutor.experienceYears}年经验`);
  return parts.join(' · ') || '资料完善中';
}

function priceText(tutor: ParentTutorCard) {
  const rate = tutor.ratePerHour;
  if (rate == null || String(rate).trim() === '') return '面议';
  return `¥${rate}/小时`;
}

function verifiedLabel(tutor: ParentTutorCard) {
  return Number(tutor.eduVerifyStatus) === 2 ? '学历认证' : '';
}

async function fetchTutors(reset = false) {
  if (loading.value || loadingMore.value) return;
  const targetLoading = reset ? loading : loadingMore;
  targetLoading.value = true;
  if (reset) listError.value = '';
  try {
    const res = await parentTutorsApi.page({
      q: keyword.value.trim() || undefined,
      cursor: reset ? null : normalizeCursor(cursor.value),
      pageSize: 10,
      subject: selectedSubject.value || undefined,
      rateMin: appliedRateMin.value,
      rateMax: appliedRateMax.value,
    });
    const nextList = Array.isArray(res?.list) ? res.list : [];
    tutorList.value = reset ? nextList : [...tutorList.value, ...nextList];
    cursor.value = normalizeCursor(res?.nextCursor);
    isLast.value = !!res?.isLast || !nextList.length;
    listError.value = '';
  } catch {
    if (reset) tutorList.value = [];
    listError.value = '老师列表加载失败';
  } finally {
    targetLoading.value = false;
  }
}

function refreshList() {
  cursor.value = null;
  isLast.value = false;
  void fetchTutors(true);
}

function handleSearch() {
  refreshList();
}

function loadMore() {
  if (loadingMore.value || loading.value || isLast.value) return;
  void fetchTutors(false);
}

function applyFilters() {
  appliedRateMin.value = parseRate(rateMinInput.value);
  appliedRateMax.value = parseRate(rateMaxInput.value);
  refreshList();
}

function resetFilters() {
  selectedSubject.value = '';
  rateMinInput.value = '';
  rateMaxInput.value = '';
  appliedRateMin.value = null;
  appliedRateMax.value = null;
  refreshList();
}

function selectSubject(subject: string) {
  selectedSubject.value = subject;
  refreshList();
}

function clearKeyword() {
  keyword.value = '';
  refreshList();
}

function goToDetail(id: number) {
  uni.navigateTo({ url: `/pages/tutor/detail?id=${id}` });
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

onMounted(() => {
  refreshList();
});

onShow(() => {
  if (!tutorList.value.length && !loading.value) {
    refreshList();
  }
});

onReachBottom(() => {
  loadMore();
});

onPullDownRefresh(async () => {
  await fetchTutors(true);
  uni.stopPullDownRefresh();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px;
  background: #f6f7fb;
}

.guest-banner,
.hero-card,
.filter-section,
.tutor-card,
.empty-state {
  background: #ffffff;
  border: 1px solid rgba(31, 35, 41, 0.08);
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.06);
}

.guest-banner {
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.guest-title {
  display: block;
  font-size: 14px;
  font-weight: 900;
  color: #1f2329;
}

.guest-desc {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #646a73;
}

.guest-btn {
  flex-shrink: 0;
  min-width: 82px;
  height: 36px;
  border-radius: 999px;
  background: #00bebd;
  color: #ffffff;
  font-size: 13px;
  font-weight: 900;
  line-height: 36px;
  border: 0;
}

.guest-btn::after {
  border: 0;
}

.hero-card {
  padding: 15px 15px 13px;
  border-radius: 16px;
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

.filter-section {
  margin-top: 14px;
  padding: 11px 11px 10px;
  border-radius: 16px;
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

.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
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

.rate-row {
  margin-top: 9px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 6px;
  align-items: end;
}

.rate-input {
  min-width: 0;
}

.rate-label {
  display: block;
  margin-bottom: 5px;
  font-size: 11px;
  color: #646a73;
}

.rate-field {
  height: 34px;
  border-radius: 10px;
  background: #f6f7fb;
  padding: 0 9px;
  font-size: 12px;
  color: #1f2329;
}

.rate-sep {
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
.tutor-list {
  display: grid;
  gap: 7px;
}

.skeleton-card {
  height: 130px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f3f5 0%, #fafbfc 50%, #f1f3f5 100%);
}

.tutor-card {
  padding: 10px;
  border-radius: 15px;
  display: flex;
  gap: 9px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 13px;
  background: #eef1f4;
  flex-shrink: 0;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.name-row {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 14px;
  font-weight: 900;
  color: #1f2329;
  line-height: 1.35;
}

.verify-badge {
  display: inline-flex;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #00a7a6;
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

.meta {
  display: block;
  margin-top: 3px;
  font-size: 10px;
  line-height: 1.5;
  color: #646a73;
}

.tag-row,
.highlight-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 5px;
}

.tag {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(0, 190, 189, 0.08);
  color: #00a7a6;
  font-size: 9px;
  font-weight: 700;
}

.intro {
  display: block;
  margin-top: 6px;
  font-size: 10px;
  line-height: 1.6;
  color: #4f5660;
}

.highlight {
  padding: 2px 6px;
  border-radius: 999px;
  background: #f6f7fb;
  color: #5f6670;
  font-size: 9px;
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
