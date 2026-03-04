<template>
  <div class="mask" role="dialog" aria-modal="true">
    <div class="panel card">
      <div class="head">
        <div class="title">{{ title }}</div>
        <button class="btn btn-muted" type="button" @click="$emit('close')">关闭</button>
      </div>
      <div class="body">
        <slot />
      </div>
      <div class="foot">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string }>()
defineEmits<{ close: [] }>()
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 30;
}

.panel {
  width: 560px;
  max-width: 100%;
  padding: 14px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title {
  font-weight: 800;
}

.body {
  margin-top: 12px;
}

.foot {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

