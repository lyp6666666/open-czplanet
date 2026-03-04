<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">用户管理</div>
        <div class="tabs">
          <button class="btn" :class="{ active: tab === 'teachers' }" type="button" @click="setTab('teachers')">教师</button>
          <button class="btn" :class="{ active: tab === 'students' }" type="button" @click="setTab('students')">学生</button>
        </div>
      </div>
      <div class="right">
        <input v-model="q" class="input search" placeholder="搜索昵称 / 手机号 / 真实姓名" />
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
        <button class="btn btn-primary" type="button" @click="openCreate">新增</button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 90px">ID</th>
            <th style="width: 160px">昵称</th>
            <th style="width: 150px">手机号</th>
            <th style="width: 120px">状态</th>
            <th>资料</th>
            <th style="width: 220px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <div class="cell-title">{{ row.name || '-' }}</div>
              <div class="cell-sub">type: {{ row.userType }}</div>
            </td>
            <td>{{ row.phone }}</td>
            <td>
              <span class="badge" :class="{ danger: row.status === 1 }">{{ statusText(row.status) }}</span>
            </td>
            <td>
              <div v-if="row.userType === 1" class="cell-sub">
                {{ [row.teacherRealName, row.teacherEducation, row.teacherSubject, row.teacherCity].filter(Boolean).join(' · ') || '-' }}
              </div>
              <div v-else class="cell-sub">
                {{ [row.studentRealName, row.studentAge ? `${row.studentAge}岁` : null, row.studentAddress].filter(Boolean).join(' · ') || '-' }}
              </div>
            </td>
            <td>
              <div class="actions">
                <button class="btn" type="button" :disabled="busyId === row.id" @click="openEdit(row.id)">修改</button>
                <button class="btn btn-danger" type="button" :disabled="busyId === row.id" @click="onDisable(row.id)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="6">
              <div class="empty">暂无数据</div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <button class="btn btn-muted" type="button" :disabled="page <= 1 || loading" @click="page -= 1">上一页</button>
      <div class="pager-text">第 {{ page }} 页 / 共 {{ totalPages }} 页（{{ total }} 条）</div>
      <button class="btn btn-muted" type="button" :disabled="page >= totalPages || loading" @click="page += 1">下一页</button>
      <select v-model.number="size" class="input size">
        <option :value="10">10 / 页</option>
        <option :value="20">20 / 页</option>
        <option :value="50">50 / 页</option>
      </select>
    </div>
  </div>

  <DialogModal v-if="editOpen" :title="editMode === 'create' ? '新增用户' : '修改用户'" @close="closeEdit">
    <div class="form">
      <div v-if="editMode === 'create'" class="row">
        <div class="label">用户类型</div>
        <select v-model.number="form.userType" class="input">
          <option :value="1">教师</option>
          <option :value="2">学生</option>
        </select>
      </div>

      <div class="row">
        <div class="label">昵称</div>
        <input v-model="form.name" class="input" />
      </div>

      <div class="row">
        <div class="label">手机号</div>
        <input v-model="form.phone" class="input" placeholder="11 位手机号" />
      </div>

      <div class="row">
        <div class="label">头像</div>
        <input v-model="form.avatar" class="input" placeholder="URL（可选）" />
      </div>

      <div class="row">
        <div class="label">性别</div>
        <select v-model.number="form.sex" class="input">
          <option :value="0">未知</option>
          <option :value="1">男</option>
          <option :value="2">女</option>
        </select>
      </div>

      <div class="row">
        <div class="label">状态</div>
        <select v-model.number="form.status" class="input">
          <option :value="0">正常</option>
          <option :value="1">拉黑</option>
        </select>
      </div>

      <div class="section" v-if="form.userType === 1">
        <div class="section-title">教师资料</div>
        <div class="row">
          <div class="label">真实姓名</div>
          <input v-model="form.teacherRealName" class="input" />
        </div>
        <div class="row">
          <div class="label">学历</div>
          <input v-model="form.teacherEducation" class="input" />
        </div>
        <div class="row">
          <div class="label">科目</div>
          <input v-model="form.teacherSubject" class="input" />
        </div>
        <div class="row">
          <div class="label">城市</div>
          <input v-model="form.teacherCity" class="input" />
        </div>
        <div class="row">
          <div class="label">课时费</div>
          <input v-model="form.teacherRatePerHour" class="input" placeholder="例如 120" />
        </div>
      </div>

      <div class="section" v-else>
        <div class="section-title">学生资料</div>
        <div class="row">
          <div class="label">真实姓名</div>
          <input v-model="form.studentRealName" class="input" />
        </div>
        <div class="row">
          <div class="label">年龄</div>
          <input v-model.number="form.studentAge" class="input" placeholder="例如 12" />
        </div>
        <div class="row">
          <div class="label">地址</div>
          <input v-model="form.studentAddress" class="input" />
        </div>
        <div class="row">
          <div class="label">预算</div>
          <input v-model="form.studentBudget" class="input" placeholder="例如 150" />
        </div>
        <div class="row">
          <div class="label">需求描述</div>
          <textarea v-model="form.studentDemandDescription" class="input textarea" rows="3" />
        </div>
      </div>

      <div v-if="editError" class="error">{{ editError }}</div>
    </div>

    <template #actions>
      <button class="btn btn-muted" type="button" @click="closeEdit">取消</button>
      <button class="btn btn-primary" type="button" :disabled="editSubmitting" @click="submitEdit">
        {{ editSubmitting ? '提交中...' : '保存' }}
      </button>
    </template>
  </DialogModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import type { AdminUserDetail, AdminUserRow } from '@/api/types'
import { createUser, disableUser, getUserDetail, pageStudents, pageTeachers, updateUser } from '@/api/users'
import DialogModal from '@/ui/DialogModal.vue'

type TabKey = 'teachers' | 'students'
type EditMode = 'create' | 'edit'

const tab = ref<TabKey>('teachers')
const q = ref('')
const rows = ref<AdminUserRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)
const busyId = ref<number | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function statusText(v?: number | null) {
  if (v === 0) return '正常'
  if (v === 1) return '拉黑'
  return '-'
}

function setTab(next: TabKey) {
  if (tab.value === next) return
  tab.value = next
  page.value = 1
  load()
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    const res = tab.value === 'teachers' ? await pageTeachers(q.value.trim() || null, page.value, size.value) : await pageStudents(q.value.trim() || null, page.value, size.value)
    rows.value = res.records
    total.value = Number(res.total || 0)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

watch([page, size], () => {
  if (page.value > totalPages.value) page.value = totalPages.value
  load()
})

watch(
  () => tab.value,
  () => {
    q.value = ''
  },
)

onMounted(load)

const editOpen = ref(false)
const editMode = ref<EditMode>('create')
const editSubmitting = ref(false)
const editError = ref<string | null>(null)
const editId = ref<number | null>(null)

const form = reactive({
  userType: 1 as 1 | 2,
  name: '',
  phone: '',
  avatar: '',
  sex: 0,
  status: 0,
  teacherRealName: '',
  teacherEducation: '',
  teacherSubject: '',
  teacherCity: '',
  teacherRatePerHour: '',
  studentRealName: '',
  studentAge: undefined as number | undefined,
  studentAddress: '',
  studentBudget: '',
  studentDemandDescription: '',
})

function resetForm(userType: 1 | 2) {
  form.userType = userType
  form.name = ''
  form.phone = ''
  form.avatar = ''
  form.sex = 0
  form.status = 0
  form.teacherRealName = ''
  form.teacherEducation = ''
  form.teacherSubject = ''
  form.teacherCity = ''
  form.teacherRatePerHour = ''
  form.studentRealName = ''
  form.studentAge = undefined
  form.studentAddress = ''
  form.studentBudget = ''
  form.studentDemandDescription = ''
}

function openCreate() {
  editMode.value = 'create'
  editId.value = null
  editError.value = null
  editSubmitting.value = false
  resetForm(tab.value === 'teachers' ? 1 : 2)
  editOpen.value = true
}

async function openEdit(id: number) {
  if (busyId.value != null) return
  busyId.value = id
  try {
    const detail: AdminUserDetail = await getUserDetail(id)
    editMode.value = 'edit'
    editId.value = id
    editError.value = null
    editSubmitting.value = false

    const u = detail.user
    resetForm(u.userType === 1 ? 1 : 2)
    form.name = u.name || ''
    form.phone = u.phone || ''
    form.avatar = u.avatar || ''
    form.sex = u.sex || 0
    form.status = u.status || 0

    if (u.userType === 1 && detail.teacherProfile) {
      form.teacherRealName = detail.teacherProfile.realName || ''
      form.teacherEducation = detail.teacherProfile.education || ''
      form.teacherSubject = detail.teacherProfile.subject || ''
      form.teacherCity = detail.teacherProfile.city || ''
      form.teacherRatePerHour = (detail.teacherProfile as unknown as { ratePerHour?: unknown }).ratePerHour ? String((detail.teacherProfile as unknown as { ratePerHour?: unknown }).ratePerHour) : ''
    }
    if (u.userType === 2 && detail.studentProfile) {
      form.studentRealName = detail.studentProfile.realName || ''
      form.studentAge = detail.studentProfile.age ?? undefined
      form.studentAddress = detail.studentProfile.address || ''
      form.studentBudget = detail.studentProfile.budget != null ? String(detail.studentProfile.budget) : ''
      form.studentDemandDescription = detail.studentProfile.demandDescription || ''
    }

    editOpen.value = true
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    busyId.value = null
  }
}

function closeEdit() {
  editOpen.value = false
}

function normalizeNumberString(s: string): string | null {
  const t = s.trim()
  if (!t) return null
  return t
}

async function submitEdit() {
  if (editSubmitting.value) return
  editSubmitting.value = true
  editError.value = null
  try {
    const base = {
      name: form.name.trim() || null,
      phone: form.phone.trim() || null,
      avatar: form.avatar.trim() || null,
      sex: form.sex || null,
      status: form.status,
    }

    if (editMode.value === 'create') {
      await createUser({
        userType: form.userType,
        ...base,
        phone: form.phone.trim(),
        teacherRealName: form.userType === 1 ? form.teacherRealName.trim() || null : undefined,
        teacherEducation: form.userType === 1 ? form.teacherEducation.trim() || null : undefined,
        teacherSubject: form.userType === 1 ? form.teacherSubject.trim() || null : undefined,
        teacherCity: form.userType === 1 ? form.teacherCity.trim() || null : undefined,
        teacherRatePerHour: form.userType === 1 ? normalizeNumberString(form.teacherRatePerHour) : undefined,
        studentRealName: form.userType === 2 ? form.studentRealName.trim() || null : undefined,
        studentAge: form.userType === 2 ? form.studentAge ?? null : undefined,
        studentAddress: form.userType === 2 ? form.studentAddress.trim() || null : undefined,
        studentBudget: form.userType === 2 ? normalizeNumberString(form.studentBudget) : undefined,
        studentDemandDescription: form.userType === 2 ? form.studentDemandDescription.trim() || null : undefined,
      })
    } else {
      const id = editId.value
      if (id == null) return
      await updateUser(id, {
        ...base,
        teacherRealName: form.userType === 1 ? form.teacherRealName.trim() || null : undefined,
        teacherEducation: form.userType === 1 ? form.teacherEducation.trim() || null : undefined,
        teacherSubject: form.userType === 1 ? form.teacherSubject.trim() || null : undefined,
        teacherCity: form.userType === 1 ? form.teacherCity.trim() || null : undefined,
        teacherRatePerHour: form.userType === 1 ? normalizeNumberString(form.teacherRatePerHour) : undefined,
        studentRealName: form.userType === 2 ? form.studentRealName.trim() || null : undefined,
        studentAge: form.userType === 2 ? form.studentAge ?? null : undefined,
        studentAddress: form.userType === 2 ? form.studentAddress.trim() || null : undefined,
        studentBudget: form.userType === 2 ? normalizeNumberString(form.studentBudget) : undefined,
        studentDemandDescription: form.userType === 2 ? form.studentDemandDescription.trim() || null : undefined,
      })
    }
    closeEdit()
    await load()
  } catch (e) {
    editError.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '提交失败'
  } finally {
    editSubmitting.value = false
  }
}

async function onDisable(id: number) {
  if (busyId.value != null) return
  if (!window.confirm('确认删除该用户？（将拉黑并禁用资料）')) return
  busyId.value = id
  try {
    await disableUser(id)
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '操作失败'
  } finally {
    busyId.value = null
  }
}
</script>

<style scoped>
.box {
  padding: 14px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.left {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.title {
  font-weight: 800;
}

.tabs {
  display: flex;
  gap: 10px;
}

.btn.active {
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.08);
}

.right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search {
  width: 260px;
  padding: 8px 10px;
}

.error {
  margin-top: 10px;
  color: var(--danger);
  font-size: 13px;
}

.table-wrap {
  margin-top: 12px;
  overflow: auto;
}

.cell-title {
  font-weight: 600;
}

.cell-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty {
  padding: 20px 0;
  color: var(--muted);
  text-align: center;
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pager-text {
  color: var(--muted);
  font-size: 12px;
}

.size {
  width: 110px;
  padding: 8px 10px;
}

.badge.danger {
  border-color: rgba(239, 68, 68, 0.4);
  color: rgba(239, 68, 68, 1);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
  align-items: center;
}

.label {
  color: var(--muted);
  font-size: 12px;
}

.section {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.section-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.textarea {
  resize: vertical;
}
</style>

