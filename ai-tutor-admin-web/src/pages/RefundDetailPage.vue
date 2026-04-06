<template>
  <div class="card box">
    <div class="head">
      <div class="title">纠纷详情</div>
      <div class="right">
        <button class="btn btn-muted" type="button" @click="goBack">返回</button>
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div v-if="detail" class="grid">
      <div class="card section">
        <div class="section-head">
          <div class="section-title">申请信息</div>
          <span class="badge">{{ detail.refundRequest.status || '-' }}</span>
        </div>

        <div class="kv">
          <div class="k">申请ID</div>
          <div class="v">{{ detail.refundRequest.id }}</div>
        </div>
        <div class="kv">
          <div class="k">类型</div>
          <div class="v">{{ detail.refundRequest.type }}</div>
        </div>
        <div class="kv">
          <div class="k">会话ID</div>
          <div class="v">{{ detail.refundRequest.roomId ?? '-' }}</div>
        </div>
        <div class="kv">
          <div class="k">订单ID</div>
          <div class="v">{{ detail.refundRequest.brokerageOrderId }}</div>
        </div>
        <div class="kv">
          <div class="k">退款比例</div>
          <div class="v">{{ detail.refundRequest.refundPercent }}%</div>
        </div>
        <div class="kv">
          <div class="k">退款金额(分)</div>
          <div class="v">{{ detail.refundRequest.refundAmountFen }}</div>
        </div>
        <div class="kv">
          <div class="k">申请说明</div>
          <div class="v">{{ detail.refundRequest.reason || '-' }}</div>
        </div>

        <div v-if="evidenceUrls.length > 0" class="img-wrap">
          <div class="imgs">
            <a v-for="u in evidenceUrls" :key="u" class="img-link" :href="u" target="_blank">
              <img class="img" :src="u" alt="evidence" />
            </a>
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-primary" type="button" :disabled="busyAction" @click="onApprove">
            同意退款
          </button>
          <button class="btn btn-danger" type="button" :disabled="busyAction" @click="openReject">
            拒绝退款
          </button>
        </div>
      </div>

      <div class="card section chat">
        <div class="section-title">聊天记录</div>

        <div v-if="!detail.chatHistory || detail.chatHistory.length === 0" class="empty">暂无聊天记录</div>
        <div v-else class="msgs">
          <div v-for="m in detail.chatHistory" :key="m.id" class="msg">
            <div class="meta">
              <span class="badge">from {{ m.fromUid }} → {{ m.toUid }}</span>
              <span class="time">{{ timeText(m.createTime) }}</span>
            </div>
            <div class="content">{{ m.content || '-' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <DialogModal v-if="rejectOpen" title="拒绝退款" @close="closeReject">
    <div class="label-text">拒绝原因</div>
    <textarea v-model="rejectReason" class="input textarea" rows="4" placeholder="请输入拒绝原因" />
    <div v-if="rejectError" class="error">{{ rejectError }}</div>
    <template #actions>
      <button class="btn btn-muted" type="button" @click="closeReject">取消</button>
      <button class="btn btn-danger" type="button" :disabled="rejectSubmitting" @click="submitReject">
        {{ rejectSubmitting ? '提交中...' : '确认拒绝' }}
      </button>
    </template>
  </DialogModal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { approveRefundRequest, getRefundRequestDetails, rejectRefundRequest } from '@/api/refunds'
import type { RefundRequestDetailResponse } from '@/api/types'
import DialogModal from '@/ui/DialogModal.vue'

const route = useRoute()
const router = useRouter()

const requestId = computed(() => Number(route.params.requestId))

const detail = ref<RefundRequestDetailResponse | null>(null)
const loading = ref(false)
const errorText = ref<string | null>(null)
const busyAction = ref(false)

function timeText(s?: string | null) {
  if (!s) return '-'
  return String(s).replace('T', ' ').slice(0, 19)
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    detail.value = await getRefundRequestDetails(requestId.value)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

async function onApprove() {
  if (busyAction.value) return
  busyAction.value = true
  try {
    await approveRefundRequest(requestId.value)
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '操作失败'
  } finally {
    busyAction.value = false
  }
}

const rejectOpen = ref(false)
const rejectReason = ref('')
const rejectSubmitting = ref(false)
const rejectError = ref<string | null>(null)

function openReject() {
  rejectOpen.value = true
  rejectReason.value = ''
  rejectSubmitting.value = false
  rejectError.value = null
}

function closeReject() {
  rejectOpen.value = false
}

async function submitReject() {
  if (rejectSubmitting.value) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    rejectError.value = '请输入拒绝原因'
    return
  }
  rejectSubmitting.value = true
  rejectError.value = null
  try {
    await rejectRefundRequest(requestId.value, { reason })
    closeReject()
    await load()
  } catch (e) {
    rejectError.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '提交失败'
  } finally {
    rejectSubmitting.value = false
  }
}

const evidenceUrls = computed<string[]>(() => {
  const raw = detail.value?.refundRequest?.evidenceImagesJson
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return parsed.map((v) => String(v)).filter((v) => v && v !== 'null' && v !== 'undefined')
    }
  } catch {
  }
  return []
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
  align-items: center;
  gap: 10px;
}

.title {
  font-weight: 800;
}

.right {
  display: flex;
  gap: 10px;
}

.error {
  margin-top: 10px;
  color: var(--danger);
  font-size: 13px;
}

.grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 12px;
}

.section {
  padding: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.section-title {
  font-weight: 700;
}

.kv {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.k {
  color: var(--muted);
  font-size: 12px;
}

.v {
  font-size: 13px;
  word-break: break-word;
}

.imgs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.img-link {
  display: block;
}

.img {
  width: 100%;
  height: 110px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.img-wrap {
  margin-top: 12px;
}

.img {
  width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.1);
}

.chat {
  display: flex;
  flex-direction: column;
}

.msgs {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.7);
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.time {
  color: var(--muted);
  font-size: 12px;
}

.content {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
}

.empty {
  margin-top: 12px;
  color: var(--muted);
  font-size: 12px;
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
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
