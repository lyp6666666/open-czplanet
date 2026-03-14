<template>
  <view class="container" v-if="job">
    <view class="header">
        <text class="title">{{ job.title }}</text>
        <text class="price">¥{{ job.budgetMin }}-{{ job.budgetMax }}/hr</text>
    </view>
    
    <view class="info-grid">
        <view class="info-item">
            <text class="label">Subject</text>
            <text class="value">{{ job.subjectName }}</text>
        </view>
        <view class="info-item">
            <text class="label">Grade</text>
            <text class="value">{{ job.gradeCode }}</text>
        </view>
        <view class="info-item">
            <text class="label">City</text>
            <text class="value">{{ job.city }}</text>
        </view>
        <view class="info-item">
            <text class="label">Mode</text>
            <text class="value">{{ job.classMode }}</text>
        </view>
    </view>

    <view class="section">
        <text class="section-title">Description</text>
        <text class="desc">{{ job.description }}</text>
    </view>

    <button v-if="userStore.isTutor" type="primary" class="action-btn" @click="handleContact">Contact Parent</button>
  </view>
  <view v-else class="loading">
      <text>Loading...</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { jobsApi } from '@/api/jobs';
import { chatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const job = ref<any>(null);

onLoad(async (options: any) => {
  if (options.id) {
    await fetchDetail(options.id);
  }
});

const fetchDetail = async (id: number) => {
  try {
    const res: any = await jobsApi.getDemand(id);
    job.value = res;
  } catch (error) {
    console.error(error);
    uni.showToast({ title: 'Failed to load details', icon: 'none' });
  }
};

const handleContact = async () => {
    if (!job.value) return;
    try {
        // Assuming job.publisherUid exists. If not, need backend to provide it.
        // Actually StudentJobPosting usually has publisherUid.
        const targetUid = job.value.publisherUid;
        if (!targetUid) {
            uni.showToast({ title: 'Cannot contact publisher', icon: 'none' });
            return;
        }
        
        const roomId: any = await chatApi.getOrCreateRoom(targetUid);
        if (roomId) {
            uni.navigateTo({ url: `/pages/chat/room?id=${roomId}` });
        }
    } catch (error) {
        console.error(error);
        uni.showToast({ title: 'Failed to start chat', icon: 'none' });
    }
};
</script>

<style lang="scss" scoped>
.container {
  padding: 20px;
  background-color: #fff;
  min-height: 100vh;
}
.header {
    margin-bottom: 20px;
    border-bottom: 1px solid #f0f0f0;
    padding-bottom: 15px;
    .title {
        font-size: 20px;
        font-weight: bold;
        display: block;
        margin-bottom: 10px;
    }
    .price {
        font-size: 18px;
        color: #ff5500;
        font-weight: bold;
    }
}
.info-grid {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 20px;
    .info-item {
        width: 50%;
        margin-bottom: 15px;
        .label {
            font-size: 12px;
            color: #999;
            display: block;
            margin-bottom: 2px;
        }
        .value {
            font-size: 14px;
            color: #333;
        }
    }
}
.section {
    margin-bottom: 30px;
    .section-title {
        font-size: 16px;
        font-weight: bold;
        margin-bottom: 10px;
        display: block;
    }
    .desc {
        font-size: 14px;
        color: #666;
        line-height: 1.6;
    }
}
.action-btn {
    width: 100%;
    border-radius: 25px;
}
.loading {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    color: #999;
}
</style>
