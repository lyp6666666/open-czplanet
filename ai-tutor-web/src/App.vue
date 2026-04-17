<script setup lang="ts">
import { computed, onErrorCaptured, ref } from 'vue'
import { useRoute } from 'vue-router'

import AppTopBar from '@/ui/layout/AppTopBar.vue'
import Toast from '@/ui/common/Toast.vue'
import { useAuthStore } from '@/stores/auth'

const fatalError = ref<unknown>(null)
const route = useRoute()
const auth = useAuthStore()

const useAppFrame = computed(() => {
  const p = route.path
  if (p.startsWith('/auth/')) return false
  if (p.startsWith('/student/onboarding/first-demand')) return false
  if (p.startsWith('/tutor/onboarding/basic')) return false
  if (p.startsWith('/tutor/onboarding/profile')) return false
  if (p === '/') return auth.isLoggedIn
  if (p.startsWith('/guide/')) return auth.isLoggedIn
  return (
    p.startsWith('/tutor/') ||
    p.startsWith('/student/') ||
    p.startsWith('/org/') ||
    p.startsWith('/organization/') ||
    p.startsWith('/chat') ||
    p === '/me' ||
    p.startsWith('/schedule')
  )
})

onErrorCaptured((err) => {
  fatalError.value = err
  return false
})
</script>

<template>
  <div v-if="fatalError" class="fatal">
    <div class="title">页面渲染失败</div>
    <pre class="detail">{{ String(fatalError) }}</pre>
  </div>
  <div v-else-if="useAppFrame" class="app">
    <AppTopBar />
    <main class="main">
      <div class="container">
        <RouterView />
      </div>
    </main>
  </div>
  <RouterView v-else />
  <Toast />
</template>

<style scoped>
.fatal {
  padding: 16px;
  max-width: 900px;
  margin: 24px auto;
  border: 1px solid #f0b8b8;
  background: #fff2f2;
  border-radius: 12px;
}

.title {
  font-weight: 700;
  margin-bottom: 10px;
}

.detail {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.main {
  padding: 18px 0 32px;
}
</style>
