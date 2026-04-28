<script setup lang="ts">
import type { LessonDetailModel } from '@/utils/lessonDetail'

const props = defineProps<{
  open: boolean
  model: LessonDetailModel | null
  cooperationName?: string | null
  primaryLabel?: string | null
  primaryDisabled?: boolean
  primaryHint?: string | null
}>()

const emit = defineEmits<{
  close: []
  primary: []
}>()

function toneClass(tone?: LessonDetailModel['statusTone']) {
  return `tone-${tone || 'slate'}`
}

function onClose() {
  emit('close')
}

function onPrimary() {
  emit('primary')
}
</script>

<template>
  <div v-if="open && model" class="mask" @click.self="onClose">
    <div class="modal-shell">
      <div class="modal-card">
        <div class="modal-head">
          <div>
            <div class="eyebrow">{{ cooperationName || '单节课详情' }}</div>
            <div class="modal-title">{{ model.title }}</div>
          </div>
          <button class="icon-btn" type="button" @click="onClose">关闭</button>
        </div>

        <div class="hero-panel">
          <div>
            <div class="hero-type">{{ model.lessonTypeLabel }}</div>
            <div class="hero-topic">{{ model.topic }}</div>
          </div>
          <span class="status-pill" :class="toneClass(model.statusTone)">{{ model.statusLabel }}</span>
        </div>

        <div class="info-grid">
          <div class="info-card">
            <span>计划开始</span>
            <strong>{{ model.startLabel }}</strong>
          </div>
          <div class="info-card">
            <span>计划结束</span>
            <strong>{{ model.endLabel }}</strong>
          </div>
          <div class="info-card">
            <span>预计时长</span>
            <strong>{{ model.durationText }}</strong>
          </div>
        </div>

        <section class="section-block">
          <div class="section-label">这一节我要学什么</div>
          <div class="section-copy">{{ model.description }}</div>
        </section>

        <section class="section-block">
          <div class="section-label">{{ model.summaryTitle }}</div>
          <div class="section-copy">{{ model.summaryText }}</div>
        </section>

        <div class="hint-line">{{ primaryHint || model.helperText }}</div>

        <div class="actions">
          <button class="btn" type="button" @click="onClose">关闭</button>
          <button
            v-if="primaryLabel"
            class="btn btn-primary"
            type="button"
            :disabled="primaryDisabled"
            @click="onPrimary"
          >
            {{ primaryLabel }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.32);
  backdrop-filter: blur(4px);
}

.modal-shell {
  width: min(640px, 100%);
}

.modal-card {
  display: grid;
  gap: 18px;
  padding: 22px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
}

.modal-head,
.hero-panel,
.info-grid,
.actions {
  display: flex;
  gap: 12px;
}

.modal-head,
.hero-panel,
.actions {
  align-items: center;
  justify-content: space-between;
}

.eyebrow,
.section-label,
.info-card span,
.hero-type {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.56);
}

.modal-title {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 800;
  color: #111827;
}

.icon-btn {
  border: 0;
  background: transparent;
  color: rgba(15, 23, 42, 0.66);
  cursor: pointer;
}

.hero-panel {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(240, 253, 250, 0.92), rgba(248, 250, 252, 0.98));
}

.hero-topic {
  margin-top: 6px;
  font-size: 17px;
  line-height: 1.55;
  color: #0f172a;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.info-card,
.section-block {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
}

.info-card strong,
.section-copy {
  color: #111827;
  line-height: 1.6;
}

.section-copy {
  font-size: 14px;
}

.hint-line {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.64);
}

.actions {
  justify-content: flex-end;
}

.btn {
  height: 40px;
  padding: 0 16px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

.btn.btn-primary {
  border-color: transparent;
  background: #0f766e;
  color: #fff;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
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

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .modal-card {
    padding: 18px;
  }

  .modal-title {
    font-size: 20px;
  }

  .hero-panel,
  .modal-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
