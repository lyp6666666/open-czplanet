<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="state">
      <text class="state-title">登录后查看收藏</text>
      <text class="state-desc">你收藏的老师或需求会统一放在这里。</text>
      <button class="mini-btn" @click="goLogin">去登录</button>
    </view>

    <template v-else>
      <view class="hero">
        <text class="eyebrow">我的收藏</text>
        <text class="title">{{ userStore.currentRole === 'tutor' ? '收藏需求' : '收藏老师' }}</text>
        <text class="subtitle">{{ userStore.currentRole === 'tutor' ? '把感兴趣的需求留住，方便之后继续申请。' : '保留喜欢的老师，之后可直接查看并发起申请。' }}</text>
      </view>

      <view v-if="error" class="state error">
        <text>{{ error }}</text>
        <button class="mini-btn" @click="reload">重试</button>
      </view>

      <view v-else-if="list.length === 0 && !loading" class="state">
        <text class="state-title">{{ userStore.currentRole === 'tutor' ? '暂无收藏需求' : '暂无收藏老师' }}</text>
        <text class="state-desc">{{ userStore.currentRole === 'tutor' ? '去需求广场收藏适合自己的需求。' : '浏览老师时可以先收藏，再慢慢比较。' }}</text>
      </view>

      <view v-else class="list">
        <view v-for="item in list" :key="itemKey(item)" class="fav-card" @click="openItem(item)">
          <template v-if="userStore.currentRole === 'tutor'">
            <view class="card-head">
              <view>
                <text class="card-title">{{ item.title || `${item.subjectName || ''}家教需求` }}</text>
                <text class="card-sub">{{ item.subjectName || '科目待定' }} · {{ modeText(item.classMode) }}</text>
              </view>
              <text class="pill">{{ budgetText(item) }}</text>
            </view>
            <text class="desc">{{ item.description || '暂无描述' }}</text>
            <view class="ops" @click.stop>
              <button class="op ghost" @click="unfavorite(item)">取消收藏</button>
              <button class="op" @click="openItem(item)">查看需求</button>
            </view>
          </template>

          <template v-else>
            <view class="tutor-row">
              <image class="avatar" :src="resolveImageUrl(item.user?.avatar)" mode="aspectFill"></image>
              <view class="tutor-main">
                <text class="card-title">{{ item.user?.name || '老师' }}</text>
                <text class="card-sub">{{ item.teacherProfile?.subject || '科目待定' }} · {{ item.teacherProfile?.city || '城市待定' }}</text>
                <text class="desc line">{{ item.teacherProfile?.introduction || '暂无简介' }}</text>
              </view>
            </view>
            <view class="ops" @click.stop>
              <button class="op ghost" @click="unfavorite(item)">取消收藏</button>
              <button class="op" @click="openItem(item)">查看老师</button>
            </view>
          </template>
        </view>
      </view>

      <view class="load-more">
        <text v-if="loading">加载中...</text>
        <text v-else-if="isLast && list.length > 0">没有更多了</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app';
import { favoritesApi } from '@/api/favorites';
import { jobsApi } from '@/api/jobs';
import { request, resolveImageUrl } from '@/utils/request';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const list = ref<any[]>([]);
const cursor = ref<number | null>(null);
const isLast = ref(false);
const loading = ref(false);
const error = ref('');

function itemKey(item: any) {
  if (userStore.currentRole === 'tutor') return item.id;
  return item.user?.id || item.teacherProfile?.userId;
}

async function load(reset = false) {
  if (!userStore.isLoggedIn || loading.value || (!reset && isLast.value)) return;
  loading.value = true;
  error.value = '';
  try {
    if (reset) {
      cursor.value = null;
      isLast.value = false;
      list.value = [];
    }
    const page: any = userStore.currentRole === 'tutor'
      ? await favoritesApi.pageDemandFavorites({ pageSize: 10, cursor: cursor.value })
      : await favoritesApi.pageTutorFavorites({ pageSize: 10, cursor: cursor.value });
    const ids = page?.items || page?.list || [];
    const details = await Promise.allSettled(ids.map((id: number) => loadDetail(Number(id))));
    const rows = details
      .filter((it): it is PromiseFulfilledResult<any> => it.status === 'fulfilled' && !!it.value)
      .map((it) => it.value);
    list.value = reset ? rows : [...list.value, ...rows];
    cursor.value = page?.cursor ?? page?.nextCursor ?? null;
    isLast.value = !!page?.isLast || ids.length === 0;
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载收藏失败';
  } finally {
    loading.value = false;
  }
}

function loadDetail(id: number) {
  if (userStore.currentRole === 'tutor') return jobsApi.getDemandView(id);
  return request({ url: `/user/card?uid=${id}`, method: 'GET' });
}

function modeText(mode?: string) {
  const s = String(mode || '').toLowerCase();
  if (s === 'online') return '线上';
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '授课方式待定';
}

function budgetText(item: any) {
  if (item.budgetMin != null && item.budgetMax != null) return `¥${item.budgetMin}-${item.budgetMax}/小时`;
  if (item.budgetMin != null) return `¥${item.budgetMin}/小时起`;
  if (item.budgetMax != null) return `≤¥${item.budgetMax}/小时`;
  return '面议';
}

async function unfavorite(item: any) {
  const id = itemKey(item);
  if (!id) return;
  try {
    if (userStore.currentRole === 'tutor') await favoritesApi.unfavoriteDemand(Number(id));
    else await favoritesApi.unfavoriteTutor(Number(id));
    list.value = list.value.filter((it) => itemKey(it) !== id);
    uni.showToast({ title: '已取消收藏', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || e?.msg || '操作失败', icon: 'none' });
  }
}

function openItem(item: any) {
  const id = itemKey(item);
  if (!id) return;
  if (userStore.currentRole === 'tutor') uni.navigateTo({ url: `/pages/job/detail?id=${id}` });
  else uni.navigateTo({ url: `/pages/tutor/detail?id=${id}` });
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

async function reload() {
  await load(true);
}

onShow(() => {
  void load(true);
});

onReachBottom(() => {
  void load(false);
});

onPullDownRefresh(async () => {
  await load(true);
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
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #102326 0%, #315b4f 70%, #d49f52 160%);
  box-shadow: 0 18px 38px rgba(16, 35, 38, 0.2);
}

.eyebrow,
.title,
.subtitle,
.card-title,
.card-sub,
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
  font-size: 23px;
  font-weight: 900;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.82;
}

.list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.fav-card {
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 12px 28px rgba(18, 37, 41, 0.06);
}

.card-head,
.tutor-row {
  display: flex;
  gap: 12px;
}

.card-head {
  justify-content: space-between;
}

.card-title {
  color: #142326;
  font-size: 17px;
  font-weight: 900;
  margin-bottom: 5px;
}

.card-sub {
  color: #7a838c;
  font-size: 12px;
}

.pill {
  align-self: flex-start;
  padding: 5px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: #edf7f5;
  font-size: 12px;
  font-weight: 900;
  flex-shrink: 0;
}

.avatar {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  background: #eef2f3;
  flex-shrink: 0;
}

.tutor-main {
  flex: 1;
  min-width: 0;
}

.desc {
  margin-top: 12px;
  color: #65717a;
  font-size: 13px;
  line-height: 1.6;
}

.desc.line {
  margin-top: 6px;
  max-height: 40px;
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
