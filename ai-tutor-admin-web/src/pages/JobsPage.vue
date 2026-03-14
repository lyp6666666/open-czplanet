<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">待审核需求</div>
        <div class="sub">状态：0 待审核，1 通过，2 拒绝</div>
      </div>
      <div class="right">
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 90px">ID</th>
            <th>标题</th>
            <th style="width: 140px">城市</th>
            <th style="width: 160px">预算/小时</th>
            <th style="width: 160px">发布时间</th>
            <th style="width: 220px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <div class="cell-title">{{ row.title || '-' }}</div>
              <div class="cell-sub">{{ row.description || '-' }}</div>
            </td>
            <td>{{ row.city || '-' }}</td>
            <td>
              <span class="badge">{{ budgetText(row) }}</span>
            </td>
            <td>{{ timeText(row.createTime) }}</td>
            <td>
              <div class="actions">
                <button class="btn btn-primary" type="button" :disabled="busyId === row.id" @click="onApprove(row.id)">
                  通过
                </button>
                <button class="btn btn-danger" type="button" :disabled="busyId === row.id" @click="openReject(row.id)">
                  拒绝
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="6">
              <div class="empty">暂无待审核需求</div>
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

  <DialogModal v-if="rejectOpen" title="拒绝需求" @close="closeReject">
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

import { approveJob, listPendingJobs, rejectJob } from '@/api/jobs'
import type { StudentJobPosting } from '@/api/types'
import DialogModal from '@/ui/DialogModal.vue'

const rows = ref<StudentJobPosting[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)
const busyId = ref<number | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function timeText(s?: string | null) {
  if (!s) return '-'
  return String(s).replace('T', ' ').slice(0, 19)
}

function budgetText(row: StudentJobPosting) {
  const min = row.budgetMin ?? null
  const max = row.budgetMax ?? null
  if (min == null && max == null) return '-'
  if (min != null && max != null) return `${min} ~ ${max}`
  return String(min ?? max)
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    const res = await listPendingJobs(page.value, size.value)
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
    await approveJob(id)
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
    await rejectJob({ id, reason })
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
  max-width: 620px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.actions {
  display: flex;
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
</style>

