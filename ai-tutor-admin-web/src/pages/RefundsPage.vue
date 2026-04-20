<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">退款申请处理</div>
        <div class="sub">查看退款申请、聊天记录与证据，支持同意/拒绝退款</div>
      </div>
      <div class="right">
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div class="filters">
      <select v-model="typeFilter" class="input filter">
        <option value="">全部类型</option>
        <option value="CHAT_INFO_FEE">沟通退费</option>
        <option value="TRIAL_INFO_FEE">试课退费</option>
      </select>
      <select v-model="statusFilter" class="input filter">
        <option value="">全部状态</option>
        <option value="PENDING">待审核</option>
        <option value="APPROVED">已同意</option>
        <option value="REJECTED">已拒绝</option>
      </select>
    </div>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 90px">申请ID</th>
            <th style="width: 120px">类型</th>
            <th style="width: 120px">状态</th>
            <th style="width: 100px">申请方</th>
            <th style="width: 140px">金额(分)</th>
            <th style="width: 120px">会话ID</th>
            <th style="width: 120px">录屏状态</th>
            <th>说明</th>
            <th style="width: 220px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <RouterLink class="link" :to="`/refunds/${row.id}`">{{ row.id }}</RouterLink>
            </td>
            <td><span class="badge">{{ refundTypeText(row.type) }}</span></td>
            <td><span class="badge">{{ refundStatusText(row.status) }}</span></td>
            <td>{{ applicantRoleText(row.applicantRole) }}</td>
            <td>{{ row.refundAmountFen ?? '-' }}</td>
            <td>{{ row.roomId ?? '-' }}</td>
            <td>{{ videoDeleteStatusText(row.evidenceVideoDeleteStatus) }}</td>
            <td>
              <div class="cell-title">{{ row.reason || '-' }}</div>
              <div class="cell-sub">{{ row.createTime ? String(row.createTime).replace('T', ' ').slice(0, 19) : '-' }}</div>
            </td>
            <td>
              <div class="actions">
                <RouterLink class="btn btn-muted" :to="`/refunds/${row.id}`">
                  查看聊天记录
                </RouterLink>
                <button class="btn btn-primary" type="button" :disabled="busyId === row.id" @click="onApprove(row.id)">
                  同意退款
                </button>
                <button class="btn btn-danger" type="button" :disabled="busyId === row.id" @click="openReject(row.id)">
                  拒绝退款
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="9">
              <div class="empty">暂无退款申请</div>
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

  <DialogModal v-if="rejectOpen" title="拒绝退款" @close="closeReject">
    <div class="modal-body">
      <div class="label-text">拒绝原因</div>
      <textarea v-model="rejectReason" class="input textarea" rows="4" placeholder="请输入拒绝原因" />
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

import { approveRefundRequest, listRefundRequests, rejectRefundRequest } from '@/api/refunds'
import type { RefundRequestRecord } from '@/api/types'
import DialogModal from '@/ui/DialogModal.vue'
import { refundStatusText, refundTypeText } from '@/utils/refunds'

const rows = ref<RefundRequestRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)
const busyId = ref<number | null>(null)
const typeFilter = ref('')
const statusFilter = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    const res = await listRefundRequests({ page: page.value, size: size.value, type: typeFilter.value || undefined, status: statusFilter.value || undefined })
    rows.value = res.records
    total.value = Number(res.total || 0)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onApprove(id: number) {
  if (busyId.value != null) return
  busyId.value = id
  try {
    await approveRefundRequest(id)
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '操作失败'
  } finally {
    busyId.value = null
  }
}

const rejectOpen = ref(false)
const rejectId = ref<number | null>(null)
const rejectReason = ref('')
const rejectSubmitting = ref(false)
const rejectError = ref<string | null>(null)

function openReject(id: number) {
  rejectOpen.value = true
  rejectId.value = id
  rejectReason.value = ''
  rejectSubmitting.value = false
  rejectError.value = null
}

function closeReject() {
  rejectOpen.value = false
  rejectId.value = null
}

async function submitReject() {
  if (rejectSubmitting.value) return
  const id = rejectId.value
  if (id == null) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    rejectError.value = '请输入拒绝原因'
    return
  }
  rejectSubmitting.value = true
  rejectError.value = null
  try {
    await rejectRefundRequest(id, { reason })
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

watch([typeFilter, statusFilter], () => {
  page.value = 1
  load()
})

onMounted(load)

function applicantRoleText(role?: string | null) {
  return role === 'TEACHER' ? '教师' : role === 'STUDENT' ? '学生' : '-'
}

function videoDeleteStatusText(status?: string | null) {
  const normalized = String(status || '').trim().toUpperCase()
  if (normalized === 'PENDING_DELETE') return '待删'
  if (normalized === 'DELETED') return '已删除'
  if (normalized === 'KEEP') return '保留'
  return '-'
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

.title {
  font-weight: 800;
}

.sub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.error {
  margin-top: 10px;
  color: var(--danger);
  font-size: 13px;
}

.filters {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter {
  min-width: 140px;
}

.table-wrap {
  margin-top: 12px;
  overflow: auto;
}

.cell-title {
  font-weight: 600;
}

.cell-sub {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}

.cell-link {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  color: rgba(0, 190, 189, 1);
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
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
</style>
