<template>
  <view class="container">
    <view v-if="userStore.isLoggedIn" class="user-info">
      <image class="avatar" :src="userStore.userInfo?.avatar || '/static/logo.png'" mode="aspectFill"></image>
      <text class="nickname">{{ userStore.userInfo?.name || 'User' }}</text>
      <text class="role-tag">{{ userStore.currentRole === 'student' ? 'Parent/Student' : 'Tutor' }}</text>
      
      <button type="default" @click="handleSwitchRole" style="margin-top: 20px; width: 200px;">
        Switch to {{ userStore.currentRole === 'student' ? 'Tutor' : 'Student' }}
      </button>
      
      <button v-if="userStore.currentRole === 'student'" type="default" @click="goToMyJobs" style="margin-top: 20px; width: 200px;">
        My Demands
      </button>

      <button type="warn" @click="handleLogout" style="margin-top: 20px; width: 200px;">Logout</button>
    </view>
    <view v-else class="login-container">
      <text class="login-tip">Please login to continue</text>
      <button type="primary" @click="handleLogin" style="margin-top: 20px; width: 200px;">WeChat Login</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();

const handleLogin = async () => {
  try {
    await userStore.login();
    uni.showToast({ title: 'Login Success', icon: 'success' });
  } catch (error: any) {
    uni.showToast({ title: error.message || 'Login Failed', icon: 'none' });
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
</script>

<style lang="scss" scoped>
.container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  .avatar {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    margin-bottom: 10px;
    background-color: #f0f0f0;
  }
  .nickname {
    font-size: 18px;
    margin-bottom: 20px;
    font-weight: bold;
  }
}
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  .login-tip {
    font-size: 16px;
    color: #666;
    margin-bottom: 20px;
  }
}
</style>
