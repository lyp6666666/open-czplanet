<template>
  <view class="page">
    <AppStateCard
      v-if="loading"
      title="课后总结加载中"
      description="正在读取课堂 AI 总结和报告草稿。"
      variant="soft"
    />
    <AppStateCard
      v-else-if="error"
      title="课后总结加载失败"
      :description="error"
      action-text="重试"
      variant="error"
      @action="load"
    />

    <template v-else-if="result">
      <view class="hero">
        <text class="eyebrow">课后总结</text>
        <text class="title">{{ titleText }}</text>
        <text class="subtitle">{{ statusText }}<template v-if="updatedText"> · {{ updatedText }}</template></text>
      </view>

      <view class="panel">
        <text class="section-title">本节课摘要</text>
        <text class="desc">{{ stageSummary }}</text>
      </view>

      <view class="panel">
        <text class="section-title">下节课计划</text>
        <text class="desc">{{ nextPlan || '暂未生成' }}</text>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">课堂重点</text>
          <text class="pill">{{ keyPoints.length }} 条</text>
        </view>
        <view v-if="keyPoints.length === 0" class="empty-line">暂无内容</view>
        <view v-for="item in keyPoints" v-else :key="item" class="bullet">
          <text class="dot"></text>
          <text class="bullet-text">{{ item }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">学生提问</text>
          <text class="pill">{{ questions.length }} 条</text>
        </view>
        <view v-if="questions.length === 0" class="empty-line">暂无内容</view>
        <view v-for="item in questions" v-else :key="item" class="bullet">
          <text class="dot"></text>
          <text class="bullet-text">{{ item }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">作业与建议</text>
          <text class="pill">{{ homework.length }} 条</text>
        </view>
        <view v-if="homework.length === 0" class="empty-line">暂无内容</view>
        <view v-for="item in homework" v-else :key="item" class="bullet">
          <text class="dot"></text>
          <text class="bullet-text">{{ item }}</text>
        </view>
      </view>

      <view v-if="result.resultStatus === 'FAILED'" class="actions">
        <button class="action primary" :disabled="busy" @click="retry">
          {{ busy ? '重试中...' : '重试生成总结' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app';
import { liveApi, type LiveAiResultResp } from '@/api/live';
import { scheduleApi } from '@/api/schedule';
import AppStateCard from '@/components/AppStateCard.vue';

const courseId = ref<number | null>(null);
const sessionId = ref<number | null>(null);
const result = ref<LiveAiResultResp | null>(null);
const loading = ref(false);
const busy = ref(false);
const error = ref('');

const titleText = computed(() => String(reportField('reportTitle') || `课程 #${courseId.value || '-'} 课后总结`));
const stageSummary = computed(() => String(summaryField('stageSummary') || reportField('parentSummary') || result.value?.preview || '暂无摘要'));
const keyPoints = computed(() => stringList(summaryField('keyPoints') || reportField('knowledgePoints')));
const questions = computed(() => stringList(result.value?.summary?.studentQuestions));
const homework = computed(() => stringList(summaryField('homeworkCandidates') || reportField('homework')));
const nextPlan = computed(() => String(reportField('nextLessonPlan') || ''));
const statusText = computed(() => {
  const s = String(result.value?.resultStatus || '').toUpperCase();
  if (s === 'READY') return '已生成';
  if (s === 'GENERATING' || s === 'PENDING') return '生成中';
  if (s === 'FAILED') return '生成失败';
  return s || '状态未知';
});
const updatedText = computed(() => formatDateTime(result.value?.updatedAt));

function summaryField(key: string): unknown {
  return result.value?.summary?.[key];
}

function reportField(key: string): unknown {
  return result.value?.report?.[key];
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map((item) => String(item || '').trim()).filter(Boolean) : [];
}

function selectLatestLessonId(items: Array<{ id?: number | null; endAt?: number | null; startAt?: number | null }>) {
  const candidates = items
    .filter((item) => Number(item?.id || 0) > 0)
    .slice()
    .sort((a, b) => {
      const aEnd = Number(a.endAt || 0);
      const bEnd = Number(b.endAt || 0);
      if (aEnd !== bEnd) return bEnd - aEnd;
      return Number(b.startAt || 0) - Number(a.startAt || 0);
    });
  return Number(candidates[0]?.id || 0);
}

async function resolveSessionId() {
  if (sessionId.value && sessionId.value > 0) return sessionId.value;
  if (!courseId.value) return 0;
  try {
    const live = await liveApi.getByCourse(courseId.value);
    sessionId.value = Number(live.sessionId || 0);
    return sessionId.value;
  } catch {
    try {
      const lessons = await scheduleApi.listCourseEvents(courseId.value);
      const lessonId = selectLatestLessonId(lessons || []);
      if (!(lessonId > 0)) return 0;
      const matched = (lessons || []).find((item) => Number(item.id) === lessonId);
      if (matched?.courseId && Number(matched.courseId) > 0) {
        const live = await liveApi.getByCourse(Number(matched.courseId));
        sessionId.value = Number(live.sessionId || 0);
        return sessionId.value;
      }
      return 0;
    } catch {
      return 0;
    }
  }
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const sid = await resolveSessionId();
    if (!(sid > 0)) {
      result.value = null;
      error.value = '当前课程还没有可查看的课堂 AI 总结';
      return;
    }
    result.value = await liveApi.aiResult(sid);
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载课后总结失败';
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}

async function retry() {
  const sid = sessionId.value;
  if (!sid || busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    result.value = await liveApi.retryAiResult(sid);
    uni.showToast({ title: '已发起重试', icon: 'none' });
  } catch (e: any) {
    error.value = e?.message || e?.msg || '重试生成失败';
  } finally {
    busy.value = false;
  }
}

function formatDateTime(value?: string | null) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  const h = `${d.getHours()}`.padStart(2, '0');
  const min = `${d.getMinutes()}`.padStart(2, '0');
  return `${m}-${day} ${h}:${min}`;
}

onLoad((options: any) => {
  const cid = Number(options?.courseId);
  const sid = Number(options?.sessionId);
  courseId.value = Number.isFinite(cid) ? cid : null;
  sessionId.value = Number.isFinite(sid) && sid > 0 ? sid : null;
  void load();
});

onPullDownRefresh(() => {
  void load();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px 96px;
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #122529 0%, #2f665f 70%, #d49f52 140%);
  box-shadow: 0 18px 38px rgba(18, 37, 41, 0.2);
}

.eyebrow,
.title,
.subtitle,
.section-title,
.desc,
.bullet-text {
  display: block;
}

.eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.72;
}

.title {
  font-size: 22px;
  font-weight: 900;
  line-height: 1.25;
}

.subtitle {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  opacity: 0.82;
}

.panel {
  margin-top: 12px;
  padding: 15px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 12px 28px rgba(18, 37, 41, 0.06);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.section-title {
  color: #172326;
  font-size: 16px;
  font-weight: 900;
}

.desc {
  margin-top: 9px;
  color: #65717a;
  font-size: 13px;
  line-height: 1.65;
}

.pill {
  padding: 4px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: #edf6f5;
  font-size: 12px;
  font-weight: 900;
}

.empty-line {
  padding: 12px 0;
  color: #8a939c;
  font-size: 13px;
}

.bullet {
  display: flex;
  gap: 9px;
  padding: 9px 0;
  border-top: 1px solid rgba(18, 37, 41, 0.06);
}

.bullet:first-of-type {
  border-top: none;
}

.dot {
  width: 6px;
  height: 6px;
  margin-top: 8px;
  flex-shrink: 0;
  border-radius: 999px;
  background: #0f766e;
}

.bullet-text {
  color: #33424a;
  font-size: 13px;
  line-height: 1.7;
}

.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(18, 37, 41, 0.08);
  box-sizing: border-box;
}

.action {
  width: 100%;
  height: 44px;
  line-height: 44px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 14px;
  font-weight: 900;
}

.action::after {
  border: 0;
}
</style>
