<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'

const router = useRouter()
const settings = useSettingsStore()

const loading = ref(false)
const error = ref<string | null>(null)
const saved = ref<string | null>(null)

const greeting = ref(DEFAULT_APPLICATION_GREETING)

async function load() {
  loading.value = true
  error.value = null
  saved.value = null
  try {
    const v = await settings.load()
    greeting.value = v
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (loading.value) return
  loading.value = true
  error.value = null
  saved.value = null
  try {
    const v = await settings.saveApplicationGreeting(greeting.value)
    greeting.value = v
    saved.value = '已保存'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    loading.value = false
  }
}

function onReset() {
  greeting.value = DEFAULT_APPLICATION_GREETING
}

function back() {
  router.back()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <button class="btn" type="button" @click="back">返回</button>
      <div class="title">设置</div>
      <div class="actions">
        <button class="btn" type="button" :disabled="loading" @click="onReset">恢复默认</button>
        <button class="btn btn-primary" type="button" :disabled="loading" @click="onSave">保存</button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="saved" class="hint ok">{{ saved }}</div>

    <div class="card form">
      <div class="sec">
        <div class="sec-title">默认申请问候语</div>
        <div class="sec-desc">发起申请时会自动填充，可在发送前临时修改。</div>
        <textarea v-model="greeting" class="txt" rows="4" />
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
  gap: 12px;
  align-items: center;
}

.title {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.form {
  padding: 12px;
}

.sec {
  display: grid;
  gap: 8px;
}

.sec-title {
  font-weight: 900;
}

.sec-desc {
  font-size: 12px;
  color: var(--muted);
}

.txt {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  resize: vertical;
  font-size: 13px;
  line-height: 1.4;
}
</style>

