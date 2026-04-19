<script setup lang="ts">
import { computed, onBeforeUnmount, onErrorCaptured, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AppTopBar from '@/ui/layout/AppTopBar.vue'
import Toast from '@/ui/common/Toast.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'

const fatalError = ref<unknown>(null)
const route = useRoute()
const auth = useAuthStore()
const chatRealtime = useChatRealtimeStore()

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
    p.startsWith('/live/') ||
    p === '/me' ||
    p.startsWith('/schedule')
  )
})

const canUseChatRealtime = computed(() => auth.user?.userType === 1 || auth.user?.userType === 2)
const currentChatRoomId = computed<number | null>(() => {
  const raw = route.params.roomId
  const roomId = typeof raw === 'string' ? Number(raw) : Array.isArray(raw) ? Number(raw[0]) : Number.NaN
  if (!Number.isFinite(roomId) || roomId <= 0) return null
  return route.name === 'chatRoom' || route.path.startsWith('/chat/') ? roomId : null
})

function syncRealtimeLifecycle() {
  chatRealtime.stop()
  if (!auth.isLoggedIn || !canUseChatRealtime.value) {
    chatRealtime.resetState()
    return
  }
  // 全局统一维护活跃会话与实时连接，避免某个具体布局组件被卸载后丢失实时能力。
  chatRealtime.setActiveRoom(currentChatRoomId.value)
  void chatRealtime.refreshUnreadFromServer()
  void chatRealtime.start()
}

watch(
  () => auth.token,
  () => {
    syncRealtimeLifecycle()
  },
)

watch(
  () => route.fullPath,
  () => {
    if (!auth.isLoggedIn || !canUseChatRealtime.value) {
      chatRealtime.setActiveRoom(null)
      return
    }
    chatRealtime.setActiveRoom(currentChatRoomId.value)
    void chatRealtime.refreshUnreadFromServer()
  },
)

onMounted(() => {
  syncRealtimeLifecycle()
})

onBeforeUnmount(() => {
  chatRealtime.stop()
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
