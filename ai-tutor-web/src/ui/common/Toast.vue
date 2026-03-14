<script setup lang="ts">
import { useToastStore } from '@/stores/toast'
import { storeToRefs } from 'pinia'

const store = useToastStore()
const { visible, message, type } = storeToRefs(store)

function close() {
  store.hide()
}
</script>

<template>
  <Transition name="toast-fade">
    <div v-if="visible" class="toast" :class="type" @click="close">
      <div class="icon">
        <svg v-if="type === 'success'" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M20 6L9 17L4 12" stroke="#52c41a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        <svg v-else-if="type === 'error'" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="10" stroke="#ff4d4f" stroke-width="2"/><path d="M12 8V12" stroke="#ff4d4f" stroke-width="2" stroke-linecap="round"/><path d="M12 16H12.01" stroke="#ff4d4f" stroke-width="2" stroke-linecap="round"/></svg>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="10" stroke="#1890ff" stroke-width="2"/><path d="M12 16V12" stroke="#1890ff" stroke-width="2" stroke-linecap="round"/><path d="M12 8H12.01" stroke="#1890ff" stroke-width="2" stroke-linecap="round"/></svg>
      </div>
      <div class="text">{{ message }}</div>
    </div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  min-width: 300px;
  max-width: 90vw;
  border: 1px solid #eee;
}

.toast.success {
  border-left: 4px solid #52c41a;
}

.toast.error {
  border-left: 4px solid #ff4d4f;
}

.toast.info {
  border-left: 4px solid #1890ff;
}

.text {
  color: #333;
  flex: 1;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}
</style>
