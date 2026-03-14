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

const userStore = useUserStore();
</script>
