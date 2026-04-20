<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { courseApi } from '@/api/course'
import { scheduleApi } from '@/api/schedule'
import type { CourseDetailVO, ScheduleEventVO, TutorApplicationVO, UserCardVO, UserSimpleVO } from '@/api/types'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { normalizeAvatarUrl } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const courseId = computed(() => Number(route.params.courseId))
const isTeacher = computed(() => auth.user?.userType === 1)

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

const detail = ref<CourseDetailVO | null>(null)
const application = ref<TutorApplicationVO | null>(null)
const participant = ref<UserSimpleVO | null>(null)
const participantCard = ref<UserCardVO | null>(null)
const lessons = ref<ScheduleEventVO[]>([])
const avatarBroken = ref(false)

const scheduleOpen = ref(false)
const scheduleTitle = ref('')
const scheduleDescription = ref('')
const scheduleStartAt = ref<number>(roundToNextHalfHour(Date.now() + 2 * 60 * 60 * 1000))
const scheduleEndAt = ref<number>(scheduleStartAt.value + 60 * 60 * 1000)
const scheduleError = ref<string | null>(null)

function roundToNextHalfHour(nowMs: number) {
  const d = new Date(nowMs)
  d.setSeconds(0, 0)
  const m = d.getMinutes()
  const next = m <= 0 ? 0 : m <= 30 ? 30 : 60
  d.setMinutes(next)
  if (next === 60) d.setHours(d.getHours() + 1, 0, 0, 0)
  return d.getTime()
}

function fmtDateTime(value: string | number | Date | null | undefined, opts?: Intl.DateTimeFormatOptions) {
  if (value == null) return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...opts,
  }).format(d)
}

function toLocalDateTimeInputValue(ms: number) {
  const d = new Date(ms)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function parseLocalDateTimeInputValue(value: string) {
  const ms = Date.parse(value)
  return Number.isFinite(ms) ? ms : Date.now()
}

function applyScheduleStartInput(value: string) {
  const nextStart = parseLocalDateTimeInputValue(value)
  const previousDuration = Math.max(30 * 60 * 1000, scheduleEndAt.value - scheduleStartAt.value)
  scheduleStartAt.value = nextStart
  if (!(scheduleEndAt.value > scheduleStartAt.value)) {
    scheduleEndAt.value = nextStart + previousDuration
  }
}

function applyScheduleEndInput(value: string) {
  scheduleEndAt.value = parseLocalDateTimeInputValue(value)
}

function formatDuration(startAt: number, endAt: number) {
  const minutes = Math.max(0, Math.round((endAt - startAt) / 60000))
  if (!minutes) return '未设置时长'
  if (minutes % 60 === 0) return `${minutes / 60} 小时`
  if (minutes > 60) return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分`
  return `${minutes} 分钟`
}

function lessonStatusText(status: string) {
  const normalized = String(status || '').trim().toUpperCase()
  if (normalized === 'PENDING') return '待确认'
  if (normalized === 'ACCEPTED') return '已确认'
  if (normalized === 'REJECTED') return '已拒绝'
  if (normalized === 'CANCELED') return '已取消'
  return normalized || '未知状态'
}

const participantUid = computed(() => {
  if (!detail.value) return null
  return isTeacher.value ? detail.value.studentUid : detail.value.teacherUid
})

const participantName = computed(() => {
  const realName = String(participant.value?.realName || '').trim()
  if (realName) return realName
  const name = String(participant.value?.name || '').trim()
  if (name) return name
  return participantUid.value ? `用户${participantUid.value}` : '对方'
})

const participantAvatar = computed(() => {
  if (avatarBroken.value) return ''
  return normalizeAvatarUrl(participant.value?.avatar)
})

const sortedLessons = computed(() =>
  lessons.value.slice().sort((a, b) => {
    const aTime = Number(a.startAt || 0)
    const bTime = Number(b.startAt || 0)
    return aTime - bTime
  }),
)

const latestLesson = computed(() => sortedLessons.value[sortedLessons.value.length - 1] || null)

const lessonStats = computed(() => ({
  total: lessons.value.length,
  pending: lessons.value.filter((item) => item.status === 'PENDING').length,
  accepted: lessons.value.filter((item) => item.status === 'ACCEPTED').length,
  canceled: lessons.value.filter((item) => item.status === 'CANCELED' || item.status === 'REJECTED').length,
}))

function goBack() {
  void router.push({ name: 'myCourses' })
}

function goChat() {
  if (!detail.value?.roomId) return
  void router.push({ name: 'chatRoom', params: { roomId: String(detail.value.roomId) } })
}

function openScheduleCreate() {
  if (!detail.value || !participantUid.value) return
  scheduleError.value = null
  scheduleTitle.value = detail.value.courseName?.trim() || `与${participantName.value}的线上课程`
  scheduleDescription.value = latestLesson.value ? '补充新增课节' : '第一节试课'
  scheduleStartAt.value = roundToNextHalfHour(Date.now() + 2 * 60 * 60 * 1000)
  scheduleEndAt.value = scheduleStartAt.value + 60 * 60 * 1000
  scheduleOpen.value = true
}

function closeScheduleCreate() {
  if (saving.value) return
  scheduleOpen.value = false
}

async function submitScheduleCreate() {
  if (!detail.value || !participantUid.value || saving.value) return
  const titleText = scheduleTitle.value.trim()
  if (!titleText) {
    scheduleError.value = '请输入课节名称'
    return
  }
  if (!(scheduleEndAt.value > scheduleStartAt.value)) {
    scheduleError.value = '结束时间必须晚于开始时间'
    return
  }
  saving.value = true
  scheduleError.value = null
  try {
    const created = await scheduleApi.createEvent({
      courseId: detail.value.courseId,
      title: titleText,
      participantUserId: participantUid.value,
      startAt: scheduleStartAt.value,
      endAt: scheduleEndAt.value,
      description: scheduleDescription.value.trim() || undefined,
    })
    lessons.value = [...lessons.value, created]
    scheduleOpen.value = false
    toast.show('课节已创建，已同步到聊天与我的课程。', 'success')
  } catch (e) {
    scheduleError.value = e instanceof Error ? e.message : '创建课节失败'
  } finally {
    saving.value = false
  }
}

async function load() {
  if (!(courseId.value > 0)) {
    error.value = '课程不存在'
    return
  }
  loading.value = true
  error.value = null
  try {
    const current = await courseApi.detail(courseId.value)
    detail.value = current
    const [courseLessons, appDetail] = await Promise.all([
      scheduleApi.listCourseEvents(current.courseId),
      applicationApi.detail(current.applicationId),
    ])
    lessons.value = courseLessons
    application.value = appDetail
    const uid = isTeacher.value ? current.studentUid : current.teacherUid
    const [users, card] = await Promise.all([userApi.batch([uid]), userApi.card(uid)])
    participant.value = users[0] || null
    participantCard.value = card
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课程详情失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page-shell">
    <section class="hero card">
      <div>
        <div class="eyebrow">长期课程详情</div>
        <h1 class="hero-title">{{ detail?.courseName || '长期课程' }}</h1>
        <p class="hero-desc">
          {{ detail?.classTime || '还未配置固定时间' }}
          <template v-if="detail?.frequencyPerWeek"> · 每周 {{ detail.frequencyPerWeek }} 节</template>
          <template v-if="detail?.lessonPrice"> · {{ detail.lessonPrice }}</template>
        </p>
      </div>
      <div class="hero-actions">
        <button class="btn" type="button" @click="goBack">返回我的课程</button>
        <button class="btn" type="button" :disabled="!detail?.roomId" @click="goChat">进入聊天</button>
        <button class="btn btn-primary" type="button" :disabled="!detail || !participantUid" @click="openScheduleCreate">新增课节</button>
      </div>
    </section>

    <div v-if="loading" class="card hint">加载中...</div>
    <div v-else-if="error" class="card hint error">{{ error }}</div>
    <template v-else-if="detail">
      <section class="summary-grid">
        <div class="card metric">
          <div class="metric-label">授课形式</div>
          <div class="metric-value">{{ detail.teachingMode === 'ONLINE' ? '线上' : detail.teachingMode === 'OFFLINE' ? '线下' : '待确认' }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">课程状态</div>
          <div class="metric-value">{{ detail.status }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">已建课节</div>
          <div class="metric-value">{{ lessonStats.total }}</div>
        </div>
        <div class="card metric">
          <div class="metric-label">待确认课节</div>
          <div class="metric-value">{{ lessonStats.pending }}</div>
        </div>
      </section>

      <section class="content-grid">
        <aside class="card side-card">
          <div class="section-title">课程关系</div>
          <div class="participant-card">
            <img v-if="participantAvatar" class="avatar" :src="participantAvatar" alt="" @error="avatarBroken = true" />
            <div v-else class="avatar fallback">{{ participantName.slice(0, 1) }}</div>
            <div class="participant-copy">
              <div class="participant-name">{{ participantName }}</div>
              <div class="participant-sub">
                {{
                  participantCard?.teacherProfile?.subject ||
                  participantCard?.jobPosting?.subjectName ||
                  participantCard?.studentProfile?.demandDescription ||
                  '资料待完善'
                }}
              </div>
            </div>
          </div>
          <div class="kv-list">
            <div class="kv-row">
              <span>申请单</span>
              <strong>#{{ detail.applicationId }}</strong>
            </div>
            <div class="kv-row">
              <span>试课周期</span>
              <strong>{{ detail.trialStartAt ? `${fmtDateTime(detail.trialStartAt)} - ${fmtDateTime(detail.trialEndAt)}` : '待开始' }}</strong>
            </div>
            <div class="kv-row">
              <span>沟通会话</span>
              <strong>{{ detail.roomId ? `#${detail.roomId}` : '未绑定' }}</strong>
            </div>
          </div>
        </aside>

        <section class="card lessons-card">
          <div class="section-head">
            <div>
              <div class="section-title">课节列表</div>
              <div class="section-desc">这里展示这门长期课程下的所有短期课节，可继续新增课节，并为调课/删课预留统一入口。</div>
            </div>
            <button class="btn btn-primary" type="button" :disabled="!participantUid" @click="openScheduleCreate">新增课节</button>
          </div>

          <div v-if="sortedLessons.length === 0" class="mini-empty">这门课还没有创建任何课节，先创建第一节试课或正式课吧。</div>
          <div v-else class="lesson-list">
            <article v-for="item in sortedLessons" :key="item.id" class="lesson-item">
              <div class="lesson-main">
                <div class="lesson-title">{{ item.title }}</div>
                <div class="lesson-time">{{ fmtDateTime(item.startAt) }} - {{ fmtDateTime(item.endAt, { hour: '2-digit', minute: '2-digit' }) }}</div>
                <div class="lesson-sub">{{ item.description || '暂无备注' }}</div>
              </div>
              <div class="lesson-side">
                <div class="status-pill">{{ lessonStatusText(item.status) }}</div>
                <div class="lesson-duration">{{ formatDuration(item.startAt, item.endAt) }}</div>
                <div class="lesson-actions">
                  <router-link class="btn btn-link" :to="{ name: 'chatRoom', params: { roomId: String(detail.roomId) } }">聊天</router-link>
                  <router-link class="btn btn-link" :to="{ name: 'schedule' }">日程</router-link>
                </div>
              </div>
            </article>
          </div>
        </section>
      </section>
    </template>

    <div v-if="scheduleOpen" class="mask" @click.self="closeScheduleCreate">
      <div class="modal card">
        <div class="modal-title">新增课节</div>
        <div class="modal-desc">为这门长期课程补充一节新的短期课。当前实现会自动把新课节挂到课程 #{{ detail?.courseId }} 下。</div>
        <div v-if="scheduleError" class="hint error">{{ scheduleError }}</div>
        <div class="field">
          <div class="label">课节名称</div>
          <input v-model="scheduleTitle" class="input" placeholder="例如：第 2 节｜函数强化" />
        </div>
        <div class="time-grid">
          <div class="field">
            <div class="label">开始时间</div>
            <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(scheduleStartAt)" @change="applyScheduleStartInput(($event.target as HTMLInputElement).value)" />
          </div>
          <div class="field">
            <div class="label">结束时间</div>
            <input class="input" type="datetime-local" :value="toLocalDateTimeInputValue(scheduleEndAt)" @change="applyScheduleEndInput(($event.target as HTMLInputElement).value)" />
          </div>
        </div>
        <div class="field">
          <div class="label">课节备注</div>
          <textarea v-model="scheduleDescription" class="textarea" rows="4" placeholder="填写本节课目标、教材、作业或调课说明" />
        </div>
        <div class="modal-actions">
          <button class="btn" type="button" :disabled="saving" @click="closeScheduleCreate">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitScheduleCreate">{{ saving ? '提交中...' : '创建课节' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  display: grid;
  gap: 16px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.18), transparent 34%),
    linear-gradient(135deg, #fffefb, #f5fbfb 58%, #edf8f7);
}

.eyebrow,
.metric-label,
.section-title,
.label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(31, 35, 41, 0.52);
}

.hero-title {
  margin: 8px 0 0;
  font-size: 32px;
  line-height: 1.05;
}

.hero-desc,
.section-desc,
.participant-sub,
.lesson-sub,
.modal-desc,
.hint {
  color: var(--muted);
}

.hero-actions,
.summary-grid,
.content-grid,
.time-grid,
.modal-actions,
.section-head,
.kv-row,
.participant-card,
.lesson-item,
.lesson-actions {
  display: flex;
  gap: 12px;
}

.hero-actions,
.section-head,
.kv-row,
.lesson-item,
.modal-actions {
  justify-content: space-between;
  align-items: center;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  padding: 18px;
  border-radius: 18px;
}

.metric-value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 800;
}

.content-grid {
  align-items: start;
}

.side-card {
  width: 320px;
  min-width: 320px;
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 22px;
}

.lessons-card {
  flex: 1 1 auto;
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 22px;
}

.participant-card {
  align-items: center;
}

.avatar {
  width: 62px;
  height: 62px;
  border-radius: 16px;
  object-fit: cover;
  background: linear-gradient(135deg, rgba(0, 190, 189, 0.18), rgba(31, 35, 41, 0.08));
}

.fallback {
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #0d7e7d;
}

.participant-name,
.lesson-title {
  font-weight: 800;
}

.kv-list {
  display: grid;
  gap: 10px;
}

.lesson-list {
  display: grid;
  gap: 12px;
}

.lesson-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(31, 35, 41, 0.035);
  align-items: flex-start;
}

.lesson-main {
  display: grid;
  gap: 6px;
}

.lesson-time,
.lesson-duration {
  font-size: 13px;
  color: rgba(31, 35, 41, 0.68);
}

.lesson-side {
  min-width: 160px;
  display: grid;
  justify-items: end;
  gap: 8px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #0d7e7d;
  font-size: 12px;
  font-weight: 700;
}

.mini-empty {
  padding: 32px 20px;
  border-radius: 18px;
  background: rgba(31, 35, 41, 0.03);
  text-align: center;
  color: var(--muted);
}

.mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(6px);
}

.modal {
  width: min(720px, 100%);
  padding: 20px;
  border-radius: 24px;
  display: grid;
  gap: 14px;
}

.modal-title {
  font-size: 18px;
  font-weight: 800;
}

.field {
  display: grid;
  gap: 8px;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.input,
.textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  outline: none;
}

.input {
  height: 44px;
  padding: 0 14px;
}

.textarea {
  min-height: 120px;
  padding: 12px 14px;
  resize: vertical;
}

.btn-link {
  text-decoration: none;
}

.hint.error {
  color: #be123c;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .side-card {
    width: auto;
    min-width: 0;
  }
}

@media (max-width: 720px) {
  .hero,
  .hero-actions,
  .lesson-item,
  .modal-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-grid,
  .time-grid {
    grid-template-columns: 1fr;
  }

  .lesson-side {
    width: 100%;
    justify-items: start;
  }
}
</style>
