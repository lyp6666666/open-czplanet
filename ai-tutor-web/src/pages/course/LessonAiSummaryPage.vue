<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { scheduleApi } from '@/api/schedule'
import { liveApi, type LiveAiResultResp } from '@/api/live'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const sessionId = ref<number>(Number(route.query.sessionId || 0))

const loading = ref(false)
const error = ref<string | null>(null)
const result = ref<LiveAiResultResp | null>(null)

function summaryField(key: string): unknown {
  return result.value?.summary?.[key]
}

function reportField(key: string): unknown {
  return result.value?.report?.[key]
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map((item) => String(item || '').trim()).filter(Boolean) : []
}

const titleText = computed(() => String(reportField('reportTitle') || `课程 #${courseId.value} 课后总结`))
const stageSummary = computed(() => String(summaryField('stageSummary') || reportField('parentSummary') || '暂无摘要'))
const keyPoints = computed(() => stringList(summaryField('keyPoints') || reportField('knowledgePoints')))
const questions = computed(() => stringList(result.value?.summary?.studentQuestions))
const homework = computed(() => stringList(summaryField('homeworkCandidates') || reportField('homework')))
const nextPlan = computed(() => String(reportField('nextLessonPlan') || ''))

function selectLatestLessonId(items: Array<{ id?: number | null; endAt?: number | null; startAt?: number | null; status?: string | null }>) {
  const candidates = items
    .filter((item) => Number(item?.id || 0) > 0)
    .slice()
    .sort((a, b) => {
      const aEnd = Number(a.endAt || 0)
      const bEnd = Number(b.endAt || 0)
      if (aEnd !== bEnd) return bEnd - aEnd
      return Number(b.startAt || 0) - Number(a.startAt || 0)
    })
  return Number(candidates[0]?.id || 0)
}

async function resolveSessionId() {
  if (sessionId.value > 0) {
    return sessionId.value
  }
  if (!(courseId.value > 0)) {
    return 0
  }
  try {
    const lessons = await scheduleApi.listCourseEvents(courseId.value)
    const lessonId = selectLatestLessonId(lessons || [])
    if (!(lessonId > 0)) {
      return 0
    }
    const live = await liveApi.getByCourse(lessonId)
    sessionId.value = Number(live.sessionId || 0)
    return sessionId.value
  } catch {
    return 0
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const resolvedSessionId = await resolveSessionId()
    if (!(resolvedSessionId > 0)) {
      error.value = '当前课程还没有可查看的课堂 AI 总结'
      result.value = null
      return
    }
    result.value = await liveApi.aiResult(resolvedSessionId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课后总结失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="lesson-ai-page">
    <section class="hero card">
      <div class="eyebrow">课后总结</div>
      <h1>{{ titleText }}</h1>
      <p>课程 #{{ courseId }} 的 AI 课后总结与报告草稿预览。</p>
      <div class="hero-actions">
        <button class="btn" type="button" @click="router.push({ name: 'courseDetail', params: { courseId: String(courseId) } })">返回课程详情</button>
      </div>
    </section>

    <section v-if="loading" class="card block">加载中...</section>
    <section v-else-if="error" class="card block error">{{ error }}</section>
    <template v-else>
      <section class="grid">
        <article class="card block">
          <div class="block-title">本节课摘要</div>
          <div class="block-text">{{ stageSummary }}</div>
        </article>

        <article class="card block">
          <div class="block-title">下节课计划</div>
          <div class="block-text">{{ nextPlan || '暂未生成' }}</div>
        </article>
      </section>

      <section class="grid">
        <article class="card block">
          <div class="block-title">课堂重点</div>
          <div v-if="keyPoints.length === 0" class="empty">暂无内容</div>
          <ul v-else class="bullet-list">
            <li v-for="item in keyPoints" :key="item">{{ item }}</li>
          </ul>
        </article>

        <article class="card block">
          <div class="block-title">学生提问</div>
          <div v-if="questions.length === 0" class="empty">暂无内容</div>
          <ul v-else class="bullet-list">
            <li v-for="item in questions" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>

      <section class="card block">
        <div class="block-title">作业与课后建议</div>
        <div v-if="homework.length === 0" class="empty">暂无内容</div>
        <ul v-else class="bullet-list">
          <li v-for="item in homework" :key="item">{{ item }}</li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped>
.lesson-ai-page {
  display: grid;
  gap: 16px;
}

.hero {
  padding: 24px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.18), transparent 32%),
    linear-gradient(135deg, #fcfffd 0%, #eef8ff 100%);
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.45);
}

h1 {
  margin: 8px 0 10px;
  font-size: 32px;
}

.hero-actions {
  margin-top: 16px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.block {
  padding: 20px;
}

.block-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.block-text,
.empty,
.bullet-list li {
  color: rgba(15, 23, 42, 0.76);
  line-height: 1.7;
}

.bullet-list {
  margin: 0;
  padding-left: 20px;
}

.error {
  color: #b91c1c;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
