<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="eyebrow">课程表</text>
        <text class="title">{{ titleText }}</text>
        <text class="subtitle">集中查看待确认、待上课、进行中和已完成课节。</text>
      </view>
      <button class="today-btn" @click="goToday">今天</button>
    </view>

    <view class="toolbar">
      <button class="nav-btn" @click="moveRange(-1)">‹</button>
      <view class="mode-tabs">
        <view class="mode-tab" :class="{ active: viewMode === 'day' }" @click="switchMode('day')">日</view>
        <view class="mode-tab" :class="{ active: viewMode === 'week' }" @click="switchMode('week')">周</view>
        <view class="mode-tab" :class="{ active: viewMode === 'month' }" @click="switchMode('month')">月</view>
      </view>
      <button class="nav-btn" @click="moveRange(1)">›</button>
    </view>

    <view class="stat-grid">
      <view class="stat-card">
        <text class="stat-num">{{ events.length }}</text>
        <text class="stat-label">总安排</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ pendingCount }}</text>
        <text class="stat-label">待确认</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ acceptedCount }}</text>
        <text class="stat-label">已确认</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ totalHoursText }}</text>
        <text class="stat-label">课时</text>
      </view>
    </view>

    <view v-if="groupCards.length" class="group-strip">
      <view
        v-for="group in groupCards"
        :key="group.key"
        class="group-chip"
        :class="group.tone"
      >
        <text class="group-count">{{ group.count }}</text>
        <text class="group-label">{{ group.label }}</text>
      </view>
    </view>

    <view class="day-strip">
      <view
        v-for="day in visibleDays"
        :key="day.key"
        class="day-chip"
        :class="{ active: isSameDay(day.date, cursorDate), today: isSameDay(day.date, today) }"
        @click="selectDay(day.date)"
      >
        <text class="day-week">{{ day.week }}</text>
        <text class="day-date">{{ day.date.getDate() }}</text>
        <text v-if="eventsOnDay(day.date).length" class="day-dot">{{ eventsOnDay(day.date).length }}</text>
      </view>
    </view>

    <view v-if="viewMode === 'month'" class="month-panel">
      <view class="month-head">
        <text class="month-head-text">本月课节分布</text>
        <text class="month-head-sub">点选日期查看当天安排</text>
      </view>
      <view class="month-weekbar">
        <text v-for="week in weekLabels" :key="week" class="month-weektext">{{ week }}</text>
      </view>
      <view class="month-grid">
        <view
          v-for="day in monthCells"
          :key="day.key"
          class="month-cell"
          :class="{
            muted: !day.inMonth,
            active: isSameDay(day.date, cursorDate),
            today: isSameDay(day.date, today),
            busy: day.count > 0,
          }"
          @click="selectDay(day.date)"
        >
          <text class="month-date">{{ day.date.getDate() }}</text>
          <text v-if="day.count" class="month-count">{{ day.count }}节</text>
          <text v-else class="month-empty">-</text>
        </view>
      </view>
    </view>

    <AppStateCard
      v-if="!userStore.isLoggedIn"
      title="登录后查看课程表"
      description="课程、试课、正式课表和待确认课节会集中显示在这里。"
      action-text="去登录"
      variant="soft"
      @action="goLogin"
    />
    <AppStateCard
      v-else-if="loading"
      title="课程表加载中"
      description="正在同步当前时间范围内的课节。"
      variant="soft"
    />
    <AppStateCard
      v-else-if="error"
      title="课程表加载失败"
      :description="error"
      action-text="重试"
      variant="error"
      @action="load"
    />
    <AppStateCard
      v-else-if="selectedDayEvents.length === 0"
      title="当天暂无课程"
      description="确认试课或正式课表后，课节会自动同步到这里。"
      variant="soft"
    />

    <view v-else class="lesson-list">
      <view v-for="event in selectedDayEvents" :key="event.id" class="lesson-card" @click="openEvent(event)">
        <view class="time-col">
          <text class="time-main">{{ formatHm(event.startAt) }}</text>
          <text class="time-sub">{{ formatHm(event.endAt) }}</text>
        </view>
        <view class="lesson-main">
          <view class="lesson-head">
            <text class="lesson-title">{{ event.title || lessonTypeText(event.lessonType) }}</text>
            <text class="status" :class="statusTone(event.status)">{{ statusText(event.status) }}</text>
          </view>
          <text class="lesson-meta">{{ lessonTypeText(event.lessonType) }} · {{ durationText(event) }}</text>
          <text v-if="event.participant" class="lesson-desc">{{ participantText(event.participant) }}</text>
          <text v-else-if="event.description" class="lesson-desc">{{ event.description }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app';
import { scheduleApi, type ScheduleEvent } from '@/api/schedule';
import { useUserStore } from '@/stores/user';
import { consumeLiveReturnRefresh } from '@/utils/liveRefresh';
import AppStateCard from '@/components/AppStateCard.vue';

type ViewMode = 'day' | 'week' | 'month';

const userStore = useUserStore();
const today = new Date();
const cursorDate = ref(startOfDay(today));
const viewMode = ref<ViewMode>('week');
const events = ref<ScheduleEvent[]>([]);
const loading = ref(false);
const error = ref('');
const weekLabels = ['一', '二', '三', '四', '五', '六', '日'];

const visibleDays = computed(() => {
  if (viewMode.value === 'month') {
    const start = startOfMonth(cursorDate.value);
    const length = daysInMonth(cursorDate.value);
    return Array.from({ length }, (_, index) => {
      const date = addDays(start, index);
      return {
        date,
        key: `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`,
        week: ['日', '一', '二', '三', '四', '五', '六'][date.getDay()],
      };
    });
  }
  const start = viewMode.value === 'week' ? startOfWeek(cursorDate.value) : startOfDay(cursorDate.value);
  const length = viewMode.value === 'week' ? 7 : 1;
  return Array.from({ length }, (_, index) => {
    const date = addDays(start, index);
    return {
      date,
      key: `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`,
      week: ['日', '一', '二', '三', '四', '五', '六'][date.getDay()],
    };
  });
});

const range = computed(() => {
  if (viewMode.value === 'month') {
    const start = startOfMonth(cursorDate.value);
    return { startAt: start.getTime(), endAt: addMonths(start, 1).getTime() };
  }
  if (viewMode.value === 'week') {
    const start = startOfWeek(cursorDate.value);
    return { startAt: start.getTime(), endAt: addDays(start, 7).getTime() };
  }
  const start = startOfDay(cursorDate.value);
  return { startAt: start.getTime(), endAt: addDays(start, 1).getTime() };
});

const titleText = computed(() => {
  if (viewMode.value === 'month') return `${cursorDate.value.getFullYear()}年${cursorDate.value.getMonth() + 1}月`;
  if (viewMode.value === 'day') return formatDate(cursorDate.value);
  const start = startOfWeek(cursorDate.value);
  const end = addDays(start, 6);
  return `${formatDate(start)} - ${formatDate(end)}`;
});

const selectedDayEvents = computed(() => eventsOnDay(cursorDate.value));
const pendingCount = computed(() => events.value.filter((it) => normalizeStatus(it.status) === 'PENDING').length);
const acceptedCount = computed(() => events.value.filter((it) => normalizeStatus(it.status) === 'ACCEPTED').length);
const groupCards = computed(() => {
  const now = Date.now();
  return [
    {
      key: 'pending',
      label: '待处理',
      tone: 'pending',
      count: events.value.filter((it) => {
        const status = normalizeStatus(it.status);
        return status === 'PENDING' || status === 'RESCHEDULE_PENDING';
      }).length,
    },
    {
      key: 'upcoming',
      label: '即将上课',
      tone: 'accepted',
      count: events.value.filter((it) => {
        const status = normalizeStatus(it.status);
        return status === 'ACCEPTED' && Number(it.startAt) >= now;
      }).length,
    },
    {
      key: 'done',
      label: '已完成',
      tone: 'done',
      count: events.value.filter((it) => normalizeStatus(it.status) === 'COMPLETED').length,
    },
    {
      key: 'abnormal',
      label: '异常',
      tone: 'muted',
      count: events.value.filter((it) => {
        const status = normalizeStatus(it.status);
        return status === 'REJECTED' || status === 'CANCELED';
      }).length,
    },
  ].filter((it) => it.count > 0);
});
const totalHoursText = computed(() => {
  const minutes = events.value.reduce((sum, it) => sum + Math.max(0, Math.round((Number(it.endAt) - Number(it.startAt)) / 60000)), 0);
  if (!minutes) return '0';
  const hours = minutes / 60;
  return Number.isInteger(hours) ? `${hours}` : hours.toFixed(1);
});
const monthCells = computed(() => {
  const gridStart = startOfWeek(startOfMonth(cursorDate.value));
  return Array.from({ length: 42 }, (_, index) => {
    const date = addDays(gridStart, index);
    return {
      date,
      count: eventsOnDay(date).length,
      inMonth: date.getMonth() === cursorDate.value.getMonth(),
      key: `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`,
    };
  });
});

function startOfDay(value: Date) {
  const d = new Date(value);
  d.setHours(0, 0, 0, 0);
  return d;
}

function startOfWeek(value: Date) {
  const d = startOfDay(value);
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  return addDays(d, diff);
}

function startOfMonth(value: Date) {
  const d = startOfDay(value);
  d.setDate(1);
  return d;
}

function addDays(value: Date, days: number) {
  const d = new Date(value);
  d.setDate(d.getDate() + days);
  return d;
}

function addMonths(value: Date, months: number) {
  const d = startOfMonth(value);
  d.setMonth(d.getMonth() + months);
  return d;
}

function daysInMonth(value: Date) {
  return new Date(value.getFullYear(), value.getMonth() + 1, 0).getDate();
}

function isSameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function normalizeStatus(status?: string | null) {
  return String(status || '').trim().toUpperCase();
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}

function formatDate(value: Date) {
  return `${value.getMonth() + 1}月${value.getDate()}日`;
}

function formatHm(ms: number) {
  const d = new Date(ms);
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

function statusText(status?: string | null) {
  const s = normalizeStatus(status);
  if (s === 'PENDING') return '待确认';
  if (s === 'ACCEPTED') return '已确认';
  if (s === 'RESCHEDULE_PENDING') return '待确认调课';
  if (s === 'REJECTED') return '已拒绝';
  if (s === 'CANCELED') return '已取消';
  if (s === 'COMPLETED') return '已完成';
  return s || '未知';
}

function statusTone(status?: string | null) {
  const s = normalizeStatus(status);
  return {
    pending: s === 'PENDING' || s === 'RESCHEDULE_PENDING',
    accepted: s === 'ACCEPTED',
    done: s === 'COMPLETED',
    muted: s === 'REJECTED' || s === 'CANCELED',
  };
}

function lessonTypeText(type?: string | null) {
  const s = normalizeStatus(type);
  if (s === 'TRIAL') return '试课';
  if (s === 'NORMAL') return '正式课';
  return '课程';
}

function durationText(event: ScheduleEvent) {
  const minutes = Math.max(0, Math.round((Number(event.endAt) - Number(event.startAt)) / 60000));
  if (!minutes) return '时长待确认';
  if (minutes % 60 === 0) return `${minutes / 60} 小时`;
  return `${minutes} 分钟`;
}

function participantText(participant: any) {
  const name = String(participant?.realName || participant?.name || participant?.nickname || '').trim();
  if (name) return `与 ${name}`;
  const id = participant?.id || participant?.uid;
  return id ? `对方 ID ${id}` : '';
}

function eventsOnDay(day: Date) {
  const start = startOfDay(day).getTime();
  const end = addDays(startOfDay(day), 1).getTime();
  return events.value
    .filter((it) => Number(it.startAt) < end && Number(it.endAt) > start)
    .slice()
    .sort((a, b) => Number(a.startAt) - Number(b.startAt));
}

function switchMode(next: ViewMode) {
  if (viewMode.value === next) return;
  viewMode.value = next;
  void load();
}

function selectDay(day: Date) {
  cursorDate.value = startOfDay(day);
  if (viewMode.value === 'day') void load();
}

function moveRange(step: number) {
  if (viewMode.value === 'month') {
    cursorDate.value = addMonths(cursorDate.value, step);
  } else {
    cursorDate.value = addDays(cursorDate.value, viewMode.value === 'week' ? step * 7 : step);
  }
  void load();
}

function goToday() {
  cursorDate.value = startOfDay(new Date());
  void load();
}

async function load() {
  if (!userStore.isLoggedIn) return;
  loading.value = true;
  error.value = '';
  try {
    events.value = await scheduleApi.listEvents({ startAt: range.value.startAt, endAt: range.value.endAt, includePending: true });
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载课程表失败';
    events.value = [];
  } finally {
    loading.value = false;
  }
}

function openEvent(event: ScheduleEvent) {
  if (event.courseId) {
    uni.navigateTo({ url: `/pages/course/lesson-detail?courseId=${event.courseId}&eventId=${event.id}` });
    return;
  }
  uni.showToast({ title: '当前课节暂未关联合作', icon: 'none' });
}

function goLogin() {
  uni.switchTab({ url: '/pages/me/index' });
}

onShow(() => {
  const returned = consumeLiveReturnRefresh();
  if (returned) {
    uni.showToast({ title: '课程表已同步课堂状态', icon: 'none' });
  }
  void load();
});

onPullDownRefresh(async () => {
  await load();
  uni.stopPullDownRefresh();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px 28px;
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 20px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #122529 0%, #176b62 68%, #d59f54 135%);
  box-shadow: 0 18px 38px rgba(18, 37, 41, 0.2);
}

.eyebrow,
.title,
.subtitle,
.stat-num,
.stat-label,
.day-week,
.day-date,
.time-main,
.time-sub,
.lesson-title,
.lesson-meta,
.lesson-desc {
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

.today-btn {
  flex: 0 0 auto;
  align-self: flex-start;
  height: 34px;
  line-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  color: #143133;
  background: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 900;
}

.today-btn::after,
.nav-btn::after {
  border: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 14px;
}

.nav-btn {
  width: 42px;
  height: 38px;
  line-height: 38px;
  border: 0;
  border-radius: 14px;
  color: #26373b;
  background: #fff;
  font-size: 24px;
  box-shadow: 0 8px 20px rgba(18, 37, 41, 0.06);
}

.mode-tabs {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 4px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(18, 37, 41, 0.06);
}

.mode-tab {
  height: 32px;
  line-height: 32px;
  border-radius: 12px;
  color: #64717a;
  text-align: center;
  font-size: 13px;
  font-weight: 900;
}

.mode-tab.active {
  color: #fff;
  background: #0f766e;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.stat-card {
  padding: 12px 6px;
  border-radius: 16px;
  background: #fff;
  text-align: center;
  box-shadow: 0 8px 20px rgba(18, 37, 41, 0.05);
}

.stat-num {
  color: #142326;
  font-size: 17px;
  font-weight: 900;
}

.stat-label {
  margin-top: 4px;
  color: #7a858e;
  font-size: 11px;
}

.group-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.group-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.07);
}

.group-chip.pending {
  background: rgba(213, 159, 84, 0.12);
}

.group-chip.accepted {
  background: rgba(15, 118, 110, 0.1);
}

.group-chip.done {
  background: rgba(32, 201, 151, 0.1);
}

.group-chip.muted {
  background: rgba(100, 113, 122, 0.1);
}

.group-count,
.group-label,
.month-head-text,
.month-head-sub,
.month-weektext,
.month-date,
.month-count,
.month-empty {
  display: block;
}

.group-count {
  color: #162529;
  font-size: 16px;
  font-weight: 900;
}

.group-label {
  color: #58666f;
  font-size: 12px;
}

.day-strip {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  overflow-x: auto;
}

.day-chip {
  position: relative;
  flex: 1 0 42px;
  min-width: 42px;
  padding: 9px 4px;
  border-radius: 16px;
  background: #fff;
  text-align: center;
  border: 1px solid rgba(18, 37, 41, 0.06);
}

.day-chip.active {
  color: #fff;
  background: #0f766e;
}

.day-chip.today:not(.active) {
  border-color: rgba(15, 118, 110, 0.32);
}

.day-week {
  font-size: 11px;
  opacity: 0.76;
}

.day-date {
  margin-top: 3px;
  font-size: 16px;
  font-weight: 900;
}

.day-dot {
  position: absolute;
  top: 5px;
  right: 5px;
  min-width: 15px;
  height: 15px;
  line-height: 15px;
  border-radius: 999px;
  color: #fff;
  background: #d59f54;
  font-size: 10px;
}

.month-panel {
  margin: 12px 0;
  padding: 14px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(18, 37, 41, 0.05);
}

.month-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.month-head-text {
  color: #15292d;
  font-size: 15px;
  font-weight: 900;
}

.month-head-sub {
  color: #758189;
  font-size: 11px;
}

.month-weekbar,
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.month-weekbar {
  margin-bottom: 6px;
}

.month-weektext {
  color: #7a858e;
  font-size: 11px;
  text-align: center;
}

.month-cell {
  min-height: 58px;
  padding: 8px 4px;
  border-radius: 14px;
  background: #f7f9f9;
  border: 1px solid transparent;
  box-sizing: border-box;
}

.month-cell.muted {
  opacity: 0.42;
}

.month-cell.active {
  border-color: rgba(15, 118, 110, 0.35);
  background: rgba(15, 118, 110, 0.12);
}

.month-cell.today:not(.active) {
  border-color: rgba(213, 159, 84, 0.45);
}

.month-cell.busy .month-count {
  color: #0f766e;
}

.month-date {
  color: #182c30;
  font-size: 13px;
  font-weight: 900;
  text-align: center;
}

.month-count,
.month-empty {
  margin-top: 6px;
  font-size: 10px;
  text-align: center;
}

.month-count {
  color: #4e5f67;
}

.month-empty {
  color: #b2bcc3;
}

.lesson-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.lesson-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.07);
  box-shadow: 0 10px 24px rgba(18, 37, 41, 0.05);
}

.time-col {
  flex: 0 0 48px;
  text-align: center;
}

.time-main {
  color: #142326;
  font-size: 15px;
  font-weight: 900;
}

.time-sub {
  margin-top: 3px;
  color: #8a949d;
  font-size: 11px;
}

.lesson-main {
  min-width: 0;
  flex: 1;
}

.lesson-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.lesson-title {
  min-width: 0;
  color: #172326;
  font-size: 15px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  flex: 0 0 auto;
  padding: 4px 8px;
  border-radius: 999px;
  color: #5f6d75;
  background: #eef2f3;
  font-size: 11px;
  font-weight: 900;
}

.status.pending {
  color: #9a5b00;
  background: #fff4dc;
}

.status.accepted {
  color: #0f766e;
  background: #e6f5f3;
}

.status.done {
  color: #25603c;
  background: #e8f6ed;
}

.status.muted {
  color: #87929b;
  background: #f1f3f4;
}

.lesson-meta,
.lesson-desc {
  margin-top: 5px;
  color: #78848c;
  font-size: 12px;
  line-height: 1.45;
}

.arrow {
  color: #8c969e;
  font-size: 22px;
}
</style>
