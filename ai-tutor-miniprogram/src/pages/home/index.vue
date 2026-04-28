<template>
  <view>
    <ParentHome v-if="userStore.currentRole === 'student'" />
    <TutorHome v-else />
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { resumePendingRedirect } from '@/utils/authRedirect';
import ParentHome from './components/ParentHome.vue';
import TutorHome from './components/TutorHome.vue';

const userStore = useUserStore();

onShow(() => {
  if (!userStore.isLoggedIn) return;
  resumePendingRedirect();
});
</script>
