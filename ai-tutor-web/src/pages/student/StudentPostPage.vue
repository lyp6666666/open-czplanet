<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
const childAge = ref<number | null>(null)
const budgetMin = ref<number | null>(null)
const budgetMax = ref<number | null>(null)
const schedule = ref('')

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
  if (!subjectId.value) {
    error.value = '请选择科目'
    return
  }
  if (!title.value.trim()) {
    error.value = '请输入标题'
    return
  }
  loading.value = true
  try {
    const id = await jobsApi.createDemand({
      subjectId: subjectId.value,
      title: title.value.trim(),
      description: description.value.trim() || undefined,
      classMode: classMode.value,
      city: classMode.value === 'online' ? undefined : city.value.trim() || undefined,
      address: classMode.value === 'online' ? undefined : address.value.trim() || undefined,
      childAge: childAge.value ?? undefined,
      budgetMin: budgetMin.value ?? undefined,
      budgetMax: budgetMax.value ?? undefined,
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
        <div class="label">期望时间（JSON）</div>
        <input v-model="schedule" class="input" placeholder='例如：["Tue 19-21","Sat 10-12"]' />
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
