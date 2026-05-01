<template>
  <view class="page">
    <view v-if="loading" class="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="tutor" class="content">
      <view class="card profile">
        <view class="head">
          <image class="avatar" :src="resolveImageUrl(tutor.user.avatar)" mode="aspectFill"></image>
          <view class="head-main">
            <view class="name-row">
              <text class="name">{{ tutor.user.name || '老师' }}</text>
              <text class="role-tag">家教</text>
            </view>
            <view class="badges" v-if="badges.length">
              <text v-for="b in badges" :key="b" class="badge">{{ b }}</text>
            </view>
            <view class="tags" v-if="subjectTags.length">
              <text v-for="t in subjectTags" :key="t" class="tag">{{ t }}</text>
            </view>
          </view>
          <view class="price" v-if="priceText">
            <text class="price-num">{{ priceText }}</text>
          </view>
        </view>

        <view class="meta">
          <text v-if="tutor.teacherProfile?.city" class="meta-item">{{ tutor.teacherProfile.city }}</text>
          <text v-if="teachingModeText" class="meta-item">{{ teachingModeText }}</text>
          <text v-if="tutor.teacherProfile?.highestEduSchool" class="meta-item">{{ tutor.teacherProfile.highestEduSchool }}</text>
          <text v-if="tutor.teacherProfile?.education" class="meta-item">{{ tutor.teacherProfile.education }}</text>
          <text v-if="tutor.teacherProfile?.experienceYears != null" class="meta-item">{{ tutor.teacherProfile.experienceYears }} 年教龄</text>
        </view>
      </view>

      <view class="card sec">
        <text class="sec-title">基本信息</text>
        <view class="kv">
          <text class="k">擅长科目</text>
          <text class="v">{{ tutor.teacherProfile?.subject || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">学历</text>
          <text class="v">{{ tutor.teacherProfile?.education || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">所在城市</text>
          <text class="v">{{ tutor.teacherProfile?.city || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">授课方式</text>
          <text class="v">{{ teachingModeText || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">课时费用</text>
          <text class="v">{{ priceText || '暂无' }}</text>
        </view>
      </view>

      <view class="card sec">
        <text class="sec-title">个人简介</text>
        <text class="desc">{{ tutor.teacherProfile?.introduction || '暂无简介' }}</text>
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
        <view class="inline-actions">
          <button class="mini-btn ghost" @click="openLatestApplication">查看申请</button>
          <button v-if="canOpenApplicationChat" class="mini-btn" @click="enterLatestChat">进入聊天</button>
        </view>
      </view>

      <view class="ops">
        <button class="op-btn ghost" :disabled="favoriteBusy" @click="toggleFavorite">{{ favorited ? '已收藏' : '收藏老师' }}</button>
        <button class="op-btn primary" @click="handleContact">发起申请</button>
      </view>
    </view>

    <view v-else class="loading">
      <text class="loading-text">暂无数据</text>
    </view>

    <view v-if="showApplyModal" class="mask" @click="closeApplyModal">
      <view class="modal card" @click.stop>
        <view class="modal-head">
          <text class="modal-title">向老师发起申请</text>
          <view class="modal-close" @click="closeApplyModal">
            <u-icon name="close" size="18" color="#646a73"></u-icon>
          </view>
        </view>

        <view class="modal-body">
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
            <textarea class="textarea" v-model="applyContent" placeholder="请简单描述你的需求，便于老师判断是否通过"></textarea>
          </view>
        </view>

        <view class="modal-footer">
          <u-button type="primary" color="#00bebd" shape="circle" :disabled="applyBusy" @click="handleApply">
            {{ applyBusy ? '发送中...' : '发送申请' }}
          </u-button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { request, resolveImageUrl } from '@/utils/request';
import { applicationApi, type TutorApplication } from '@/api/application';
import { chatApi } from '@/api/chat';
import { favoritesApi } from '@/api/favorites';
import { useUserStore } from '@/stores/user';
import { ensureStudentMode } from '@/utils/studentGuard';

const userStore = useUserStore();
const tutor = ref<any>(null);
const loading = ref(false);
const latestApplication = ref<TutorApplication | null>(null);
const showApplyModal = ref(false);
const applyContent = ref('您好老师，我这边有一个家教需求，方便聊聊吗？');
const applyTeachingMode = ref<'ONLINE' | 'OFFLINE'>('ONLINE');
const applyBusy = ref(false);
const favorited = ref(false);
const favoriteBusy = ref(false);
const teachingModeOptions = [
  { label: '线上', value: 'ONLINE' as const },
  { label: '线下', value: 'OFFLINE' as const },
];
const availableTeachingModeOptions = computed(() => {
  const teacherMode = String(tutor.value?.teacherProfile?.teachingMode || '').trim().toUpperCase();
  if (teacherMode === 'ONLINE') return teachingModeOptions.filter((it) => it.value === 'ONLINE');
  if (teacherMode === 'OFFLINE') return teachingModeOptions.filter((it) => it.value === 'OFFLINE');
  return teachingModeOptions;
});
const canOpenApplicationChat = computed(() => latestApplication.value?.chatAccessStatus === 'CHAT_ENABLED' && !!latestApplication.value?.roomId);
const subjectTags = computed(() => {
  const raw = String(tutor.value?.teacherProfile?.subject || '').trim();
  if (!raw) return [];
  return raw
    .split(/[,，、/|\\s]+/g)
    .map(s => s.trim())
    .filter(Boolean)
    .slice(0, 6);
});

const teachingModeText = computed(() => {
  const v = String(tutor.value?.teacherProfile?.teachingMode || '').trim().toUpperCase();
  if (!v) return '';
  if (v === 'ONLINE') return '线上';
  if (v === 'OFFLINE') return '线下';
  if (v === 'BOTH') return '线上/线下';
  return '';
});

const priceText = computed(() => {
  const v = tutor.value?.teacherProfile?.ratePerHour;
  if (v === null || v === undefined || v === '') return '';
  return `${v} 元/小时`;
});

const badges = computed(() => {
  const arr: string[] = [];
  const rn = Number(tutor.value?.realnameVerifyStatus);
  const edu = Number(tutor.value?.eduVerifyStatus);
  if (rn === 2) arr.push('实名认证');
  if (edu === 2) arr.push('学历认证');
  return arr;
});

onLoad(async (options: any) => {
  if (options.id) {
    await fetchDetail(options.id);
  }
  if (String(options?.__intent || '') === 'open-tutor-apply' && userStore.isLoggedIn && userStore.currentRole === 'student') {
    setTimeout(() => {
      void handleContact();
    }, 300);
  }
});

const fetchDetail = async (id: string) => {
  try {
    loading.value = true;
    const res: any = await request({
      url: `/user/card?uid=${id}`
    });
    tutor.value = res;
    await loadFavoriteState();
    await loadLatestApplication();
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

async function loadFavoriteState() {
  favorited.value = false;
  if (!userStore.isLoggedIn || userStore.currentRole !== 'student') return;
  const uid = tutor.value?.user?.id;
  if (!uid) return;
  try {
    const ids = await favoritesApi.checkTutorFavorites([uid]);
    favorited.value = Array.isArray(ids) && ids.some((it) => Number(it) === Number(uid));
  } catch {
    favorited.value = false;
  }
}

async function loadLatestApplication() {
  latestApplication.value = null;
  if (!userStore.isLoggedIn || userStore.currentRole !== 'student') return;
  const tutorId = Number(tutor.value?.teacherProfile?.id || 0);
  if (!(tutorId > 0)) return;
  try {
    const page = await applicationApi.sent({ pageSize: 20 });
    const rows = Array.isArray(page?.list) ? page.list : [];
    latestApplication.value =
      rows.find((it) => String(it.contextType || '').toUpperCase() === 'TUTOR' && Number(it.contextId) === tutorId) || null;
  } catch {
    latestApplication.value = null;
  }
}

async function toggleFavorite() {
  const uid = tutor.value?.user?.id;
  if (!uid || favoriteBusy.value) return;
  if (!ensureStudentMode('学生/家长身份可以收藏老师。')) return;
  favoriteBusy.value = true;
  try {
    if (favorited.value) await favoritesApi.unfavoriteTutor(uid);
    else await favoritesApi.favoriteTutor(uid);
    favorited.value = !favorited.value;
    uni.showToast({ title: favorited.value ? '已收藏' : '已取消收藏', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || e?.msg || '操作失败', icon: 'none' });
  } finally {
    favoriteBusy.value = false;
  }
}

const handleContact = async () => {
  if (!ensureStudentMode('学生/家长身份可以向老师发起申请。', 'open-tutor-apply')) return;
  if (!tutor.value) return;
  const teacherMode = String(tutor.value?.teacherProfile?.teachingMode || '').trim().toUpperCase();
  if (teacherMode === 'ONLINE') applyTeachingMode.value = 'ONLINE';
  else if (teacherMode === 'OFFLINE') applyTeachingMode.value = 'OFFLINE';
  else applyTeachingMode.value = 'ONLINE';
  showApplyModal.value = true;
};

const closeApplyModal = () => {
  if (applyBusy.value) return;
  showApplyModal.value = false;
};

const handleApply = async () => {
  if (!tutor.value) return;
  if (applyBusy.value) return;
  const targetUid = tutor.value.user?.id;
  const tutorId = tutor.value.teacherProfile?.id;
  const content = String(applyContent.value || '').trim();
  if (!targetUid || !tutorId) return;
  if (!content) {
    uni.showToast({ title: '请填写申请语', icon: 'none' });
    return;
  }
  if (!availableTeachingModeOptions.value.some((it) => it.value === applyTeachingMode.value)) {
    uni.showToast({ title: '请选择老师支持的授课形式', icon: 'none' });
    return;
  }
  applyBusy.value = true;
  try {
    const clientRequestId = `mp-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const msg: any = await chatApi.startChatByApplication({
      receiverUid: targetUid,
      contextType: 'TUTOR',
      contextId: tutorId,
      content,
      teachingMode: applyTeachingMode.value,
      clientRequestId,
    });
    const roomId = msg?.message?.roomId;
    const applicationId = msg?.body?.applicationId || msg?.message?.body?.applicationId;
    if (roomId) {
      showApplyModal.value = false;
      uni.navigateTo({ url: `/pages/chat/room?id=${roomId}` });
      return;
    }
    uni.showToast({ title: '申请已发送', icon: 'success' });
    showApplyModal.value = false;
    if (applicationId) {
      latestApplication.value = {
        id: Number(applicationId),
        senderUid: Number(userStore.userInfo?.id || 0),
        receiverUid: Number(targetUid),
        contextType: 'TUTOR',
        contextId: Number(tutorId),
        content,
        status: 'PENDING',
        teachingMode: applyTeachingMode.value,
      } as TutorApplication;
      setTimeout(() => {
        uni.navigateTo({ url: `/pages/application/detail?id=${applicationId}` });
      }, 450);
    }
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '发送申请失败', icon: 'none' });
  } finally {
    applyBusy.value = false;
  }
};

function applicationStatusText(status?: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'PENDING') return '待处理';
  if (s === 'ACCEPTED') return '已通过';
  if (s === 'REJECTED') return '已拒绝';
  return s || '未知';
}

function applicationHint(item: TutorApplication) {
  if (item.status === 'PENDING') return '等待老师处理你的申请';
  if (item.chatAccessStatus === 'PAYMENT_REQUIRED') return '等待教师支付信息费';
  if (item.chatAccessStatus === 'CHAT_ENABLED') return '聊天已开放，可继续确认试课';
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

.profile {
  padding: 18px 16px;
  display: grid;
  gap: 12px;
}

.head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.head-main {
  flex: 1;
  min-width: 0;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: #f0f0f0;
  border: 2px solid #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.name {
  font-size: 18px;
  font-weight: 900;
  color: var(--text);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-tag {
  font-size: 12px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 900;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}

.meta-item {
  padding: 4px 10px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
}

.badges {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.badge {
  font-size: 10px;
  line-height: 16px;
  color: #0a78ff;
  background: rgba(10, 120, 255, 0.1);
  border: 1px solid rgba(10, 120, 255, 0.25);
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 900;
}

.tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 10px;
  line-height: 16px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
  border: 1px solid rgba(0, 190, 189, 0.25);
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 900;
}

.price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.price-num {
  color: #ff4d4f;
  font-weight: 900;
  font-size: 13px;
  white-space: nowrap;
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

.ops {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.inline-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.mini-btn {
  height: 34px;
  line-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.mini-btn.ghost {
  color: #35444b;
  background: #eef2f3;
}

.op-btn {
  height: 42px;
  line-height: 42px;
  border: 0;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 900;
}

.op-btn.ghost {
  color: #35444b;
  background: #eef2f3;
}

.op-btn.primary {
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
  border-radius: 16px;
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

.modal-close {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.form-item {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
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

.label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 900;
}

.picker-value {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  display: flex;
  align-items: center;
  color: var(--text);
  background: #fff;
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  background: #fff;
  color: var(--text);
}

.textarea {
  min-height: 88px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 10px 12px;
  background: #fff;
  color: var(--text);
  box-sizing: border-box;
  width: 100%;
}

.modal-footer {
  margin-top: 6px;
}
</style>
