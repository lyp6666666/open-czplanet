<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { assetsApi } from '@/api/assets'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)

const avatar = ref('')
const avatarUploading = ref(false)

const realName = ref('')

const canSubmit = computed(() => {
  return !!(avatar.value.trim() && realName.value.trim())
})

async function onSelectAvatarFile(e: Event) {
  error.value = null
  const input = e.target as HTMLInputElement | null
  const f = input?.files?.[0]
  if (!f) return
  if (!f.type || !f.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }
  if (f.size > 2 * 1024 * 1024) {
    error.value = '头像文件不能超过 2MB'
    return
  }
  avatarUploading.value = true
  try {
    const r = await assetsApi.uploadImage(f, 'avatar')
    avatar.value = r.url
  } catch (e2) {
    error.value = e2 instanceof Error ? e2.message : '头像上传失败'
  } finally {
    avatarUploading.value = false
    if (input) input.value = ''
  }
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  error.value = null
  try {
    await userApi.updateUserInfo({
      baseUserInfo: { avatar: avatar.value.trim() },
      teacherExtInfo: { realName: realName.value.trim() },
    })
    await auth.refreshMe()
    await router.replace('/tutor/onboarding/profile')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    await router.replace('/auth/tutor')
    return
  }
  const me = await auth.refreshMe()
  if (me?.userType !== 1) {
    await router.replace('/student/post')
    return
  }
  if (me?.avatar) avatar.value = me.avatar

  if (me?.teacherProfile?.realName) realName.value = me.teacherProfile.realName
  const completed = !!(me?.avatar && me.teacherProfile?.realName?.trim())
  if (completed) {
    await router.replace('/tutor/jobs')
  }
})
</script>

<template>
  <div class="page">
    <div class="shell">
      <div class="card board">
        <aside class="left">
          <div class="brand">
            <div class="logo">Hi，欢迎成为老师</div>
            <div class="slogan">开启高效接单方式，仅需3步</div>
          </div>
          <div class="illustration" />
        </aside>

        <section class="right">
          <div class="right-head">
            <div class="r-title">创建老师名片</div>
            <div class="r-desc">完善基础信息后即可进入需求页</div>
          </div>

          <div v-if="error" class="hint error">{{ error }}</div>

          <div class="form">
            <div class="field">
              <div class="label">头像</div>
              <div class="avatar-row">
                <img v-if="avatar" class="avatar-img" :src="avatar" alt="avatar" />
                <div v-else class="avatar-img fallback">T</div>
                <input class="avatar-file" type="file" accept="image/*" :disabled="avatarUploading || loading" @change="onSelectAvatarFile" />
              </div>

            </div>

            <label class="field">
              <div class="label">姓名</div>
              <input v-model="realName" class="input" placeholder="请输入姓名" :disabled="loading" />
            </label>

            <div class="actions">
              <button class="btn btn-primary" type="button" :disabled="loading || !canSubmit" @click="submit">下一步</button>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #12b4ab;
  padding: 24px;
}

.shell {
  width: min(980px, 100%);
}

.card.board {
  display: grid;
  grid-template-columns: 44% 56%;
  overflow: hidden;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.16);
}

.left {
  background: #e7fbfa;
  padding: 34px 30px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #0b2f2d;
}

.slogan {
  margin-top: 10px;
  font-size: 14px;
  color: rgba(11, 47, 45, 0.7);
}

.illustration {
  flex: 1;
  border-radius: 12px;
  background:
    radial-gradient(circle at 25% 35%, rgba(18, 180, 171, 0.22), transparent 55%),
    radial-gradient(circle at 70% 55%, rgba(18, 180, 171, 0.18), transparent 55%),
    linear-gradient(135deg, rgba(18, 180, 171, 0.12), rgba(18, 180, 171, 0.04));
}

.right {
  padding: 34px 34px 26px;
}

.right-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.r-title {
  font-size: 18px;
  font-weight: 700;
  color: #0b2f2d;
}

.r-desc {
  font-size: 12px;
  color: rgba(11, 47, 45, 0.55);
}

.hint {
  margin: 10px 0 16px;
  font-size: 13px;
}

.hint.error {
  color: #d03050;
}

.form {
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: rgba(11, 47, 45, 0.7);
}

.input {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-img {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: cover;
  background: rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  color: rgba(0, 0, 0, 0.55);
}

.avatar-file {
  font-size: 12px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-top: 8px;
}

.avatar-item {
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  padding: 0;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
}

.avatar-item-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
  object-fit: cover;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.btn {
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #fff;
  color: rgba(11, 47, 45, 0.85);
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary {
  border: none;
  background: #12b4ab;
  color: #fff;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 860px) {
  .card.board {
    grid-template-columns: 1fr;
  }
  .left {
    display: none;
  }
}
</style>
