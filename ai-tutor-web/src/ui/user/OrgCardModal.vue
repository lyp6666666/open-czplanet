<script setup lang="ts">
import { ref, watch } from 'vue'
import { userApi } from '@/api/user'
import type { OrganizationProfile } from '@/api/types'

const props = defineProps<{
  open: boolean
  orgId: number | null
}>()

const emit = defineEmits<{
  close: []
}>()

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<OrganizationProfile | null>(null)

function close() {
  emit('close')
}

watch(
  () => [props.open, props.orgId] as const,
  async ([open, id]) => {
    if (!open || !id) return
    loading.value = true
    error.value = null
    data.value = null
    try {
      data.value = await userApi.orgPublicProfile(id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载失败'
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)
</script>

<template>
  <div v-if="open" class="mask" @click.self="close">
    <div class="modal card">
      <div class="m-head">
        <div class="m-title">机构详情</div>
        <button class="icon-btn" type="button" @click="close">×</button>
      </div>

      <div v-if="loading" class="hint">加载中...</div>
      <div v-else-if="error" class="hint error">{{ error }}</div>

      <div v-else-if="data" class="content">
        <div class="name">{{ data.orgName }}</div>
        <div class="meta">
          <span v-if="data.address">{{ data.address }}</span>
          <span v-if="data.contactPhone">{{ data.contactPhone }}</span>
        </div>

        <div class="sec">
          <div class="sec-title">机构介绍</div>
          <div class="sec-body">{{ data.intro || '—' }}</div>
        </div>

        <div class="hint notice">
          <div class="n-title">责任说明</div>
          <div class="n-body">
            机构为需求发布与履约主体，平台提供信息撮合、支付托管与纠纷介入机制；平台不直接保证授课质量与履约结果。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 60;
}

.modal {
  width: min(560px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
  max-height: min(78vh, 720px);
  overflow: auto;
  background: #fff;
}

.m-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.m-title {
  font-weight: 900;
  font-size: 18px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  display: grid;
  place-items: center;
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

.content {
  display: grid;
  gap: 12px;
}

.name {
  font-size: 18px;
  font-weight: 900;
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
  white-space: pre-wrap;
  word-break: break-word;
}

.hint.notice {
  border-color: rgba(255, 170, 0, 0.28);
  background: rgba(255, 170, 0, 0.06);
}

.n-title {
  font-weight: 900;
  margin-bottom: 4px;
}

.n-body {
  color: var(--muted);
  line-height: 1.6;
}
</style>
