<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const savedHint = ref<string | null>(null)

const isTeacher = computed(() => auth.user?.userType === 1)

const name = ref('')
const sex = ref<number | null>(null)

const teacherRealName = ref('')
const teacherEducation = ref('')
const teacherSubject = ref('')
const teacherExperienceYears = ref<number | null>(null)
const teacherRatePerHour = ref<number | null>(null)
const teacherIntroduction = ref('')

const studentRealName = ref('')
const studentChildAge = ref<number | null>(null)
const studentAddress = ref('')
const studentDemandDescription = ref('')
const studentBudget = ref<number | null>(null)

async function load() {
  if (!auth.isLoggedIn) return
  loading.value = true
  error.value = null
  savedHint.value = null
  try {
    const me = await auth.refreshMe()
    name.value = me?.name || auth.user?.name || ''
    sex.value = me?.sex ?? auth.user?.sex ?? null

    const tp = me?.teacherProfile
    const sp = me?.studentProfile

    teacherRealName.value = tp?.realName || ''
    teacherEducation.value = tp?.education || ''
    teacherSubject.value = tp?.subject || ''
    teacherExperienceYears.value = tp?.experienceYears ?? null
    teacherRatePerHour.value = tp?.ratePerHour != null ? Number(tp.ratePerHour) : null
    teacherIntroduction.value = tp?.introduction || ''

    studentRealName.value = sp?.realName || ''
    studentChildAge.value = sp?.childAge ?? null
    studentAddress.value = sp?.address || ''
    studentDemandDescription.value = sp?.demandDescription || ''
    studentBudget.value = sp?.budget != null ? Number(sp.budget) : null
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  savedHint.value = null
  error.value = null
  try {
    await userApi.updateUserInfo({
      baseUserInfo: {
        name: name.value.trim() || undefined,
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
    error.value = e instanceof Error ? e.message : '保存失败'
  }
}

function onLogout() {
  auth.logout()
  void router.replace({ name: 'home' })
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
        <button class="btn" type="button" @click="onLogout">退出登录</button>
        <button class="btn btn-primary" type="button" :disabled="loading" @click="onSave">保存</button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="savedHint" class="hint ok">{{ savedHint }}</div>

    <div class="card form">
      <div class="sec">
        <div class="sec-title">基础信息</div>
        <div class="grid">
          <label class="field">
            <div class="label">昵称</div>
            <input v-model="name" class="input" placeholder="请输入昵称" />
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

      <div class="sec">
        <div class="sec-title">{{ isTeacher ? '教师资料' : '家长资料' }}</div>
        <div class="grid" v-if="isTeacher">
          <label class="field">
            <div class="label">真实姓名</div>
            <input v-model="teacherRealName" class="input" placeholder="例如：张老师" />
          </label>
          <label class="field">
            <div class="label">学历</div>
            <input v-model="teacherEducation" class="input" placeholder="例如：本科" />
          </label>
          <label class="field">
            <div class="label">教授科目</div>
            <input v-model="teacherSubject" class="input" placeholder="例如：数学/英语" />
          </label>
          <label class="field">
            <div class="label">教学经验（年）</div>
            <input v-model.number="teacherExperienceYears" class="input" inputmode="numeric" placeholder="例如：3" />
          </label>
          <label class="field">
            <div class="label">每小时价格</div>
            <input v-model.number="teacherRatePerHour" class="input" inputmode="decimal" placeholder="例如：120" />
          </label>
          <label class="field span2">
            <div class="label">简介</div>
            <textarea v-model="teacherIntroduction" class="textarea" rows="4" placeholder="写点你的优势与授课风格" />
          </label>
        </div>

        <div class="grid" v-else>
          <label class="field">
            <div class="label">姓名</div>
            <input v-model="studentRealName" class="input" placeholder="例如：王女士" />
          </label>
          <label class="field">
            <div class="label">孩子年龄</div>
            <input v-model.number="studentChildAge" class="input" inputmode="numeric" placeholder="例如：9" />
          </label>
          <label class="field span2">
            <div class="label">地址</div>
            <input v-model="studentAddress" class="input" placeholder="例如：北京·朝阳·望京" />
          </label>
          <label class="field span2">
            <div class="label">需求描述</div>
            <textarea v-model="studentDemandDescription" class="textarea" rows="4" placeholder="例如：希望提高应用题，孩子基础一般" />
          </label>
          <label class="field">
            <div class="label">预算</div>
            <input v-model.number="studentBudget" class="input" inputmode="decimal" placeholder="例如：120" />
          </label>
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
  .grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: auto;
  }
}
</style>

