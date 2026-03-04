<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import AutoTextarea from '@/ui/form/AutoTextarea.vue'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const doneHint = ref<string | null>(null)

const subjectSelected = ref<string[]>([])
const subjectOtherName = ref('')

const gradeCode = ref('')
const studentGender = ref<'' | 'male' | 'female'>('')
const classMode = ref<'online' | 'offline' | 'both'>('both')
const teacherGenderPreference = ref<'male' | 'female' | 'both'>('both')
const availableTime = ref('')

const description = ref('')
const teacherRequirementDetail = ref('')

const frequencyOptions = [1, 2, 3, 4, 5, 6, 7] as const
const frequencyPerWeek = ref<number>(2)

const budgetMode = ref<'single' | 'range'>('single')
const budgetSingle = ref<string | number>('')
const budgetMin = ref<string | number>('')
const budgetMax = ref<string | number>('')

const storedCity = (localStorage.getItem('ai_tutor_city') || '北京').trim()
const city = ref(storedCity && storedCity !== '全国' ? storedCity : '')
const address = ref('')
const cityModalOpen = ref(false)

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

function splitOtherSubjects(raw: string): string[] {
  return raw
    .split(/[，,、;；]/g)
    .map((x) => x.trim())
    .filter(Boolean)
}

const subjectTokens = computed(() => {
  const out: string[] = []
  for (const s of subjectSelected.value) {
    if (!s || s === SUBJECT_OTHER_VALUE) continue
    if (!out.includes(s)) out.push(s)
  }
  if (subjectSelected.value.includes(SUBJECT_OTHER_VALUE)) {
    for (const s of splitOtherSubjects(subjectOtherName.value)) {
      if (!out.includes(s)) out.push(s)
    }
  }
  return out.slice(0, 5)
})

const subjectName = computed(() => {
  if (subjectTokens.value.length === 0) return null
  return subjectTokens.value.join('、')
})

const subjectOther = computed(() => subjectSelected.value.includes(SUBJECT_OTHER_VALUE))

function buildTitle() {
  const g = gradeLabel.value
  const sList = subjectTokens.value
  if (g && sList.length === 1) return `${g}${sList[0]}家教`
  if (g && sList.length > 1) return `${g}${sList[0]}等家教`
  if (g) return `${g}家教需求`
  if (sList.length === 1) return `${sList[0]}家教`
  if (sList.length > 1) return `${sList[0]}等家教`
  return '家教需求'
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

async function onSubmit() {
  doneHint.value = null
  error.value = null
  if (!studentGender.value) {
    error.value = '请选择学员性别'
    return
  }
  if (subjectTokens.value.length === 0) {
    error.value = '请选择教学科目'
    return
  }
  if (subjectOther.value && splitOtherSubjects(subjectOtherName.value).length === 0) {
    error.value = '请输入其他科目'
    return
  }
  if (!gradeCode.value) {
    error.value = '请选择学生年级'
    return
  }
  if (!description.value.trim()) {
    error.value = '请填写学生情况描述'
    return
  }
  if (description.value.trim().length < 10) {
    error.value = '学生情况描述至少10个字'
    return
  }
  if ((classMode.value === 'offline' || classMode.value === 'both') && (!city.value.trim() || !address.value.trim())) {
    error.value = '上门辅导必须填写城市与上课地址'
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
    doneHint.value = '发布成功'
    await router.replace({ name: 'studentMineJobs', query: { highlight: String(id) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    loading.value = false
  }
}

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
  <div class="wrap">
    <div class="head">
      <div class="title">发布需求</div>
      <button class="btn btn-primary" type="button" :disabled="loading" @click="onSubmit">
        {{ loading ? '提交中...' : '发布' }}
      </button>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="doneHint" class="hint ok">{{ doneHint }}</div>

    <div class="card form">
      <div class="section">
        <div class="section-title">请填写学生的基本信息</div>

        <div class="row">
          <label class="field">
            <div class="label"><span class="req">*</span>学员性别</div>
            <select v-model="studentGender" class="input">
              <option value="">请选择</option>
              <option value="male">男</option>
              <option value="female">女</option>
            </select>
          </label>

          <label class="field">
            <div class="label"><span class="req">*</span>学生年级</div>
            <select v-model="gradeCode" class="input">
              <option value="">请选择</option>
              <option v-for="o in gradeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>
        </div>

        <div class="field">
          <div class="label"><span class="req">*</span>教学科目（多选）</div>
          <div class="chips">
            <label v-for="s in SUBJECT_PRESETS" :key="s" class="chip">
              <input v-model="subjectSelected" type="checkbox" :value="s" :disabled="loading" />
              <span>{{ s }}</span>
            </label>
            <label class="chip">
              <input v-model="subjectSelected" type="checkbox" :value="SUBJECT_OTHER_VALUE" :disabled="loading" />
              <span>其他</span>
            </label>
          </div>
          <div v-if="subjectSelected.includes(SUBJECT_OTHER_VALUE)" class="other-box">
            <input v-model="subjectOtherName" class="input" placeholder="输入其他科目，如：编程、围棋" :disabled="loading" />
            <div class="other-hint">多个用逗号分隔</div>
          </div>
        </div>

        <div class="row">
          <label class="field">
            <div class="label"><span class="req">*</span>授课方式</div>
            <select v-model="classMode" class="input">
              <option value="offline">上门辅导</option>
              <option value="online">网络辅导</option>
              <option value="both">均可</option>
            </select>
          </label>
        </div>

        <div class="row" v-if="classMode !== 'online'">
          <label class="field">
            <div class="label"><span class="req">*</span>城市</div>
            <button class="input" type="button" @click="cityModalOpen = true">{{ city || '请选择城市' }}</button>
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
            <input v-model="address" class="input" placeholder="例如：朝阳区望京街道望京花园小区3号楼2单元1203" />
          </label>
        </div>

        <label class="field">
          <div class="label">可上课时间</div>
          <input v-model="availableTime" class="input" placeholder="例如:每周六下午2点到4点，2周一次" />
        </label>

        <label class="field">
          <div class="label"><span class="req">*</span>学生情况描述</div>
          <AutoTextarea
            v-model="description"
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
          <select v-model="teacherGenderPreference" class="input">
            <option value="male">男</option>
            <option value="female">女</option>
            <option value="both">均可</option>
          </select>
        </label>
        <label class="field">
          <div class="label"><span class="req">*</span>每周几次课</div>
          <select v-model.number="frequencyPerWeek" class="input">
            <option v-for="n in frequencyOptions" :key="n" :value="n">{{ n }}</option>
          </select>
        </label>
        <div class="field">
          <div class="label"><span class="req">*</span>预算（每小时）</div>
          <div class="budget-row-container">
            <div class="budget-toggle">
              <div class="toggle-bg" :class="{ right: budgetMode === 'range' }"></div>
              <button class="toggle-item" :class="{ active: budgetMode === 'single' }" type="button" @click="budgetMode = 'single'">
                固定金额
              </button>
              <button class="toggle-item" :class="{ active: budgetMode === 'range' }" type="button" @click="budgetMode = 'range'">
                范围预算
              </button>
            </div>

            <div class="budget-inputs">
              <div v-if="budgetMode === 'single'" class="single-input-wrapper">
                <input
                  v-model="budgetSingle"
                  class="input"
                  type="number"
                  inputmode="decimal"
                  min="0"
                  placeholder="例如：100"
                  :disabled="loading"
                />
                <span class="sep placeholder">-</span>
                <div class="input placeholder"></div>
              </div>
              <div v-else class="range-input-wrapper">
                <input
                  v-model="budgetMin"
                  class="input"
                  type="number"
                  inputmode="decimal"
                  min="0"
                  placeholder="最低"
                  :disabled="loading"
                />
                <span class="sep">-</span>
                <input
                  v-model="budgetMax"
                  class="input"
                  type="number"
                  inputmode="decimal"
                  min="0"
                  placeholder="最高"
                  :disabled="loading"
                />
              </div>
            </div>
          </div>
        </div>
        <label class="field">
          <div class="label"><span class="req">*</span>对教员的详细要求</div>
          <AutoTextarea
            v-model="teacherRequirementDetail"
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

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  cursor: pointer;
  user-select: none;
  background: #fff;
}

.chip:has(input:checked) {
  border-color: var(--primary);
  background: var(--primary-weak);
  color: var(--text);
}

.other-box {
  border: 1px dashed var(--border);
  border-radius: 12px;
  padding: 10px;
  display: grid;
  gap: 6px;
  background: rgba(0, 0, 0, 0.02);
}

.other-hint {
  font-size: 12px;
  color: var(--muted);
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

.budget-row-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.budget-toggle {
  position: relative;
  display: flex;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 999px;
  padding: 4px;
  width: 180px;
  height: 40px;
  flex-shrink: 0;
}

.toggle-bg {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #fff;
  border-radius: 999px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
}

.toggle-bg.right {
  left: 50%;
}

.toggle-item {
  flex: 1;
  position: relative;
  z-index: 1;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  color: rgba(11, 47, 45, 0.55);
  cursor: pointer;
  transition: color 0.3s;
}

.toggle-item.active {
  color: #0b2f2d;
}

.budget-inputs {
  flex: 1;
}

.single-input-wrapper,
.range-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
}

.range-input-wrapper .input,
.single-input-wrapper .input {
  flex: 1;
  min-width: 0;
}

.sep {
  color: rgba(0, 0, 0, 0.3);
  font-weight: 700;
}

.sep.placeholder {
  visibility: hidden;
}

.input.placeholder {
  visibility: hidden;
  border: none;
  background: transparent;
}

@media (max-width: 600px) {
  .budget-row-container {
    flex-direction: column;
    align-items: stretch;
  }

  .budget-toggle {
    width: 100%;
  }
}
</style>
