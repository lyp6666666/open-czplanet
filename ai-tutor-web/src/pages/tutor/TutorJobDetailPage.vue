<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { StudentJobPosting } from '@/api/types'
import { formatClassMode, formatEducationRequirement, formatScheduleText, formatStageCode } from '@/utils/present'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<StudentJobPosting | null>(null)
const favorited = ref(false)

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await jobsApi.getDemand(id.value)
    try {
      const fav = await favoritesApi.checkDemandFavorites([id.value])
      favorited.value = Array.isArray(fav) && fav.includes(id.value)
    } catch {
      favorited.value = false
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onChat() {
  if (!data.value) return
  try {
    const roomId = await chatApi.getOrCreateRoom(data.value.parentId)
    await router.push({ name: 'chatRoom', params: { roomId }, query: { otherUid: String(data.value.parentId) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发起沟通失败'
  }
}

async function onToggleFavorite() {
  if (!data.value) return
  try {
    if (favorited.value) {
      await favoritesApi.unfavoriteDemand(data.value.id)
      favorited.value = false
    } else {
      await favoritesApi.favoriteDemand(data.value.id)
      favorited.value = true
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <button class="btn" type="button" @click="router.back()">返回</button>
      <div class="title">需求详情</div>
      <div class="head-ops">
        <button class="btn" type="button" :disabled="loading || !data" @click="onToggleFavorite">
          {{ favorited ? '已收藏' : '收藏' }}
        </button>
        <button class="btn btn-primary" type="button" :disabled="loading || !data" @click="onChat">立即沟通</button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="data" class="card detail">
      <div class="t">{{ data.title }}</div>
      <div class="meta">
        <span v-if="data.city">{{ data.city }}</span>
        <span v-if="data.classMode">{{ formatClassMode(data.classMode) }}</span>
        <span v-if="data.stageCode">{{ formatStageCode(data.stageCode) }}</span>
        <span v-if="data.educationRequirement">{{ formatEducationRequirement(data.educationRequirement) }}</span>
        <span v-if="data.budgetMin || data.budgetMax">{{ data.budgetMin || '-' }}-{{ data.budgetMax || '-' }}/小时</span>
      </div>

      <div class="sec">
        <div class="sec-title">需求描述</div>
        <div class="sec-body">{{ data.description || '—' }}</div>
      </div>

      <div class="sec">
        <div class="sec-title">孩子年龄</div>
        <div class="sec-body">{{ data.childAge ?? '—' }}</div>
      </div>

      <div class="sec" v-if="data.classMode !== 'online'">
        <div class="sec-title">上课地址</div>
        <div class="sec-body">{{ data.address || '—' }}</div>
      </div>

      <div class="sec">
        <div class="sec-title">期望时间</div>
        <div class="sec-body">{{ data.schedule ? formatScheduleText(data.schedule) : '—' }}</div>
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
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
}

.head-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.title {
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.detail {
  padding: 16px;
  display: grid;
  gap: 10px;
}

.t {
  font-weight: 900;
  font-size: 16px;
}

.meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.sec {
  display: grid;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}

.sec-title {
  font-size: 12px;
  color: var(--muted);
  font-weight: 800;
}

.sec-body {
  font-size: 13px;
  line-height: 1.6;
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
