<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import { homeGuestApi } from '@/api/homeGuest'
import type { SubjectTreeNode } from '@/api/types'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const doneHint = ref<string | null>(null)

const subjects = ref<SubjectTreeNode[]>([])
const subjectId = ref<number | null>(null)

const title = ref('')
const description = ref('')
const classMode = ref<'online' | 'offline' | 'both'>('online')
const city = ref('北京')
const address = ref('')
const frequencyPerWeek = ref<number>(2)
const publisherIdentity = ref<'PARENT' | 'STUDENT_SELF'>('PARENT')
const childAge = ref<number | null>(null)
const budgetMin = ref<number | null>(null)
const budgetMax = ref<number | null>(null)
const stageCode = ref<'PRESCHOOL' | 'PRIMARY' | 'JUNIOR' | 'SENIOR' | 'OTHER'>('PRIMARY')
const educationRequirement = ref<string>('UNLIMITED')
const schedule = ref('')

const stageOptions = [
  { value: 'PRESCHOOL', label: '幼教育' },
  { value: 'PRIMARY', label: '小学' },
  { value: 'JUNIOR', label: '初中' },
  { value: 'SENIOR', label: '高中' },
  { value: 'OTHER', label: '其他' },
]

const eduOptions = [
  { value: 'UNLIMITED', label: '不限' },
  { value: 'TOP2', label: 'top2' },
  { value: 'C985', label: '985' },
  { value: 'C211', label: '211' },
  { value: 'DOUBLE_FIRST_CLASS', label: '双一流' },
  { value: 'FIRST_TIER', label: '一本' },
  { value: 'BACHELOR', label: '本科' },
  { value: 'OVERSEAS', label: '海归' },
  { value: 'QS50', label: 'QS前50' },
]

const subjectOptions = computed(() => {
  const out: Array<{ id: number; label: string }> = []
  function walk(node: SubjectTreeNode, prefix: string) {
    const label = prefix ? `${prefix} / ${node.name}` : node.name
    out.push({ id: node.id, label })
    node.children?.forEach((c) => walk(c, label))
  }
  subjects.value.forEach((n) => walk(n, ''))
  return out
})

async function loadSubjects() {
  subjects.value = await homeGuestApi.getSubjectTree()
  if (subjectId.value == null && subjectOptions.value.length > 0) {
    const first = subjectOptions.value[0]
    if (first) subjectId.value = first.id
  }
}

async function onSubmit() {
  doneHint.value = null
  error.value = null
  // 前端校验与后端保持一致，避免无效数据写入导致教师端筛选/展示异常
  if (!subjectId.value) {
    error.value = '请选择科目'
    return
  }
  if (!title.value.trim()) {
    error.value = '请输入标题'
    return
  }
  if (!description.value.trim()) {
    error.value = '请输入需求描述'
    return
  }
  if (!classMode.value) {
    error.value = '请选择授课方式'
    return
  }
  if (!stageCode.value) {
    error.value = '请选择授课学段'
    return
  }
  if (!educationRequirement.value) {
    error.value = '请选择学历要求'
    return
  }
  if (!frequencyPerWeek.value || frequencyPerWeek.value < 1 || frequencyPerWeek.value > 7) {
    error.value = '请选择授课频次（每周 1~7 次）'
    return
  }
  if (!publisherIdentity.value) {
    error.value = '请选择发布者身份'
    return
  }
  if ((classMode.value === 'offline' || classMode.value === 'both') && (!city.value.trim() || !address.value.trim())) {
    error.value = '线下授课必须填写城市与授课地址'
    return
  }
  if (budgetMin.value != null && budgetMin.value <= 0) {
    error.value = '预算下限需大于 0'
    return
  }
  if (budgetMax.value != null && budgetMax.value <= 0) {
    error.value = '预算上限需大于 0'
    return
  }
  if (budgetMin.value != null && budgetMax.value != null && budgetMin.value > budgetMax.value) {
    error.value = '预算下限不能大于预算上限'
    return
  }
  loading.value = true
  try {
    const id = await jobsApi.createDemand({
      subjectId: subjectId.value,
      title: title.value.trim(),
      description: description.value.trim(),
      classMode: classMode.value,
      city: classMode.value === 'online' ? undefined : city.value.trim() || undefined,
      address: classMode.value === 'online' ? undefined : address.value.trim() || undefined,
      frequencyPerWeek: frequencyPerWeek.value,
      childAge: childAge.value ?? undefined,
      budgetMin: budgetMin.value ?? undefined,
      budgetMax: budgetMax.value ?? undefined,
      stageCode: stageCode.value,
      educationRequirement: educationRequirement.value,
      publisherIdentity: publisherIdentity.value,
      schedule: schedule.value.trim() || undefined,
    })
    doneHint.value = '发布成功'
    await router.replace({ name: 'studentMineJobs', query: { highlight: String(id) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadSubjects().catch((e) => {
    error.value = e instanceof Error ? e.message : '加载科目失败'
  })
})

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
      <label class="field">
        <div class="label">科目</div>
        <select v-model.number="subjectId" class="input">
          <option v-for="o in subjectOptions" :key="o.id" :value="o.id">{{ o.label }}</option>
        </select>
      </label>

      <label class="field">
        <div class="label">标题</div>
        <input v-model="title" class="input" placeholder="例如：小学三年级数学家教" />
      </label>

      <label class="field">
        <div class="label">需求描述</div>
        <textarea v-model="description" class="textarea" rows="4" placeholder="希望老师重点讲解应用题与计算..." />
      </label>

      <div class="row">
        <label class="field">
          <div class="label">授课方式</div>
          <select v-model="classMode" class="input">
            <option value="online">线上</option>
            <option value="offline">线下</option>
            <option value="both">均可</option>
          </select>
        </label>
        <label class="field">
          <div class="label">孩子年龄</div>
          <input v-model.number="childAge" class="input" inputmode="numeric" placeholder="例如：9" />
        </label>
      </div>

      <div class="row">
        <label class="field">
          <div class="label">授课学段</div>
          <select v-model="stageCode" class="input">
            <option v-for="o in stageOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </label>
        <label class="field">
          <div class="label">学历要求</div>
          <select v-model="educationRequirement" class="input">
            <option v-for="o in eduOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </label>
      </div>

      <div class="row">
        <label class="field">
          <div class="label">授课频次</div>
          <select v-model.number="frequencyPerWeek" class="input">
            <option v-for="n in 7" :key="n" :value="n">每周 {{ n }} 次</option>
          </select>
        </label>
        <label class="field">
          <div class="label">发布者身份</div>
          <select v-model="publisherIdentity" class="input">
            <option value="PARENT">学生家长</option>
            <option value="STUDENT_SELF">学生本人</option>
          </select>
        </label>
      </div>

      <div class="row" v-if="classMode !== 'online'">
        <label class="field">
          <div class="label">城市</div>
          <input v-model="city" class="input" placeholder="例如：北京" />
        </label>
        <label class="field">
          <div class="label">上课地址</div>
          <input v-model="address" class="input" placeholder="例如：朝阳·望京" />
        </label>
      </div>

      <div class="row">
        <label class="field">
          <div class="label">预算下限（每小时）</div>
          <input v-model.number="budgetMin" class="input" inputmode="decimal" placeholder="例如：80" />
        </label>
        <label class="field">
          <div class="label">预算上限（每小时）</div>
          <input v-model.number="budgetMax" class="input" inputmode="decimal" placeholder="例如：120" />
        </label>
      </div>

      <label class="field">
        <div class="label">期望时间（JSON 格式）</div>
        <input v-model="schedule" class="input" placeholder='例如：["周二 19-21","周六 10-12"]' />
      </label>
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
