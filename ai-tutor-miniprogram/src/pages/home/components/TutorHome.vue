<template>
  <view class="container">
    <view v-if="!userStore.isLoggedIn" class="empty-state">
      <text class="tip">登录后查看需求</text>
      <u-button type="primary" color="#00bebd" shape="circle" @click="goLogin">去登录</u-button>
    </view>
    <view v-else>
      <view class="search-bar">
        <u-icon name="search" color="#646a73" size="20"></u-icon>
        <input class="search-input" type="text" v-model="keyword" placeholder="搜索需求..." @confirm="onSearch" />
      </view>

      <scroll-view class="filters" scroll-x>
        <view class="filter-chip" :class="{ active: classModeFilter === '' }" @click="setClassMode('')">不限</view>
        <view class="filter-chip" :class="{ active: classModeFilter === 'online' }" @click="setClassMode('online')">线上</view>
        <view class="filter-chip" :class="{ active: classModeFilter === 'offline' }" @click="setClassMode('offline')">线下</view>
        <view class="filter-chip" :class="{ active: classModeFilter === 'both' }" @click="setClassMode('both')">线上/线下</view>
      </scroll-view>
      
      <view class="job-list">
        <view v-for="job in jobList" :key="job.id" class="job-card" @click="goToDetail(job.id)">
          <view class="header">
              <view class="title-row">
                <text class="title">{{ job.title || job.subjectName || '需求' }}</text>
                <text v-if="job.publisherIdentity === 'ORGANIZATION'" class="badge-org">机构单</text>
              </view>
              <text class="price">{{ formatBudget(job) }}</text>
          </view>
          <view class="meta">
            <text v-if="formatPlace(job)" class="meta-item">{{ formatPlace(job) }}</text>
            <text v-if="formatClassMode(job.classMode)" class="meta-item">{{ formatClassMode(job.classMode) }}</text>
            <text v-if="job.frequencyPerWeek" class="meta-item">每周 {{ job.frequencyPerWeek }} 次</text>
            <text v-if="formatEducation(job.educationRequirement)" class="meta-item">{{ formatEducation(job.educationRequirement) }}</text>
          </view>
          <view class="tags" v-if="job.subjectName || job.stageCode || job.gradeCode || job.teacherGenderPreference">
              <text class="tag" v-if="job.subjectName">{{ job.subjectName }}</text>
              <text class="tag" v-if="formatStage(job.stageCode)">{{ formatStage(job.stageCode) }}</text>
              <text class="tag" v-if="formatGrade(job.gradeCode)">{{ formatGrade(job.gradeCode) }}</text>
              <text class="tag" v-if="formatTeacherGender(job.teacherGenderPreference)">{{ formatTeacherGender(job.teacherGenderPreference) }}</text>
          </view>
          <text class="desc">{{ job.description || '暂无描述' }}</text>
        </view>
      </view>
      <view v-if="jobList.length === 0" class="empty-list">
        <text class="empty-text">暂无需求数据</text>
        <u-button type="primary" color="#00bebd" shape="circle" @click="fetchJobs">刷新</u-button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { jobsApi } from '@/api/jobs';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const keyword = ref('');
const jobList = ref<any[]>([]);
const cursor = ref<number | null>(null);
const classModeFilter = ref<'online' | 'offline' | 'both' | ''>('');

const normalizeCursor = (v: unknown): number | null => {
  if (v == null) return null;
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v !== 'string') return null;
  const s = v.trim();
  if (!s || s === 'null' || s === 'undefined') return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
};

const fetchJobs = async () => {
  try {
    const res: any = await jobsApi.feedDemands({
        q: keyword.value,
        cursor: normalizeCursor(cursor.value),
        classMode: classModeFilter.value || undefined,
        pageSize: 10
    });
    
    if (res && Array.isArray(res.list)) {
      if (cursor.value) {
          jobList.value = [...jobList.value, ...res.list];
      } else {
          jobList.value = res.list;
      }
      cursor.value = normalizeCursor(res.nextCursor);
    }
  } catch (error) {
    console.error(error);
  }
};

const setClassMode = (v: '' | 'online' | 'offline' | 'both') => {
  classModeFilter.value = v;
  onSearch();
};

const onSearch = () => {
  cursor.value = null;
  jobList.value = [];
  fetchJobs();
};

const goToDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/job/detail?id=${id}` });
};

const goLogin = () => {
  uni.switchTab({ url: '/pages/me/index' });
};

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchJobs();
  }
});

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
</script>

<style lang="scss" scoped>
.container {
  padding: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  
  .tip {
    font-size: 16px;
    color: var(--muted);
    margin-bottom: 24px;
  }
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

.filters {
  white-space: nowrap;
  margin-bottom: 12px;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(31, 35, 41, 0.12);
  background: #ffffff;
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
  margin-right: 8px;
}

.filter-chip.active {
  color: var(--primary);
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.10);
}

.job-list {
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

.job-card {
  padding: 16px;
  background-color: var(--card);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.08);
  border: 1px solid var(--border);
  
  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;
    
    .title-row {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
      flex: 1;
    }

    .title {
      font-weight: 900;
      font-size: 15px;
      color: var(--text);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 210px;
    }
    
    .price {
      color: #ff4d4f;
      font-weight: 900;
      font-size: 13px;
      white-space: nowrap;
      margin-left: 10px;
    }
  }

  .badge-org {
    font-size: 10px;
    line-height: 16px;
    color: #0a78ff;
    background: rgba(10, 120, 255, 0.10);
    border: 1px solid rgba(10, 120, 255, 0.25);
    padding: 0 8px;
    border-radius: 999px;
    font-weight: 900;
    flex-shrink: 0;
  }

  .meta {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 10px;
    color: var(--muted);
    font-size: 12px;
  }

  .meta-item {
    padding: 3px 10px;
    border-radius: 999px;
    border: 1px solid rgba(31, 35, 41, 0.08);
    background: rgba(255, 255, 255, 0.7);
  }
  
  .tags {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 10px;
    gap: 6px;
    
    .tag {
      font-size: 10px;
      color: var(--primary);
      background-color: rgba(0, 190, 189, 0.10);
      border: 1px solid rgba(0, 190, 189, 0.22);
      padding: 2px 8px;
      border-radius: 999px;
      font-weight: 900;
    }
  }
  
  .desc {
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
</style>
