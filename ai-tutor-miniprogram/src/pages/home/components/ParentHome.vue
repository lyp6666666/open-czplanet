<template>
  <view class="container">
    <view v-if="!userStore.isLoggedIn" class="guest-tip">
      <text>可先浏览老师，发起申请时再登录</text>
      <u-button size="mini" type="primary" color="#00bebd" shape="circle" @click="goLogin">登录</u-button>
    </view>

    <view class="search-bar">
      <u-icon name="search" color="#646a73" size="20"></u-icon>
      <input class="search-input" type="text" v-model="keyword" placeholder="搜索家教..." @confirm="onSearch" />
    </view>

    <view class="tutor-list">
      <view v-for="tutor in tutorList" :key="tutor.userId" class="tutor-card" @click="goToDetail(tutor.userId)">
        <image class="avatar" :src="resolveImageUrl(tutor.avatar)" mode="aspectFill"></image>
        <view class="info">
          <view class="header">
            <text class="name">{{ tutor.displayName }}</text>
            <text class="price" v-if="tutor.price">{{ tutor.price }}</text>
          </view>
          <view class="tags">
            <text class="tag" v-for="(tag, index) in (tutor.subjectTags || [])" :key="index">{{ tag }}</text>
          </view>
          <text class="intro">{{ tutor.introduction }}</text>
        </view>
      </view>
    </view>
    <view v-if="tutorList.length === 0" class="empty-list">
      <text class="empty-text">暂无家教数据</text>
      <u-button type="primary" color="#00bebd" shape="circle" @click="fetchTutors">刷新</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { request, resolveImageUrl } from '@/utils/request';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const keyword = ref('');
const tutorList = ref<any[]>([]);
const cursor = ref<number | null>(null);

const normalizeCursor = (v: unknown): number | null => {
  if (v == null) return null;
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v === 'string') {
    const s = v.trim();
    if (!s || s === 'null' || s === 'undefined') return null;
    const n = Number(s);
    return Number.isFinite(n) ? n : null;
  }
  return null;
};

const fetchTutors = async () => {
  try {
    const res: any = await request({
      url: '/api/v1/parent/tutors/page',
      data: {
        q: keyword.value,
        cursor: normalizeCursor(cursor.value),
        pageSize: 10
      },
      loading: true
    });
    if (res && Array.isArray(res.list)) {
      if (cursor.value) {
        tutorList.value = [...tutorList.value, ...res.list];
      } else {
        tutorList.value = res.list;
      }
      cursor.value = normalizeCursor(res.nextCursor);
    }
  } catch (error) {
    console.error(error);
  }
};

const onSearch = () => {
  cursor.value = null;
  tutorList.value = [];
  fetchTutors();
};

const goToDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/tutor/detail?id=${id}` });
};

const goLogin = () => {
  uni.switchTab({ url: '/pages/me/index' });
};

onMounted(() => {
  fetchTutors();
});
</script>

<style lang="scss" scoped>
.container {
  padding: 16px;
}

.guest-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: rgba(0, 190, 189, 0.08);
  border: 1px solid rgba(0, 190, 189, 0.18);
  border-radius: 12px;
  color: var(--muted);
  font-size: 13px;
}

.search-bar {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  padding: 0 16px;
  height: 44px;
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid var(--border);

  .search-input {
    flex: 1;
    margin-left: 10px;
    font-size: 14px;
    color: var(--text);
  }
}

.tutor-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  .empty-text {
    font-size: 14px;
    color: var(--muted);
    margin-bottom: 16px;
  }
}

.tutor-card {
  display: flex;
  padding: 16px;
  background-color: var(--card);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.08);
  border: 1px solid var(--border);

  .avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    margin-right: 16px;
    background-color: #f0f0f0;
    flex-shrink: 0;
  }

  .info {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;

      .name {
        font-weight: 900;
        font-size: 16px;
        color: var(--text);
      }

      .price {
        font-size: 14px;
        color: #ff4d4f;
        font-weight: 700;
      }
    }

    .tags {
      display: flex;
      flex-wrap: wrap;
      margin-bottom: 8px;
      gap: 6px;

      .tag {
        font-size: 11px;
        color: var(--primary);
        background-color: rgba(0, 190, 189, 0.1);
        padding: 2px 8px;
        border-radius: 6px;
      }
    }

    .intro {
      font-size: 13px;
      color: var(--muted);
      line-height: 1.5;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
</style>
