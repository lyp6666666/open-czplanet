<template>
  <view class="login-card">
    <view class="header">
      <text class="title">{{ isTutor ? '家教登录' : '家长/学生登录' }}</text>
      <text class="subtitle">{{ isTutor ? '我是老师' : '我要找老师' }}</text>
    </view>

    <view class="role-toggle">
      <view 
        class="toggle-item" 
        :class="{ active: !isTutor }" 
        @click="isTutor = false"
      >
        家长/学生
      </view>
      <view 
        class="toggle-item" 
        :class="{ active: isTutor }" 
        @click="isTutor = true"
      >
        家教
      </view>
    </view>

    <view class="form">
      <view class="input-group">
        <text class="label">手机号</text>
        <input 
          class="input" 
          type="number" 
          v-model="phone" 
          placeholder="请输入手机号" 
          maxlength="11"
        />
      </view>

      <view class="input-group">
        <text class="label">验证码</text>
        <view class="code-row">
          <input 
            class="input code-input" 
            type="number" 
            v-model="code" 
            placeholder="请输入验证码" 
            maxlength="6"
          />
          <view 
            class="send-btn" 
            :class="{ disabled: countdown > 0 }"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </view>
        </view>
      </view>

      <view class="agreement">
        <radio 
          :checked="agreed" 
          @click="agreed = !agreed" 
          color="#00bebd" 
          style="transform:scale(0.7)"
        />
        <text class="text">我已阅读并同意 <text class="link">《用户协议》</text> 和 <text class="link">《隐私政策》</text></text>
      </view>

      <button class="submit-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录 / 注册' }}
      </button>

      <view class="divider">
        <text class="line"></text>
        <text class="text">或</text>
        <text class="line"></text>
      </view>

      <button class="wechat-btn" @click="handleWechatLogin">
        微信一键登录
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  loading?: boolean;
}>();

const emit = defineEmits(['login', 'wechat-login', 'send-code']);

const isTutor = ref(false);
const phone = ref('');
const code = ref('');
const agreed = ref(false);
const countdown = ref(0);
let timer: any = null;

const handleSendCode = () => {
  if (countdown.value > 0) return;
  if (!phone.value || phone.value.length !== 11) {
    uni.showToast({ title: '手机号格式错误', icon: 'none' });
    return;
  }
  emit('send-code', phone.value);

  countdown.value = 60;
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

const handleLogin = () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' });
    return;
  }
  if (!phone.value || !code.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' });
    return;
  }
  emit('login', {
    phone: phone.value,
    code: code.value,
    role: isTutor.value ? 'tutor' : 'student'
  });
};

const handleWechatLogin = () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' });
    return;
  }
  emit('wechat-login', isTutor.value ? 'tutor' : 'student');
};
</script>

<style lang="scss" scoped>
.login-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 30px 24px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
  width: 100%;
  box-sizing: border-box;
}

.header {
  margin-bottom: 24px;
  text-align: center;
  
  .title {
    display: block;
    font-size: 20px;
    font-weight: 900;
    color: #1f2329;
    margin-bottom: 6px;
  }
  
  .subtitle {
    font-size: 14px;
    color: #646a73;
  }
}

.role-toggle {
  display: flex;
  background: #f6f7fb;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 24px;
  
  .toggle-item {
    flex: 1;
    text-align: center;
    padding: 8px 0;
    font-size: 14px;
    font-weight: 600;
    color: #646a73;
    border-radius: 8px;
    transition: all 0.3s;
    
    &.active {
      background: #ffffff;
      color: #00bebd;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }
  }
}

.form {
  .input-group {
    margin-bottom: 16px;
    
    .label {
      display: block;
      font-size: 12px;
      color: #646a73;
      margin-bottom: 8px;
    }
    
    .input {
      height: 44px;
      background: #ffffff;
      border: 1px solid rgba(31, 35, 41, 0.12);
      border-radius: 12px;
      padding: 0 12px;
      font-size: 14px;
      color: #1f2329;
      
      &:focus {
        border-color: #00bebd;
      }
    }
    
    .code-row {
      display: flex;
      gap: 10px;
      
      .code-input {
        flex: 1;
      }
      
      .send-btn {
        width: 100px;
        height: 44px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f6f7fb;
        border-radius: 12px;
        font-size: 13px;
        color: #00bebd;
        font-weight: 600;
        
        &.disabled {
          color: #999;
          background: #f0f0f0;
        }
      }
    }
  }
  
  .agreement {
    display: flex;
    align-items: center;
    margin-bottom: 24px;
    
    .text {
      font-size: 12px;
      color: #646a73;
      margin-left: 4px;
    }
    
    .link {
      color: #00bebd;
    }
  }
  
  .submit-btn {
    height: 44px;
    background: #00bebd;
    border-radius: 12px;
    color: #ffffff;
    font-size: 16px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    
    &:active {
      opacity: 0.9;
    }
    
    &[disabled] {
      opacity: 0.6;
    }
  }
  
  .divider {
    display: flex;
    align-items: center;
    margin: 20px 0;
    
    .line {
      flex: 1;
      height: 1px;
      background: rgba(31, 35, 41, 0.1);
    }
    
    .text {
      padding: 0 10px;
      font-size: 12px;
      color: #999;
    }
  }
  
  .wechat-btn {
    height: 44px;
    background: #07c160;
    border-radius: 12px;
    color: #ffffff;
    font-size: 16px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    
    &:active {
      opacity: 0.9;
    }
  }
}
</style>
