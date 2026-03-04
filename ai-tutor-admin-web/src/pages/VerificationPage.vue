<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">教师认证审核</div>
        <div class="sub">实名与学历审核分别处理，支持跳转查看证据材料</div>
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
            <th style="width: 120px">用户ID</th>
            <th style="width: 180px">实名状态</th>
            <th style="width: 180px">学历状态</th>
            <th>基础信息</th>
            <th style="width: 260px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.userId">
            <td>
              <RouterLink class="link" :to="`/verification/${row.userId}`">{{ row.userId }}</RouterLink>
            </td>
            <td>
              <span class="badge">{{ statusText(row.realnameVerifyStatus) }}</span>
            </td>
            <td>
              <span class="badge">{{ statusText(row.eduVerifyStatus) }}</span>
            </td>
            <td>
              <div class="cell-title">{{ row.realName || '-' }}</div>
              <div class="cell-sub">{{ [row.education, row.subject, row.city].filter(Boolean).join(' · ') || '-' }}</div>
            </td>
            <td>
              <div class="actions">
                <button
                  v-if="row.realnameVerifyStatus === 1"
                  class="btn btn-primary"
                  type="button"
                  :disabled="busyKey === `REALNAME_${row.userId}`"
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
                  拒绝实名
                </button>

                <button
                  v-if="row.eduVerifyStatus === 1"
                  class="btn btn-primary"
                  type="button"
                  :disabled="busyKey === `EDU_${row.userId}`"
                  @click="onApprove(row.userId, 'EDU')"
                >
                  通过学历
                </button>
                <button
                  v-if="row.eduVerifyStatus === 1"
                  class="btn btn-danger"
                  type="button"
                  :disabled="busyKey === `EDU_${row.userId}`"
                  @click="openReject(row.userId, 'EDU')"
                >
                  拒绝学历
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="5">
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

function statusText(v?: number | null) {
  if (v == null) return '-'
  if (v === 0) return '未提交'
  if (v === 1) return '待审核'
  if (v === 2) return '已通过'
  if (v === 3) return '已拒绝'
  return String(v)
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
</style>

