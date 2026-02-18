<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { jobsApi } from '@/api/jobs'
import { homeGuestApi } from '@/api/homeGuest'
import type { StudentJobPosting, SubjectTreeNode } from '@/api/types'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const doneHint = ref<string | null>(null)

const subjects = ref<SubjectTreeNode[]>([])
const form = ref<StudentJobPosting | null>(null)

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

async function load() {
  loading.value = true
  error.value = null
  doneHint.value = null
  try {
    const [d, tree] = await Promise.all([jobsApi.getDemand(id.value), homeGuestApi.getSubjectTree()])
    form.value = d
    if (form.value) {
      if (!form.value.stageCode) form.value.stageCode = 'PRIMARY'
      if (!form.value.educationRequirement) form.value.educationRequirement = 'UNLIMITED'
      if (!form.value.frequencyPerWeek) form.value.frequencyPerWeek = 2
      if (!form.value.publisherIdentity) form.value.publisherIdentity = 'PARENT'
      if (!form.value.classMode) form.value.classMode = 'online'
    }
    subjects.value = tree
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
  // 编辑页保存同发布页强校验保持一致，确保数据可用于教师端筛选与展示
  if (!form.value.title?.trim()) {
    error.value = '请输入标题'
    return
  }
  if (!String(form.value.description || '').trim()) {
    error.value = '请输入需求描述'
    return
  }
  if (!form.value.classMode) {
    error.value = '请选择授课方式'
    return
  }
  if (!form.value.stageCode) {
    error.value = '请选择授课学段'
    return
  }
  if (!form.value.educationRequirement) {
    error.value = '请选择学历要求'
    return
  }
  if (!form.value.frequencyPerWeek || form.value.frequencyPerWeek < 1 || form.value.frequencyPerWeek > 7) {
    error.value = '请选择授课频次（每周 1~7 次）'
    return
  }
  if (!form.value.publisherIdentity) {
    error.value = '请选择发布者身份'
    return
  }
  if ((form.value.classMode === 'offline' || form.value.classMode === 'both') && (!String(form.value.city || '').trim() || !String(form.value.address || '').trim())) {
    error.value = '线下授课必须填写城市与授课地址'
    return
  }
  const bmin = form.value.budgetMin != null ? Number(form.value.budgetMin) : null
  const bmax = form.value.budgetMax != null ? Number(form.value.budgetMax) : null
  if (bmin != null && bmin <= 0) {
    error.value = '预算下限需大于 0'
    return
  }
  if (bmax != null && bmax <= 0) {
    error.value = '预算上限需大于 0'
    return
  }
  if (bmin != null && bmax != null && bmin > bmax) {
    error.value = '预算下限不能大于预算上限'
    return
  }
  saving.value = true
  try {
    await jobsApi.updateDemand(id.value, {
      subjectId: form.value.subjectId,
      title: form.value.title.trim(),
      description: String(form.value.description || '').trim(),
      childAge: form.value.childAge ?? undefined,
      classMode: form.value.classMode || undefined,
      city: form.value.classMode === 'online' ? undefined : form.value.city || undefined,
      address: form.value.classMode === 'online' ? undefined : form.value.address || undefined,
      frequencyPerWeek: form.value.frequencyPerWeek ?? undefined,
      publisherIdentity: form.value.publisherIdentity || undefined,
      budgetMin: bmin ?? undefined,
      budgetMax: bmax ?? undefined,
      stageCode: form.value.stageCode || undefined,
      educationRequirement: form.value.educationRequirement || undefined,
      schedule: form.value.schedule || undefined,
      status: form.value.status ?? undefined,
    })
    doneHint.value = '已保存'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
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
      <label class="field">
        <div class="label">科目</div>
        <select v-model.number="form.subjectId" class="input">
          <option v-for="o in subjectOptions" :key="o.id" :value="o.id">{{ o.label }}</option>
        </select>
      </label>

      <label class="field">
        <div class="label">标题</div>
        <input v-model="form.title" class="input" placeholder="例如：小学三年级数学家教" />
      </label>

      <label class="field">
        <div class="label">需求描述</div>
        <textarea v-model="form.description" class="textarea" rows="4" placeholder="希望老师重点讲解应用题与计算..." />
      </label>

      <div class="row">
        <label class="field">
          <div class="label">授课方式</div>
          <select v-model="form.classMode" class="input">
            <option value="online">线上</option>
            <option value="offline">线下</option>
            <option value="both">均可</option>
          </select>
        </label>
        <label class="field">
          <div class="label">孩子年龄</div>
          <input v-model.number="form.childAge" class="input" inputmode="numeric" placeholder="例如：9" />
        </label>
      </div>

      <div class="row">
        <label class="field">
          <div class="label">授课学段</div>
          <select v-model="form.stageCode" class="input">
            <option v-for="o in stageOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </label>
        <label class="field">
          <div class="label">学历要求</div>
          <select v-model="form.educationRequirement" class="input">
            <option v-for="o in eduOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </label>
      </div>

      <div class="row">
        <label class="field">
          <div class="label">授课频次</div>
          <select v-model.number="form.frequencyPerWeek" class="input">
            <option v-for="n in 7" :key="n" :value="n">每周 {{ n }} 次</option>
          </select>
        </label>
        <label class="field">
          <div class="label">发布者身份</div>
          <select v-model="form.publisherIdentity" class="input">
            <option value="PARENT">学生家长</option>
            <option value="STUDENT_SELF">学生本人</option>
          </select>
        </label>
      </div>

      <div class="row" v-if="form.classMode !== 'online'">
        <label class="field">
          <div class="label">城市</div>
          <input v-model="form.city" class="input" placeholder="例如：北京" />
        </label>
        <label class="field">
          <div class="label">上课地址</div>
          <input v-model="form.address" class="input" placeholder="例如：朝阳·望京" />
        </label>
      </div>

      <div class="row">
        <label class="field">
          <div class="label">预算下限（每小时）</div>
          <input v-model.number="form.budgetMin" class="input" inputmode="decimal" placeholder="例如：80" />
        </label>
        <label class="field">
          <div class="label">预算上限（每小时）</div>
          <input v-model.number="form.budgetMax" class="input" inputmode="decimal" placeholder="例如：120" />
        </label>
      </div>

      <label class="field">
        <div class="label">期望时间（JSON 格式）</div>
        <input v-model="form.schedule" class="input" placeholder='例如：["周二 19-21","周六 10-12"]' />
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

.actions {
  display: flex;
  gap: 10px;
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
