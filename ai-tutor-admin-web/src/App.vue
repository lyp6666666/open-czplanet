<template>
  <AdminShell v-if="showShell">
    <RouterView />
  </AdminShell>
  <RouterView v-else />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { setAuthInvalidHandler } from '@/api/http'
import { useAdminAuthStore } from '@/stores/auth'
import AdminShell from '@/ui/layout/AdminShell.vue'

const route = useRoute()
const router = useRouter()
const auth = useAdminAuthStore()

const showShell = computed(() => route.name !== 'login')

onMounted(() => {
  auth.loadFromStorage()
  setAuthInvalidHandler(() => {
    auth.logout()
    router.replace({ name: 'login' })
  })
})
</script>

