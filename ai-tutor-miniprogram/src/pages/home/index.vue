<template>
  <view>
    <ParentHome v-if="userStore.currentRole === 'student'" />
    <TutorHome v-else />
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import ParentHome from './components/ParentHome.vue';
import TutorHome from './components/TutorHome.vue';
import { onShow } from '@dcloudio/uni-app';

const userStore = useUserStore();

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.switchTab({ url: '/pages/me/index' });
  }
});
</script>
