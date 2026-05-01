<template>
  <view class="page">
    <AppStateCard
      v-if="loading"
      title="课节详情加载中"
      description="正在同步课程、课节和课堂状态。"
      variant="soft"
    />
    <AppStateCard
      v-else-if="error"
      title="课节详情加载失败"
      :description="error"
      action-text="重试"
      variant="error"
      @action="load"
    />

    <template v-else-if="lesson">
      <view class="hero">
        <text class="eyebrow">{{ lessonTypeText(lesson.lessonType) }}</text>
        <text class="title">{{ lesson.title || courseTitle }}</text>
        <text class="subtitle">{{ formatRange(lesson.startAt, lesson.endAt) }}</text>
      </view>

      <view class="panel">
        <view class="row">
          <text class="k">课节状态</text>
          <text class="v">{{ lessonStatusText(lesson.status) }}</text>
        </view>
        <view class="row">
          <text class="k">授课方式</text>
          <text class="v">{{ modeText(course?.teachingMode) }}</text>
        </view>
        <view class="row">
          <text class="k">课时费</text>
          <text class="v">{{ priceText }}</text>
        </view>
        <view class="row">
          <text class="k">对方</text>
          <text class="v">{{ participantText }}</text>
        </view>
        <view v-if="lesson.description" class="note">{{ lesson.description }}</view>
      </view>

      <view v-if="lesson.status === 'RESCHEDULE_PENDING'" class="panel warn">
        <text class="section-title">调课待确认</text>
        <text class="desc">{{ rescheduleText }}</text>
      </view>

      <view v-if="liveStatusText" class="panel">
        <view class="panel-head">
          <text class="section-title">线上课堂</text>
          <text class="pill">{{ liveStatusText }}</text>
        </view>
        <text class="desc">{{ liveHintText }}</text>
      </view>

      <view v-if="canViewAiSummary" class="panel" @click="goAiSummary">
        <view class="panel-head">
          <text class="section-title">课后总结</text>
          <text class="pill">{{ aiStatusText }}</text>
        </view>
        <text class="desc">{{ aiPreviewText }}</text>
      </view>

      <view v-if="actionError" class="op-error">{{ actionError }}</view>

      <view class="actions">
        <button v-if="canRespond" class="action ghost" :disabled="busy" @click="respond('REJECT')">拒绝</button>
        <button v-if="canRespond" class="action primary" :disabled="busy" @click="respond('ACCEPT')">确认课节</button>
        <button v-if="canJoinLive" class="action primary single" :disabled="busy" @click="goLivePrepare">进入课堂</button>
        <button v-if="canConfirmReschedule" class="action primary single" :disabled="busy" @click="confirmReschedule">确认改期</button>
        <button v-if="canReschedule" class="action ghost" :disabled="busy" @click="openReschedule">调课</button>
        <button v-if="canCancel" class="action danger" :disabled="busy" @click="cancelLesson">取消课节</button>
        <button v-if="canComplete" class="action primary single" :disabled="busy" @click="completeLesson">标记结课</button>
      </view>

      <view v-if="rescheduleOpen" class="sheet-mask" @click.self="rescheduleOpen = false">
        <view class="sheet">
          <text class="sheet-title">发起调课</text>
          <picker mode="date" :value="rescheduleDate" @change="rescheduleDate = String($event.detail.value)">
            <view class="field">改期日期：{{ rescheduleDate }}</view>
          </picker>
          <picker mode="time" :value="rescheduleStart" @change="rescheduleStart = String($event.detail.value)">
            <view class="field">开始时间：{{ rescheduleStart }}</view>
          </picker>
          <picker mode="time" :value="rescheduleEnd" @change="rescheduleEnd = String($event.detail.value)">
            <view class="field">结束时间：{{ rescheduleEnd }}</view>
          </picker>
          <textarea v-model="rescheduleRemark" class="textarea" placeholder="说明调课原因，方便对方确认" maxlength="200" />
          <button class="submit" :disabled="busy" @click="submitReschedule">{{ busy ? '提交中...' : '提交调课' }}</button>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { appointmentApi } from '@/api/appointment';
import { courseApi, type CourseDetail } from '@/api/course';
import { liveApi, type LiveSessionResp } from '@/api/live';
import { scheduleApi, type ScheduleEvent } from '@/api/schedule';
import { useUserStore } from '@/stores/user';
import { consumeLiveReturnRefresh } from '@/utils/liveRefresh';
import AppStateCard from '@/components/AppStateCard.vue';

const userStore = useUserStore();
const courseId = ref<number | null>(null);
const eventId = ref<number | null>(null);
const course = ref<CourseDetail | null>(null);
const lesson = ref<ScheduleEvent | null>(null);
const live = ref<LiveSessionResp | null>(null);
const loading = ref(false);
const busy = ref(false);
const error = ref('');
const actionError = ref('');

const rescheduleOpen = ref(false);
const rescheduleDate = ref('');
const rescheduleStart = ref('19:00');
const rescheduleEnd = ref('20:00');
const rescheduleRemark = ref('');

const courseTitle = computed(() => course.value?.courseName || '家教课程合作');
const priceText = computed(() => {
  if (lesson.value?.lessonPriceFen != null) return `¥${(Number(lesson.value.lessonPriceFen) / 100).toFixed(2)}`;
  return course.value?.lessonPrice || course.value?.latestProposal?.pricePerHour || '待确认';
});
const participantText = computed(() => {
  const p = lesson.value?.participant;
  const name = String(p?.realName || p?.name || p?.nickname || '').trim();
  if (name) return name;
  const uid = userStore.currentRole === 'tutor' ? course.value?.studentUid : course.value?.teacherUid;
  return uid ? `用户 ${uid}` : '待确认';
});
const canRespond = computed(() => lesson.value?.status === 'PENDING' && lesson.value.creatorUserId !== userStore.userInfo?.id);
const canCancel = computed(() => ['PENDING', 'ACCEPTED', 'RESCHEDULE_PENDING'].includes(normalizeStatus(lesson.value?.status)));
const canReschedule = computed(() => normalizeStatus(lesson.value?.status) === 'ACCEPTED');
const canConfirmReschedule = computed(() => {
  const item = lesson.value;
  return normalizeStatus(item?.status) === 'RESCHEDULE_PENDING' && !!item?.proposedBy && item.proposedBy !== userStore.userInfo?.id;
});
const canComplete = computed(() => normalizeStatus(lesson.value?.status) === 'ACCEPTED' && Number(lesson.value?.endAt || 0) <= Date.now());
const canJoinLive = computed(() => {
  if (!course.value || course.value.teachingMode !== 'ONLINE') return false;
  const status = normalizeStatus(lesson.value?.status);
  if (status !== 'ACCEPTED') return false;
  const now = Date.now();
  const start = Number(lesson.value?.startAt || 0);
  const end = Number(lesson.value?.endAt || 0);
  return start - now <= 15 * 60 * 1000 && end > now;
});
const canViewAiSummary = computed(() => {
  const status = normalizeStatus(lesson.value?.status);
  return status === 'COMPLETED' || status === 'ACCEPTED' || normalizeStatus(course.value?.aiResultStatus) === 'READY';
});
const aiStatusText = computed(() => {
  const s = normalizeStatus(course.value?.aiResultStatus);
  if (s === 'READY') return '已生成';
  if (s === 'FAILED') return '生成失败';
  if (s === 'PENDING' || s === 'GENERATING') return '生成中';
  return '查看详情';
});
const aiPreviewText = computed(() => {
  if (course.value?.aiPreview) return course.value.aiPreview;
  if (normalizeStatus(course.value?.aiResultStatus) === 'FAILED') return '课后总结生成失败，可进入详情页重试。';
  if (normalizeStatus(course.value?.aiResultStatus) === 'READY') return '本节课的课后总结已生成，可查看完整内容。';
  return '课程结束后可查看课堂摘要、重点和作业建议。';
});
const liveStatusText = computed(() => {
  if (!course.value || course.value.teachingMode !== 'ONLINE') return '';
  if (!live.value) return '待准备';
  if (live.value.status === 'IN_PROGRESS') return '进行中';
  if (live.value.joinableNow) return '可进入';
  return live.value.status || '待准备';
});
const liveHintText = computed(() => {
  if (live.value?.joinBlockedReason) return live.value.joinBlockedReason;
  if (canJoinLive.value) return '课程已接近开始时间，可进入课堂准备页检查设备。';
  return '到达开放时间后，可从这里进入课堂准备页。';
});
const rescheduleText = computed(() => {
  if (!lesson.value?.proposedStartAt || !lesson.value?.proposedEndAt) return '对方发起了调课申请，请确认是否接受。';
  return `${formatRange(lesson.value.proposedStartAt, lesson.value.proposedEndAt)} · ${lesson.value.proposedBy === userStore.userInfo?.id ? '由你发起' : '等待你确认'}`;
});

function normalizeStatus(status?: string | null) {
  return String(status || '').trim().toUpperCase();
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}

function formatDateTime(value?: string | number | null) {
  if (value == null) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getMonth() + 1}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

function formatRange(startAt?: number | string | null, endAt?: number | string | null) {
  return `${formatDateTime(startAt)} - ${formatDateTime(endAt).slice(-5)}`;
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

function lessonTypeText(type?: string | null) {
  const s = normalizeStatus(type);
  if (s === 'TRIAL') return '试课';
  if (s === 'NORMAL') return '正式课';
  return '课程';
}

function modeText(mode?: string | null) {
  if (mode === 'ONLINE') return '线上';
  if (mode === 'OFFLINE') return '线下';
  return '待确认';
}

function toLocalIso(date: string, time: string) {
  return `${date}T${time}:00`;
}

function durationMinutes(start: string, end: string) {
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  return Math.max(15, (eh * 60 + em) - (sh * 60 + sm));
}

async function load() {
  if (!courseId.value || !eventId.value) return;
  loading.value = true;
  error.value = '';
  actionError.value = '';
  try {
    const [detail, lessonList] = await Promise.all([
      courseApi.detail(courseId.value),
      scheduleApi.listCourseEvents(courseId.value),
    ]);
    course.value = detail;
    lesson.value = lessonList.find((item) => Number(item.id) === eventId.value) || null;
    if (!lesson.value) throw new Error('课节不存在或已更新');
    if (detail.teachingMode === 'ONLINE') {
      try {
        live.value = await liveApi.getByCourse(detail.courseId);
      } catch {
        live.value = null;
      }
    } else {
      live.value = null;
    }
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载课节失败';
  } finally {
    loading.value = false;
  }
}

async function respond(action: 'ACCEPT' | 'REJECT') {
  if (!lesson.value || busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    lesson.value = await scheduleApi.respond(lesson.value.id, action);
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '处理课节失败';
  } finally {
    busy.value = false;
  }
}

async function cancelLesson() {
  if (!lesson.value || busy.value) return;
  const ok = await confirm('确认取消这节课吗？');
  if (!ok) return;
  busy.value = true;
  actionError.value = '';
  try {
    lesson.value = await scheduleApi.cancel(lesson.value.id, '从小程序课节详情取消');
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '取消课节失败';
  } finally {
    busy.value = false;
  }
}

function openReschedule() {
  if (!lesson.value) return;
  const start = new Date(lesson.value.startAt);
  const end = new Date(lesson.value.endAt);
  rescheduleDate.value = `${start.getFullYear()}-${pad2(start.getMonth() + 1)}-${pad2(start.getDate())}`;
  rescheduleStart.value = `${pad2(start.getHours())}:${pad2(start.getMinutes())}`;
  rescheduleEnd.value = `${pad2(end.getHours())}:${pad2(end.getMinutes())}`;
  rescheduleRemark.value = lesson.value.description || '';
  rescheduleOpen.value = true;
}

async function submitReschedule() {
  if (!lesson.value || busy.value) return;
  const minutes = durationMinutes(rescheduleStart.value, rescheduleEnd.value);
  busy.value = true;
  actionError.value = '';
  try {
    await appointmentApi.reschedule(lesson.value.id, {
      proposedStartTime: toLocalIso(rescheduleDate.value, rescheduleStart.value),
      durationMinutes: minutes,
      remark: rescheduleRemark.value.trim() || undefined,
    });
    rescheduleOpen.value = false;
    await load();
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '发起调课失败';
  } finally {
    busy.value = false;
  }
}

async function confirmReschedule() {
  if (!lesson.value || busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    await appointmentApi.confirmReschedule(lesson.value.id);
    await load();
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '确认改期失败';
  } finally {
    busy.value = false;
  }
}

async function completeLesson() {
  if (!lesson.value || busy.value) return;
  const ok = await confirm('确认将这节课标记为已结课吗？');
  if (!ok) return;
  busy.value = true;
  actionError.value = '';
  try {
    await appointmentApi.complete(lesson.value.id);
    await load();
  } catch (e: any) {
    actionError.value = e?.message || e?.msg || '结课失败';
  } finally {
    busy.value = false;
  }
}

function goLivePrepare() {
  if (!course.value || !lesson.value) return;
  uni.navigateTo({ url: `/pages/live/prepare?courseId=${course.value.courseId}&eventId=${lesson.value.id}` });
}

function goAiSummary() {
  if (!course.value) return;
  uni.navigateTo({ url: `/pages/course/ai-summary?courseId=${course.value.courseId}` });
}

function confirm(content: string) {
  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '确认操作',
      content,
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
}

onLoad((options: any) => {
  const cid = Number(options?.courseId);
  const eid = Number(options?.eventId);
  courseId.value = Number.isFinite(cid) ? cid : null;
  eventId.value = Number.isFinite(eid) ? eid : null;
});

onShow(() => {
  const returned = consumeLiveReturnRefresh((payload) => {
    const matchedCourse = Number(payload.courseId || 0) === Number(courseId.value || 0);
    const matchedEvent = !payload.eventId || Number(payload.eventId || 0) === Number(eventId.value || 0);
    return matchedCourse && matchedEvent;
  });
  if (returned) {
    uni.showToast({ title: '课堂结束，课节状态已刷新', icon: 'none' });
  }
  void load();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px calc(92px + env(safe-area-inset-bottom));
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #122529 0%, #315b4f 62%, #d49f52 140%);
  box-shadow: 0 18px 38px rgba(18, 37, 41, 0.2);
}

.eyebrow,
.title,
.subtitle,
.section-title,
.desc,
.k,
.v {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.72;
  margin-bottom: 7px;
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

.panel.warn {
  background: #fff9ed;
  border-color: rgba(213, 159, 84, 0.26);
}

.panel-head,
.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.panel-head {
  align-items: center;
  margin-bottom: 10px;
}

.section-title {
  color: #172326;
  font-size: 16px;
  font-weight: 900;
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

.note,
.desc {
  margin-top: 10px;
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

.op-error {
  margin-top: 12px;
  padding: 12px;
  border-radius: 14px;
  color: #c24141;
  background: #fff0f0;
  font-size: 13px;
}

.actions {
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

.action,
.submit {
  flex: 1;
  height: 44px;
  line-height: 44px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 14px;
  font-weight: 900;
}

.action::after,
.submit::after {
  border: 0;
}

.action.single {
  flex: 1 1 100%;
}

.action.ghost {
  color: #33424a;
  background: #eef2f3;
}

.action.danger {
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

.field,
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
  margin-top: 14px;
}
</style>
