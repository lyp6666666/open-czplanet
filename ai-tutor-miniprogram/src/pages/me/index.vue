<template>
  <view class="container">
    <view v-if="userStore.isLoggedIn" class="user-info">
      <image class="avatar" :src="resolveImageUrl(userStore.userInfo?.avatar)" mode="aspectFill"></image>
      <text class="nickname">{{ userStore.userInfo?.name || '用户' }}</text>
      <text class="role-tag">{{ userStore.currentRole === 'student' ? '家长/学生' : '家教' }}</text>

      <view class="action-list">
        <view class="action-item" @click="handleSwitchRole">
          <text>切换到 {{ userStore.currentRole === 'student' ? '家教版' : '学生版' }}</text>
          <u-icon name="arrow-right" color="#c8c7cc" size="16"></u-icon>
        </view>

        <view v-if="userStore.currentRole === 'student'" class="action-item" @click="goToMyJobs">
          <text>我的需求</text>
          <u-icon name="arrow-right" color="#c8c7cc" size="16"></u-icon>
        </view>

        <view class="action-item" @click="goToCourses">
          <view>
            <text>我的合作</text>
            <text class="sub">查看试课、正式课表和退费进度</text>
          </view>
          <u-icon name="arrow-right" color="#c8c7cc" size="16"></u-icon>
        </view>

        <view class="action-item" @click="goToFavorites">
          <view>
            <text>我的收藏</text>
            <text class="sub">{{ userStore.currentRole === 'student' ? '管理收藏的老师' : '管理收藏的需求' }}</text>
          </view>
          <u-icon name="arrow-right" color="#c8c7cc" size="16"></u-icon>
        </view>

        <view class="action-item" @click="goToEmailSettings">
          <view>
            <text>邮箱提醒</text>
            <text class="sub">{{ emailSubText }}</text>
          </view>
          <u-icon name="arrow-right" color="#c8c7cc" size="16"></u-icon>
        </view>

        <view class="action-item" @click="handleLogout">
          <text style="color: #dd524d;">退出登录</text>
        </view>
      </view>
    </view>

    <view v-else class="login-wrapper">
      <LoginCard
        @login="handleLogin"
        @wechat-login="handleWechatLogin"
        @send-code="handleSendCode"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { userApi } from '@/api/user';
import { useUserStore } from '@/stores/user';
import LoginCard from '@/components/LoginCard.vue';
import { resolveImageUrl } from '@/utils/request';
import { resumePendingRedirect } from '@/utils/authRedirect';

const userStore = useUserStore();
const emailStatus = ref<any>(null);
const emailPrimaryVerified = computed(() => emailStatus.value?.primaryEmail?.verifyStatus === 'VERIFIED');
const emailSubText = computed(() =>
  emailPrimaryVerified.value ? '已开启邮件提醒，可接收消息、开课提醒和课后总结' : '绑定后可接收消息、开课提醒和课后总结',
);

function continueAfterLogin() {
  if (resumePendingRedirect()) {
    return;
  }
  void loadEmailStatus();
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

const handleLogout = () => {
  userStore.logout();
};

const handleSwitchRole = () => {
  if (userStore.currentRole === 'student') {
    userStore.switchRole('tutor');
  } else {
    userStore.switchRole('student');
  }
};

const goToMyJobs = () => {
  uni.navigateTo({ url: '/pages/my-jobs/index' });
};

const goToCourses = () => {
  uni.navigateTo({ url: '/pages/course/list' });
};

const goToFavorites = () => {
  uni.navigateTo({ url: '/pages/favorites/index' });
};

const goToEmailSettings = () => {
  uni.navigateTo({ url: '/pages/account/email' });
};

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

onMounted(() => {
  void loadEmailStatus();
});

onShow(() => {
  if (userStore.isLoggedIn) {
    continueAfterLogin();
  }
});
</script>

<style lang="scss" scoped>
.container {
  padding: 20px;
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background-color: var(--bg);
}

.login-wrapper {
  width: 100%;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
  width: 100%;
  box-sizing: border-box;

  .avatar {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    margin-bottom: 12px;
    background-color: #f0f0f0;
    border: 2px solid #fff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }

  .nickname {
    font-size: 18px;
    margin-bottom: 4px;
    font-weight: 900;
    color: #1f2329;
  }

  .role-tag {
    font-size: 12px;
    color: #00bebd;
    background: rgba(0, 190, 189, 0.1);
    padding: 2px 8px;
    border-radius: 4px;
    margin-bottom: 24px;
  }

  .action-list {
    width: 100%;

    .action-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 0;
      border-bottom: 1px solid rgba(31, 35, 41, 0.08);
      font-size: 14px;
      color: #1f2329;

      .sub {
        display: block;
        margin-top: 4px;
        font-size: 12px;
        color: #8f959e;
      }

      &:last-child {
        border-bottom: none;
      }

      &:active {
        opacity: 0.7;
      }
    }
  }
}
</style>
