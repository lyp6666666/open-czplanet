<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import type { DemandViewVO } from '@/api/types'
import { useToastStore } from '@/stores/toast'
import { formatClassMode, formatEducationRequirement, formatScheduleText } from '@/utils/present'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<DemandViewVO | null>(null)
const actionBusy = ref(false)

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await jobsApi.getDemandView(id.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

function isClosed(it: Pick<DemandViewVO, 'status'> | null | undefined): boolean {
  return !!it && it.status === 0
}

async function togglePublishStatus() {
  if (!data.value || actionBusy.value) return
  actionBusy.value = true
  try {
    const nextStatus = isClosed(data.value) ? 1 : 0
    await jobsApi.updateDemand(data.value.id, { status: nextStatus })
    toast.show(nextStatus === 1 ? '需求已重新公开' : '需求已关闭', 'success')
    await load()
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '操作失败', 'error')
  } finally {
    actionBusy.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <div class="head">
      <button class="btn" type="button" @click="router.back()">返回</button>
      <div class="title">需求详情</div>
      <div class="head-ops">
        <button class="btn" type="button" @click="router.push({ name: 'studentEditJob', params: { id: data?.id } })" :disabled="!data">
          编辑
        </button>
        <button class="btn" type="button" :disabled="!data || actionBusy" @click="togglePublishStatus">
          {{ isClosed(data) ? '重新公开' : '关闭需求' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="data" class="card detail">
      <div class="title-row">
        <div class="t">{{ data.title }}</div>
        <span v-if="isClosed(data)" class="closed-badge">已关闭</span>
      </div>
      <div class="meta">
        <span>{{ (data.classMode || '').toLowerCase() === 'online' ? '线上' : data.city || '线下' }}</span>
        <span>{{ formatClassMode(data.classMode) }}</span>
        <span>每周{{ data.frequencyPerWeek || '-' }}次</span>
        <span>{{ formatEducationRequirement(data.educationRequirement) }}</span>
        <span v-if="data.budgetMin || data.budgetMax">{{ data.budgetMin || '-' }}-{{ data.budgetMax || '-' }}/小时</span>
      </div>

      <div v-if="isClosed(data)" class="hint notice">
        当前需求已停止公开推荐。点击右上角“重新公开”后，会重新回到首页/需求页的公开推荐池。
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
          <div class="pub-tag">发布时间：{{ data.createTime ? String(data.createTime).replace('T', ' ').slice(0, 16) : '' }}</div>
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

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.t {
  font-weight: 900;
  font-size: 16px;
}

.closed-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  color: #b54708;
  border: 1px solid rgba(245, 158, 11, 0.35);
  background: rgba(245, 158, 11, 0.12);
  font-weight: 800;
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

.hint.notice {
  border-color: rgba(255, 170, 0, 0.28);
  background: rgba(255, 170, 0, 0.06);
}
</style>
