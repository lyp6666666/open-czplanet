<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import type { OrganizationProfile } from '@/api/types'

const route = useRoute()
const router = useRouter()

const orgUserId = computed(() => Number(route.params.orgUserId))
const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<OrganizationProfile | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await userApi.orgPublicProfile(orgUserId.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
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
      <div class="title">机构主页</div>
      <div />
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="data" class="card box">
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

.title {
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.box {
  padding: 16px;
  display: grid;
  gap: 10px;
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

.n-title {
  font-weight: 900;
  margin-bottom: 4px;
}

.n-body {
  color: var(--muted);
  line-height: 1.6;
}
</style>
