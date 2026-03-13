<template>
  <view class="container">
    <view class="search-bar">
      <input class="search-input" type="text" v-model="keyword" placeholder="Search jobs..." @confirm="onSearch" />
    </view>
    <view class="job-list">
      <view v-for="job in jobList" :key="job.id" class="job-card" @click="goToDetail(job.id)">
        <view class="header">
            <text class="title">{{ job.title || job.subjectName }}</text>
            <text class="price">¥{{ job.budgetMin }}-{{ job.budgetMax }}/hr</text>
        </view>
        <view class="tags">
            <text class="tag">{{ job.gradeCode }}</text>
            <text class="tag">{{ job.city || 'Online' }}</text>
            <text class="tag">{{ job.classMode }}</text>
        </view>
        <text class="desc">{{ job.description }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { jobsApi } from '@/api/jobs';

const keyword = ref('');
const jobList = ref<any[]>([]);
const cursor = ref<number | null>(null);

const fetchJobs = async () => {
  try {
    const res: any = await jobsApi.feedDemands({
        q: keyword.value,
        cursor: cursor.value,
        pageSize: 10
    });
    
    if (res && res.items) {
      if (cursor.value) {
          jobList.value = [...jobList.value, ...res.items];
      } else {
          jobList.value = res.items;
      }
      cursor.value = res.nextCursor;
    }
  } catch (error) {
    console.error(error);
  }
};

const onSearch = () => {
  cursor.value = null;
  jobList.value = [];
  fetchJobs();
};

const goToDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/job/detail?id=${id}` });
};

onMounted(() => {
  fetchJobs();
});
</script>

<style lang="scss" scoped>
.container {
  padding: 10px;
}
.search-bar {
  margin-bottom: 15px;
  .search-input {
    background-color: #f5f5f5;
    padding: 10px;
    border-radius: 20px;
    font-size: 14px;
  }
}
.job-card {
  padding: 15px;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  
  .header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 10px;
      .title {
          font-weight: bold;
          font-size: 16px;
      }
      .price {
          color: #ff5500;
          font-weight: bold;
      }
  }
  
  .tags {
      display: flex;
      flex-wrap: wrap;
      margin-bottom: 10px;
      .tag {
        font-size: 12px;
        color: #666;
        background-color: #f5f5f5;
        padding: 2px 6px;
        border-radius: 4px;
        margin-right: 5px;
      }
  }
  
  .desc {
      font-size: 13px;
      color: #999;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
      text-overflow: ellipsis;
  }
}
</style>
