<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">教师认证审核</div>
        <div class="sub">集中处理实名认证与学信网认证，建议先进入详情页查看材料后再操作</div>
      </div>
      <div class="right">
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div class="summary-strip">
      <div class="summary-card">
        <div class="summary-label">当前页待审核教师</div>
        <div class="summary-value">{{ rows.length }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">实名认证待审</div>
        <div class="summary-value">{{ pendingRealnameCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">学信网待审</div>
        <div class="summary-value">{{ pendingEduCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">总待审核记录</div>
        <div class="summary-value">{{ total }}</div>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 110px">用户ID</th>
            <th style="width: 250px">教师信息</th>
            <th style="width: 240px">待审核项</th>
            <th style="width: 180px">提交时间</th>
            <th>材料概览</th>
            <th style="width: 280px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.userId">
            <td>
              <RouterLink class="link" :to="`/verification/${row.userId}`">{{ row.userId }}</RouterLink>
            </td>
            <td>
              <div class="cell-title">{{ row.realName || `教师 #${row.userId}` }}</div>
              <div class="cell-sub">{{ [row.education, row.highestEduSchool, row.subject, row.city].filter(Boolean).join(' · ') || '资料待补充' }}</div>
            </td>
            <td>
              <div class="status-list">
                <div class="status-item">
                  <span class="status-label">实名认证</span>
                  <span class="status-pill" :class="statusToneClass(row.realnameVerifyStatus)">{{ statusText(row.realnameVerifyStatus) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">学信网</span>
                  <span class="status-pill" :class="statusToneClass(row.eduVerifyStatus)">{{ statusText(row.eduVerifyStatus) }}</span>
                </div>
              </div>
            </td>
            <td>
              <div class="cell-title">{{ latestSubmitTime(row) }}</div>
              <div class="cell-sub">{{ pendingItems(row).join('、') || '无待审核项' }}</div>
            </td>
            <td>
              <div class="material-list">
                <div class="material-item">身份证：{{ realnameProofSummary(row) }}</div>
                <div class="material-item">学信网：{{ eduProofSummary(row) }}</div>
              </div>
            </td>
            <td>
              <div class="actions">
                <RouterLink class="btn btn-muted" :to="`/verification/${row.userId}`">
                  查看材料
                </RouterLink>
                <button
                  v-if="row.realnameVerifyStatus === 1"
                  class="btn btn-primary"
                  type="button"
                  :disabled="busyKey === `REALNAME_${row.userId}` || !hasRealnameProof(row)"
                  @click="onApprove(row.userId, 'REALNAME')"
                >
                  通过实名
                </button>
                <button
                  v-if="row.realnameVerifyStatus === 1"
                  class="btn btn-danger"
                  type="button"
                  :disabled="busyKey === `REALNAME_${row.userId}`"
                  @click="openReject(row.userId, 'REALNAME')"
                >
                  驳回实名
                </button>

                <button
                  v-if="row.eduVerifyStatus === 1"
                  class="btn btn-primary"
                  type="button"
                  :disabled="busyKey === `EDU_${row.userId}` || eduProofCount(row) === 0"
                  @click="onApprove(row.userId, 'EDU')"
                >
                  通过学信
                </button>
                <button
                  v-if="row.eduVerifyStatus === 1"
                  class="btn btn-danger"
                  type="button"
                  :disabled="busyKey === `EDU_${row.userId}`"
                  @click="openReject(row.userId, 'EDU')"
                >
                  驳回学信
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="6">
              <div class="empty">暂无待审核认证</div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <button class="btn btn-muted" type="button" :disabled="page <= 1 || loading" @click="page -= 1">
        上一页
      </button>
      <div class="pager-text">第 {{ page }} 页 / 共 {{ totalPages }} 页（{{ total }} 条）</div>
      <button class="btn btn-muted" type="button" :disabled="page >= totalPages || loading" @click="page += 1">
        下一页
      </button>
      <select v-model.number="size" class="input size">
        <option :value="10">10 / 页</option>
        <option :value="20">20 / 页</option>
        <option :value="50">50 / 页</option>
      </select>
    </div>
  </div>

  <DialogModal v-if="rejectOpen" title="拒绝认证" @close="closeReject">
    <div class="modal-body">
      <div class="label-text">拒绝原因</div>
      <textarea v-model="rejectReason" class="input textarea" rows="4" placeholder="请输入拒绝原因，并明确需要补充的材料" />
      <div v-if="rejectError" class="error">{{ rejectError }}</div>
    </div>
    <template #actions>
      <button class="btn btn-muted" type="button" @click="closeReject">取消</button>
      <button class="btn btn-danger" type="button" :disabled="rejectSubmitting" @click="submitReject">
        {{ rejectSubmitting ? '提交中...' : '确认拒绝' }}
      </button>
    </template>
  </DialogModal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { approveVerification, listPendingVerifications, rejectVerification } from '@/api/verification'
import type { TeacherProfile } from '@/api/types'
import type { VerificationType } from '@/api/verification'
import DialogModal from '@/ui/DialogModal.vue'

const rows = ref<TeacherProfile[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)
const busyKey = ref<string | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const pendingRealnameCount = computed(() => rows.value.filter((row) => row.realnameVerifyStatus === 1).length)
const pendingEduCount = computed(() => rows.value.filter((row) => row.eduVerifyStatus === 1).length)

function statusText(v?: number | null) {
  if (v == null) return '-'
  if (v === 0) return '未提交'
  if (v === 1) return '待审核'
  if (v === 2) return '已通过'
  if (v === 3) return '已拒绝'
  return String(v)
}

function statusToneClass(v?: number | null) {
  if (v === 1) return 'tone-pending'
  if (v === 2) return 'tone-success'
  if (v === 3) return 'tone-danger'
  return 'tone-default'
}

function timeText(s?: string | null) {
  if (!s) return '-'
  return String(s).replace('T', ' ').slice(0, 19)
}

function parseJsonStringArray(raw?: string | null): string[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw) as unknown
    if (Array.isArray(arr)) return arr.map((x) => String(x)).filter((x) => x.trim().length > 0)
  } catch {
    void 0
  }
  const s = raw.trim()
  if (!s) return []
  if (s.includes(',')) return s.split(',').map((x) => x.trim()).filter(Boolean)
  return [s]
}

function hasRealnameProof(row: TeacherProfile) {
  return !!row.realnameVerifyIdFrontUrl && !!row.realnameVerifyIdBackUrl
}

function eduProofCount(row: TeacherProfile) {
  return parseJsonStringArray(row.eduVerifyProofUrls).length
}

function pendingItems(row: TeacherProfile) {
  const items: string[] = []
  if (row.realnameVerifyStatus === 1) items.push('实名认证')
  if (row.eduVerifyStatus === 1) items.push('学信网认证')
  return items
}

function latestSubmitTime(row: TeacherProfile) {
  const times = [row.realnameVerifySubmitTime, row.eduVerifySubmitTime].filter(Boolean).map((it) => String(it))
  if (times.length === 0) return '-'
  const sorted = times.sort()
  return timeText(sorted[sorted.length - 1])
}

function realnameProofSummary(row: TeacherProfile) {
  return hasRealnameProof(row) ? '正反面齐全' : '材料不完整'
}

function eduProofSummary(row: TeacherProfile) {
  const count = eduProofCount(row)
  return count > 0 ? `${count} 张截图` : '暂无截图'
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    const res = await listPendingVerifications(page.value, size.value)
    rows.value = res.records
    total.value = Number(res.total || 0)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onApprove(userId: number, type: VerificationType) {
  if (busyKey.value) return
  busyKey.value = `${type}_${userId}`
  try {
    await approveVerification({ userId, type })
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '操作失败'
  } finally {
    busyKey.value = null
  }
}

const rejectOpen = ref(false)
const rejectUserId = ref<number | null>(null)
const rejectType = ref<VerificationType>('REALNAME')
const rejectReason = ref('')
const rejectSubmitting = ref(false)
const rejectError = ref<string | null>(null)

function openReject(userId: number, type: VerificationType) {
  rejectOpen.value = true
  rejectUserId.value = userId
  rejectType.value = type
  rejectReason.value = ''
  rejectSubmitting.value = false
  rejectError.value = null
}

function closeReject() {
  rejectOpen.value = false
  rejectUserId.value = null
}

async function submitReject() {
  if (rejectSubmitting.value) return
  const userId = rejectUserId.value
  if (userId == null) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    rejectError.value = '请输入拒绝原因'
    return
  }
  rejectSubmitting.value = true
  rejectError.value = null
  try {
    await rejectVerification({ userId, type: rejectType.value, reason })
    closeReject()
    await load()
  } catch (e) {
    rejectError.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '提交失败'
  } finally {
    rejectSubmitting.value = false
  }
}

watch([page, size], () => {
  if (page.value > totalPages.value) page.value = totalPages.value
  load()
})

onMounted(load)
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

.title {
  font-weight: 800;
}

.sub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.summary-strip {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.summary-card {
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.96));
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.summary-label {
  color: var(--muted);
  font-size: 12px;
}

.summary-value {
  margin-top: 6px;
  font-weight: 800;
  font-size: 18px;
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
  line-height: 1.5;
}

.status-list,
.material-list {
  display: grid;
  gap: 8px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.status-label,
.material-item {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid transparent;
}

.tone-default {
  color: rgba(15, 23, 42, 0.7);
  background: rgba(15, 23, 42, 0.05);
  border-color: rgba(15, 23, 42, 0.08);
}

.tone-pending {
  color: #b45309;
  background: rgba(255, 125, 0, 0.12);
  border-color: rgba(255, 125, 0, 0.18);
}

.tone-success {
  color: #047857;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.16);
}

.tone-danger {
  color: #be123c;
  background: rgba(244, 63, 94, 0.08);
  border-color: rgba(244, 63, 94, 0.14);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.textarea {
  width: 100%;
  resize: vertical;
  min-height: 90px;
}

.label-text {
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.link {
  color: rgba(0, 190, 189, 1);
}

@media (max-width: 1100px) {
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-strip {
    grid-template-columns: 1fr;
  }
}
</style>
