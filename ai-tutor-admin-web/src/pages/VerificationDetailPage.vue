<template>
  <div class="card box">
    <div class="head">
      <div class="title">认证详情</div>
      <div class="right">
        <button class="btn btn-muted" type="button" @click="goBack">返回</button>
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div v-if="profile" class="grid">
      <div class="card section">
        <div class="section-title">基础信息</div>
        <div class="kv">
          <div class="k">用户ID</div>
          <div class="v">{{ profile.userId }}</div>
        </div>
        <div class="kv">
          <div class="k">姓名</div>
          <div class="v">{{ profile.realName || '-' }}</div>
        </div>
        <div class="kv">
          <div class="k">学历</div>
          <div class="v">{{ profile.education || '-' }}</div>
        </div>
        <div class="kv">
          <div class="k">毕业院校</div>
          <div class="v">{{ profile.highestEduSchool || '-' }}</div>
        </div>
        <div class="kv">
          <div class="k">科目</div>
          <div class="v">{{ profile.subject || '-' }}</div>
        </div>
        <div class="kv">
          <div class="k">城市</div>
          <div class="v">{{ profile.city || '-' }}</div>
        </div>
      </div>

      <div class="card section">
        <div class="section-head">
          <div class="section-title">实名认证</div>
          <span class="badge">{{ statusText(profile.realnameVerifyStatus) }}</span>
        </div>
        <div class="kv">
          <div class="k">证件号（脱敏）</div>
          <div class="v">{{ profile.realnameVerifyIdnoMasked || '-' }}</div>
        </div>
        <div class="kv">
          <div class="k">提交时间</div>
          <div class="v">{{ timeText(profile.realnameVerifySubmitTime) }}</div>
        </div>
        <div class="kv">
          <div class="k">审核时间</div>
          <div class="v">{{ timeText(profile.realnameVerifyTime) }}</div>
        </div>
        <div class="kv" v-if="profile.realnameVerifyRejectReason">
          <div class="k">拒绝原因</div>
          <div class="v danger">{{ profile.realnameVerifyRejectReason }}</div>
        </div>

        <div class="proofs">
          <div class="proof" v-if="profile.realnameVerifyIdFrontUrl">
            <div class="proof-title">身份证正面</div>
            <a :href="profile.realnameVerifyIdFrontUrl" target="_blank" class="proof-link">打开链接</a>
            <img class="img" :src="profile.realnameVerifyIdFrontUrl" alt="id-front" />
          </div>
          <div class="proof" v-if="profile.realnameVerifyIdBackUrl">
            <div class="proof-title">身份证反面</div>
            <a :href="profile.realnameVerifyIdBackUrl" target="_blank" class="proof-link">打开链接</a>
            <img class="img" :src="profile.realnameVerifyIdBackUrl" alt="id-back" />
          </div>
        </div>

        <div class="actions" v-if="profile.realnameVerifyStatus === 1">
          <button class="btn btn-primary" type="button" :disabled="busyKey === 'REALNAME'" @click="onApprove('REALNAME')">
            通过实名
          </button>
          <button class="btn btn-danger" type="button" :disabled="busyKey === 'REALNAME'" @click="openReject('REALNAME')">
            拒绝实名
          </button>
        </div>
      </div>

      <div class="card section">
        <div class="section-head">
          <div class="section-title">学历认证</div>
          <span class="badge">{{ statusText(profile.eduVerifyStatus) }}</span>
        </div>
        <div class="kv">
          <div class="k">提交时间</div>
          <div class="v">{{ timeText(profile.eduVerifySubmitTime) }}</div>
        </div>
        <div class="kv">
          <div class="k">审核时间</div>
          <div class="v">{{ timeText(profile.eduVerifyTime) }}</div>
        </div>
        <div class="kv" v-if="profile.eduVerifyRejectReason">
          <div class="k">拒绝原因</div>
          <div class="v danger">{{ profile.eduVerifyRejectReason }}</div>
        </div>

        <div class="proofs">
          <div v-for="u in eduUrls" :key="u" class="proof">
            <div class="proof-title">学历材料</div>
            <a :href="u" target="_blank" class="proof-link">打开链接</a>
            <img class="img" :src="u" alt="edu-proof" />
          </div>
          <div v-if="eduUrls.length === 0" class="empty">暂无学历材料</div>
        </div>

        <div class="actions" v-if="profile.eduVerifyStatus === 1">
          <button class="btn btn-primary" type="button" :disabled="busyKey === 'EDU'" @click="onApprove('EDU')">
            通过学历
          </button>
          <button class="btn btn-danger" type="button" :disabled="busyKey === 'EDU'" @click="openReject('EDU')">
            拒绝学历
          </button>
        </div>
      </div>
    </div>
  </div>

  <DialogModal v-if="rejectOpen" title="拒绝认证" @close="closeReject">
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

import { approveVerification, getVerificationDetails, rejectVerification } from '@/api/verification'
import type { TeacherProfile } from '@/api/types'
import type { VerificationType } from '@/api/verification'
import DialogModal from '@/ui/DialogModal.vue'

const route = useRoute()
const router = useRouter()

const profile = ref<TeacherProfile | null>(null)
const loading = ref(false)
const errorText = ref<string | null>(null)
const busyKey = ref<VerificationType | null>(null)

const userId = computed(() => Number(route.params.userId))

function timeText(s?: string | null) {
  if (!s) return '-'
  return String(s).replace('T', ' ').slice(0, 19)
}

function statusText(v?: number | null) {
  if (v == null) return '-'
  if (v === 0) return '未提交'
  if (v === 1) return '待审核'
  if (v === 2) return '已通过'
  if (v === 3) return '已拒绝'
  return String(v)
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

const eduUrls = computed(() => parseJsonStringArray(profile.value?.eduVerifyProofUrls))

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    profile.value = await getVerificationDetails(userId.value)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

async function onApprove(type: VerificationType) {
  if (busyKey.value) return
  busyKey.value = type
  try {
    await approveVerification({ userId: userId.value, type })
    await load()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '操作失败'
  } finally {
    busyKey.value = null
  }
}

const rejectOpen = ref(false)
const rejectType = ref<VerificationType>('REALNAME')
const rejectReason = ref('')
const rejectSubmitting = ref(false)
const rejectError = ref<string | null>(null)

function openReject(type: VerificationType) {
  rejectOpen.value = true
  rejectType.value = type
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
    await rejectVerification({ userId: userId.value, type: rejectType.value, reason })
    closeReject()
    await load()
  } catch (e) {
    rejectError.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '提交失败'
  } finally {
    rejectSubmitting.value = false
  }
}

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
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.danger {
  color: var(--danger);
}

.proofs {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.proof-title {
  font-weight: 600;
  font-size: 13px;
}

.proof-link {
  display: inline-block;
  margin-top: 4px;
  color: rgba(0, 190, 189, 1);
  font-size: 12px;
}

.img {
  margin-top: 8px;
  width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.1);
}

.actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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

.empty {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
