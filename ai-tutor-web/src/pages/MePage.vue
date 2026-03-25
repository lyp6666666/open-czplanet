<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { assetsApi } from '@/api/assets'
import { userApi } from '@/api/user'
import { teacherVerificationApi } from '@/api/verification'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'
import AutoTextarea from '@/ui/form/AutoTextarea.vue'

const auth = useAuthStore()
const router = useRouter()
const toast = useToastStore()

const loading = ref(false)
const savedHint = ref<string | null>(null)

const isTeacher = computed(() => auth.user?.userType === 1)

const name = ref('')
const sex = ref<number | null>(null)
const avatar = ref('')
const avatarUploading = ref(false)
const avatarHint = ref<string | null>(null)
const avatarLoaded = ref(false)
let avatarProbeId = 0

const avatarSrc = computed(() => {
  const v = String(avatar.value || '').trim()
  if (!v) return ''
  const low = v.toLowerCase()
  if (low === 'null' || low === 'undefined') return ''
  return v
})

watch(
  avatarSrc,
  (src) => {
    avatarLoaded.value = false
    if (!src) return
    const probeId = (avatarProbeId += 1)
    const img = new Image()
    img.onload = () => {
      if (probeId !== avatarProbeId) return
      avatarLoaded.value = true
    }
    img.onerror = () => {
      if (probeId !== avatarProbeId) return
      avatarLoaded.value = false
    }
    img.src = src
  },
  { immediate: true },
)

const teacherRealName = ref('')
const teacherEducation = ref('')
const teacherSubject = ref('')
const teacherExperienceYears = ref<number | null>(null)
const teacherRatePerHour = ref<number | null>(null)
const teacherIntroduction = ref('')
const teacherCity = ref('')
const teacherHighestEduSchool = ref('')
const teacherTeachingMode = ref<string>('')
const teacherCityModalOpen = ref(false)

const teacherAllowNational = computed(() => {
  const v = String(teacherTeachingMode.value || '').trim().toUpperCase()
  if (!v) return true
  return v === 'ONLINE'
})

const teacherCityHotCities = computed(() => {
  const base = [teacherCity.value, localStorage.getItem('ai_tutor_city') || '', '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.map((x) => String(x || '').trim()).filter(Boolean)))
})

function onSelectTeacherCity(v: string) {
  const raw = String(v || '').trim()
  const mode = String(teacherTeachingMode.value || '').trim().toUpperCase()
  if ((mode === 'OFFLINE' || mode === 'BOTH') && raw === '全国') {
    toast.show('线下授课请选择具体城市', 'error')
    return
  }
  if (raw) localStorage.setItem('ai_tutor_city', raw)
  teacherCity.value = raw
}

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

const studentProfileMissing = computed(() => {
  if (isTeacher.value) return []
  const missing: string[] = []
  if (!avatarSrc.value) missing.push('头像')
  if (!studentRealName.value.trim()) missing.push('姓名')
  if (!studentDemandDescription.value.trim()) missing.push('孩子描述')
  return missing
})

const studentProfileNudgeText = computed(() => {
  if (isTeacher.value) return ''
  if (studentProfileMissing.value.length === 0) return '你的主页资料已较完善，老师更愿意通过申请并主动匹配你。'
  return `这些信息会在老师查看你的主页时展示，建议完善：${studentProfileMissing.value.join('、')}。完善后更容易通过申请并获得老师主动匹配。`
})

async function load() {
  if (!auth.isLoggedIn) return
  loading.value = true
  savedHint.value = null
  avatarHint.value = null
  try {
    const me = await auth.refreshMe()
    name.value = me?.name || auth.user?.name || ''
    sex.value = me?.sex ?? auth.user?.sex ?? null
    avatar.value = me?.avatar || auth.user?.avatar || ''

    if (me?.teacherProfile) {
      teacherRealName.value = me.teacherProfile.realName || ''
      teacherEducation.value = me.teacherProfile.education || ''
      teacherSubject.value = me.teacherProfile.subject || ''
      teacherExperienceYears.value = me.teacherProfile.experienceYears ?? null
      teacherRatePerHour.value = me.teacherProfile.ratePerHour != null ? Number(me.teacherProfile.ratePerHour) : null
      teacherIntroduction.value = me.teacherProfile.introduction || ''
      teacherCity.value = me.teacherProfile.city || ''
      teacherHighestEduSchool.value = me.teacherProfile.highestEduSchool || ''
      teacherTeachingMode.value = me.teacherProfile.teachingMode || ''
      realnameVerifyStatus.value = me.teacherProfile.realnameVerifyStatus ?? 0
      realnameVerifyRejectReason.value = me.teacherProfile.realnameVerifyRejectReason || ''
      realnameIdnoMasked.value = me.teacherProfile.realnameVerifyIdnoMasked || ''
      eduVerifyStatus.value = me.teacherProfile.eduVerifyStatus ?? 0
      eduVerifyRejectReason.value = me.teacherProfile.eduVerifyRejectReason || ''
    }

    if (me?.studentProfile) {
      studentRealName.value = me.studentProfile.realName || ''
      studentChildAge.value = me.studentProfile.childAge ?? null
      studentAddress.value = me.studentProfile.address || ''
      studentDemandDescription.value = me.studentProfile.demandDescription || ''
      studentBudget.value = me.studentProfile.budget != null ? Number(me.studentProfile.budget) : null
    }
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '加载失败', 'error')
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
    toast.show('请选择图片文件', 'error')
    return
  }
  if (f.size > 5 * 1024 * 1024) {
    toast.show('头像文件不能超过 5MB', 'error')
    return
  }
  avatarUploading.value = true
  try {
    const r = await assetsApi.uploadImage(f, 'avatar')
    avatar.value = r.url
    avatarHint.value = '头像已上传，点击保存生效'
  } catch (e2) {
    toast.show(e2 instanceof Error ? e2.message : '头像上传失败', 'error')
  } finally {
    avatarUploading.value = false
    if (input) input.value = ''
  }
}

async function onSave() {
  savedHint.value = null
  try {
    if (!isTeacher.value) {
      if (studentChildAge.value !== null && !Number.isFinite(studentChildAge.value)) {
        toast.show('年龄必须是数字', 'error')
        return
      }
    }

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
            city: teacherCity.value.trim() || undefined,
            highestEduSchool: teacherHighestEduSchool.value.trim() || undefined,
            teachingMode: teacherTeachingMode.value.trim() || undefined,
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
    toast.show(e instanceof Error ? e.message : '保存失败', 'error')
  }
}

const eduStatusText = computed(() => {
  if (eduVerifyStatus.value === 2) return '已完成学历认证'
  if (eduVerifyStatus.value === 1) return '审核中'
  if (eduVerifyStatus.value === 3) return '未通过'
  return '未认证'
})

const realnameStatusText = computed(() => {
  if (realnameVerifyStatus.value === 2) return '已通过实名认证'
  if (realnameVerifyStatus.value === 1) return '审核中'
  if (realnameVerifyStatus.value === 3) return '未通过'
  return '未认证'
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
    toast.show('请选择图片文件', 'error')
    return
  }
  if (f.size > 20 * 1024 * 1024) {
    toast.show('图片文件不能超过 20MB', 'error')
    return
  }
  setUploading(true)
  try {
    const r = await assetsApi.uploadImage(f, 'other')
    setUrl(r.url)
  } catch (e2) {
    toast.show(e2 instanceof Error ? e2.message : '上传失败', 'error')
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
  if (f.size > 20 * 1024 * 1024) {
    eduHint.value = '图片文件不能超过 20MB'
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

function openSettings() {
  void router.push({ name: 'settings' })
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
        <button class="btn" type="button" @click="openSettings">设置</button>
        <button class="btn" type="button" @click="onLogout">退出登录</button>
        <button class="btn btn-primary" type="button" :disabled="loading" @click="onSave">保存</button>
      </div>
    </div>

    <div v-if="savedHint" class="hint ok">{{ savedHint }}</div>
    <div v-else-if="avatarHint" class="hint ok">{{ avatarHint }}</div>

    <div class="card form">
      <div class="sec">
        <div class="sec-title">基础信息</div>
        <div class="grid">
          <label class="field span2">
            <div class="label">头像</div>
            <div class="avatar-row">
              <img v-if="avatarSrc && avatarLoaded" class="avatar-img" :src="avatarSrc" alt="avatar" />
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

      <div class="sec" v-if="isTeacher">
        <div class="sec-title">认证中心</div>
        <div class="verify-cards">
          <!-- Realname Verification -->
          <div class="verify-card" :class="{ done: realnameVerifyStatus === 2 }">
            <div class="vc-icon">🆔</div>
            <div class="vc-info">
              <div class="vc-title">实名认证</div>
              <div class="vc-desc">{{ realnameStatusText }}</div>
            </div>
            <button class="btn sm" :disabled="realnameVerifyStatus === 1 || realnameVerifyStatus === 2" @click="realnameModalOpen = true">
              {{ realnameVerifyStatus === 2 ? '已认证' : (realnameVerifyStatus === 3 ? '重新认证' : '去认证') }}
            </button>
          </div>

          <!-- Education Verification -->
          <div class="verify-card" :class="{ done: eduVerifyStatus === 2 }">
            <div class="vc-icon">🎓</div>
            <div class="vc-info">
              <div class="vc-title">学历认证 (学信网)</div>
              <div class="vc-desc">{{ eduStatusText }}</div>
            </div>
            <button class="btn sm" :disabled="eduVerifyStatus === 1 || eduVerifyStatus === 2" @click="openEduVerify">
              {{ eduVerifyStatus === 2 ? '已认证' : (eduVerifyStatus === 3 ? '重新认证' : '去认证') }}
            </button>
          </div>
        </div>
      </div>

      <div class="sec">
        <div class="sec-title">{{ isTeacher ? '教师资料' : '学生资料' }}</div>
        <div v-if="!isTeacher" class="hint">{{ studentProfileNudgeText }}</div>
        <div class="grid" v-if="isTeacher">
          <label class="field">
            <div class="label-row">
              <div class="label">学历</div>
            </div>
            <input v-model="teacherEducation" class="input" placeholder="例如：本科" />
            <div v-if="eduVerifyStatus === 3 && eduVerifyRejectReason" class="mini-hint error">驳回原因：{{ eduVerifyRejectReason }}</div>
          </label>
          <label class="field">
            <div class="label">所在城市</div>
            <button class="input" type="button" :disabled="loading" @click="teacherCityModalOpen = true">
              {{ teacherCity.trim() || '请选择城市' }}
            </button>
            <CitySelectModal
              :open="teacherCityModalOpen"
              :model-value="teacherCity"
              :hot-cities="teacherCityHotCities"
              :allow-national="teacherAllowNational"
              @update:model-value="onSelectTeacherCity"
              @close="teacherCityModalOpen = false"
            />
          </label>
          <label class="field">
            <div class="label">最高学历学校</div>
            <input v-model="teacherHighestEduSchool" class="input" placeholder="例如：北京大学" />
          </label>
          <label class="field">
            <div class="label">教授科目</div>
            <input v-model="teacherSubject" class="input" placeholder="例如：数学/英语" />
          </label>
          <label class="field">
            <div class="label">教学方式</div>
            <select v-model="teacherTeachingMode" class="input">
              <option value="">不设置</option>
              <option value="ONLINE">线上教学</option>
              <option value="OFFLINE">线下教学</option>
              <option value="BOTH">均可</option>
            </select>
          </label>
          <label class="field">
            <div class="label">教学经验（年）</div>
            <input v-model.number="teacherExperienceYears" class="input" inputmode="numeric" placeholder="例如：3" />
          </label>
          <label class="field span2">
            <div class="label">简介</div>
            <AutoTextarea v-model="teacherIntroduction" class="textarea" :rows="6" placeholder="写点你的优势与授课风格" />
          </label>
        </div>

        <div class="grid" v-else>
          <label class="field">
            <div class="label">姓名</div>
            <input v-model="studentRealName" class="input" placeholder="例如：王女士" />
          </label>
          <label class="field">
            <div class="label">年龄</div>
            <input v-model.number="studentChildAge" class="input" inputmode="numeric" placeholder="例如：9" />
          </label>
          <label class="field span2">
            <div class="label">地址</div>
            <input v-model="studentAddress" class="input" placeholder="例如：朝阳区望京街道望京花园小区3号楼2单元1203" />
          </label>
          <label class="field span2">
            <div class="label">孩子描述</div>
            <AutoTextarea v-model="studentDemandDescription" class="textarea" :rows="4" placeholder="例如：希望提高应用题，孩子基础一般" />
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

.verify-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.verify-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
}

.verify-card.done {
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.04);
}

.vc-icon {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.04);
  display: grid;
  place-items: center;
  font-size: 20px;
}

.verify-card.done .vc-icon {
  background: rgba(0, 190, 189, 0.15);
}

.vc-info {
  flex: 1;
  display: grid;
  gap: 4px;
}

.vc-title {
  font-weight: 800;
  font-size: 14px;
}

.vc-desc {
  font-size: 12px;
  color: var(--muted);
}

.btn.sm {
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
}
</style>
