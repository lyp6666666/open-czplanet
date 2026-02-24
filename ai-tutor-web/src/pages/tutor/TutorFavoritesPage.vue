<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { DemandViewVO } from '@/api/types'
import { formatClassMode, formatEducationRequirement } from '@/utils/present'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)

const list = ref<DemandViewVO[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await favoritesApi.pageDemandFavorites({ pageSize: 10, cursor: cursor.value })
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast

    const ids = (page.list || []).filter((id) => typeof id === 'number' && Number.isFinite(id))
    const settled = await Promise.allSettled(ids.map((id) => jobsApi.getDemandView(id)))
    const views = settled.flatMap((r) => (r.status === 'fulfilled' && r.value ? [r.value] : []))
    list.value = [...list.value, ...views]
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  void router.push({ name: 'tutorJobDetail', params: { id } })
}

async function onUnfavorite(id: number) {
  try {
    await favoritesApi.unfavoriteDemand(id)
    list.value = list.value.filter((it) => it.id !== id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

onMounted(() => {
  void loadMore()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">我的收藏</div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card list">
      <div v-if="list.length === 0 && !loading" class="empty">
        <div class="empty-title">暂无收藏需求</div>
        <div class="empty-desc">去需求列表页收藏你感兴趣的需求</div>
        <button class="btn btn-primary" type="button" @click="router.push('/tutor/jobs')">去逛逛</button>
      </div>

      <div v-else class="items">
        <div v-for="it in list" :key="it.id" class="item">
          <div class="main">
            <div class="t" @click="goDetail(it.id)">{{ it.title }}</div>
            <div class="meta">
              <span v-if="it.city">{{ it.city }}</span>
              <span v-if="it.classMode">{{ formatClassMode(it.classMode) }}</span>
              <span v-if="it.educationRequirement">{{ formatEducationRequirement(it.educationRequirement) }}</span>
              <span v-if="it.budgetMin || it.budgetMax">{{ it.budgetMin || '-' }}-{{ it.budgetMax || '-' }}/小时</span>
            </div>
          </div>
          <div class="ops">
            <button class="btn" type="button" @click="goDetail(it.id)">查看</button>
            <button class="btn danger" type="button" @click="onUnfavorite(it.id)">取消收藏</button>
          </div>
        </div>
      </div>

      <div class="footer" v-if="list.length > 0">
        <button class="btn" type="button" :disabled="loading || isLast" @click="loadMore">
          <span v-if="isLast">没有更多了</span>
          <span v-else>{{ loading ? '加载中...' : '加载更多' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 900;
}

.list {
  padding: 14px;
}

.empty {
  padding: 28px 10px;
  display: grid;
  gap: 10px;
  justify-items: start;
}

.empty-title {
  font-weight: 900;
}

.empty-desc {
  color: var(--muted);
  font-size: 13px;
}

.items {
  display: grid;
  gap: 10px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
}

.t {
  font-weight: 900;
  font-size: 14px;
  cursor: pointer;
}

.meta {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.ops {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.danger {
  border-color: rgba(255, 0, 0, 0.25);
  color: #b42318;
}

.footer {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

.hint {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: #fff;
}

.hint.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
}
</style>

