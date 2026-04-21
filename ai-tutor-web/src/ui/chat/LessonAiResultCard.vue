<script setup lang="ts">
import type { ChatMessageBody } from '@/api/types'

const props = defineProps<{
  body: Extract<ChatMessageBody, { type: 'lesson_ai_result' }>
}>()

const emit = defineEmits<{
  openSummary: [courseId: number]
}>()

function statusText(status: string | null | undefined) {
  const normalized = String(status || '').trim().toUpperCase()
  if (normalized === 'READY') return '已生成'
  if (normalized === 'FINALIZING') return '生成中'
  if (normalized === 'FAILED') return '生成失败'
  if (normalized === 'OFF') return '未开启'
  return normalized || '处理中'
}

function openSummary() {
  const courseId = Number(props.body.contextId || props.body.eventId || 0)
  if (!(courseId > 0)) return
  emit('openSummary', courseId)
}
</script>

<template>
  <div class="lesson-ai-card">
    <div class="head">
      <div class="title">{{ body.title || '本节课 AI 总结' }}</div>
      <span class="status">{{ statusText(body.status) }}</span>
    </div>
    <div class="desc">课程已结束，AI 已整理本节课结果。</div>
    <div v-if="body.content" class="preview">{{ body.content }}</div>
    <div class="meta">报告草稿：{{ body.reportStatus || '待确认' }}</div>
    <div class="ops">
      <button class="btn btn-primary" type="button" @click="openSummary">查看课后总结</button>
    </div>
  </div>
</template>

<style scoped>
.lesson-ai-card {
  display: grid;
  gap: 10px;
  min-width: 280px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.title {
  font-weight: 700;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.14);
  color: #0f766e;
  font-size: 12px;
}

.desc,
.meta,
.preview {
  color: rgba(15, 23, 42, 0.72);
  line-height: 1.6;
}

.preview {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(241, 245, 249, 0.9);
}

.ops {
  display: flex;
  justify-content: flex-end;
}
</style>
