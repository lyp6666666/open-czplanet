<script setup lang="ts">
import type { LessonDetailModel } from '@/utils/lessonDetail'

defineProps<{
  model: LessonDetailModel
}>()

function toneClass(tone: LessonDetailModel['statusTone']) {
  return `tone-${tone}`
}
</script>

<template>
  <div class="preview-card">
    <div class="preview-head">
      <div>
        <div class="preview-type">{{ model.lessonTypeLabel }}</div>
        <div class="preview-title">{{ model.title }}</div>
      </div>
      <span class="status-pill" :class="toneClass(model.statusTone)">{{ model.statusLabel }}</span>
    </div>
    <div class="preview-topic">{{ model.topic }}</div>
    <div class="preview-grid">
      <div class="preview-kv">
        <span>开始</span>
        <strong>{{ model.startLabel }}</strong>
      </div>
      <div class="preview-kv">
        <span>结束</span>
        <strong>{{ model.endLabel }}</strong>
      </div>
    </div>
    <div class="preview-foot">{{ model.helperText }}</div>
  </div>
</template>

<style scoped>
.preview-card {
  width: min(320px, 78vw);
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.16);
}

.preview-head,
.preview-grid {
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.preview-grid {
  gap: 10px;
}

.preview-type,
.preview-kv span {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.56);
}

.preview-title {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}

.preview-topic,
.preview-foot {
  color: rgba(15, 23, 42, 0.72);
  line-height: 1.55;
  font-size: 13px;
}

.preview-kv {
  flex: 1 1 0;
  display: grid;
  gap: 4px;
}

.preview-kv strong {
  font-size: 13px;
  color: #111827;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.tone-slate {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.tone-amber {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.tone-sky {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.tone-emerald {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.tone-rose {
  background: rgba(244, 63, 94, 0.12);
  color: #be123c;
}
</style>
