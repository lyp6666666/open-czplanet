<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { assetsApi } from '@/api/assets'
import { userApi } from '@/api/user'
import { teacherVerificationApi } from '@/api/verification'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const savedHint = ref<string | null>(null)

const isTeacher = computed(() => auth.user?.userType === 1)

const name = ref('')
const sex = ref<number | null>(null)
const avatar = ref('')
const avatarUploading = ref(false)
const avatarHint = ref<string | null>(null)

const teacherRealName = ref('')
const teacherEducation = ref('')
const teacherSubject = ref('')
const teacherExperienceYears = ref<number | null>(null)
const teacherRatePerHour = ref<number | null>(null)
const teacherIntroduction = ref('')

const realnameVerifyStatus = ref<number>(0)
const realnameVerifyRejectReason = ref('')
const realnameIdnoMasked = ref('')

const eduVerifyStatus = ref<number>(0)
const eduVerifyRejectReason = ref('')

const realnameModalOpen = ref(false)
const realnameMethod = ref<'ID_PHOTO' | 'NAME_IDNO'>('ID_PHOTO')
const idFrontUrl = ref('')
const idBackUrl = ref('')
const idFrontUploading = ref(false)
const idBackUploading = ref(false)
const realnameIdNo = ref('')
const realnameSubmitBusy = ref(false)
const realnameHint = ref<string | null>(null)

const eduModalOpen = ref(false)
const eduProofUrls = ref<string[]>([])
const eduUploading = ref(false)
const eduSubmitBusy = ref(false)
const eduHint = ref<string | null>(null)

const studentRealName = ref('')
const studentChildAge = ref<number | null>(null)
const studentAddress = ref('')
const studentDemandDescription = ref('')
const studentBudget = ref<number | null>(null)

async function load() {
  if (!auth.isLoggedIn) return
  loading.value = true
  error.value = null
  savedHint.value = null
  avatarHint.value = null
  try {
    const me = await auth.refreshMe()
    name.value = me?.name || auth.user?.name || ''
    sex.value = me?.sex ?? auth.user?.sex ?? null
    avatar.value = me?.avatar || auth.user?.avatar || ''

    const tp = me?.teacherProfile
    const sp = me?.studentProfile

    teacherRealName.value = tp?.realName || ''
    teacherEducation.value = tp?.education || ''
    teacherSubject.value = tp?.subject || ''
    teacherExperienceYears.value = tp?.experienceYears ?? null
    teacherRatePerHour.value = tp?.ratePerHour != null ? Number(tp.ratePerHour) : null
    teacherIntroduction.value = tp?.introduction || ''
    realnameVerifyStatus.value = tp?.realnameVerifyStatus ?? 0
    realnameVerifyRejectReason.value = tp?.realnameVerifyRejectReason || ''
    realnameIdnoMasked.value = tp?.realnameVerifyIdnoMasked || ''
    eduVerifyStatus.value = tp?.eduVerifyStatus ?? 0
    eduVerifyRejectReason.value = tp?.eduVerifyRejectReason || ''

    studentRealName.value = sp?.realName || ''
    studentChildAge.value = sp?.childAge ?? null
    studentAddress.value = sp?.address || ''
    studentDemandDescription.value = sp?.demandDescription || ''
    studentBudget.value = sp?.budget != null ? Number(sp.budget) : null
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onSelectAvatar(e: Event) {
  avatarHint.value = null
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
    avatarHint.value = '头像已上传，点击保存生效'
  } catch (e2) {
    error.value = e2 instanceof Error ? e2.message : '头像上传失败'
  } finally {
    avatarUploading.value = false
    if (input) input.value = ''
  }
}

async function onSave() {
  savedHint.value = null
  error.value = null
  try {
    await userApi.updateUserInfo({
      baseUserInfo: {
        name: name.value.trim() || undefined,
        avatar: avatar.value.trim() || undefined,
        sex: sex.value ?? undefined,
      },
      teacherExtInfo: isTeacher.value
        ? {
            realName: teacherRealName.value.trim() || undefined,
            education: teacherEducation.value.trim() || undefined,
            subject: teacherSubject.value.trim() || undefined,
            experienceYears: teacherExperienceYears.value ?? undefined,
            ratePerHour: teacherRatePerHour.value ?? undefined,
            introduction: teacherIntroduction.value.trim() || undefined,
          }
        : undefined,
      studentExtInfo: !isTeacher.value
        ? {
            realName: studentRealName.value.trim() || undefined,
            childAge: studentChildAge.value ?? undefined,
            address: studentAddress.value.trim() || undefined,
            demandDescription: studentDemandDescription.value.trim() || undefined,
            budget: studentBudget.value ?? undefined,
          }
        : undefined,
    })
    savedHint.value = '已保存'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  }
}

const eduStatusText = computed(() => {
  if (eduVerifyStatus.value === 2) return '已完成学历认证'
  if (eduVerifyStatus.value === 1) return '审核中'
  if (eduVerifyStatus.value === 3) return '未通过'
  return '未认证'
})

const eduStatusClass = computed(() => {
  if (eduVerifyStatus.value === 2) return 'ok'
  if (eduVerifyStatus.value === 1) return 'pending'
  if (eduVerifyStatus.value === 3) return 'error'
  return 'plain'
})

function openEduVerify() {
  if (eduVerifyStatus.value === 1) return
  eduHint.value = null
  eduProofUrls.value = []
  eduModalOpen.value = true
}

function closeRealnameVerify() {
  realnameModalOpen.value = false
  realnameHint.value = null
}

function closeEduVerify() {
  eduModalOpen.value = false
  eduHint.value = null
}

async function uploadOtherImage(e: Event, setUrl: (v: string) => void, setUploading: (v: boolean) => void) {
  const input = e.target as HTMLInputElement | null
  const f = input?.files?.[0]
  if (!f) return
  if (!f.type || !f.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }
  if (f.size > 5 * 1024 * 1024) {
    error.value = '图片文件不能超过 5MB'
    return
  }
  setUploading(true)
  try {
    const r = await assetsApi.uploadImage(f, 'other')
    setUrl(r.url)
  } catch (e2) {
    error.value = e2 instanceof Error ? e2.message : '上传失败'
  } finally {
    setUploading(false)
    if (input) input.value = ''
  }
}

async function onUploadIdFront(e: Event) {
  await uploadOtherImage(
    e,
    (v) => {
      idFrontUrl.value = v
    },
    (v) => {
      idFrontUploading.value = v
    },
  )
}

async function onUploadIdBack(e: Event) {
  await uploadOtherImage(
    e,
    (v) => {
      idBackUrl.value = v
    },
    (v) => {
      idBackUploading.value = v
    },
  )
}

async function onAddEduProof(e: Event) {
  const input = e.target as HTMLInputElement | null
  const f = input?.files?.[0]
  if (!f) return
  if (!f.type || !f.type.startsWith('image/')) {
    eduHint.value = '请选择图片文件'
    return
  }
  if (f.size > 5 * 1024 * 1024) {
    eduHint.value = '图片文件不能超过 5MB'
    return
  }
  if (eduProofUrls.value.length >= 3) {
    eduHint.value = '最多上传3张截图'
    return
  }
  eduUploading.value = true
  eduHint.value = null
  try {
    const r = await assetsApi.uploadImage(f, 'other')
    eduProofUrls.value = [...eduProofUrls.value, r.url]
  } catch (e2) {
    eduHint.value = e2 instanceof Error ? e2.message : '上传失败'
  } finally {
    eduUploading.value = false
    if (input) input.value = ''
  }
}

function removeEduProof(i: number) {
  eduProofUrls.value = eduProofUrls.value.filter((_, idx) => idx !== i)
}

async function submitRealnameVerify() {
  if (realnameSubmitBusy.value) return
  realnameHint.value = null
  realnameSubmitBusy.value = true
  try {
    if (realnameMethod.value === 'ID_PHOTO') {
      if (!idFrontUrl.value.trim() || !idBackUrl.value.trim()) {
        realnameHint.value = '请上传身份证人像面与国徽面截图'
        return
      }
      await teacherVerificationApi.submitRealnameIdPhoto(idFrontUrl.value.trim(), idBackUrl.value.trim())
    } else {
      const rn = teacherRealName.value.trim()
      if (!rn) {
        realnameHint.value = '请先填写真实姓名'
        return
      }
      if (!realnameIdNo.value.trim()) {
        realnameHint.value = '请输入身份证号'
        return
      }
      await teacherVerificationApi.submitRealnameNameIdno(rn, realnameIdNo.value.trim())
    }
    closeRealnameVerify()
    savedHint.value = '已提交实名认证，等待审核'
    await load()
  } catch (e) {
    realnameHint.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    realnameSubmitBusy.value = false
  }
}

async function submitEduVerify() {
  if (eduSubmitBusy.value) return
  eduHint.value = null
  if (!eduProofUrls.value.length) {
    eduHint.value = '请上传学信网截图'
    return
  }
  eduSubmitBusy.value = true
  try {
    await teacherVerificationApi.submitEducation(eduProofUrls.value)
    closeEduVerify()
    savedHint.value = '已提交学历认证，等待审核'
    await load()
  } catch (e) {
    eduHint.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    eduSubmitBusy.value = false
  }
}

function onLogout() {
  auth.logout()
  void router.replace({ name: 'home' })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">我的</div>
      <div class="actions">
        <button class="btn" type="button" @click="onLogout">退出登录</button>
        <button class="btn btn-primary" type="button" :disabled="loading" @click="onSave">保存</button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="savedHint" class="hint ok">{{ savedHint }}</div>
    <div v-else-if="avatarHint" class="hint ok">{{ avatarHint }}</div>

    <div class="card form">
      <div class="sec">
        <div class="sec-title">基础信息</div>
        <div class="grid">
          <label class="field span2">
            <div class="label">头像</div>
            <div class="avatar-row">
              <img v-if="avatar" class="avatar-img" :src="avatar" alt="avatar" />
              <div v-else class="avatar-img fallback">U</div>
              <input class="avatar-file" type="file" accept="image/*" :disabled="avatarUploading" @change="onSelectAvatar" />
            </div>
          </label>
          <label class="field">
            <div class="label">真实姓名</div>
            <input v-model="teacherRealName" class="input" placeholder="请输入真实姓名" />
          </label>
          <label class="field">
            <div class="label">性别</div>
            <select v-model="sex" class="input">
              <option :value="null">不设置</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </label>
        </div>
      </div>

      <div class="sec">
        <div class="sec-title">{{ isTeacher ? '教师资料' : '家长资料' }}</div>
        <div class="grid" v-if="isTeacher">
          <label class="field">
            <div class="label-row">
              <div class="label">学历</div>
              <div class="verify">
                <span class="badge" :class="eduStatusClass">{{ eduStatusText }}</span>
                <button
                  class="link-btn"
                  type="button"
                  :disabled="eduVerifyStatus === 1 || eduVerifyStatus === 2"
                  @click="openEduVerify"
                >
                  {{ eduVerifyStatus === 3 ? '重新提交' : '去认证' }}
                </button>
              </div>
            </div>
            <input v-model="teacherEducation" class="input" placeholder="例如：本科" />
            <div v-if="eduVerifyStatus === 3 && eduVerifyRejectReason" class="mini-hint error">驳回原因：{{ eduVerifyRejectReason }}</div>
          </label>
          <label class="field">
            <div class="label">教授科目</div>
            <input v-model="teacherSubject" class="input" placeholder="例如：数学/英语" />
          </label>
          <label class="field">
            <div class="label">教学经验（年）</div>
            <input v-model.number="teacherExperienceYears" class="input" inputmode="numeric" placeholder="例如：3" />
          </label>
          <label class="field span2">
            <div class="label">简介</div>
            <textarea v-model="teacherIntroduction" class="textarea" rows="6" placeholder="写点你的优势与授课风格" />
          </label>
        </div>

        <div class="grid" v-else>
          <label class="field">
            <div class="label">姓名</div>
            <input v-model="studentRealName" class="input" placeholder="例如：王女士" />
          </label>
          <label class="field">
            <div class="label">孩子年龄</div>
            <input v-model.number="studentChildAge" class="input" inputmode="numeric" placeholder="例如：9" />
          </label>
          <label class="field span2">
            <div class="label">地址</div>
            <input v-model="studentAddress" class="input" placeholder="例如：北京·朝阳·望京" />
          </label>
          <label class="field span2">
            <div class="label">需求描述</div>
            <textarea v-model="studentDemandDescription" class="textarea" rows="4" placeholder="例如：希望提高应用题，孩子基础一般" />
          </label>
          <label class="field">
            <div class="label">预算</div>
            <input v-model.number="studentBudget" class="input" inputmode="decimal" placeholder="例如：120" />
          </label>
        </div>
      </div>
    </div>

    <div v-if="realnameModalOpen" class="mask" @click.self="closeRealnameVerify">
      <div class="modal card">
        <div class="m-title">实名认证</div>
        <div class="tabs">
          <button class="tab" :class="{ active: realnameMethod === 'ID_PHOTO' }" type="button" @click="realnameMethod = 'ID_PHOTO'">
            上传身份证
          </button>
          <button class="tab" :class="{ active: realnameMethod === 'NAME_IDNO' }" type="button" @click="realnameMethod = 'NAME_IDNO'">
            姓名+身份证号
          </button>
        </div>

        <div v-if="realnameMethod === 'ID_PHOTO'" class="pane">
          <div class="upload-grid">
            <div class="upload-item">
              <div class="upload-label">身份证人像面</div>
              <img v-if="idFrontUrl" class="proof-img" :src="idFrontUrl" alt="id-front" />
              <div v-else class="proof-img placeholder">未上传</div>
              <input type="file" accept="image/*" :disabled="idFrontUploading || realnameSubmitBusy" @change="onUploadIdFront" />
            </div>
            <div class="upload-item">
              <div class="upload-label">身份证国徽面</div>
              <img v-if="idBackUrl" class="proof-img" :src="idBackUrl" alt="id-back" />
              <div v-else class="proof-img placeholder">未上传</div>
              <input type="file" accept="image/*" :disabled="idBackUploading || realnameSubmitBusy" @change="onUploadIdBack" />
            </div>
          </div>
        </div>

        <div v-else class="pane">
          <div class="field">
            <div class="label">姓名</div>
            <input v-model="teacherRealName" class="input" placeholder="请输入真实姓名" :disabled="realnameSubmitBusy" />
          </div>
          <div class="field">
            <div class="label">身份证号</div>
            <input v-model="realnameIdNo" class="input" placeholder="请输入18位身份证号" :disabled="realnameSubmitBusy" />
          </div>
        </div>

        <div v-if="realnameHint" class="hint error">{{ realnameHint }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="realnameSubmitBusy" @click="closeRealnameVerify">取消</button>
          <button class="btn btn-primary" type="button" :disabled="realnameSubmitBusy" @click="submitRealnameVerify">
            {{ realnameSubmitBusy ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="eduModalOpen" class="mask" @click.self="closeEduVerify">
      <div class="modal card">
        <div class="m-title">学历认证</div>
        <div class="m-desc">请上传学信网截图，确保包含姓名、学历信息与页面特征</div>
        <div class="proof-list">
          <div v-for="(u, idx) in eduProofUrls" :key="u" class="proof-item">
            <img class="proof-img" :src="u" alt="edu-proof" />
            <button class="mini-btn" type="button" :disabled="eduSubmitBusy" @click="removeEduProof(idx)">移除</button>
          </div>
          <div v-if="eduProofUrls.length < 3" class="proof-item">
            <div class="proof-img placeholder">添加截图</div>
            <input type="file" accept="image/*" :disabled="eduUploading || eduSubmitBusy" @change="onAddEduProof" />
          </div>
        </div>
        <div v-if="eduHint" class="hint error">{{ eduHint }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="eduSubmitBusy" @click="closeEduVerify">取消</button>
          <button class="btn btn-primary" type="button" :disabled="eduSubmitBusy" @click="submitEduVerify">
            {{ eduSubmitBusy ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 900;
}

.actions {
  display: flex;
  gap: 10px;
}

.form {
  padding: 16px;
}

.sec {
  display: grid;
  gap: 12px;
}

.sec + .sec {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.sec-title {
  font-weight: 900;
  font-size: 13px;
}

.grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: 8px;
}

.span2 {
  grid-column: span 2;
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.verify {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  color: rgba(0, 0, 0, 0.68);
}

.badge.ok {
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.08);
  color: rgba(0, 190, 189, 1);
}

.badge.pending {
  border-color: rgba(255, 125, 0, 0.35);
  background: rgba(255, 125, 0, 0.08);
  color: rgba(255, 125, 0, 1);
}

.badge.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
  color: rgba(208, 48, 80, 1);
}

.link-btn {
  border: none;
  background: transparent;
  color: var(--primary);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.link-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mini-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
}

.mini-hint.error {
  color: rgba(208, 48, 80, 1);
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-img {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.avatar-img.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  background: rgba(0, 190, 189, 0.08);
}

.avatar-file {
  flex: 1 1 auto;
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.textarea {
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 10px 12px;
  outline: none;
  background: #fff;
  resize: vertical;
}

.input:focus,
.textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.hint {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: #fff;
}

.hint.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
}

.hint.ok {
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.06);
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 50;
}

.modal {
  width: min(640px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.m-title {
  font-weight: 900;
  font-size: 16px;
}

.m-desc {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.tab {
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-weight: 700;
  font-size: 13px;
}

.tab.active {
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.pane {
  display: grid;
  gap: 12px;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.upload-item {
  display: grid;
  gap: 8px;
}

.upload-label {
  font-size: 12px;
  color: var(--muted);
}

.proof-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.proof-item {
  display: grid;
  gap: 8px;
}

.proof-img {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 12px;
  border: 1px solid var(--border);
  object-fit: cover;
  background: rgba(0, 0, 0, 0.04);
  display: grid;
  place-items: center;
  color: rgba(0, 0, 0, 0.55);
  font-size: 12px;
}

.proof-img.placeholder {
  background: rgba(0, 190, 189, 0.06);
  border-color: rgba(0, 190, 189, 0.2);
}

.mini-btn {
  height: 34px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
}

@media (max-width: 860px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: auto;
  }
  .upload-grid {
    grid-template-columns: 1fr;
  }
  .proof-list {
    grid-template-columns: 1fr;
  }
}
</style>
