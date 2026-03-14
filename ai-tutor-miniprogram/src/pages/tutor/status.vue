<template>
  <view class="container">
    <view v-if="status === 'PENDING'" class="status-content">
        <icon type="waiting" size="80" color="#10aeff"/>
        <text class="title">Under Review</text>
        <text class="desc">Your application is being reviewed. Please wait patiently.</text>
        <button class="btn" @click="goHome">Back to Home</button>
    </view>
    
    <view v-else-if="status === 'REJECTED'" class="status-content">
        <icon type="warn" size="80" color="#f5222d"/>
        <text class="title">Application Rejected</text>
        <text class="desc">Reason: Profile incomplete or verification failed.</text>
        <button class="btn primary" @click="reapply">Edit Application</button>
        <button class="btn" @click="goHome">Back to Home</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const status = ref('');

onLoad((options: any) => {
    status.value = options.status || 'PENDING';
});

const goHome = () => {
    uni.reLaunch({ url: '/pages/home/index' });
};

const reapply = () => {
    uni.navigateTo({ url: '/pages/tutor/onboarding/index' });
};
</script>

<style lang="scss" scoped>
.container {
    padding: 40px 20px;
    background-color: #fff;
    min-height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
}
.status-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    
    .title {
        font-size: 24px;
        font-weight: bold;
        margin-top: 20px;
        margin-bottom: 10px;
    }
    .desc {
        font-size: 14px;
        color: #666;
        margin-bottom: 40px;
        line-height: 1.5;
    }
    .btn {
        width: 200px;
        margin-bottom: 15px;
        border-radius: 25px;
        &.primary {
            background-color: #007aff;
            color: #fff;
        }
    }
}
</style>
