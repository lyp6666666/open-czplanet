<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import type { StudentJobPosting } from '@/api/types'
import { formatClassMode, formatDemandBizStatus } from '@/utils/present'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const list = ref<StudentJobPosting[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const highlightId = computed(() => {
  const raw = route.query.highlight
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await jobsApi.mineDemands({ pageSize: 10, cursor: cursor.value })
    list.value = [...list.value, ...(page.list || [])]
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function goEdit(id: number) {
  void router.push({ name: 'studentEditJob', params: { id } })
}

function goDetail(id: number) {
  void router.push({ name: 'studentMineJobDetail', params: { id } })
}

onMounted(() => {
  void loadMore()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">我的需求</div>
      <button class="btn btn-primary" type="button" @click="router.push('/student/post')">发布新需求</button>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card list">
      <div v-if="list.length === 0 && !loading" class="empty">
        <div class="empty-title">还没有需求</div>
        <div class="empty-desc">发布一条需求，让老师主动来开聊</div>
        <button class="btn btn-primary" type="button" @click="router.push('/student/post')">去发布</button>
      </div>

      <div v-else class="items">
        <div
          v-for="it in list"
          :key="it.id"
          class="item"
          :class="{ hl: highlightId != null && it.id === highlightId }"
        >
          <div class="main">
            <div class="trow">
              <div class="t">{{ it.title }}</div>
              <div class="tag">{{ formatDemandBizStatus(it.bizStatus, it.status) }}</div>
            </div>
            <div class="meta">
              <span v-if="it.city">{{ it.city }}</span>
              <span v-if="it.classMode">{{ formatClassMode(it.classMode) }}</span>
              <span v-if="it.budgetMin || it.budgetMax">
                {{ it.budgetMin || '-' }}-{{ it.budgetMax || '-' }}/小时
              </span>
            </div>
          </div>
          <div class="ops">
            <button class="btn" type="button" @click="goDetail(it.id)">查看</button>
            <button class="btn" type="button" @click="goEdit(it.id)">编辑</button>
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

.item.hl {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.ops {
  display: flex;
  gap: 8px;
}

.t {
  font-weight: 900;
  font-size: 14px;
}

.trow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--muted);
  flex: 0 0 auto;
}

.meta {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
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
