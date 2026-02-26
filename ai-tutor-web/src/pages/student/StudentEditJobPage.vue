<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import AutoTextarea from '@/ui/form/AutoTextarea.vue'
import type { StudentJobPosting } from '@/api/types'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const doneHint = ref<string | null>(null)

const form = ref<StudentJobPosting | null>(null)
const cityModalOpen = ref(false)
const cities = computed(() => {
  const base = [form.value?.city || '', localStorage.getItem('ai_tutor_city') || '', '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.map((x) => String(x || '').trim()).filter(Boolean)))
})

const budgetMode = ref<'single' | 'range'>('single')
const budgetSingle = ref<string | number>('')
const budgetMin = ref<string | number>('')
const budgetMax = ref<string | number>('')

const subjectPreset = ref<string>('')
const subjectOtherName = ref('')

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

function resolveStageCodeByGradeCode(raw: string | null | undefined): StudentJobPosting['stageCode'] {
  if (!raw) return null
  return gradeOptions.find((o) => o.value === raw)?.stageCode ?? null
}

const gradeLabel = computed(() => gradeOptions.find((o) => o.value === (form.value?.gradeCode || ''))?.label ?? '')

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

function hydrateSubjectFromDemand(d: StudentJobPosting) {
  const name = (d.subjectName || '').trim()
  const isOther = d.subjectIsOther === 1
  if (!name) {
    subjectPreset.value = ''
    subjectOtherName.value = ''
    return
  }
  if (isOther) {
    subjectPreset.value = SUBJECT_OTHER_VALUE
    subjectOtherName.value = name
    return
  }
  if ((SUBJECT_PRESETS as readonly string[]).includes(name)) {
    subjectPreset.value = name
    subjectOtherName.value = ''
    return
  }
  subjectPreset.value = SUBJECT_OTHER_VALUE
  subjectOtherName.value = name
}

function hydrateBudgetFromDemand(d: StudentJobPosting) {
  const rawMin = d.budgetMin == null ? '' : String(d.budgetMin).trim()
  const rawMax = d.budgetMax == null ? '' : String(d.budgetMax).trim()
  const minNum = rawMin ? Number(rawMin) : NaN
  const maxNum = rawMax ? Number(rawMax) : NaN
  if (Number.isFinite(minNum) && Number.isFinite(maxNum)) {
    if (minNum === maxNum) {
      budgetMode.value = 'single'
      budgetSingle.value = minNum
      budgetMin.value = ''
      budgetMax.value = ''
      return
    }
    budgetMode.value = 'range'
    budgetSingle.value = ''
    budgetMin.value = minNum
    budgetMax.value = maxNum
    return
  }
  budgetMode.value = 'single'
  budgetSingle.value = ''
  budgetMin.value = ''
  budgetMax.value = ''
}

async function load() {
  loading.value = true
  error.value = null
  doneHint.value = null
  try {
    const d = await jobsApi.getDemand(id.value)
    form.value = d
    if (form.value) {
      if (!form.value.classMode) form.value.classMode = 'online'
      if (!form.value.teacherGenderPreference) form.value.teacherGenderPreference = 'both'
      if (!form.value.gradeCode) form.value.gradeCode = ''
      if (!form.value.studentGender) form.value.studentGender = ''
      if (!form.value.availableTime) form.value.availableTime = ''
      if (!form.value.teacherRequirementDetail) form.value.teacherRequirementDetail = ''
      hydrateSubjectFromDemand(form.value)
      hydrateBudgetFromDemand(form.value)
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.value) return
  error.value = null
  doneHint.value = null
  if (!String(form.value.studentGender || '').trim()) {
    error.value = '请选择学员性别'
    return
  }
  if (!subjectPreset.value) {
    error.value = '请选择教学科目'
    return
  }
  if (subjectOther.value && !subjectOtherName.value.trim()) {
    error.value = '请输入其他科目'
    return
  }
  if (!String(form.value.gradeCode || '').trim()) {
    error.value = '请选择学生年级'
    return
  }
  if (!form.value.classMode) {
    error.value = '请选择授课方式'
    return
  }
  const desc = String(form.value.description || '').trim()
  if (!desc) {
    error.value = '请填写学生情况描述'
    return
  }
  if (desc.length < 10) {
    error.value = '学生情况描述至少10个字'
    return
  }
  const reqDetail = String(form.value.teacherRequirementDetail || '').trim()
  if (!reqDetail) {
    error.value = '请填写对教员的详细要求'
    return
  }
  if (reqDetail.length < 10) {
    error.value = '对教员的详细要求至少10个字'
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
  if ((form.value.classMode === 'offline' || form.value.classMode === 'both') && (!String(form.value.city || '').trim() || !String(form.value.address || '').trim())) {
    error.value = '上门辅导必须填写城市与上课地址'
    return
  }
  saving.value = true
  try {
    const stage = resolveStageCodeByGradeCode(form.value.gradeCode)
    await jobsApi.updateDemand(id.value, {
      title: buildTitle(),
      subjectName: subjectName.value as string,
      subjectOther: subjectOther.value,
      description: desc,
      studentGender: form.value.studentGender as string,
      gradeCode: form.value.gradeCode || undefined,
      teacherGenderPreference: form.value.teacherGenderPreference || undefined,
      availableTime: String(form.value.availableTime || '').trim() || undefined,
      teacherRequirementDetail: reqDetail,
      classMode: form.value.classMode || undefined,
      city: form.value.classMode === 'online' ? undefined : form.value.city || undefined,
      address: form.value.classMode === 'online' ? undefined : form.value.address || undefined,
      budgetMin: budgetMinNum,
      budgetMax: budgetMaxNum,
      stageCode: stage || undefined,
      status: form.value.status ?? undefined,
    })
    doneHint.value = '已保存'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function onSelectCity(v: string) {
  if (!form.value) return
  if (form.value.classMode === 'offline' || form.value.classMode === 'both') {
    if (String(v || '').trim() === '全国') {
      error.value = '上门辅导请选择具体城市'
      return
    }
  }
  const raw = String(v || '').trim()
  if (raw) localStorage.setItem('ai_tutor_city', raw)
  form.value.city = raw
}

onMounted(() => {
  void load()
})

watch(
  () => form.value?.classMode,
  (v) => {
    if (!form.value) return
    if (v === 'online') {
      form.value.city = null
      form.value.address = null
      return
    }
    if (!String(form.value.city || '').trim() || String(form.value.city || '').trim() === '全国') {
      const stored = String(localStorage.getItem('ai_tutor_city') || '').trim()
      form.value.city = stored && stored !== '全国' ? stored : form.value.city
    }
  },
)
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">编辑需求</div>
      <div class="actions">
        <button class="btn" type="button" @click="router.back()">返回</button>
        <button class="btn btn-primary" type="button" :disabled="saving || loading" @click="onSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="doneHint" class="hint ok">{{ doneHint }}</div>

    <div v-if="form" class="card form">
      <div class="section">
        <div class="section-title">请填写学生的基本信息</div>

        <div class="row">
          <label class="field">
            <div class="label"><span class="req">*</span>学员性别</div>
            <select v-model="form.studentGender" class="input">
              <option value="">请选择</option>
              <option value="male">男</option>
              <option value="female">女</option>
            </select>
          </label>

          <label class="field">
            <div class="label"><span class="req">*</span>学生年级</div>
            <select v-model="form.gradeCode" class="input">
              <option value="">请选择</option>
              <option v-for="o in gradeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>
        </div>

        <label class="field">
          <div class="label"><span class="req">*</span>教学科目</div>
          <select v-model="subjectPreset" class="input">
            <option value="">请选择</option>
            <option v-for="s in SUBJECT_PRESETS" :key="s" :value="s">{{ s }}</option>
            <option :value="SUBJECT_OTHER_VALUE">其他</option>
          </select>
          <input
            v-if="subjectPreset === SUBJECT_OTHER_VALUE"
            v-model="subjectOtherName"
            class="input"
            placeholder="请输入科目"
          />
        </label>

        <div class="row">
          <label class="field">
            <div class="label"><span class="req">*</span>授课方式</div>
            <select v-model="form.classMode" class="input">
              <option value="offline">上门辅导</option>
              <option value="online">网络辅导</option>
              <option value="both">均可</option>
            </select>
          </label>
        </div>

        <div class="row" v-if="form.classMode !== 'online'">
          <label class="field">
            <div class="label"><span class="req">*</span>城市</div>
            <button class="input" type="button" @click="cityModalOpen = true">{{ String(form.city || '').trim() || '请选择城市' }}</button>
            <CitySelectModal
              :open="cityModalOpen"
              :model-value="String(form.city || '').trim()"
              :hot-cities="cities"
              :allow-national="false"
              @update:model-value="onSelectCity"
              @close="cityModalOpen = false"
            />
          </label>
          <label class="field">
            <div class="label"><span class="req">*</span>上课地址</div>
            <input v-model="form.address" class="input" placeholder="例如：朝阳区望京街道望京花园小区3号楼2单元1203" />
          </label>
        </div>

        <label class="field">
          <div class="label">可上课时间</div>
          <input v-model="form.availableTime" class="input" placeholder="例如:每周六下午2点到4点，2周一次" />
        </label>

        <label class="field">
          <div class="label"><span class="req">*</span>学生情况描述</div>
          <AutoTextarea
            v-model="form.description"
            class="textarea"
            :rows="4"
            placeholder="请至少填写10个字，例如：孩子基础、学习情况、性格等"
          />
        </label>
      </div>

      <div class="section">
        <div class="section-title">请填写您对教师的要求</div>
        <label class="field">
          <div class="label"><span class="req">*</span>教师性别</div>
          <select v-model="form.teacherGenderPreference" class="input">
            <option value="male">男</option>
            <option value="female">女</option>
            <option value="both">均可</option>
          </select>
        </label>
        <div class="field">
          <div class="label"><span class="req">*</span>预算（每小时）</div>
          <select v-model="budgetMode" class="input">
            <option value="single">填写一个数</option>
            <option value="range">填写上下限</option>
          </select>
          <div class="row" v-if="budgetMode === 'single'">
            <div class="field">
              <div class="label">预算</div>
              <input v-model="budgetSingle" class="input" type="number" inputmode="decimal" min="0" placeholder="例如：100" />
            </div>
          </div>
          <div class="row" v-else>
            <div class="field">
              <div class="label">预算下限</div>
              <input v-model="budgetMin" class="input" type="number" inputmode="decimal" min="0" placeholder="例如：80" />
            </div>
            <div class="field">
              <div class="label">预算上限</div>
              <input v-model="budgetMax" class="input" type="number" inputmode="decimal" min="0" placeholder="例如：120" />
            </div>
          </div>
        </div>
        <label class="field">
          <div class="label"><span class="req">*</span>对教员的详细要求</div>
          <AutoTextarea
            v-model="form.teacherRequirementDetail"
            class="textarea"
            :rows="4"
            placeholder="请至少填写10个字，例如：学历、经验、性格等"
          />
        </label>
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
  display: grid;
  gap: 12px;
}

.section {
  display: grid;
  gap: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 900;
  color: var(--text);
}

.row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.req {
  color: #ff3b30;
  margin-right: 4px;
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

@media (max-width: 860px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
