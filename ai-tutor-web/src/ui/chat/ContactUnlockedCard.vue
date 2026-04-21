<script setup lang="ts">
import type { ChatMessageBody } from '@/api/types'

defineProps<{
  body: Extract<ChatMessageBody, { type: 'contact_unlocked' }>
  canView?: boolean
}>()

const emit = defineEmits<{
  view: []
}>()
</script>

<template>
  <div class="cardx">
    <div class="eyebrow">支付成功</div>
    <div class="h1">聊天功能开启</div>
    <div class="hint">
      平台已确认信息费支付，可以使用聊天商定具体细节，注意不能透露任何联系方式，合适即可点击发起合作。
    </div>
    <button v-if="canView" class="view-btn" type="button" @click="emit('view')">查看对方的联系方式</button>
  </div>
</template>

<style scoped>
.cardx {
  min-width: 260px;
  max-width: 360px;
  border-radius: 14px;
  border: 1px solid rgba(14, 165, 233, 0.2);
  padding: 10px 10px 12px;
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.08), rgba(255, 255, 255, 0.96));
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.08);
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.12);
  color: #0369a1;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 8px;
}

.h1 {
  font-weight: 900;
  margin-bottom: 6px;
}

.hint {
  font-size: 12px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.6;
}

.view-btn {
  margin-top: 10px;
  height: 34px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
</style>
