<script setup lang="ts">
defineProps<{
  open: boolean
  uid: number | null
  phone: string
  loading: boolean
  error: string | null
}>()

const emit = defineEmits<{
  close: []
}>()

function close() {
  emit('close')
}
</script>

<template>
  <div v-if="open" class="mask" @click.self="close">
    <div class="modal card">
      <div class="m-head">
        <div class="title">联系方式</div>
        <button class="icon-btn" type="button" @click="close">×</button>
      </div>

      <div v-if="loading" class="hint">加载中...</div>
      <div v-else-if="error" class="hint error">{{ error }}</div>
      <div v-else class="kv">
        <div class="row">
          <span class="k">UID</span>
          <span class="v">{{ uid || '-' }}</span>
        </div>
        <div class="row">
          <span class="k">手机号</span>
          <span class="v">{{ phone || '-' }}</span>
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
  width: min(520px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.m-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-weight: 900;
  font-size: 16px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.hint {
  font-size: 12px;
  color: var(--text);
}

.hint.error {
  color: #ff4d4f;
  font-weight: 700;
}

.kv {
  display: grid;
  gap: 10px;
}

.row {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 10px;
  font-size: 13px;
}

.k {
  color: var(--muted);
}

.v {
  font-weight: 800;
}
</style>

