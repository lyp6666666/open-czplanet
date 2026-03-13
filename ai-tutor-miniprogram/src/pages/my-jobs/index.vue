<template>
  <view class="container">
    <view class="job-list">
      <view v-for="job in jobList" :key="job.id" class="job-card" @click="goToDetail(job.id)">
        <view class="header">
            <text class="title">{{ job.title }}</text>
            <text class="status">{{ job.status === 1 ? 'Open' : 'Closed' }}</text>
        </view>
        <view class="info">
            <text class="tag">{{ job.subjectName }}</text>
            <text class="tag">{{ job.city }}</text>
        </view>
        <text class="time">Posted: {{ formatDate(job.createTime) }}</text>
      </view>
      <view v-if="jobList.length === 0" class="empty">
          <text>No demands posted yet.</text>
      </view>
    </view>
    <view class="fab-btn" @click="goToPost">
        <text class="plus">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { jobsApi } from '@/api/jobs';
import { onShow } from '@dcloudio/uni-app';

const jobList = ref<any[]>([]);

const fetchJobs = async () => {
  try {
    const res: any = await jobsApi.mineDemands({ pageSize: 20 });
    if (res && res.items) {
        jobList.value = res.items;
    }
  } catch (error) {
    console.error(error);
  }
};

const goToDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/job/detail?id=${id}` });
};

const goToPost = () => {
  uni.navigateTo({ url: `/pages/post/index` });
};

const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    return dateStr.split('T')[0];
};

onShow(() => {
  fetchJobs();
});
</script>

<style lang="scss" scoped>
.container {
  padding: 10px;
  background-color: #f8f8f8;
  min-height: 100vh;
}
.job-card {
  padding: 15px;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  
  .header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 10px;
      .title {
          font-weight: bold;
          font-size: 16px;
      }
      .status {
          font-size: 12px;
          color: #007aff;
          background-color: #e6f2ff;
          padding: 2px 6px;
          border-radius: 4px;
      }
  }
  .info {
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
  .time {
      font-size: 12px;
      color: #999;
  }
}
.empty {
    text-align: center;
    color: #999;
    padding-top: 50px;
}
.fab-btn {
    position: fixed;
    bottom: 30px;
    right: 30px;
    width: 50px;
    height: 50px;
    border-radius: 25px;
    background-color: #007aff;
    display: flex;
    justify-content: center;
    align-items: center;
    box-shadow: 0 4px 10px rgba(0,122,255,0.3);
    
    .plus {
        color: #fff;
        font-size: 30px;
        font-weight: bold;
        margin-top: -2px;
    }
}
</style>
