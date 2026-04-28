<template>
  <view class="page">
    <view v-if="loading" class="state">加载中...</view>
    <view v-else-if="error" class="state error">
      <text>{{ error }}</text>
      <button class="mini-btn" @click="load">重试</button>
    </view>

    <template v-else-if="detail">
      <view class="hero">
        <text class="eyebrow">课程详情</text>
        <text class="title">{{ detail.courseName || '家教课程合作' }}</text>
        <text class="subtitle">{{ stageText(detail.status) }} · {{ participantText }}</text>
      </view>

      <view class="panel">
        <view class="row">
          <text class="k">授课方式</text>
          <text class="v">{{ modeText(detail.teachingMode) }}</text>
        </view>
        <view class="row">
          <text class="k">课时费</text>
          <text class="v">{{ detail.lessonPrice || detail.latestProposal?.pricePerHour || '待确认' }}</text>
        </view>
        <view class="row">
          <text class="k">上课频次</text>
          <text class="v">{{ detail.frequencyPerWeek ? `${detail.frequencyPerWeek} 次/周` : '待确认' }}</text>
        </view>
        <view v-if="detail.trialStartAt" class="row">
          <text class="k">试课时间</text>
          <text class="v">{{ formatDateTime(detail.trialStartAt) }}</text>
        </view>
      </view>

      <view v-if="detail.latestRefund" class="panel refund">
        <view class="panel-head">
          <text class="section-title">退费进度</text>
          <text class="pill red">{{ refundStatusText(detail.latestRefund.status) }}</text>
        </view>
        <text class="desc">{{ detail.latestRefund.reason || '平台正在处理退费申请。' }}</text>
        <view v-if="detail.latestRefund.refundAmountFen" class="refund-money">{{ formatFen(detail.latestRefund.refundAmountFen) }}</view>
      </view>

      <view v-if="detail.aiPreview" class="panel">
        <text class="section-title">课后摘要</text>
        <text class="desc">{{ detail.aiPreview }}</text>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">课节安排</text>
          <text class="pill">{{ lessons.length }} 节</text>
        </view>
        <view v-if="lessons.length === 0" class="empty-line">暂无课节，确认试课或正式课表后会自动同步。</view>
        <view v-for="(lesson, index) in sortedLessons" :key="lesson.id" class="lesson">
          <view>
            <text class="lesson-title">{{ index === 0 ? '首节试课' : lesson.title }}</text>
            <text class="lesson-time">{{ formatRange(lesson.startAt, lesson.endAt) }}</text>
            <text class="lesson-status">{{ lessonStatusText(lesson.status) }}</text>
          </view>
          <view v-if="canRespondLesson(lesson)" class="lesson-actions">
            <button class="tiny ghost" @click.stop="respondLesson(lesson.id, 'REJECT')">拒绝</button>
            <button class="tiny" @click.stop="respondLesson(lesson.id, 'ACCEPT')">确认</button>
          </view>
        </view>
      </view>

      <view v-if="actionError" class="op-error">{{ actionError }}</view>

      <view class="bottom-bar">
        <button v-if="detail.roomId" class="bar-btn ghost" @click="goChat">聊天</button>
        <button v-if="canSubmitTrial" class="bar-btn primary" @click="trialOpen = true">确认试课结果</button>
        <button v-else-if="canSubmitWeekly" class="bar-btn primary" @click="weeklyOpen = true">提交正式课表</button>
        <button v-else-if="canApplyRefund" class="bar-btn primary danger" @click="refundOpen = true">申请试课退费</button>
        <button v-else class="bar-btn primary" @click="load">刷新</button>
      </view>

      <view v-if="trialOpen" class="sheet-mask" @click.self="trialOpen = false">
        <view class="sheet">
          <text class="sheet-title">试课结果</text>
          <view class="seg">
            <view class="seg-item" :class="{ active: trialResult === 'PASS' }" @click="trialResult = 'PASS'">通过</view>
            <view class="seg-item" :class="{ active: trialResult === 'FAIL' }" @click="trialResult = 'FAIL'">不继续</view>
          </view>
          <textarea v-model="trialReason" class="textarea" placeholder="补充说明（选填）" maxlength="200" />
          <button class="submit" :disabled="saving" @click="submitTrial">{{ saving ? '提交中...' : '提交结果' }}</button>
        </view>
      </view>

      <view v-if="weeklyOpen" class="sheet-mask" @click.self="weeklyOpen = false">
        <view class="sheet">
          <text class="sheet-title">正式课表</text>
          <view class="form-row">
            <text>每周星期</text>
            <picker :range="dayOptions" range-key="label" :value="weeklyDayIndex" @change="onDayChange">
              <view class="picker-value">{{ dayOptions[weeklyDayIndex].label }}</view>
            </picker>
          </view>
          <view class="form-row">
            <text>开始时间</text>
            <picker mode="time" :value="weeklyStart" @change="weeklyStart = String($event.detail.value)">
              <view class="picker-value">{{ weeklyStart }}</view>
            </picker>
          </view>
          <view class="form-row">
            <text>结束时间</text>
            <picker mode="time" :value="weeklyEnd" @change="weeklyEnd = String($event.detail.value)">
              <view class="picker-value">{{ weeklyEnd }}</view>
            </picker>
          </view>
          <input v-model="weeklyPrice" class="input" type="digit" placeholder="正式课时费（元/小时，选填）" />
          <input v-model="weeklyWeeks" class="input" type="number" placeholder="持续周数" />
          <button class="submit" :disabled="saving" @click="submitWeekly">{{ saving ? '提交中...' : '提交课表' }}</button>
        </view>
      </view>

      <view v-if="refundOpen" class="sheet-mask" @click.self="refundOpen = false">
        <view class="sheet">
          <text class="sheet-title">申请试课退费</text>
          <textarea v-model="refundReason" class="textarea" placeholder="说明退费原因" maxlength="200" />
          <button class="submit danger" :disabled="saving" @click="applyRefund">{{ saving ? '提交中...' : '提交退费申请' }}</button>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { courseApi, type CourseDetail } from '@/api/course';
import { scheduleApi, type ScheduleEvent } from '@/api/schedule';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const courseId = ref<number | null>(null);
const detail = ref<CourseDetail | null>(null);
const lessons = ref<ScheduleEvent[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const actionError = ref('');

const trialOpen = ref(false);
const trialResult = ref<'PASS' | 'FAIL'>('PASS');
const trialReason = ref('');
const weeklyOpen = ref(false);
const weeklyDayIndex = ref(1);
const weeklyStart = ref('19:00');
const weeklyEnd = ref('21:00');
const weeklyPrice = ref('');
const weeklyWeeks = ref('16');
const refundOpen = ref(false);
const refundReason = ref('');

const dayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
];

const isTeacher = computed(() => userStore.currentRole === 'tutor');
const participantText = computed(() => {
  if (!detail.value) return '';
  return isTeacher.value ? `学生 ${detail.value.studentUid}` : `老师 ${detail.value.teacherUid}`;
});
const sortedLessons = computed(() => lessons.value.slice().sort((a, b) => Number(a.startAt || 0) - Number(b.startAt || 0)));
const canSubmitTrial = computed(() => !isTeacher.value && detail.value?.status === 'TRIAL_WAIT_STUDENT_DECISION');
const canSubmitWeekly = computed(() => !isTeacher.value && detail.value?.status === 'TRIAL_WAIT_WEEKLY_SCHEDULE');
const canApplyRefund = computed(() => isTeacher.value && detail.value?.status === 'TRIAL_FAILED' && !detail.value?.latestRefund);

function normalizeStatus(status?: string | null) {
  return String(status || '').trim().toUpperCase();
}

function stageText(status?: string | null) {
  const s = normalizeStatus(status);
  if (s === 'WAIT_PAY') return '待支付信息费';
  if (s === 'COMMUNICATING') return '沟通中';
  if (s === 'TRIALING') return '试课进行中';
  if (s === 'TRIAL_WAIT_STUDENT_DECISION') return '待学生确认试课';
  if (s === 'TRIAL_WAIT_WEEKLY_SCHEDULE') return '待提交正式课表';
  if (s === 'TRIAL_FAILED') return '试课未继续';
  if (s === 'TEACHING') return '正式授课中';
  if (s === 'FINISHED') return '已完成';
  if (s.includes('REFUND')) return '退费处理中';
  return s || '状态未知';
}

function lessonStatusText(status?: string | null) {
  const s = normalizeStatus(status);
  if (s === 'PENDING') return '待确认';
  if (s === 'ACCEPTED') return '已确认';
  if (s === 'RESCHEDULE_PENDING') return '待确认调课';
  if (s === 'REJECTED') return '已拒绝';
  if (s === 'CANCELED') return '已取消';
  if (s === 'COMPLETED') return '已结课';
  return s || '未知';
}

function refundStatusText(status?: string | null) {
  const s = normalizeStatus(status);
  if (s === 'PENDING') return '审核中';
  if (s === 'APPROVED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  if (s === 'PAID') return '已退款';
  return s || '待确认';
}

function modeText(mode?: string | null) {
  if (mode === 'ONLINE') return '线上';
  if (mode === 'OFFLINE') return '线下';
  return '待确认';
}

function formatDateTime(value?: string | number | null) {
  if (value == null) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  const h = `${d.getHours()}`.padStart(2, '0');
  const min = `${d.getMinutes()}`.padStart(2, '0');
  return `${m}-${day} ${h}:${min}`;
}

function formatRange(startAt: number, endAt: number) {
  return `${formatDateTime(startAt)} - ${formatDateTime(endAt).slice(-5)}`;
}

function formatFen(value?: number | null) {
  if (value == null) return '';
  return `¥${(Number(value) / 100).toFixed(2)}`;
}

function canRespondLesson(lesson: ScheduleEvent) {
  return lesson.status === 'PENDING' && lesson.creatorUserId !== userStore.userInfo?.id;
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  error.value = '';
  actionError.value = '';
  try {
    detail.value = await courseApi.detail(courseId.value);
    lessons.value = await scheduleApi.listCourseEvents(courseId.value);
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载课程失败';
  } finally {
    loading.value = false;
  }
}

async function respondLesson(eventId: number, action: 'ACCEPT' | 'REJECT') {
  actionError.value = '';
  try {
    const next = await scheduleApi.respond(eventId, action);
    lessons.value = lessons.value.map((it) => (it.id === eventId ? next : it));
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '处理课节失败';
  }
}

async function submitTrial() {
  if (!courseId.value || saving.value) return;
  saving.value = true;
  actionError.value = '';
  try {
    await courseApi.submitTrialResult(courseId.value, { result: trialResult.value, reason: trialReason.value });
    trialOpen.value = false;
    await load();
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '提交试课结果失败';
  } finally {
    saving.value = false;
  }
}

function toMinutes(value: string) {
  const [h, m] = value.split(':').map((it) => Number(it));
  return h * 60 + m;
}

function onDayChange(e: any) {
  weeklyDayIndex.value = Number(e.detail.value || 0);
}

async function submitWeekly() {
  if (!courseId.value || !detail.value || saving.value) return;
  const startMinute = toMinutes(weeklyStart.value);
  const endMinute = toMinutes(weeklyEnd.value);
  if (!(endMinute > startMinute)) {
    actionError.value = '结束时间必须晚于开始时间';
    return;
  }
  const price = Number(weeklyPrice.value);
  saving.value = true;
  actionError.value = '';
  try {
    lessons.value = await scheduleApi.submitWeeklySchedule(courseId.value, {
      participantUserId: isTeacher.value ? detail.value.studentUid : detail.value.teacherUid,
      roomId: detail.value.roomId,
      title: detail.value.courseName || '正式每周课',
      description: '试课通过后确认的固定课表',
      weeks: Number(weeklyWeeks.value) || 16,
      lessonPriceFen: Number.isFinite(price) && price > 0 ? Math.round(price * 100) : undefined,
      slots: [{ dayOfWeek: dayOptions[weeklyDayIndex.value].value, startMinute, endMinute }]
    });
    weeklyOpen.value = false;
    await load();
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '提交课表失败';
  } finally {
    saving.value = false;
  }
}

async function applyRefund() {
  if (!courseId.value || saving.value) return;
  if (!refundReason.value.trim()) {
    actionError.value = '请填写退费原因';
    return;
  }
  saving.value = true;
  actionError.value = '';
  try {
    await courseApi.applyTrialRefund(courseId.value, { reason: refundReason.value.trim() });
    refundOpen.value = false;
    await load();
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '提交退费申请失败';
  } finally {
    saving.value = false;
  }
}

function goChat() {
  if (!detail.value?.roomId) return;
  uni.navigateTo({ url: `/pages/chat/room?id=${detail.value.roomId}` });
}

onLoad((options: any) => {
  const n = Number(options?.id);
  courseId.value = Number.isFinite(n) ? n : null;
});

onShow(() => {
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
  background: linear-gradient(135deg, #122529 0%, #315b4f 55%, #d49f52 140%);
  box-shadow: 0 18px 38px rgba(18, 37, 41, 0.2);
}

.eyebrow,
.title,
.subtitle,
.section-title,
.desc,
.k,
.v,
.lesson-title,
.lesson-time,
.lesson-status {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 6px;
}

.title {
  font-size: 23px;
  font-weight: 900;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
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

.panel-head,
.row,
.lesson {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.panel-head {
  align-items: center;
  margin-bottom: 10px;
}

.section-title {
  font-size: 16px;
  font-weight: 900;
  color: #152326;
}

.row {
  padding: 10px 0;
  border-bottom: 1px solid rgba(18, 37, 41, 0.07);
}

.row:last-child {
  border-bottom: none;
}

.k {
  flex-shrink: 0;
  color: #82909a;
  font-size: 13px;
}

.v {
  color: #162326;
  font-size: 14px;
  font-weight: 800;
  text-align: right;
}

.desc {
  color: #65717a;
  font-size: 13px;
  line-height: 1.65;
}

.pill {
  padding: 4px 9px;
  border-radius: 999px;
  background: #edf6f5;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
}

.pill.red {
  background: #fff0f0;
  color: #c24141;
}

.refund-money {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 900;
  color: #c24141;
}

.empty-line {
  padding: 14px 0;
  color: #8a939c;
  font-size: 13px;
}

.lesson {
  align-items: center;
  padding: 13px 0;
  border-top: 1px solid rgba(18, 37, 41, 0.07);
}

.lesson-title {
  color: #172326;
  font-size: 14px;
  font-weight: 900;
  margin-bottom: 4px;
}

.lesson-time,
.lesson-status {
  color: #77828b;
  font-size: 12px;
  line-height: 1.5;
}

.lesson-actions {
  display: flex;
  gap: 8px;
}

.tiny {
  width: 56px;
  height: 30px;
  line-height: 30px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: #0f766e;
  color: #fff;
  font-size: 12px;
}

.tiny.ghost {
  background: #eef2f3;
  color: #5d6872;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 10px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(18, 37, 41, 0.08);
  box-sizing: border-box;
}

.bar-btn,
.submit,
.mini-btn {
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 14px;
}

.bar-btn {
  flex: 1;
  height: 44px;
  line-height: 44px;
}

.bar-btn.ghost {
  flex: 0.8;
  background: #eef2f3;
  color: #33424a;
}

.bar-btn.danger,
.submit.danger {
  background: #c24141;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(12, 20, 22, 0.45);
}

.sheet {
  width: 100%;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  border-radius: 22px 22px 0 0;
  background: #fff;
  box-sizing: border-box;
}

.sheet-title {
  display: block;
  margin-bottom: 14px;
  color: #142326;
  font-size: 18px;
  font-weight: 900;
}

.seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.seg-item {
  height: 38px;
  line-height: 38px;
  border-radius: 12px;
  text-align: center;
  color: #65717a;
  background: #f1f4f4;
  font-size: 14px;
}

.seg-item.active {
  background: #0f766e;
  color: #fff;
  font-weight: 900;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(18, 37, 41, 0.07);
  color: #243238;
  font-size: 14px;
}

.picker-value {
  color: #0f766e;
  font-weight: 800;
}

.input,
.textarea {
  width: 100%;
  margin-top: 10px;
  padding: 12px;
  border-radius: 12px;
  background: #f4f7f7;
  color: #172326;
  box-sizing: border-box;
  font-size: 14px;
}

.textarea {
  height: 96px;
}

.submit {
  width: 100%;
  height: 44px;
  line-height: 44px;
  margin-top: 14px;
}

.state {
  margin-top: 18px;
  padding: 22px;
  border-radius: 18px;
  background: #fff;
  color: #707982;
  text-align: center;
  font-size: 14px;
}

.mini-btn {
  display: inline-block;
  margin-top: 12px;
  padding: 0 18px;
  height: 34px;
  line-height: 34px;
}

.op-error {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff0f0;
  color: #c24141;
  font-size: 13px;
}
</style>
