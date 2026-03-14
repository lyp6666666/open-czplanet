<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const loading = ref(false)

const education = ref('')
const educationOther = ref('')
const storedCity = (localStorage.getItem('ai_tutor_city') || '北京').trim()
const city = ref(storedCity || '')
const cityModalOpen = ref(false)
const highestEduSchool = ref('')
const introduction = ref('')

const subjectSelected = ref<string[]>([])
const subjectOther = ref('')

const teachingMode = ref<'ONLINE' | 'OFFLINE' | 'BOTH' | ''>('')

const eduOptions = ['本科', '硕士', '博士', '专科', '海外', '其他']

const allowNational = computed(() => teachingMode.value === 'ONLINE')

const cities = computed(() => {
  const base = [city.value, localStorage.getItem('ai_tutor_city') || '', '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.map((x) => String(x || '').trim()).filter(Boolean)))
})

function onSelectCity(v: string) {
  const raw = String(v || '').trim()
  if ((teachingMode.value === 'OFFLINE' || teachingMode.value === 'BOTH') && raw === '全国') {
    toast.show('线下授课请选择具体城市', 'error')
    return
  }
  if (raw) localStorage.setItem('ai_tutor_city', raw)
  city.value = raw
}

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
    toast.show(e instanceof Error ? e.message : '保存失败', 'error')
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
  <div class="page">
    <div class="shell">
      <div class="card board">
        <aside class="left">
          <div class="brand">
            <div class="logo">Hi，欢迎成为老师</div>
            <div class="slogan">开启高效接单方式，仅需3步</div>
          </div>
          <div class="illustration" />
        </aside>

        <section class="right">
          <div class="right-head">
            <div class="r-title">完善个人简历</div>
            <button class="skip-btn" type="button" :disabled="loading" @click="skip">跳过</button>
          </div>

          <div class="form">
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
              <button class="input" type="button" :disabled="loading" @click="cityModalOpen = true">{{ city.trim() || '请选择城市' }}</button>
              <CitySelectModal
                :open="cityModalOpen"
                :model-value="city"
                :hot-cities="cities"
                :allow-national="allowNational"
                @update:model-value="onSelectCity"
                @close="cityModalOpen = false"
              />
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
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.r-title {
  font-size: 18px;
  font-weight: 700;
  color: #0b2f2d;
}

.skip-btn {
  font-size: 13px;
  color: rgba(11, 47, 45, 0.6);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
}

.skip-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #0b2f2d;
}

.hint {
  margin: 10px 0 16px;
  font-size: 13px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 0, 0, 0.12);
  background: rgba(255, 0, 0, 0.04);
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

.input,
.textarea {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.textarea {
  resize: vertical;
}

.input:focus,
.textarea:focus {
  border-color: #12b4ab;
  box-shadow: 0 0 0 4px rgba(18, 180, 171, 0.12);
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
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.chip:has(input:checked) {
  border-color: #12b4ab;
  background: rgba(18, 180, 171, 0.08);
  color: #0b2f2d;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.btn {
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #fff;
  color: rgba(11, 47, 45, 0.85);
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary {
  border: none;
  background: #12b4ab;
  color: #fff;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 860px) {
  .card.board {
    grid-template-columns: 1fr;
  }
  .left {
    display: none;
  }
}
</style>
