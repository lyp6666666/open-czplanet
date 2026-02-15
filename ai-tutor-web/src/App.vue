<script setup lang="ts">
import { onErrorCaptured, ref } from 'vue'

const fatalError = ref<unknown>(null)

onErrorCaptured((err) => {
  fatalError.value = err
  return false
})
</script>

<template>
  <div v-if="fatalError" class="fatal">
    <div class="title">页面渲染失败</div>
    <pre class="detail">{{ String(fatalError) }}</pre>
  </div>
  <RouterView v-else />
</template>

<style scoped>
.fatal {
  padding: 16px;
  max-width: 900px;
  margin: 24px auto;
  border: 1px solid #f0b8b8;
  background: #fff2f2;
  border-radius: 12px;
}

.title {
  font-weight: 700;
  margin-bottom: 10px;
}

.detail {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}
</style>
