<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">机构账号</div>
        <div class="sub">支持查询、编辑、禁用（删除）机构账号</div>
      </div>
      <div class="right">
        <input v-model="q" class="input search" placeholder="搜索机构名 / 登录账号 / 手机号" />
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
        <button class="btn btn-primary" type="button" @click="openCreate">创建机构</button>
      </div>
    </div>

    <div v-if="hint" class="hint ok">{{ hint }}</div>
    <div v-if="errorText" class="hint error">{{ errorText }}</div>

    <div v-if="lastCreated" class="card inner">
      <div class="inner-title">最近创建</div>
      <div class="kv">
        <div class="kv-row"><span class="k">机构用户ID</span><span class="v">{{ lastCreated.orgUserId }}</span></div>
        <div class="kv-row"><span class="k">登录账号</span><span class="v">{{ lastCreated.username }}</span></div>
        <div class="kv-row"><span class="k">初始密码</span><span class="v">{{ lastCreated.initialPassword }}</span></div>
      </div>
    </div>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 90px">ID</th>
            <th>机构</th>
            <th style="width: 180px">账号</th>
            <th style="width: 160px">联系人手机</th>
            <th style="width: 140px">状态</th>
            <th style="width: 160px">最近登录</th>
            <th style="width: 220px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.orgUserId">
            <td>{{ row.orgUserId }}</td>
            <td>
              <div class="cell-title">{{ row.orgName || '-' }}</div>
              <div class="cell-sub">创建：{{ fmtTime(row.createTime) }}</div>
            </td>
            <td>
              <div class="cell-title">{{ row.username || '-' }}</div>
              <div class="cell-sub">强制改密：{{ row.mustChangePassword === 1 ? '是' : '否' }}</div>
            </td>
            <td>{{ row.contactPhone || '-' }}</td>
            <td>
              <span class="badge" :class="{ danger: isDisabled(row) }">{{ statusText(row) }}</span>
            </td>
            <td>{{ fmtTime(row.lastLoginTime) }}</td>
            <td>
              <div class="actions">
                <button class="btn" type="button" :disabled="busyId === row.orgUserId" @click="openEdit(row.orgUserId)">编辑</button>
                <button class="btn btn-danger" type="button" :disabled="busyId === row.orgUserId" @click="onDisable(row.orgUserId)">
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="7">
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

  <DialogModal v-if="createOpen" title="创建机构账号" @close="closeCreate">
    <div class="form">
      <div class="row">
        <div class="label">机构名称</div>
        <input v-model="createForm.orgName" class="input" placeholder="例如：默默机构" />
      </div>
      <div class="row">
        <div class="label">联系人姓名</div>
        <input v-model="createForm.contactName" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">联系人手机号</div>
        <input v-model="createForm.contactPhone" class="input" placeholder="11 位手机号" />
      </div>
      <div class="row">
        <div class="label">地址</div>
        <input v-model="createForm.address" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">机构介绍</div>
        <textarea v-model="createForm.intro" class="input textarea" rows="3" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">营业执照号</div>
        <input v-model="createForm.licenseNo" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">登录账号</div>
        <input v-model="createForm.username" class="input" placeholder="可选；留空自动生成" />
      </div>
      <div class="row">
        <div class="label">初始密码</div>
        <input v-model="createForm.initialPassword" class="input" placeholder="可选；留空自动生成（至少 8 位）" />
      </div>
      <div class="row">
        <div class="label">平台分成%</div>
        <input v-model.number="createForm.splitPlatformPercent" class="input" placeholder="默认 50" />
      </div>
      <div class="row">
        <div class="label">机构分成%</div>
        <input v-model.number="createForm.splitOrgPercent" class="input" placeholder="默认 50" />
      </div>

      <div v-if="createError" class="hint error">{{ createError }}</div>
    </div>

    <template #actions>
      <button class="btn btn-muted" type="button" @click="closeCreate">取消</button>
      <button class="btn btn-primary" type="button" :disabled="createSubmitting" @click="submitCreate">
        {{ createSubmitting ? '创建中...' : '创建' }}
      </button>
    </template>
  </DialogModal>

  <DialogModal v-if="editOpen" title="编辑机构账号" @close="closeEdit">
    <div class="form">
      <div class="row">
        <div class="label">机构名称</div>
        <input v-model="editForm.orgName" class="input" placeholder="例如：默默机构" />
      </div>
      <div class="row">
        <div class="label">联系人姓名</div>
        <input v-model="editForm.contactName" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">联系人手机号</div>
        <input v-model="editForm.contactPhone" class="input" placeholder="11 位手机号" />
      </div>
      <div class="row">
        <div class="label">地址</div>
        <input v-model="editForm.address" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">机构介绍</div>
        <textarea v-model="editForm.intro" class="input textarea" rows="3" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">营业执照号</div>
        <input v-model="editForm.licenseNo" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">登录账号</div>
        <input v-model="editForm.username" class="input" placeholder="可选" />
      </div>
      <div class="row">
        <div class="label">重置密码</div>
        <input v-model="editForm.initialPassword" class="input" placeholder="留空不改；填写则强制改密" />
      </div>
      <div class="row">
        <div class="label">账号状态</div>
        <select v-model.number="editForm.accountStatus" class="input">
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
      </div>
      <div class="row">
        <div class="label">平台分成%</div>
        <input v-model.number="editForm.splitPlatformPercent" class="input" placeholder="默认 50" />
      </div>
      <div class="row">
        <div class="label">机构分成%</div>
        <input v-model.number="editForm.splitOrgPercent" class="input" placeholder="默认 50" />
      </div>

      <div v-if="editError" class="hint error">{{ editError }}</div>
    </div>

    <template #actions>
      <button class="btn btn-muted" type="button" @click="closeEdit">取消</button>
      <button class="btn btn-primary" type="button" :disabled="editSubmitting" @click="submitEdit">
        {{ editSubmitting ? '保存中...' : '保存' }}
      </button>
    </template>
  </DialogModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import type { AdminOrganizationCreateResponse, AdminOrganizationDetail, AdminOrganizationRow } from '@/api/types'
import { createOrganization, disableOrganization, getOrganizationDetail, pageOrganizations, updateOrganization } from '@/api/organizations'
import DialogModal from '@/ui/DialogModal.vue'

const q = ref('')
const rows = ref<AdminOrganizationRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)
const hint = ref<string | null>(null)
const busyId = ref<number | null>(null)
const lastCreated = ref<AdminOrganizationCreateResponse | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function normalizeString(v: string): string | null {
  const s = String(v || '').trim()
  return s ? s : null
}

function normalizePhone(v: string): string | null {
  const raw = normalizeString(v)
  if (!raw) return null
  const digits = raw.replace(/[^\d]/g, '')
  return digits ? digits : null
}

function fmtTime(v: string | null | undefined): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

function isDisabled(row: Pick<AdminOrganizationRow, 'userStatus' | 'accountStatus'>): boolean {
  return row.userStatus === 1 || row.accountStatus === 0
}

function statusText(row: Pick<AdminOrganizationRow, 'userStatus' | 'accountStatus'>): string {
  if (row.userStatus === 1) return '已拉黑'
  if (row.accountStatus === 0) return '已禁用'
  return '正常'
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  hint.value = null
  try {
    const res = await pageOrganizations(q.value.trim() || null, page.value, size.value)
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

onMounted(load)

const createOpen = ref(false)
const createSubmitting = ref(false)
const createError = ref<string | null>(null)

const createForm = reactive({
  orgName: '',
  username: '',
  initialPassword: '',
  contactName: '',
  contactPhone: '',
  address: '',
  intro: '',
  licenseNo: '',
  splitPlatformPercent: 50,
  splitOrgPercent: 50,
})

function openCreate() {
  createError.value = null
  errorText.value = null
  hint.value = null
  createOpen.value = true
}

function closeCreate() {
  createOpen.value = false
  createError.value = null
}

async function submitCreate() {
  if (createSubmitting.value) return
  createError.value = null
  errorText.value = null
  hint.value = null

  const orgName = normalizeString(createForm.orgName)
  const contactPhone = normalizePhone(createForm.contactPhone)
  if (!orgName) {
    createError.value = '机构名称不能为空'
    return
  }
  if (!contactPhone || !/^1\d{10}$/.test(contactPhone)) {
    createError.value = '联系人手机号格式不正确'
    return
  }

  createSubmitting.value = true
  try {
    const res = await createOrganization({
      orgName,
      username: normalizeString(createForm.username),
      initialPassword: normalizeString(createForm.initialPassword),
      contactName: normalizeString(createForm.contactName),
      contactPhone,
      address: normalizeString(createForm.address),
      intro: normalizeString(createForm.intro),
      licenseNo: normalizeString(createForm.licenseNo),
      splitPlatformPercent: Number.isFinite(createForm.splitPlatformPercent) ? Number(createForm.splitPlatformPercent) : null,
      splitOrgPercent: Number.isFinite(createForm.splitOrgPercent) ? Number(createForm.splitOrgPercent) : null,
    })
    lastCreated.value = res
    hint.value = '创建成功，请线下发放账号与初始密码'
    createOpen.value = false
    page.value = 1
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '创建失败'
  } finally {
    createSubmitting.value = false
  }
}

const editOpen = ref(false)
const editSubmitting = ref(false)
const editError = ref<string | null>(null)
const editId = ref<number | null>(null)

const editForm = reactive({
  orgName: '',
  username: '',
  initialPassword: '',
  accountStatus: 1,
  contactName: '',
  contactPhone: '',
  address: '',
  intro: '',
  licenseNo: '',
  splitPlatformPercent: 50,
  splitOrgPercent: 50,
})

function fillEditForm(detail: AdminOrganizationDetail) {
  editForm.orgName = detail.orgName || ''
  editForm.username = detail.username || ''
  editForm.initialPassword = ''
  editForm.accountStatus = detail.accountStatus == null ? 1 : Number(detail.accountStatus)
  editForm.contactName = detail.contactName || ''
  editForm.contactPhone = detail.contactPhone || ''
  editForm.address = detail.address || ''
  editForm.intro = detail.intro || ''
  editForm.licenseNo = detail.licenseNo || ''
  editForm.splitPlatformPercent = detail.splitPlatformPercent == null ? 50 : Number(detail.splitPlatformPercent)
  editForm.splitOrgPercent = detail.splitOrgPercent == null ? 50 : Number(detail.splitOrgPercent)
}

async function openEdit(id: number) {
  if (!id || busyId.value != null) return
  busyId.value = id
  editError.value = null
  errorText.value = null
  hint.value = null
  try {
    const detail = await getOrganizationDetail(id)
    editId.value = id
    fillEditForm(detail)
    editOpen.value = true
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    busyId.value = null
  }
}

function closeEdit() {
  editOpen.value = false
  editError.value = null
  editId.value = null
}

async function submitEdit() {
  const id = editId.value
  if (!id || editSubmitting.value) return
  editError.value = null
  errorText.value = null
  hint.value = null

  const orgName = normalizeString(editForm.orgName)
  const contactPhone = normalizePhone(editForm.contactPhone)
  if (!orgName) {
    editError.value = '机构名称不能为空'
    return
  }
  if (!contactPhone || !/^1\d{10}$/.test(contactPhone)) {
    editError.value = '联系人手机号格式不正确'
    return
  }
  const pwd = normalizeString(editForm.initialPassword)
  if (pwd && pwd.length < 8) {
    editError.value = '重置密码至少 8 位'
    return
  }

  editSubmitting.value = true
  try {
    await updateOrganization(id, {
      orgName,
      username: normalizeString(editForm.username),
      initialPassword: pwd,
      accountStatus: Number.isFinite(editForm.accountStatus) ? Number(editForm.accountStatus) : null,
      contactName: normalizeString(editForm.contactName),
      contactPhone,
      address: normalizeString(editForm.address),
      intro: normalizeString(editForm.intro),
      licenseNo: normalizeString(editForm.licenseNo),
      splitPlatformPercent: Number.isFinite(editForm.splitPlatformPercent) ? Number(editForm.splitPlatformPercent) : null,
      splitOrgPercent: Number.isFinite(editForm.splitOrgPercent) ? Number(editForm.splitOrgPercent) : null,
    })
    hint.value = '保存成功'
    editOpen.value = false
    await load()
  } catch (e) {
    editError.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '保存失败'
  } finally {
    editSubmitting.value = false
  }
}

async function onDisable(id: number) {
  if (!id || busyId.value != null) return
  if (!window.confirm('确认删除该机构账号？（将禁用登录并拉黑机构）')) return
  busyId.value = id
  errorText.value = null
  hint.value = null
  try {
    await disableOrganization(id)
    hint.value = '已删除（已禁用）'
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '删除失败'
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.left {
  display: grid;
  gap: 6px;
}

.title {
  font-weight: 700;
  font-size: 16px;
}

.sub {
  color: var(--muted);
  font-size: 12px;
}

.right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search {
  width: 280px;
}

.table-wrap {
  margin-top: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  overflow: hidden;
}

.cell-title {
  font-weight: 700;
}

.cell-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.empty {
  padding: 18px 12px;
  color: var(--muted);
  text-align: center;
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pager-text {
  color: var(--muted);
  font-size: 12px;
}

.size {
  width: 120px;
}

.form {
  display: grid;
  gap: 10px;
}

.row {
  display: grid;
  grid-template-columns: 120px 1fr;
  align-items: center;
  gap: 10px;
}

.label {
  color: var(--muted);
  font-size: 12px;
}

.textarea {
  min-height: 80px;
}

.hint {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: #fff;
}

.hint.ok {
  border-color: rgba(0, 190, 189, 0.28);
  background: rgba(0, 190, 189, 0.08);
}

.hint.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
}

.inner {
  margin-top: 12px;
  padding: 12px;
}

.inner-title {
  font-weight: 700;
  margin-bottom: 10px;
}

.kv {
  display: grid;
  gap: 8px;
}

.kv-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 10px;
}

.k {
  color: var(--muted);
  font-size: 12px;
}

.v {
  font-weight: 600;
}

.badge.danger {
  border-color: rgba(239, 68, 68, 0.3);
  color: var(--danger);
}
</style>
