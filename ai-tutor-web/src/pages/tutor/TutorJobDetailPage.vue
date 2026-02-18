<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { DemandViewVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatClassMode, formatEducationRequirement, formatScheduleText } from '@/utils/present'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<DemandViewVO | null>(null)
const favorited = ref(false)

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await jobsApi.getDemandView(id.value)
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
    if (!auth.me) {
      await auth.refreshMe()
    }
    const greeting = auth.me?.teacherProfile?.defaultGreeting ?? null
    const roomId = await chatApi.startRoom(data.value.parentId, greeting)
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
        <span>{{ (data.classMode || '').toLowerCase() === 'online' ? '线上' : data.city || '线下' }}</span>
        <span>{{ formatClassMode(data.classMode) }}</span>
        <span>每周{{ data.frequencyPerWeek || '-' }}次</span>
        <span>{{ formatEducationRequirement(data.educationRequirement) }}</span>
        <span v-if="data.budgetMin || data.budgetMax">{{ data.budgetMin || '-' }}-{{ data.budgetMax || '-' }}/小时</span>
      </div>

      <div class="sec">
        <div class="sec-title">需求描述</div>
        <div class="sec-body">{{ data.description || '—' }}</div>
      </div>

      <div v-if="data.publisher" class="sec publisher">
        <img v-if="data.publisher.avatar" class="avatar" :src="data.publisher.avatar" alt="avatar" />
        <div v-else class="avatar fallback">{{ (data.publisher.displayName || 'U').slice(0, 1) }}</div>
        <div class="pub-info">
          <div class="pub-name">{{ data.publisher.displayName }}</div>
          <div class="pub-tag">{{ data.publisher.identityLabel }}</div>
        </div>
      </div>

      <div class="sec" v-if="data.classMode !== 'online'">
        <div class="sec-title">工作地址</div>
        <div class="sec-body">{{ [data.city, data.address].filter(Boolean).join(' · ') || '—' }}</div>
      </div>

      <div class="sec">
        <div class="sec-title">授课时间</div>
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

.publisher {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  background: rgba(0, 190, 189, 0.08);
}

.pub-info {
  display: grid;
  gap: 4px;
}

.pub-name {
  font-weight: 900;
}

.pub-tag {
  font-size: 12px;
  color: var(--muted);
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
