<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { assetsApi } from '@/api/assets'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'
import { normalizeAvatarUrl } from '@/utils/avatar'

const router = useRouter()
const settings = useSettingsStore()
const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)
const saved = ref<string | null>(null)

const greeting = ref(DEFAULT_APPLICATION_GREETING)

// -----------------------------------------------------------------------------
// 头像逻辑
// -----------------------------------------------------------------------------
const avatarFileInput = ref<HTMLInputElement | null>(null)
const avatarBusy = ref(false)
const avatarError = ref<string | null>(null)
// 头像上传上限需要与后端/Nacos 配置保持一致，当前统一为 4MB。
const AVATAR_MAX_SIZE_BYTES = 4 * 1024 * 1024

const userInitial = computed(() => {
  const n = auth.user?.name?.trim()
  return n && n.length > 0 ? n.slice(0, 1) : 'U'
})

const avatarSrc = computed(() => normalizeAvatarUrl(auth.user?.avatar))

function onAvatarClick() {
  if (avatarBusy.value) return
  avatarFileInput.value?.click()
}

async function onAvatarSelected(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 重置 input，允许重复选择同一文件
  if (avatarFileInput.value) avatarFileInput.value.value = ''

  if (file.size > AVATAR_MAX_SIZE_BYTES) {
    avatarError.value = '图片大小不能超过 4MB'
    return
  }

  avatarBusy.value = true
  avatarError.value = null
  try {
    const r = await assetsApi.uploadImage(file, 'avatar')
    await userApi.updateUserInfo({ baseUserInfo: { avatar: r.url } })
    await auth.refreshMe()
    saved.value = '头像已更新'
  } catch (err) {
    avatarError.value = err instanceof Error ? err.message : '头像上传失败'
  } finally {
    avatarBusy.value = false
  }
}

// -----------------------------------------------------------------------------
// 基础信息
// -----------------------------------------------------------------------------
const phoneMasked = computed(() => {
  const p = auth.user?.phone || ''
  const s = String(p)
  if (s.length < 7) return s
  return `${s.slice(0, 3)}****${s.slice(-4)}`
})

const isTeacher = computed(() => auth.role === 'TEACHER')

async function load() {
  loading.value = true
  error.value = null
  saved.value = null
  try {
    const v = await settings.load()
    greeting.value = v
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (loading.value) return
  loading.value = true
  error.value = null
  saved.value = null
  try {
    const v = await settings.saveApplicationGreeting(greeting.value)
    greeting.value = v
    saved.value = '已保存'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    loading.value = false
  }
}

function onReset() {
  greeting.value = DEFAULT_APPLICATION_GREETING
}

function back() {
  router.back()
}

function goUpdatePhone() {
  router.push({ name: 'updatePhone' })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="head">
        <button class="btn-back" type="button" @click="back">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path
              d="M15 18L9 12L15 6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          返回
        </button>
        <div class="title">设置</div>
        <div class="placeholder"></div>
      </div>

      <div class="content">
        <!-- 错误提示 -->
        <div v-if="error || avatarError" class="hint error">{{ error || avatarError }}</div>
        <div v-if="saved" class="hint ok">{{ saved }}</div>

        <!-- 1. 个人资料卡片 -->
        <section class="card profile-card">
          <div class="card-header">个人资料</div>
          <div class="profile-row">
            <div class="avatar-wrapper" @click="onAvatarClick">
              <img v-if="auth.user?.avatar" class="avatar" :src="avatarSrc" alt="avatar" />
              <div v-else class="avatar fallback">{{ userInitial }}</div>
              <div class="avatar-mask">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                  <circle cx="12" cy="13" r="4" />
                </svg>
              </div>
              <input
                ref="avatarFileInput"
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                style="display: none"
                @change="onAvatarSelected"
              />
            </div>
            <div class="info">
              <div class="nickname">{{ auth.user?.name || '未设置昵称' }}</div>
              <div class="role-badge">{{ isTeacher ? '教师端' : '学生端' }}</div>
            </div>
          </div>
        </section>

        <!-- 2. 账号安全 -->
        <section class="card">
          <div class="card-header">账号安全</div>
          <div class="list-item">
            <div class="item-label">手机号</div>
            <div class="item-value">{{ phoneMasked }}</div>
            <button class="btn-text" type="button" @click="goUpdatePhone">更换</button>
          </div>
        </section>

        <!-- 3. 业务设置 (仅教师端显示打招呼语设置，或者双端都显示根据需求，目前保留原逻辑双端都可设) -->
        <section class="card">
          <div class="card-header">
            <span>默认申请语</span>
            <div class="header-actions">
              <button class="btn-text" type="button" :disabled="loading" @click="onReset">恢复默认</button>
              <button class="btn-primary-sm" type="button" :disabled="loading" @click="onSave">保存</button>
            </div>
          </div>
          <div class="greeting-box">
            <div class="greeting-tip">发起家教申请时会自动填充，可在发送前临时修改。</div>
            <textarea
              v-model="greeting"
              class="textarea"
              rows="4"
              placeholder="请输入默认申请语..."
              :disabled="loading"
            />
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 80px 20px 20px;
  display: flex;
  justify-content: center;
}

.container {
  width: 100%;
  max-width: 600px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
}

.head {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 100;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
  position: relative;
  z-index: 1;
}

.btn-back:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #333;
}

.title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 18px;
  font-weight: 800;
  color: #1a1a1a;
}

.placeholder {
  width: 48px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.card-header {
  padding: 16px;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Profile Section */
.profile-row {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 64px;
  height: 64px;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.fallback {
  background: #e6f7ff;
  color: #00b578;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 600;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nickname {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.role-badge {
  display: inline-block;
  background: #e6f7ff;
  color: #00b578;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  align-self: flex-start;
}

/* List Items */
.list-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.list-item:last-child {
  border-bottom: none;
}

.item-label {
  width: 80px;
  color: #666;
  font-size: 14px;
}

.item-value {
  flex: 1;
  color: #1a1a1a;
  font-weight: 500;
  font-size: 14px;
}

/* Buttons */
.btn-text {
  border: none;
  background: transparent;
  color: #00b578;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.btn-text:hover {
  background: #f0fdf9;
}

.btn-text:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.btn-primary-sm {
  border: none;
  background: #00b578;
  color: #fff;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary-sm:hover {
  background: #009462;
}

.btn-primary-sm:disabled {
  background: #a0dcc5;
  cursor: not-allowed;
}

/* Greeting Section */
.greeting-box {
  padding: 16px;
}

.greeting-tip {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  resize: vertical;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
}

.textarea:focus {
  border-color: #00b578;
}

/* Hints */
.hint {
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 8px;
}

.hint.error {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.hint.ok {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}
</style>
