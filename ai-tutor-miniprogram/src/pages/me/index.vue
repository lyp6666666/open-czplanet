<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="login-shell">
      <view class="guest-hero">
        <text class="guest-title">登录后完善资料与管理合作</text>
        <text class="guest-desc">登录后可以编辑个人资料、查看合作进展、管理收藏，并继续之前的申请或聊天流程。</text>
      </view>
      <LoginCard
        @login="handleLogin"
        @wechat-login="handleWechatLogin"
        @send-code="handleSendCode"
      />
      <view v-if="showDebugTools" class="preview-entry">
        <text class="preview-label">开发预览</text>
        <view class="preview-actions">
          <button class="preview-btn ghost" @click="previewLogin('student')">学生资料态</button>
          <button class="preview-btn" @click="previewLogin('tutor')">教师资料态</button>
        </view>
      </view>
    </view>

    <template v-else>
      <view class="hero-card">
        <view class="hero-main">
          <view class="avatar-wrap" @click="chooseAvatar">
            <image class="avatar" :src="avatarSrc" mode="aspectFill"></image>
            <view class="avatar-edit">更换头像</view>
          </view>
          <view class="hero-copy">
            <text class="nickname">{{ displayName }}</text>
            <text class="role-tag">{{ userStore.currentRole === 'student' ? '家长/学生' : '家教' }}</text>
            <text class="profile-tip">{{ profileTip }}</text>
          </view>
        </view>
      </view>

      <view v-if="savedHint" class="notice success">{{ savedHint }}</view>
      <view v-else-if="avatarHint" class="notice success">{{ avatarHint }}</view>
      <view v-if="pageError" class="notice error">{{ pageError }}</view>

      <view class="section-card">
        <view class="section-head">
          <view>
            <text class="section-title">快捷入口</text>
            <text class="section-sub">保持和网页端一致的合作、收藏、提醒入口。</text>
          </view>
        </view>

        <view class="quick-grid">
          <view v-if="userStore.currentRole === 'student'" class="quick-card" @click="goToMyJobs">
            <text class="quick-name">我的需求</text>
            <text class="quick-desc">查看已发布需求、收到的申请与待完善信息</text>
          </view>
          <view class="quick-card" @click="goToCourses">
            <text class="quick-name">我的合作</text>
            <text class="quick-desc">跟进试课、正式课表和退费进度</text>
          </view>
          <view class="quick-card" @click="goToSchedule">
            <text class="quick-name">课程表</text>
            <text class="quick-desc">查看今天、本周待上课和待确认课节</text>
          </view>
          <view class="quick-card" @click="goToFavorites">
            <text class="quick-name">我的收藏</text>
            <text class="quick-desc">{{ favoriteHint }}</text>
          </view>
          <view class="quick-card" @click="goToEmailSettings">
            <text class="quick-name">邮箱提醒</text>
            <text class="quick-desc">{{ emailSubText }}</text>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <view>
            <text class="section-title">基础信息</text>
            <text class="section-sub">这些内容会影响对方查看你的主页和合作判断。</text>
          </view>
        </view>

        <view class="form-grid">
          <view class="field">
            <text class="label">{{ userStore.currentRole === 'student' ? '昵称' : '主页展示名' }}</text>
            <input v-model="baseName" class="input" type="text" placeholder="请输入展示名称" />
          </view>
          <view class="field">
            <text class="label">性别</text>
            <picker :range="genderOptions" range-key="label" :value="genderIndex" @change="onGenderChange">
              <view class="picker-field">{{ genderLabel }}</view>
            </picker>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <view>
            <text class="section-title">{{ isTutorProfile ? '教师资料' : '学生资料' }}</text>
            <text class="section-sub">{{ isTutorProfile ? '尽量补齐授课信息，和网页端资料层级保持一致。' : '完善学生信息后，老师更容易判断是否适合接单。' }}</text>
          </view>
        </view>

        <view v-if="isTutorProfile" class="form-grid">
          <view class="field">
            <text class="label">真实姓名</text>
            <input v-model="teacherForm.realName" class="input" type="text" placeholder="请输入真实姓名" />
          </view>
          <view class="field">
            <text class="label">所在城市</text>
            <input v-model="teacherForm.city" class="input" type="text" placeholder="例如：上海" />
          </view>
          <view class="field">
            <text class="label">教授科目</text>
            <input v-model="teacherForm.subject" class="input" type="text" placeholder="例如：数学 / 英语" />
          </view>
          <view class="field">
            <text class="label">教学经验（年）</text>
            <input v-model="teacherForm.experienceYears" class="input" type="number" placeholder="例如：3" />
          </view>
          <view class="field">
            <text class="label">课时费（元/小时）</text>
            <input v-model="teacherForm.ratePerHour" class="input" type="number" placeholder="例如：180" />
          </view>
          <view class="field">
            <text class="label">授课方式</text>
            <picker :range="teachingModeOptions" range-key="label" :value="teachingModeIndex" @change="onTeachingModeChange">
              <view class="picker-field">{{ teachingModeLabel }}</view>
            </picker>
          </view>
          <view class="field">
            <text class="label">学历</text>
            <input v-model="teacherForm.education" class="input" type="text" placeholder="例如：本科 / 硕士" />
          </view>
          <view class="field">
            <text class="label">最高学历学校</text>
            <input v-model="teacherForm.highestEduSchool" class="input" type="text" placeholder="例如：复旦大学" />
          </view>
          <view class="field span-2">
            <text class="label">个人简介</text>
            <textarea v-model="teacherForm.introduction" class="textarea" maxlength="500" auto-height cursor-spacing="120" placeholder="介绍自己的授课经验、擅长方向和风格"></textarea>
          </view>
        </view>

        <view v-else class="form-grid">
          <view class="field">
            <text class="label">真实姓名</text>
            <input v-model="studentForm.realName" class="input" type="text" placeholder="请输入真实姓名" />
          </view>
          <view class="field">
            <text class="label">孩子年龄</text>
            <input v-model="studentForm.childAge" class="input" type="number" placeholder="例如：9" />
          </view>
          <view class="field">
            <text class="label">所在城市</text>
            <input v-model="studentForm.city" class="input" type="text" placeholder="例如：杭州" />
          </view>
          <view class="field">
            <text class="label">预算（元/小时）</text>
            <input v-model="studentForm.budget" class="input" type="number" placeholder="例如：160" />
          </view>
          <view class="field span-2">
            <text class="label">地址</text>
            <input v-model="studentForm.address" class="input" type="text" placeholder="例如：浦东新区张江镇..." />
          </view>
          <view class="field span-2">
            <text class="label">孩子描述 / 需求说明</text>
            <textarea v-model="studentForm.demandDescription" class="textarea" maxlength="500" auto-height cursor-spacing="120" placeholder="例如：希望提升阅读与写作，周末线下上课"></textarea>
          </view>
        </view>
      </view>

      <view v-if="isTutorProfile" class="section-card">
        <view class="section-head">
          <view>
            <text class="section-title">认证中心</text>
            <text class="section-sub">复用现有入驻资料上传与提交流程。</text>
          </view>
        </view>

        <view class="verify-list">
          <view class="verify-card">
            <view class="verify-copy">
              <text class="verify-title">实名认证</text>
              <text class="verify-desc">状态：{{ realnameStatusText }}</text>
              <text v-if="teacherForm.realnameVerifyRejectReason" class="verify-tip error">驳回原因：{{ teacherForm.realnameVerifyRejectReason }}</text>
            </view>
            <button class="verify-btn ghost" :disabled="submittingRealname" @click="chooseIdFront">
              {{ teacherForm.realnameVerifyIdFrontUrl ? '重传人像面' : '上传人像面' }}
            </button>
            <button class="verify-btn ghost" :disabled="submittingRealname" @click="chooseIdBack">
              {{ teacherForm.realnameVerifyIdBackUrl ? '重传国徽面' : '上传国徽面' }}
            </button>
            <button class="verify-btn" :disabled="submittingRealname" @click="submitRealname">
              {{ submittingRealname ? '提交中...' : '提交实名认证' }}
            </button>
          </view>

          <view class="verify-card">
            <view class="verify-copy">
              <text class="verify-title">学历认证</text>
              <text class="verify-desc">状态：{{ eduStatusText }}</text>
              <text class="verify-tip">已上传 {{ eduProofCount }} 张截图</text>
              <text v-if="teacherForm.eduVerifyRejectReason" class="verify-tip error">驳回原因：{{ teacherForm.eduVerifyRejectReason }}</text>
            </view>
            <button class="verify-btn ghost" :disabled="submittingEdu" @click="chooseEduProof">
              {{ eduProofCount ? '继续添加截图' : '上传学历截图' }}
            </button>
            <button class="verify-btn" :disabled="submittingEdu || !eduProofCount" @click="submitEducation">
              {{ submittingEdu ? '提交中...' : '提交学历认证' }}
            </button>
          </view>
        </view>
      </view>

      <view v-if="showDebugTools" class="section-card dev-card">
        <view class="section-head">
          <view>
            <text class="section-title">开发联调</text>
            <text class="section-sub">仅开发环境显示，可切换当前请求的 API 地址。</text>
          </view>
        </view>

        <view class="dev-panel">
          <text class="dev-label">当前 API 地址</text>
          <text class="dev-current">{{ runtimeBaseUrl }}</text>
          <input
            v-model="baseUrlInput"
            class="input"
            type="text"
            placeholder="例如：http://192.168.1.10:8080"
            confirm-type="done"
            @confirm="applyBaseUrl"
          />
          <view class="dev-actions">
            <button class="hero-btn ghost" @click="resetBaseUrl">恢复默认</button>
            <button class="hero-btn" @click="applyBaseUrl">保存地址</button>
          </view>
        </view>
      </view>

      <view class="action-safe-space"></view>
      <view class="bottom-action-bar">
        <button class="action-btn ghost" @click="handleLogout">退出登录</button>
        <button class="action-btn ghost" @click="handleSwitchRole">
          切换到 {{ userStore.currentRole === 'student' ? '家教版' : '学生版' }}
        </button>
        <button class="action-btn primary" :disabled="saving" @click="saveProfile">
          {{ saving ? '保存中...' : '保存资料' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import LoginCard from '@/components/LoginCard.vue';
import { assetsApi } from '@/api/assets';
import { tutorApi } from '@/api/tutor';
import { userApi } from '@/api/user';
import { useUserStore } from '@/stores/user';
import type { TeacherProfile, StudentProfile } from '@/types/domain';
import { clearBaseUrl, getBaseUrl, resolveImageUrl, setBaseUrl } from '@/utils/request';
import { resumePendingRedirect } from '@/utils/authRedirect';

const userStore = useUserStore();
const isDev = Boolean((import.meta as any).env?.DEV);
const accountInfo = typeof uni.getAccountInfoSync === 'function' ? uni.getAccountInfoSync() : null;
const showDebugTools = Boolean(
  isDev ||
  (accountInfo as any)?.miniProgram?.envVersion === 'develop' ||
  (accountInfo as any)?.miniProgram?.envVersion === 'trial',
);
const saving = ref(false);
const submittingRealname = ref(false);
const submittingEdu = ref(false);
const savedHint = ref('');
const avatarHint = ref('');
const pageError = ref('');
const emailStatus = ref<any>(null);
const baseUrlInput = ref(getBaseUrl());
const runtimeBaseUrl = ref(getBaseUrl());

const teacherForm = reactive({
  realName: '',
  city: '',
  subject: '',
  experienceYears: '',
  ratePerHour: '',
  introduction: '',
  education: '',
  highestEduSchool: '',
  teachingMode: 'ONLINE',
  realnameVerifyStatus: 0,
  realnameVerifyRejectReason: '',
  realnameVerifyIdFrontUrl: '',
  realnameVerifyIdBackUrl: '',
  eduVerifyStatus: 0,
  eduVerifyRejectReason: '',
  eduVerifyProofUrls: [] as string[],
});

const studentForm = reactive({
  realName: '',
  childAge: '',
  city: '',
  budget: '',
  address: '',
  demandDescription: '',
});

const baseName = ref('');
const avatar = ref('');
const sex = ref<number | null>(null);

const genderOptions = [
  { label: '不设置', value: null },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];
const teachingModeOptions = [
  { label: '线上', value: 'ONLINE' },
  { label: '线下', value: 'OFFLINE' },
  { label: '线上/线下', value: 'BOTH' },
];

const isTutorProfile = computed(() => userStore.currentRole === 'tutor');
const displayName = computed(() => baseName.value || userStore.userInfo?.name || '用户');
const avatarSrc = computed(() => resolveImageUrl(avatar.value || userStore.userInfo?.avatar || undefined));
const favoriteHint = computed(() => (userStore.currentRole === 'student' ? '管理收藏的老师并继续比较' : '管理收藏的需求并继续跟进'));
const emailPrimaryVerified = computed(() => emailStatus.value?.primaryEmail?.verifyStatus === 'VERIFIED');
const emailSubText = computed(() =>
  emailPrimaryVerified.value ? '已开启邮件提醒，可接收消息、开课提醒和课后总结' : '绑定后可接收消息、开课提醒和课后总结',
);
const profileTip = computed(() => {
  if (userStore.currentRole === 'tutor') {
    if (userStore.tutorStatus === 'APPROVED') return '已开通教师视角，可浏览需求、发起申请并跟进合作。';
    if (userStore.tutorStatus === 'PENDING') return '教师资料审核中，审核通过后即可完整使用教师端功能。';
    if (userStore.tutorStatus === 'REJECTED') return '教师资料需重新完善后重新提交，审核通过即可进入需求广场。';
    return '补齐教师资料后可以切换到教师视角接单与沟通。';
  }
  return '在学生视角下可以浏览老师、发布需求、管理合作与收藏。';
});
const genderIndex = computed(() => {
  const idx = genderOptions.findIndex((item) => item.value === sex.value);
  return idx < 0 ? 0 : idx;
});
const genderLabel = computed(() => genderOptions[genderIndex.value]?.label || '不设置');
const teachingModeIndex = computed(() => {
  const idx = teachingModeOptions.findIndex((item) => item.value === teacherForm.teachingMode);
  return idx < 0 ? 0 : idx;
});
const teachingModeLabel = computed(() => teachingModeOptions[teachingModeIndex.value]?.label || '线上');
const realnameStatusText = computed(() => {
  if (teacherForm.realnameVerifyStatus === 2) return '已通过';
  if (teacherForm.realnameVerifyStatus === 1) return '审核中';
  if (teacherForm.realnameVerifyStatus === 3) return '未通过';
  return '未认证';
});
const eduStatusText = computed(() => {
  if (teacherForm.eduVerifyStatus === 2) return '已通过';
  if (teacherForm.eduVerifyStatus === 1) return '审核中';
  if (teacherForm.eduVerifyStatus === 3) return '未通过';
  return '未认证';
});
const eduProofCount = computed(() => teacherForm.eduVerifyProofUrls.length);

function parseProofUrls(raw?: string) {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.map((item) => String(item || '').trim()).filter(Boolean) : [];
  } catch {
    return String(raw)
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }
}

function syncBaseUrlInput() {
  const value = getBaseUrl();
  runtimeBaseUrl.value = value;
  baseUrlInput.value = value;
}

function onGenderChange(event: any) {
  const index = Number(event?.detail?.value ?? 0);
  sex.value = genderOptions[index]?.value ?? null;
}

function onTeachingModeChange(event: any) {
  const index = Number(event?.detail?.value ?? 0);
  teacherForm.teachingMode = teachingModeOptions[index]?.value || 'ONLINE';
}

async function loadEmailStatus() {
  if (!userStore.isLoggedIn) {
    emailStatus.value = null;
    return;
  }
  try {
    emailStatus.value = await userApi.emailStatus();
  } catch {
    emailStatus.value = null;
  }
}

function fillForms() {
  const user = userStore.userInfo;
  baseName.value = user?.name || '';
  avatar.value = user?.avatar || '';
  sex.value = user?.sex ?? null;

  const teacher = (user?.teacherProfile || {}) as TeacherProfile;
  teacherForm.realName = teacher.realName || '';
  teacherForm.city = teacher.city || '';
  teacherForm.subject = teacher.subject || '';
  teacherForm.experienceYears = teacher.experienceYears != null ? String(teacher.experienceYears) : '';
  teacherForm.ratePerHour = teacher.ratePerHour != null ? String(teacher.ratePerHour) : '';
  teacherForm.introduction = teacher.introduction || '';
  teacherForm.education = teacher.education || '';
  teacherForm.highestEduSchool = teacher.highestEduSchool || '';
  teacherForm.teachingMode = teacher.teachingMode || 'ONLINE';
  teacherForm.realnameVerifyStatus = Number(teacher.realnameVerifyStatus || 0);
  teacherForm.realnameVerifyRejectReason = teacher.realnameVerifyRejectReason || '';
  teacherForm.realnameVerifyIdFrontUrl = teacher.realnameVerifyIdFrontUrl || '';
  teacherForm.realnameVerifyIdBackUrl = teacher.realnameVerifyIdBackUrl || '';
  teacherForm.eduVerifyStatus = Number(teacher.eduVerifyStatus || 0);
  teacherForm.eduVerifyRejectReason = teacher.eduVerifyRejectReason || '';
  teacherForm.eduVerifyProofUrls = parseProofUrls(teacher.eduVerifyProofUrls);

  const student = (user?.studentProfile || {}) as StudentProfile;
  studentForm.realName = student.realName || '';
  studentForm.childAge = student.childAge != null ? String(student.childAge) : '';
  studentForm.city = student.city || '';
  studentForm.budget = student.budget != null ? String(student.budget) : '';
  studentForm.address = student.address || '';
  studentForm.demandDescription = student.demandDescription || '';
}

async function refreshProfile() {
  if (!userStore.isLoggedIn) return;
  pageError.value = '';
  try {
    await userStore.refreshUserInfo();
    fillForms();
    await loadEmailStatus();
  } catch (error: any) {
    pageError.value = error?.message || '资料加载失败';
  }
}

function continueAfterLogin() {
  if (resumePendingRedirect()) return;
  void refreshProfile();
}

const handleSendCode = async (phone: string) => {
  try {
    await userStore.sendSmsCode(phone);
    uni.showToast({ title: '验证码已发送', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' });
  }
};

const handleLogin = async (data: { phone: string; code: string; role: 'student' | 'tutor' }) => {
  try {
    await userStore.loginBySms(data.phone, data.code, data.role);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(continueAfterLogin, 250);
  } catch (e: any) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' });
  }
};

const handleWechatLogin = async (role: 'student' | 'tutor') => {
  try {
    await userStore.login(role);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(continueAfterLogin, 250);
  } catch (error: any) {
    uni.showToast({ title: error.message || '登录失败', icon: 'none' });
  }
};

const previewLogin = (role: 'student' | 'tutor') => {
  userStore.devPreviewLogin(role);
  avatarHint.value = '';
  savedHint.value = '';
  pageError.value = '';
  fillForms();
  void loadEmailStatus();
};

const handleSwitchRole = () => {
  userStore.switchRole(userStore.currentRole === 'student' ? 'tutor' : 'student');
};

const handleLogout = () => {
  userStore.logout();
  savedHint.value = '';
  avatarHint.value = '';
  pageError.value = '';
  syncBaseUrlInput();
};

const goToMyJobs = () => {
  uni.navigateTo({ url: '/pages/my-jobs/index' });
};
const goToCourses = () => {
  uni.navigateTo({ url: '/pages/course/list' });
};
const goToSchedule = () => {
  uni.navigateTo({ url: '/pages/schedule/index' });
};
const goToFavorites = () => {
  uni.navigateTo({ url: '/pages/favorites/index' });
};
const goToEmailSettings = () => {
  uni.navigateTo({ url: '/pages/account/email' });
};

async function chooseAndUpload(biz: 'avatar' | 'other', onSuccess: (url: string) => void) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths?.[0];
      if (!filePath) return;
      uni.showLoading({ title: '上传中...', mask: true });
      try {
        const uploaded = await assetsApi.uploadImage(filePath, biz);
        onSuccess(uploaded.url);
      } catch (error: any) {
        uni.showToast({ title: error?.message || '上传失败', icon: 'none' });
      } finally {
        uni.hideLoading();
      }
    },
  });
}

const chooseAvatar = async () => {
  await chooseAndUpload('avatar', (url) => {
    avatar.value = url;
    avatarHint.value = '头像已上传，保存后生效';
    savedHint.value = '';
  });
};

const chooseIdFront = async () => {
  await chooseAndUpload('other', (url) => {
    teacherForm.realnameVerifyIdFrontUrl = url;
  });
};

const chooseIdBack = async () => {
  await chooseAndUpload('other', (url) => {
    teacherForm.realnameVerifyIdBackUrl = url;
  });
};

const chooseEduProof = async () => {
  if (teacherForm.eduVerifyProofUrls.length >= 3) {
    uni.showToast({ title: '最多上传 3 张截图', icon: 'none' });
    return;
  }
  await chooseAndUpload('other', (url) => {
    teacherForm.eduVerifyProofUrls = [...teacherForm.eduVerifyProofUrls, url];
  });
};

async function saveProfile() {
  if (saving.value) return;
  saving.value = true;
  pageError.value = '';
  savedHint.value = '';
  try {
    const baseUserInfo: any = {
      name: baseName.value.trim() || undefined,
      avatar: avatar.value.trim() || undefined,
      sex: sex.value ?? undefined,
    };

    const teacherExtInfo = isTutorProfile.value
      ? {
          realName: teacherForm.realName.trim() || undefined,
          city: teacherForm.city.trim() || undefined,
          subject: teacherForm.subject.trim() || undefined,
          experienceYears: teacherForm.experienceYears ? Number(teacherForm.experienceYears) : undefined,
          ratePerHour: teacherForm.ratePerHour ? Number(teacherForm.ratePerHour) : undefined,
          introduction: teacherForm.introduction.trim() || undefined,
          education: teacherForm.education.trim() || undefined,
          highestEduSchool: teacherForm.highestEduSchool.trim() || undefined,
          teachingMode: teacherForm.teachingMode || undefined,
        }
      : undefined;

    const studentExtInfo = !isTutorProfile.value
      ? {
          realName: studentForm.realName.trim() || undefined,
          childAge: studentForm.childAge ? Number(studentForm.childAge) : undefined,
          city: studentForm.city.trim() || undefined,
          budget: studentForm.budget ? Number(studentForm.budget) : undefined,
          address: studentForm.address.trim() || undefined,
          demandDescription: studentForm.demandDescription.trim() || undefined,
        }
      : undefined;

    await userApi.updateUserInfo({ baseUserInfo, teacherExtInfo, studentExtInfo });
    await refreshProfile();
    savedHint.value = '资料已保存';
    avatarHint.value = '';
  } catch (error: any) {
    pageError.value = error?.message || '保存失败';
  } finally {
    saving.value = false;
  }
}

async function submitRealname() {
  if (submittingRealname.value) return;
  if (!teacherForm.realName.trim()) {
    uni.showToast({ title: '请先填写真实姓名', icon: 'none' });
    return;
  }
  if (!teacherForm.realnameVerifyIdFrontUrl || !teacherForm.realnameVerifyIdBackUrl) {
    uni.showToast({ title: '请先上传身份证正反面', icon: 'none' });
    return;
  }
  submittingRealname.value = true;
  try {
    await saveProfile();
    await tutorApi.submitRealname({
      method: 'ID_PHOTO',
      idFrontUrl: teacherForm.realnameVerifyIdFrontUrl,
      idBackUrl: teacherForm.realnameVerifyIdBackUrl,
    });
    await refreshProfile();
    savedHint.value = '实名认证已提交';
  } catch (error: any) {
    pageError.value = error?.message || '实名认证提交失败';
  } finally {
    submittingRealname.value = false;
  }
}

async function submitEducation() {
  if (submittingEdu.value) return;
  if (!teacherForm.eduVerifyProofUrls.length) {
    uni.showToast({ title: '请先上传学历截图', icon: 'none' });
    return;
  }
  submittingEdu.value = true;
  try {
    await saveProfile();
    await tutorApi.submitEducation(teacherForm.eduVerifyProofUrls);
    await refreshProfile();
    savedHint.value = '学历认证已提交';
  } catch (error: any) {
    pageError.value = error?.message || '学历认证提交失败';
  } finally {
    submittingEdu.value = false;
  }
}

const applyBaseUrl = () => {
  if (!showDebugTools) return;
  const value = setBaseUrl(baseUrlInput.value);
  runtimeBaseUrl.value = value;
  baseUrlInput.value = value;
  uni.showToast({ title: 'API 地址已更新', icon: 'success' });
};

const resetBaseUrl = () => {
  if (!showDebugTools) return;
  const value = clearBaseUrl();
  runtimeBaseUrl.value = value;
  baseUrlInput.value = value;
  uni.showToast({ title: '已恢复默认地址', icon: 'success' });
};

onMounted(() => {
  syncBaseUrlInput();
  fillForms();
  if (userStore.isLoggedIn) {
    void refreshProfile();
  }
});

onShow(() => {
  syncBaseUrlInput();
  if (userStore.isLoggedIn) {
    void refreshProfile();
  }
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px calc(132px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, #f5f7fb 0%, #f8fbfb 100%);
  box-sizing: border-box;
}

.login-shell {
  display: grid;
  gap: 14px;
}

.preview-entry,
.guest-hero,
.hero-card,
.section-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(31, 35, 41, 0.08);
  box-shadow: 0 12px 28px rgba(31, 35, 41, 0.06);
  border-radius: 18px;
}

.guest-hero {
  padding: 18px;
}

.preview-entry {
  padding: 14px;
}

.preview-label,
.preview-btn {
  display: block;
}

.preview-label {
  font-size: 12px;
  font-weight: 800;
  color: #8f959e;
}

.preview-actions {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.preview-btn {
  height: 38px;
  line-height: 38px;
  border-radius: 12px;
  border: 0;
  font-size: 12px;
  font-weight: 800;
  background: #00bebd;
  color: #ffffff;
}

.preview-btn::after {
  border: 0;
}

.preview-btn.ghost {
  background: #f1f3f5;
  color: #4f5660;
}

.guest-title,
.guest-desc,
.nickname,
.role-tag,
.profile-tip,
.section-title,
.section-sub,
.quick-name,
.quick-desc,
.label,
.verify-title,
.verify-desc,
.verify-tip,
.dev-label,
.dev-current,
.notice {
  display: block;
}

.guest-title {
  font-size: 18px;
  font-weight: 900;
  color: #1f2329;
}

.guest-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #646a73;
}

.hero-card {
  padding: 16px 16px 15px;
}

.hero-main {
  display: flex;
  gap: 14px;
  align-items: center;
}

.avatar-wrap {
  width: 84px;
  flex-shrink: 0;
}

.avatar {
  width: 84px;
  height: 84px;
  border-radius: 24px;
  background: #eef1f6;
}

.avatar-edit {
  margin-top: 8px;
  font-size: 12px;
  text-align: center;
  color: #00a7a6;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.nickname {
  font-size: 22px;
  font-weight: 900;
  color: #1f2329;
}

.role-tag {
  width: fit-content;
  margin-top: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #00a7a6;
  font-size: 12px;
  font-weight: 700;
}

.profile-tip {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: #646a73;
}

.dev-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

.hero-btn,
.verify-btn,
.logout-btn {
  height: 42px;
  line-height: 42px;
  border-radius: 12px;
  border: 0;
  font-size: 14px;
  font-weight: 800;
  background: #00bebd;
  color: #ffffff;
}

.hero-btn::after,
.verify-btn::after,
.logout-btn::after {
  border: 0;
}

.hero-btn.ghost,
.verify-btn.ghost {
  background: rgba(31, 35, 41, 0.06);
  color: #1f2329;
}

.notice {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 12px;
  line-height: 1.6;
}

.notice.success {
  background: rgba(0, 190, 189, 0.1);
  color: #0f766e;
}

.notice.error {
  background: rgba(221, 82, 77, 0.1);
  color: #b42318;
}

.section-card {
  margin-top: 14px;
  padding: 15px 15px 16px;
}

.section-head {
  margin-bottom: 12px;
}

.section-title {
  font-size: 17px;
  font-weight: 900;
  color: #1f2329;
}

.section-sub {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #8f959e;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.quick-card {
  min-height: 92px;
  padding: 13px;
  border-radius: 13px;
  background: linear-gradient(180deg, rgba(246, 247, 251, 0.9) 0%, rgba(240, 246, 247, 0.94) 100%);
}

.quick-name {
  font-size: 15px;
  font-weight: 900;
  color: #1f2329;
}

.quick-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #646a73;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 9px;
}

.field {
  min-width: 0;
}

.field.span-2 {
  grid-column: span 2;
}

.label {
  margin-bottom: 5px;
  font-size: 12px;
  color: #646a73;
  font-weight: 800;
}

.input,
.picker-field,
.textarea {
  width: 100%;
  box-sizing: border-box;
  border-radius: 12px;
  background: #f6f7fb;
  border: 1px solid rgba(31, 35, 41, 0.08);
  color: #1f2329;
  font-size: 14px;
}

.input,
.picker-field {
  min-height: 42px;
  padding: 0 11px;
  display: flex;
  align-items: center;
}

.textarea {
  min-height: 108px;
  padding: 11px;
  line-height: 1.6;
}

.verify-list {
  display: grid;
  gap: 12px;
}

.verify-card {
  padding: 13px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid rgba(31, 35, 41, 0.08);
}

.verify-copy {
  margin-bottom: 12px;
}

.verify-title {
  font-size: 15px;
  font-weight: 900;
  color: #1f2329;
}

.verify-desc,
.verify-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #646a73;
}

.verify-tip.error {
  color: #b42318;
}

.verify-btn + .verify-btn {
  margin-top: 8px;
}

.dev-card {
  background: linear-gradient(180deg, rgba(0, 190, 189, 0.08) 0%, rgba(33, 111, 255, 0.04) 100%);
}

.dev-label {
  font-size: 12px;
  color: #646a73;
}

.dev-current {
  margin: 6px 0 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #1f2329;
  word-break: break-all;
}

.logout-btn {
  margin-top: 16px;
  background: rgba(221, 82, 77, 0.1);
  color: #dd524d;
}

@media (max-width: 375px) {
  .quick-grid,
  .form-grid,
  .dev-actions {
    grid-template-columns: 1fr;
  }

  .preview-actions,
  .bottom-action-bar {
    grid-template-columns: 1fr;
  }

  .field.span-2 {
    grid-column: span 1;
  }

  .action-safe-space {
    height: 176px;
  }
}
</style>
.action-safe-space {
  height: 120px;
}

.bottom-action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: 0.92fr 1.08fr 1.1fr;
  gap: 8px;
  padding: 10px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(245, 247, 251, 0.96);
  backdrop-filter: blur(16px);
  border-top: 1px solid rgba(31, 35, 41, 0.08);
  box-shadow: 0 -8px 20px rgba(31, 35, 41, 0.05);
}

.action-btn {
  height: 44px;
  line-height: 44px;
  border-radius: 14px;
  border: 0;
  font-size: 13px;
  font-weight: 800;
}

.action-btn::after {
  border: 0;
}

.action-btn.ghost {
  background: #eef2f6;
  color: #4f5660;
}

.action-btn.primary {
  background: #00bebd;
  color: #ffffff;
}
