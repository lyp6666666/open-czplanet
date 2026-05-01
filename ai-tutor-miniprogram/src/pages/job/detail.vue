<template>
  <view class="page">
    <view v-if="loading" class="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="job" class="content">
      <view class="card hero">
        <view class="hero-head">
          <text class="title">{{ job.title || job.subjectName || '需求详情' }}</text>
          <text class="price">{{ budgetText }}</text>
        </view>
        <view class="meta">
          <text v-if="placeText" class="meta-item">{{ placeText }}</text>
          <text v-if="classModeText" class="meta-item">{{ classModeText }}</text>
          <text v-if="job.frequencyPerWeek" class="meta-item">每周 {{ job.frequencyPerWeek }} 次</text>
          <text v-if="educationText" class="meta-item">{{ educationText }}</text>
          <text v-if="teacherGenderText" class="meta-item">{{ teacherGenderText }}</text>
          <text v-if="stageText" class="meta-item">{{ stageText }}</text>
          <text v-if="gradeText" class="meta-item">{{ gradeText }}</text>
        </view>
        <view v-if="job.publisherIdentity === 'ORGANIZATION'" class="org-tip">
          <text class="org-tip-text">机构单：请注意甄别信息，沟通确认上课安排与费用规则。</text>
        </view>
      </view>

      <view class="card sec">
        <text class="sec-title">需求描述</text>
        <text class="desc">{{ job.description || '暂无描述' }}</text>
      </view>

      <view v-if="job.teacherRequirementDetail" class="card sec">
        <text class="sec-title">对教员的要求</text>
        <text class="desc">{{ job.teacherRequirementDetail }}</text>
      </view>

      <view v-if="job.availableTime || scheduleText" class="card sec">
        <text class="sec-title">可上课时间</text>
        <text v-if="job.availableTime" class="desc">{{ job.availableTime }}</text>
        <text v-else class="desc">{{ scheduleText }}</text>
      </view>

      <view v-if="showAddress" class="card sec">
        <text class="sec-title">上课地点</text>
        <view class="kv">
          <text class="k">城市</text>
          <text class="v">{{ job.city || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">地址</text>
          <text class="v">{{ job.address || '暂无' }}</text>
        </view>
      </view>

      <view v-if="job.publisher" class="card publisher">
        <view class="pub-row">
          <image class="pub-avatar" :src="resolveImageUrl(job.publisher.avatar)" mode="aspectFill"></image>
          <view class="pub-main">
            <text class="pub-name">{{ job.publisher.displayName || '发布者' }}</text>
            <text class="pub-sub">{{ job.publisher.identityLabel || '发布者' }}</text>
          </view>
        </view>
        <view v-if="isOwner" class="owner-actions">
          <button class="plain-btn" @click="goEdit">编辑需求</button>
          <button class="plain-btn" :disabled="closingBusy" @click="toggleDemandStatus">
            {{ closingBusy ? '处理中...' : isClosed ? '重新打开' : '关闭需求' }}
          </button>
          <button class="primary-btn" @click="viewApplications">收到的申请</button>
        </view>
        <view v-if="userStore.isTutor" class="publisher-actions">
          <button class="plain-btn" :disabled="favoriteBusy" @click="toggleFavorite">{{ favorited ? '已收藏' : '收藏需求' }}</button>
          <button class="primary-btn" @click="handleContact">发起沟通</button>
        </view>
      </view>

      <view v-if="latestApplication" class="card sec">
        <text class="sec-title">最近申请进度</text>
        <view class="kv">
          <text class="k">当前状态</text>
          <text class="v">{{ applicationStatusText(latestApplication.status) }}</text>
        </view>
        <view class="kv">
          <text class="k">下一步</text>
          <text class="v">{{ applicationHint(latestApplication) }}</text>
        </view>
        <view class="modal-actions">
          <button class="plain-btn" @click="openLatestApplication">查看申请</button>
          <button v-if="canOpenApplicationChat" class="primary-btn" @click="enterLatestChat">进入聊天</button>
        </view>
      </view>
    </view>

    <view v-else class="loading">
      <text class="loading-text">暂无数据</text>
    </view>

    <view v-if="showApplyModal" class="mask" @click="closeApplyModal">
      <view class="modal card" @click.stop>
        <view class="modal-head">
          <text class="modal-title">发起需求沟通</text>
        </view>
        <view class="form-item">
          <text class="label">授课形式</text>
          <view class="mode-seg">
            <view
              v-for="item in availableTeachingModeOptions"
              :key="item.value"
              class="mode-item"
              :class="{ active: applyTeachingMode === item.value }"
              @click="applyTeachingMode = item.value"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
        <view class="form-item">
          <text class="label">申请语</text>
          <textarea v-model="applyContent" class="textarea" placeholder="简单说明你的授课思路和可配合时间"></textarea>
        </view>
        <view class="modal-actions">
          <button class="plain-btn" :disabled="applyBusy" @click="closeApplyModal">取消</button>
          <button class="primary-btn" :disabled="applyBusy" @click="submitApply">{{ applyBusy ? '发送中...' : '发送申请' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { jobsApi } from '@/api/jobs';
import { applicationApi, type TutorApplication } from '@/api/application';
import { favoritesApi } from '@/api/favorites';
import { useUserStore } from '@/stores/user';
import { resolveImageUrl } from '@/utils/request';
import { ensureTutorApproved } from '@/utils/tutorGuard';

const userStore = useUserStore();
const job = ref<any>(null);
const loading = ref(false);
const latestApplication = ref<TutorApplication | null>(null);
const favorited = ref(false);
const favoriteBusy = ref(false);
const closingBusy = ref(false);
const showApplyModal = ref(false);
const applyBusy = ref(false);
const applyContent = ref('您好，我想申请沟通这个家教需求。');
const applyTeachingMode = ref<'ONLINE' | 'OFFLINE'>('ONLINE');
const teachingModeOptions = [
  { label: '线上', value: 'ONLINE' as const },
  { label: '线下', value: 'OFFLINE' as const },
];
const availableTeachingModeOptions = computed(() => {
  const mode = normalizeLower(job.value?.classMode);
  if (mode === 'online') return teachingModeOptions.filter((it) => it.value === 'ONLINE');
  if (mode === 'offline') return teachingModeOptions.filter((it) => it.value === 'OFFLINE');
  return teachingModeOptions;
});
const canOpenApplicationChat = computed(() => latestApplication.value?.chatAccessStatus === 'CHAT_ENABLED' && !!latestApplication.value?.roomId);

onLoad(async (options: any) => {
  if (options.id) {
    await fetchDetail(options.id);
  }
  if (String(options?.__intent || '') === 'open-demand-apply' && userStore.isLoggedIn && userStore.currentRole === 'tutor') {
    setTimeout(() => {
      void handleContact();
    }, 300);
  }
});

const fetchDetail = async (id: number) => {
  try {
    loading.value = true;
    const res: any = await jobsApi.getDemandView(id);
    job.value = res;
    await loadFavoriteState(Number(id));
    await loadLatestApplication();
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

async function loadFavoriteState(id: number) {
  favorited.value = false;
  if (!userStore.isLoggedIn || !userStore.isTutor || !id) return;
  try {
    const ids = await favoritesApi.checkDemandFavorites([id]);
    favorited.value = Array.isArray(ids) && ids.some((it) => Number(it) === id);
  } catch {
    favorited.value = false;
  }
}

async function loadLatestApplication() {
  latestApplication.value = null;
  if (!userStore.isLoggedIn || !userStore.isTutor || !job.value?.id) return;
  try {
    const page = await applicationApi.sent({ pageSize: 20 });
    const rows = Array.isArray(page?.list) ? page.list : [];
    latestApplication.value =
      rows.find((it) => String(it.contextType || '').toUpperCase() === 'DEMAND' && Number(it.contextId) === Number(job.value.id)) || null;
  } catch {
    latestApplication.value = null;
  }
}

const isOwner = computed(() => {
  const mine = Number(userStore.userInfo?.id || 0);
  const publisherUid = Number(job.value?.publisher?.uid || 0);
  return !!mine && !!publisherUid && mine === publisherUid;
});

const isClosed = computed(() => {
  return Number(job.value?.status) !== 1 || String(job.value?.bizStatus || '').toUpperCase().includes('CLOSED');
});

async function toggleFavorite() {
  if (!job.value?.id || favoriteBusy.value) return;
  if (!ensureTutorApproved('教师审核通过后才能收藏需求。')) return;
  favoriteBusy.value = true;
  try {
    if (favorited.value) await favoritesApi.unfavoriteDemand(job.value.id);
    else await favoritesApi.favoriteDemand(job.value.id);
    favorited.value = !favorited.value;
    uni.showToast({ title: favorited.value ? '已收藏' : '已取消收藏', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || e?.msg || '操作失败', icon: 'none' });
  } finally {
    favoriteBusy.value = false;
  }
}

const normalizeLower = (v: unknown) => String(v || '').trim().toLowerCase();

const classModeText = computed(() => {
  const s = normalizeLower(job.value?.classMode);
  if (!s) return '';
  if (s === 'online') return '线上';
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '';
});

const placeText = computed(() => {
  const s = normalizeLower(job.value?.classMode);
  if (s === 'online') return '线上';
  const city = String(job.value?.city || '').trim();
  if (city) return city;
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '';
});

const budgetText = computed(() => {
  const min = job.value?.budgetMin;
  const max = job.value?.budgetMax;
  if (min != null && max != null && String(min) !== '' && String(max) !== '') return `¥${min}-${max}/小时`;
  if (min != null && String(min) !== '') return `¥${min}/小时起`;
  if (max != null && String(max) !== '') return `≤¥${max}/小时`;
  return '面议';
});

const stageText = computed(() => {
  const s = String(job.value?.stageCode || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'PRESCHOOL') return '幼小';
  if (s === 'PRIMARY') return '小学';
  if (s === 'JUNIOR') return '初中';
  if (s === 'SENIOR') return '高中';
  if (s === 'OTHER') return '其他';
  return s;
});

const gradeText = computed(() => {
  const s = String(job.value?.gradeCode || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'PRESCHOOL') return '学前';
  if (s.startsWith('GRADE')) return `小学${s.replace('GRADE', '')}年级`;
  if (s.startsWith('JUNIOR')) return `初${s.replace('JUNIOR', '')}`;
  if (s.startsWith('SENIOR')) return `高${s.replace('SENIOR', '')}`;
  if (s === 'SELF_EXAM') return '自考';
  if (s.startsWith('COLLEGE')) return `大学${s.replace('COLLEGE', '')}`;
  if (s === 'ADULT') return '成人';
  return s;
});

const educationText = computed(() => {
  const s = String(job.value?.educationRequirement || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'TOP2') return 'TOP2';
  if (s === 'C985') return '985';
  if (s === 'C211') return '211';
  if (s === 'DOUBLE_FIRST_CLASS') return '双一流';
  if (s === 'FIRST_TIER') return '一本';
  if (s === 'BACHELOR') return '本科及以上';
  if (s === 'OVERSEAS') return '海外';
  if (s === 'QS50') return 'QS50';
  return s;
});

const teacherGenderText = computed(() => {
  const s = normalizeLower(job.value?.teacherGenderPreference);
  if (!s) return '';
  if (s === 'male') return '偏好男老师';
  if (s === 'female') return '偏好女老师';
  if (s === 'both') return '性别不限';
  return '';
});

const scheduleText = computed(() => {
  const raw = String(job.value?.schedule || '').trim();
  if (!raw) return '';
  try {
    const obj = JSON.parse(raw);
    return JSON.stringify(obj, null, 2);
  } catch {
    return raw;
  }
});

const showAddress = computed(() => {
  const s = normalizeLower(job.value?.classMode);
  return s === 'offline' || s === 'both';
});

function goEdit() {
  if (!job.value?.id) return;
  uni.navigateTo({ url: `/pages/post/index?id=${job.value.id}` });
}

async function toggleDemandStatus() {
  if (!job.value?.id || closingBusy.value) return;
  closingBusy.value = true;
  try {
    const next = isClosed.value ? 1 : 0;
    await jobsApi.updateDemand(Number(job.value.id), { status: next });
    job.value.status = next;
    uni.showToast({ title: next === 1 ? '已重新打开' : '已关闭需求', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || e?.msg || '操作失败', icon: 'none' });
  } finally {
    closingBusy.value = false;
  }
}

function viewApplications() {
  if (!job.value?.id) return;
  uni.navigateTo({ url: `/pages/application/list?tab=received&contextType=DEMAND&contextId=${job.value.id}` });
}

const handleContact = async () => {
    if (!job.value) return;
    if (!ensureTutorApproved('教师审核通过后才能发起沟通。', 'open-demand-apply')) return;
    const mode = normalizeLower(job.value?.classMode);
    if (mode === 'online') applyTeachingMode.value = 'ONLINE';
    else if (mode === 'offline') applyTeachingMode.value = 'OFFLINE';
    else applyTeachingMode.value = 'ONLINE';
    applyContent.value = `您好，我想申请沟通这个${job.value.subjectName || '家教'}需求。`;
    showApplyModal.value = true;
};

function closeApplyModal() {
  if (applyBusy.value) return;
  showApplyModal.value = false;
}

async function submitApply() {
  if (!job.value || applyBusy.value) return;
  const targetUid = job.value?.publisher?.uid;
  if (!targetUid) {
    uni.showToast({ title: '缺少发布者信息', icon: 'none' });
    return;
  }
  const content = String(applyContent.value || '').trim();
  if (!content) {
    uni.showToast({ title: '请填写申请语', icon: 'none' });
    return;
  }
  if (!availableTeachingModeOptions.value.some((it) => it.value === applyTeachingMode.value)) {
    uni.showToast({ title: '请选择需求支持的授课形式', icon: 'none' });
    return;
  }
  applyBusy.value = true;
  try {
    const created: any = await applicationApi.create({
      receiverUid: targetUid,
      contextType: 'DEMAND',
      contextId: Number(job.value.id),
      content,
      teachingMode: applyTeachingMode.value,
      clientRequestId: `mp-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    });
    uni.showToast({ title: '申请已发送', icon: 'success' });
    showApplyModal.value = false;
    if (created?.id) {
      latestApplication.value = {
        id: Number(created.id),
        senderUid: Number(userStore.userInfo?.id || 0),
        receiverUid: Number(targetUid),
        contextType: 'DEMAND',
        contextId: Number(job.value.id),
        content,
        status: 'PENDING',
        teachingMode: applyTeachingMode.value,
      } as TutorApplication;
      setTimeout(() => {
        uni.navigateTo({ url: `/pages/application/detail?id=${created.id}` });
      }, 450);
    }
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '申请发送失败', icon: 'none' });
  } finally {
    applyBusy.value = false;
  }
}

function applicationStatusText(status?: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '待处理';
  if (s === 'ACCEPTED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  return s || '未知';
}

function applicationHint(item: TutorApplication) {
  if (item.status === 'PENDING') return '等待需求方处理你的申请';
  if (item.chatAccessStatus === 'PAYMENT_REQUIRED') return '待教师支付信息费';
  if (item.chatAccessStatus === 'CHAT_ENABLED') return '聊天已开放，可继续确认合作';
  if (item.status === 'REJECTED') return '本次申请已结束，可重新发起';
  return '可进入申请详情查看';
}

function openLatestApplication() {
  if (!latestApplication.value?.id) return;
  uni.navigateTo({ url: `/pages/application/detail?id=${latestApplication.value.id}` });
}

function enterLatestChat() {
  if (!latestApplication.value?.roomId) return;
  uni.navigateTo({ url: `/pages/chat/room?id=${latestApplication.value.roomId}` });
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg);
}

.content {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
}

.hero {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 900;
  color: var(--text);
  flex: 1;
  min-width: 0;
}

.price {
  font-size: 13px;
  font-weight: 900;
  color: #ff4d4f;
  white-space: nowrap;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--muted);
  font-size: 12px;
}

.meta-item {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(255, 255, 255, 0.7);
}

.org-tip {
  background: rgba(10, 120, 255, 0.06);
  border: 1px solid rgba(10, 120, 255, 0.16);
  border-radius: 12px;
  padding: 10px 12px;
}

.org-tip-text {
  color: #0a78ff;
  font-size: 12px;
  line-height: 1.6;
}

.sec {
  padding: 16px;
}

.sec-title {
  font-size: 14px;
  font-weight: 900;
  color: var(--text);
  margin-bottom: 12px;
  display: block;
}

.desc {
  background: rgba(31, 35, 41, 0.04);
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 12px;
  padding: 12px;
  font-size: 13px;
  color: var(--text);
  line-height: 1.7;
  white-space: pre-wrap;
}

.kv {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid rgba(31, 35, 41, 0.08);
}

.kv:first-of-type {
  border-top: none;
  padding-top: 0;
}

.k {
  width: 84px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  flex-shrink: 0;
}

.v {
  color: var(--text);
  font-size: 13px;
  line-height: 1.7;
  flex: 1;
}

.publisher {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.owner-actions,
.publisher-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 10px;
}

.pub-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pub-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: 1px solid rgba(31, 35, 41, 0.12);
  background: #f0f0f0;
  flex-shrink: 0;
}

.pub-main {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 2px;
}

.pub-name {
  font-size: 14px;
  font-weight: 900;
  color: var(--text);
}

.pub-sub {
  font-size: 12px;
  color: var(--muted);
}

.plain-btn,
.primary-btn {
  height: 42px;
  line-height: 42px;
  border: 0;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 900;
  width: 100%;
}

.plain-btn {
  color: #35444b;
  background: #eef2f3;
}

.primary-btn {
  color: #fff;
  background: #0f766e;
}

.loading {
  padding-top: 120px;
  display: flex;
  justify-content: center;
}

.loading-text {
  color: var(--muted);
  font-size: 14px;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 60;
}

.modal {
  width: min(560px, 100%);
  padding: 16px;
  display: grid;
  gap: 12px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-size: 16px;
  font-weight: 900;
  color: var(--text);
}

.form-item {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 900;
}

.mode-seg {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.mode-item {
  height: 40px;
  line-height: 40px;
  border-radius: 12px;
  text-align: center;
  color: #5d6972;
  background: #f3f5f6;
  font-size: 13px;
  font-weight: 800;
}

.mode-item.active {
  color: #fff;
  background: #0f766e;
}

.textarea {
  min-height: 88px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 12px;
  background: #fff;
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
  box-sizing: border-box;
}

.modal-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
</style>
