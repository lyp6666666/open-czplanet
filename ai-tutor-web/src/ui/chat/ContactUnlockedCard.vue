<script setup lang="ts">
import type { ChatMessageBody } from '@/api/types'

defineProps<{
  body: Extract<ChatMessageBody, { type: 'contact_unlocked' }>
  canView: boolean
}>()

const emit = defineEmits<{
  (e: 'view'): void
}>()
</script>

<template>
  <div class="cardx">
    <div class="h1">联系方式已解锁</div>
    <div class="hint">平台已确认信息费支付，可查看对方联系方式。</div>
    <div v-if="canView" class="ops">
      <button class="btn btn-primary" type="button" @click="emit('view')">查看联系方式</button>
    </div>
  </div>
</template>

<style scoped>
.cardx {
  min-width: 260px;
  max-width: 360px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 10px 10px 12px;
  background: rgba(0, 190, 189, 0.06);
}

.h1 {
  font-weight: 900;
  margin-bottom: 6px;
}

.hint {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
}

.ops {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
