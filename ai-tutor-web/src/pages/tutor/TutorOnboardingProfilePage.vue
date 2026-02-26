<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)

const education = ref('')
const educationOther = ref('')
const city = ref('')
const highestEduSchool = ref('')
const introduction = ref('')

const subjectSelected = ref<string[]>([])
const subjectOther = ref('')

const teachingMode = ref<'ONLINE' | 'OFFLINE' | 'BOTH' | ''>('')

const eduOptions = ['本科', '硕士', '博士', '专科', '海外', '其他']

const finalEducation = computed(() => {
  if (education.value !== '其他') return education.value.trim()
  return educationOther.value.trim()
})

const finalSubjects = computed(() => {
  const list = subjectSelected.value.slice()
  const idx = list.indexOf(SUBJECT_OTHER_VALUE)
  if (idx >= 0) {
    list.splice(idx, 1)
    if (subjectOther.value.trim()) list.push(subjectOther.value.trim())
  }
  return Array.from(new Set(list.map((s) => s.trim()).filter(Boolean)))
})

const canSubmit = computed(() => {
  return !!(
    finalEducation.value &&
    city.value.trim() &&
    highestEduSchool.value.trim() &&
    introduction.value.trim() &&
    finalSubjects.value.length > 0 &&
    teachingMode.value
  )
})

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  error.value = null
  try {
    await userApi.updateUserInfo({
      teacherExtInfo: {
        education: finalEducation.value,
        city: city.value.trim(),
        highestEduSchool: highestEduSchool.value.trim(),
        introduction: introduction.value.trim(),
        subject: finalSubjects.value.join(','),
        teachingMode: teachingMode.value,
      },
    })
    await auth.refreshMe()
    await router.replace('/tutor/jobs')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    loading.value = false
  }
}

async function skip() {
  await router.replace('/tutor/jobs')
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
  const basicCompleted = !!(me?.avatar && me.teacherProfile?.realName?.trim())
  if (!basicCompleted) {
    await router.replace('/tutor/onboarding/basic')
    return
  }

  if (me?.teacherProfile?.education) education.value = me.teacherProfile.education
  if (me?.teacherProfile?.city) city.value = me.teacherProfile.city
  if (me?.teacherProfile?.highestEduSchool) highestEduSchool.value = me.teacherProfile.highestEduSchool
  if (me?.teacherProfile?.introduction) introduction.value = me.teacherProfile.introduction
  const tm = me?.teacherProfile?.teachingMode
  if (tm === 'ONLINE' || tm === 'OFFLINE' || tm === 'BOTH') teachingMode.value = tm

  const rawSubject = me?.teacherProfile?.subject || ''
  const parts = rawSubject
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  const presets = new Set<string>(SUBJECT_PRESETS as unknown as string[])
  const selected: string[] = []
  let other = ''
  for (const p of parts) {
    if (presets.has(p)) selected.push(p)
    else other = other ? `${other},${p}` : p
  }
  if (other) {
    selected.push(SUBJECT_OTHER_VALUE)
    subjectOther.value = other
  }
  subjectSelected.value = selected
})
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <div class="title">完善个人简历</div>
      <button class="btn" type="button" :disabled="loading" @click="skip">跳过</button>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card form">
      <label class="field">
        <div class="label">学历</div>
        <select v-model="education" class="input" :disabled="loading">
          <option value="" disabled>请选择学历</option>
          <option v-for="o in eduOptions" :key="o" :value="o">{{ o }}</option>
        </select>
      </label>

      <label v-if="education === '其他'" class="field">
        <div class="label">补充学历</div>
        <input v-model="educationOther" class="input" placeholder="例如：双一流/211/985/海外院校" :disabled="loading" />
      </label>

      <label class="field">
        <div class="label">所在城市</div>
        <input v-model="city" class="input" placeholder="例如：北京" :disabled="loading" />
      </label>

      <label class="field">
        <div class="label">最高学历学校</div>
        <input v-model="highestEduSchool" class="input" placeholder="例如：北京大学" :disabled="loading" />
      </label>

      <label class="field">
        <div class="label">自我介绍</div>
        <textarea v-model="introduction" class="textarea" rows="4" placeholder="简单介绍你的教学经历与优势" :disabled="loading" />
      </label>

      <div class="field">
        <div class="label">擅长学科（多选）</div>
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
        <input
          v-if="subjectSelected.includes(SUBJECT_OTHER_VALUE)"
          v-model="subjectOther"
          class="input"
          placeholder="输入其他学科，多个用逗号分隔"
          :disabled="loading"
        />
      </div>

      <label class="field">
        <div class="label">支持教学方式</div>
        <select v-model="teachingMode" class="input" :disabled="loading">
          <option value="" disabled>请选择教学方式</option>
          <option value="ONLINE">线上教学</option>
          <option value="OFFLINE">线下教学</option>
          <option value="BOTH">均可</option>
        </select>
      </label>

      <div class="actions">
        <button class="btn btn-primary" type="button" :disabled="loading || !canSubmit" @click="submit">保存并完成</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}

.head.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
}

.title {
  font-weight: 900;
}

.form {
  padding: 12px;
  display: grid;
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

.input,
.textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  font-size: 13px;
}

.textarea {
  resize: vertical;
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
  padding: 6px 10px;
  font-size: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}
</style>
