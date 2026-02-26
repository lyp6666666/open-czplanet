<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import AutoTextarea from '@/ui/form/AutoTextarea.vue'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const doneHint = ref<string | null>(null)

const subjectPreset = ref<string>('')
const subjectOtherName = ref('')

const gradeCode = ref('')
const studentGender = ref<'' | 'male' | 'female'>('')
const classMode = ref<'online' | 'offline' | 'both'>('both')
const teacherGenderPreference = ref<'male' | 'female' | 'both'>('both')
const availableTime = ref('')

const description = ref('')
const teacherRequirementDetail = ref('')

const city = ref('北京')
const address = ref('')

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

async function onSubmit() {
  doneHint.value = null
  error.value = null
  if (!studentGender.value) {
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
  if (!gradeCode.value) {
    error.value = '请选择学生年级'
    return
  }
  if ((classMode.value === 'offline' || classMode.value === 'both') && (!city.value.trim() || !address.value.trim())) {
    error.value = '上门辅导必须填写城市与上课地址'
    return
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
      teacherRequirementDetail: teacherRequirementDetail.value.trim() || undefined,
      classMode: classMode.value,
      city: classMode.value === 'online' ? undefined : city.value.trim() || undefined,
      address: classMode.value === 'online' ? undefined : address.value.trim() || undefined,
      frequencyPerWeek: 2,
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
      city.value = '北京'
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
            <input v-model="city" class="input" placeholder="例如：北京" />
          </label>
          <label class="field">
            <div class="label"><span class="req">*</span>上课地址</div>
            <input v-model="address" class="input" placeholder="例如：朝阳·望京" />
          </label>
        </div>

        <label class="field">
          <div class="label">可上课时间</div>
          <input v-model="availableTime" class="input" placeholder="例如:每周六下午2点到4点，2周一次" />
        </label>

        <label class="field">
          <div class="label">学生情况描述</div>
          <AutoTextarea
            v-model="description"
            class="textarea"
            :rows="4"
            placeholder="请详细说明学员基础、学习状况、性格等便于有针对性地安排合适的教员"
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
          <div class="label">对教员的详细要求</div>
          <AutoTextarea
            v-model="teacherRequirementDetail"
            class="textarea"
            :rows="4"
            placeholder="对教员的学历，教学经验，性格等要求"
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
