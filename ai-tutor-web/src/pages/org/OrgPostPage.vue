<script setup lang="ts">
import StudentPostPage from '@/pages/student/StudentPostPage.vue'
</script>

<template>
  <StudentPostPage publisher-identity="ORGANIZATION" after-success-route-name="orgMineJobs" api-variant="org" show-back back-route-name="orgMineJobs" />
</template>
