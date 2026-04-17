<template>
  <div class="grid">
    <div class="card stat">
      <div class="k">总用户数</div>
      <div class="v">{{ stats?.totalUsers ?? '-' }}</div>
    </div>
    <div class="card stat">
      <div class="k">活跃教师</div>
      <div class="v">{{ stats?.activeTeachers ?? '-' }}</div>
    </div>
    <div class="card stat">
      <div class="k">待审核需求</div>
      <div class="v">{{ stats?.pendingJobs ?? '-' }}</div>
    </div>
    <div class="card stat">
      <div class="k">待审核认证</div>
      <div class="v">{{ stats?.pendingVerifications ?? '-' }}</div>
    </div>
    <div class="card stat">
      <div class="k">待处理退款</div>
      <div class="v">{{ stats?.pendingRefunds ?? '-' }}</div>
    </div>

    <div class="card box">
      <div class="box-head">
        <div class="box-title">快捷入口</div>
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </div>
      <div class="links">
        <RouterLink class="btn btn-muted" to="/home-carousel">去首页轮播</RouterLink>
        <RouterLink class="btn btn-muted" to="/jobs">去需求审核</RouterLink>
        <RouterLink class="btn btn-muted" to="/verification">去认证审核</RouterLink>
        <RouterLink class="btn btn-muted" to="/refunds">去退款纠纷</RouterLink>
      </div>
      <div v-if="errorText" class="error">{{ errorText }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getDashboardStats } from '@/api/dashboard'
import type { DashboardStatsResponse } from '@/api/types'

const stats = ref<DashboardStatsResponse | null>(null)
const loading = ref(false)
const errorText = ref<string | null>(null)

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    stats.value = await getDashboardStats()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stat {
  padding: 14px;
}

.k {
  font-size: 12px;
  color: var(--muted);
}

.v {
  margin-top: 10px;
  font-weight: 800;
  font-size: 24px;
}

.box {
  grid-column: 1 / -1;
  padding: 14px;
}

.box-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.box-title {
  font-weight: 700;
}

.links {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.error {
  margin-top: 10px;
  color: var(--danger);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
