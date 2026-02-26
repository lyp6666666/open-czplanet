<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { assetsApi } from '@/api/assets'
import { jobsApi } from '@/api/jobs'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'
import AutoTextarea from '@/ui/form/AutoTextarea.vue'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const STORAGE_STUDENT_FIRST_DEMAND_COMPLETED_KEY_PREFIX = 'ai_tutor_student_first_demand_completed:'

function buildStudentFirstDemandCompletedKey(uid: number) {
  return `${STORAGE_STUDENT_FIRST_DEMAND_COMPLETED_KEY_PREFIX}${uid}`
}

const router = useRouter()
const auth = useAuthStore()

const step = ref<1 | 2 | 3>(1)
const loading = ref(false)
const error = ref<string | null>(null)
const uid = ref<number | null>(null)

const avatar = ref('')
const avatarUploading = ref(false)
const name = ref('')

const subjectPreset = ref<string>('')
const subjectOtherName = ref('')

const gradeCode = ref('')
const studentGender = ref<'' | 'male' | 'female'>('')
const classMode = ref<'online' | 'offline' | 'both'>('both')
const teacherGenderPreference = ref<'male' | 'female' | 'both'>('both')
const availableTime = ref('')
const description = ref('')
const storedCity = (localStorage.getItem('ai_tutor_city') || '北京').trim()
const city = ref(storedCity && storedCity !== '全国' ? storedCity : '')
const address = ref('')
const cityModalOpen = ref(false)

const teacherRequirementDetail = ref('')

const frequencyOptions = [1, 2, 3, 4, 5, 6, 7] as const
const frequencyPerWeek = ref<number>(2)

const budgetMode = ref<'single' | 'range'>('single')
const budgetSingle = ref<string | number>('')
const budgetMin = ref<string | number>('')
const budgetMax = ref<string | number>('')

watch(city, (v) => {
  const raw = String(v || '').trim()
  if (!raw) return
  localStorage.setItem('ai_tutor_city', raw)
})

const cities = computed(() => {
  const base = [city.value, localStorage.getItem('ai_tutor_city') || '', '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.map((x) => String(x || '').trim()).filter(Boolean)))
})

const gradeOptions: Array<{ value: string; label: string; stageCode: 'PRESCHOOL' | 'PRIMARY' | 'JUNIOR' | 'SENIOR' | 'OTHER' }> = [
  { value: 'PRESCHOOL', label: '幼儿', stageCode: 'PRESCHOOL' },
  { value: 'GRADE1', label: '一年级', stageCode: 'PRIMARY' },
  { value: 'GRADE2', label: '二年级', stageCode: 'PRIMARY' },
  { value: 'GRADE3', label: '三年级', stageCode: 'PRIMARY' },
  { value: 'GRADE4', label: '四年级', stageCode: 'PRIMARY' },
  { value: 'GRADE5', label: '五年级', stageCode: 'PRIMARY' },
  { value: 'GRADE6', label: '六年级', stageCode: 'PRIMARY' },
  { value: 'JUNIOR1', label: '初一', stageCode: 'JUNIOR' },
  { value: 'JUNIOR2', label: '初二', stageCode: 'JUNIOR' },
  { value: 'JUNIOR3', label: '初三', stageCode: 'JUNIOR' },
  { value: 'SENIOR1', label: '高一', stageCode: 'SENIOR' },
  { value: 'SENIOR2', label: '高二', stageCode: 'SENIOR' },
  { value: 'SENIOR3', label: '高三', stageCode: 'SENIOR' },
  { value: 'SELF_EXAM', label: '自考生', stageCode: 'OTHER' },
  { value: 'COLLEGE1', label: '大一', stageCode: 'OTHER' },
  { value: 'COLLEGE2', label: '大二', stageCode: 'OTHER' },
  { value: 'COLLEGE3', label: '大三', stageCode: 'OTHER' },
  { value: 'COLLEGE4', label: '大四', stageCode: 'OTHER' },
  { value: 'ADULT', label: '成人', stageCode: 'OTHER' },
]

const stageCode = computed(() => gradeOptions.find((o) => o.value === gradeCode.value)?.stageCode ?? null)
const gradeLabel = computed(() => gradeOptions.find((o) => o.value === gradeCode.value)?.label ?? '')

const subjectName = computed(() => {
  if (!subjectPreset.value) return null
  if (subjectPreset.value === SUBJECT_OTHER_VALUE) return subjectOtherName.value.trim() || null
  return subjectPreset.value
})

const subjectOther = computed(() => subjectPreset.value === SUBJECT_OTHER_VALUE)

function buildTitle() {
  const g = gradeLabel.value
  const s = subjectName.value
  if (g && s) return `${g}${s}家教`
  if (g) return `${g}家教需求`
  if (s) return `${s}家教`
  return '家教需求'
}

const canNextStep1 = computed(() => !!(name.value.trim() && avatar.value.trim()))

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

async function nextStep1() {
  if (!canNextStep1.value) return
  loading.value = true
  error.value = null
  try {
    await userApi.updateUserInfo({ baseUserInfo: { name: name.value.trim(), avatar: avatar.value.trim() } })
    await auth.refreshMe()
    step.value = 2
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    loading.value = false
  }
}

function validateStudentDemandBasics(): string | null {
  if (!studentGender.value) return '请选择学员性别'
  if (!subjectPreset.value) return '请选择教学科目'
  if (subjectOther.value && !subjectOtherName.value.trim()) return '请输入其他科目'
  if (!gradeCode.value) return '请选择学生年级'
  if (!description.value.trim()) return '请填写学生情况描述'
  if (description.value.trim().length < 10) return '学生情况描述至少10个字'
  if ((classMode.value === 'offline' || classMode.value === 'both') && (!city.value.trim() || !address.value.trim())) return '上门辅导必须填写城市与上课地址'
  return null
}

function nextStep2() {
  error.value = null
  const err = validateStudentDemandBasics()
  if (err) {
    error.value = err
    return
  }
  step.value = 3
}

function onSelectCity(v: string) {
  if (classMode.value === 'offline' || classMode.value === 'both') {
    if (String(v || '').trim() === '全国') {
      error.value = '上门辅导请选择具体城市'
      return
    }
  }
  city.value = String(v || '').trim()
}

async function skipDemand() {
  if (!uid.value) return
  localStorage.setItem(buildStudentFirstDemandCompletedKey(uid.value), '1')
  await router.replace('/')
}

async function submitDemand() {
  error.value = null
  const err = validateStudentDemandBasics()
  if (err) {
    error.value = err
    return
  }
  if (!teacherRequirementDetail.value.trim()) {
    error.value = '请填写对教员的详细要求'
    return
  }
  if (teacherRequirementDetail.value.trim().length < 10) {
    error.value = '对教员的详细要求至少10个字'
    return
  }
  if (!Number.isFinite(frequencyPerWeek.value) || frequencyPerWeek.value < 1 || frequencyPerWeek.value > 7) {
    error.value = '每周上课次数需在 1~7 之间'
    return
  }
  let budgetMinNum: number
  let budgetMaxNum: number
  if (budgetMode.value === 'single') {
    const raw = String(budgetSingle.value ?? '').trim()
    if (!raw) {
      error.value = '请填写预算'
      return
    }
    const v = Number(raw)
    if (!Number.isFinite(v) || v <= 0) {
      error.value = '预算需为大于 0 的数字'
      return
    }
    budgetMinNum = v
    budgetMaxNum = v
  } else {
    const rawMin = String(budgetMin.value ?? '').trim()
    const rawMax = String(budgetMax.value ?? '').trim()
    if (!rawMin || !rawMax) {
      error.value = '请填写预算上下限'
      return
    }
    const vMin = Number(rawMin)
    const vMax = Number(rawMax)
    if (!Number.isFinite(vMin) || vMin <= 0) {
      error.value = '预算下限需为大于 0 的数字'
      return
    }
    if (!Number.isFinite(vMax) || vMax <= 0) {
      error.value = '预算上限需为大于 0 的数字'
      return
    }
    if (vMin > vMax) {
      error.value = '预算下限不能大于预算上限'
      return
    }
    budgetMinNum = vMin
    budgetMaxNum = vMax
  }
  loading.value = true
  try {
    const id = await jobsApi.createDemand({
      title: buildTitle(),
      subjectName: subjectName.value as string,
      subjectOther: subjectOther.value,
      description: description.value.trim(),
      studentGender: studentGender.value,
      gradeCode: gradeCode.value,
      teacherGenderPreference: teacherGenderPreference.value,
      availableTime: availableTime.value.trim() || undefined,
      teacherRequirementDetail: teacherRequirementDetail.value.trim(),
      classMode: classMode.value,
      city: classMode.value === 'online' ? undefined : city.value.trim() || undefined,
      address: classMode.value === 'online' ? undefined : address.value.trim() || undefined,
      frequencyPerWeek: frequencyPerWeek.value,
      budgetMin: budgetMinNum,
      budgetMax: budgetMaxNum,
      stageCode: stageCode.value as string,
      educationRequirement: 'UNLIMITED',
      publisherIdentity: 'PARENT',
    })
    if (uid.value) {
      localStorage.setItem(buildStudentFirstDemandCompletedKey(uid.value), '1')
    }
    await router.replace({ name: 'studentMineJobs', query: { highlight: String(id) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    await router.replace('/auth/student')
    return
  }
  const me = await auth.refreshMe()
  if (me?.userType !== 2) {
    await router.replace('/tutor/jobs')
    return
  }
  uid.value = me.id
  if (localStorage.getItem(buildStudentFirstDemandCompletedKey(me.id)) === '1') {
    await router.replace('/student/post')
    return
  }
  if (me?.avatar) avatar.value = me.avatar
  if (me?.name) name.value = me.name
})

watch(
  () => classMode.value,
  (v) => {
    if (v === 'online') {
      address.value = ''
    }
  },
)
</script>

<template>
  <div class="page">
    <div class="shell">
      <div class="card board">
        <aside class="left">
          <div class="brand">
            <div class="logo">Hi，欢迎来找家教</div>
            <div class="slogan">为您更快匹配老师，仅需3步</div>
          </div>
          <div class="illustration" />
        </aside>

        <section class="right">
          <div class="right-head">
            <div class="r-title">
              <span v-if="step === 1">创建学生名片</span>
              <span v-else-if="step === 2">发布您的第一个需求</span>
              <span v-else>填写您对教师的要求</span>
            </div>
            <div class="r-desc">第 {{ step }}/3 步</div>
          </div>

          <div v-if="error" class="hint error">{{ error }}</div>

          <div v-if="step === 1" class="form">
            <div class="field">
              <div class="label">头像</div>
              <div class="avatar-row">
                <img v-if="avatar" class="avatar-img" :src="avatar" alt="avatar" />
                <div v-else class="avatar-img fallback">S</div>
                <input class="avatar-file" type="file" accept="image/*" :disabled="avatarUploading || loading" @change="onSelectAvatarFile" />
              </div>
            </div>

            <label class="field">
              <div class="label">姓名</div>
              <input v-model="name" class="input" placeholder="请输入姓名" :disabled="loading" />
            </label>

            <div class="actions">
              <button class="btn btn-primary" type="button" :disabled="loading || avatarUploading || !canNextStep1" @click="nextStep1">
                下一步
              </button>
            </div>
          </div>

          <div v-else-if="step === 2" class="form">
            <div class="section">
              <div class="section-title">请填写学生的基本信息</div>

              <div class="row">
                <label class="field">
                  <div class="label"><span class="req">*</span>学员性别</div>
                  <select v-model="studentGender" class="input" :disabled="loading">
                    <option value="">请选择</option>
                    <option value="male">男</option>
                    <option value="female">女</option>
                  </select>
                </label>

                <label class="field">
                  <div class="label"><span class="req">*</span>学生年级</div>
                  <select v-model="gradeCode" class="input" :disabled="loading">
                    <option value="">请选择</option>
                    <option v-for="o in gradeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
                  </select>
                </label>
              </div>

              <label class="field">
                <div class="label"><span class="req">*</span>教学科目</div>
                <select v-model="subjectPreset" class="input" :disabled="loading">
                  <option value="">请选择</option>
                  <option v-for="s in SUBJECT_PRESETS" :key="s" :value="s">{{ s }}</option>
                  <option :value="SUBJECT_OTHER_VALUE">其他</option>
                </select>
                <input
                  v-if="subjectPreset === SUBJECT_OTHER_VALUE"
                  v-model="subjectOtherName"
                  class="input"
                  placeholder="请输入科目"
                  :disabled="loading"
                />
              </label>

              <label class="field">
                <div class="label"><span class="req">*</span>授课方式</div>
                <select v-model="classMode" class="input" :disabled="loading">
                  <option value="offline">上门辅导</option>
                  <option value="online">网络辅导</option>
                  <option value="both">均可</option>
                </select>
              </label>

              <div class="row" v-if="classMode !== 'online'">
                <label class="field">
                  <div class="label"><span class="req">*</span>城市</div>
                  <button class="input" type="button" :disabled="loading" @click="cityModalOpen = true">{{ city || '请选择城市' }}</button>
                  <CitySelectModal
                    :open="cityModalOpen"
                    :model-value="city"
                    :hot-cities="cities"
                    :allow-national="false"
                    @update:model-value="onSelectCity"
                    @close="cityModalOpen = false"
                  />
                </label>
                <label class="field">
                  <div class="label"><span class="req">*</span>上课地址</div>
                  <input v-model="address" class="input" placeholder="例如：朝阳区望京街道望京花园小区3号楼2单元1203" :disabled="loading" />
                </label>
              </div>

              <label class="field">
                <div class="label">可上课时间</div>
                <input v-model="availableTime" class="input" placeholder="例如:每周六下午2点到4点，2周一次" :disabled="loading" />
              </label>

              <label class="field">
                <div class="label"><span class="req">*</span>学生情况描述</div>
                <AutoTextarea
                  v-model="description"
                  class="textarea"
                  :rows="4"
                  placeholder="请至少填写10个字，例如：孩子基础、学习情况、性格等"
                  :disabled="loading"
                />
              </label>
            </div>

            <div class="actions split">
              <button class="btn" type="button" :disabled="loading" @click="skipDemand">跳过</button>
              <div class="right-actions">
                <button class="btn" type="button" :disabled="loading" @click="step = 1">上一步</button>
                <button class="btn btn-primary" type="button" :disabled="loading" @click="nextStep2">下一步</button>
              </div>
            </div>
          </div>

          <div v-else class="form">
            <div class="section">
              <div class="section-title">请填写您对教师的要求</div>
              <label class="field">
                <div class="label"><span class="req">*</span>教师性别</div>
                <select v-model="teacherGenderPreference" class="input" :disabled="loading">
                  <option value="male">男</option>
                  <option value="female">女</option>
                  <option value="both">均可</option>
                </select>
              </label>
              <label class="field">
                <div class="label"><span class="req">*</span>对教员的详细要求</div>
                <AutoTextarea
                  v-model="teacherRequirementDetail"
                  class="textarea"
                  :rows="5"
                  placeholder="请至少填写10个字，例如：学历、经验、性格等"
                  :disabled="loading"
                />
              </label>
              <label class="field">
                <div class="label"><span class="req">*</span>每周几次课</div>
                <select v-model.number="frequencyPerWeek" class="input" :disabled="loading">
                  <option v-for="n in frequencyOptions" :key="n" :value="n">{{ n }}</option>
                </select>
              </label>
              <div class="field">
                <div class="label"><span class="req">*</span>预算（每小时）</div>
                <select v-model="budgetMode" class="input" :disabled="loading">
                  <option value="single">填写一个数</option>
                  <option value="range">填写上下限</option>
                </select>
                <div class="row" v-if="budgetMode === 'single'">
                  <div class="field">
                    <div class="label">预算</div>
                    <input v-model="budgetSingle" class="input" type="number" inputmode="decimal" min="0" placeholder="例如：100" :disabled="loading" />
                  </div>
                </div>
                <div class="row" v-else>
                  <div class="field">
                    <div class="label">预算下限</div>
                    <input v-model="budgetMin" class="input" type="number" inputmode="decimal" min="0" placeholder="例如：80" :disabled="loading" />
                  </div>
                  <div class="field">
                    <div class="label">预算上限</div>
                    <input v-model="budgetMax" class="input" type="number" inputmode="decimal" min="0" placeholder="例如：120" :disabled="loading" />
                  </div>
                </div>
              </div>
            </div>

            <div class="actions split">
              <button class="btn" type="button" :disabled="loading" @click="step = 2">上一步</button>
              <button class="btn btn-primary" type="button" :disabled="loading" @click="submitDemand">发布需求</button>
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

.req {
  color: #ff3b30;
  margin-right: 4px;
}

.input {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.textarea {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  resize: vertical;
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
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.avatar-img.fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: rgba(11, 47, 45, 0.7);
  background: rgba(18, 180, 171, 0.12);
}

.avatar-file {
  flex: 1;
}

.section {
  display: grid;
  gap: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 900;
  color: #0b2f2d;
}

.row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.actions.split {
  justify-content: space-between;
}

.right-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 860px) {
  .card.board {
    grid-template-columns: 1fr;
  }
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
