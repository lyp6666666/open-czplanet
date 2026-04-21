<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">认证详情</div>
        <div class="sub">按材料核验教师实名认证与学信网认证，确认无误后再做通过或驳回</div>
      </div>
      <div class="right">
        <button class="btn btn-muted" type="button" @click="goBack">返回</button>
        <button class="btn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div v-if="profile" class="layout">
      <div class="summary-col">
        <div class="card summary-card">
          <div class="section-title">教师信息</div>
          <div class="hero">
            <div class="hero-avatar">{{ profile.realName?.slice(0, 1) || '师' }}</div>
            <div class="hero-info">
              <div class="hero-name">{{ profile.realName || `教师 #${profile.userId}` }}</div>
              <div class="hero-sub">{{ [profile.education, profile.highestEduSchool, profile.subject, profile.city].filter(Boolean).join(' · ') || '资料待补充' }}</div>
            </div>
          </div>

          <div class="metric-grid">
            <div class="metric">
              <div class="metric-label">用户ID</div>
              <div class="metric-value">{{ profile.userId }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">待审核项</div>
              <div class="metric-value">{{ pendingCount }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">实名材料</div>
              <div class="metric-value">{{ realnameProofReady ? '2 张' : '缺失' }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">学信材料</div>
              <div class="metric-value">{{ eduUrls.length }} 张</div>
            </div>
          </div>

          <div class="status-stack">
            <div class="status-row">
              <div class="status-label">实名认证</div>
              <span class="status-pill" :class="statusToneClass(profile.realnameVerifyStatus)">
                {{ statusText(profile.realnameVerifyStatus) }}
              </span>
            </div>
            <div class="status-row">
              <div class="status-label">学信网认证</div>
              <span class="status-pill" :class="statusToneClass(profile.eduVerifyStatus)">
                {{ statusText(profile.eduVerifyStatus) }}
              </span>
            </div>
          </div>
        </div>

        <div class="card guide-card">
          <div class="section-title">审核提示</div>
          <div class="guide-item">实名认证需核对身份证正反面是否齐全、清晰，且姓名与教师资料一致。</div>
          <div class="guide-item">学信网截图建议同时覆盖姓名、学校、学历层次与页面特征。</div>
          <div class="guide-item">若材料缺页、模糊、信息不一致，优先驳回并在原因中写明补充要求。</div>
        </div>
      </div>

      <div class="detail-col">
        <div class="card audit-card">
          <div class="section-head">
            <div>
              <div class="section-title">实名认证审核</div>
              <div class="section-sub">仅支持基于身份证图片人工审核</div>
            </div>
            <span class="status-pill" :class="statusToneClass(profile.realnameVerifyStatus)">
              {{ statusText(profile.realnameVerifyStatus) }}
            </span>
          </div>

          <div class="info-grid">
            <div class="kv">
              <div class="k">提交时间</div>
              <div class="v">{{ timeText(profile.realnameVerifySubmitTime) }}</div>
            </div>
            <div class="kv">
              <div class="k">审核时间</div>
              <div class="v">{{ timeText(profile.realnameVerifyTime) }}</div>
            </div>
            <div class="kv">
              <div class="k">审核方式</div>
              <div class="v">{{ realnameMethodText(profile.realnameVerifyMethod) }}</div>
            </div>
            <div class="kv">
              <div class="k">证件号</div>
              <div class="v">{{ profile.realnameVerifyIdnoMasked || '-' }}</div>
            </div>
          </div>

          <div v-if="profile.realnameVerifyRejectReason" class="audit-note danger">
            驳回原因：{{ profile.realnameVerifyRejectReason }}
          </div>
          <div v-if="!realnameProofReady" class="audit-note warn">
            当前缺少身份证正反面完整材料，如为历史无图提交，请驳回并要求老师改为图片上传。
          </div>

          <div class="proof-grid">
            <div class="proof-card">
              <div class="proof-head">
                <div class="proof-title">身份证人像面</div>
                <button v-if="realnameFrontUrl" class="link-btn" type="button" @click="previewImage(realnameFrontUrl, '身份证人像面')">
                  查看大图
                </button>
              </div>
              <button
                v-if="realnameFrontUrl"
                class="proof-button"
                type="button"
                @click="previewImage(realnameFrontUrl, '身份证人像面')"
              >
                <img class="proof-img" :src="realnameFrontUrl" alt="id-front" />
              </button>
              <div v-else class="proof-empty">暂无身份证人像面</div>
            </div>

            <div class="proof-card">
              <div class="proof-head">
                <div class="proof-title">身份证国徽面</div>
                <button v-if="realnameBackUrl" class="link-btn" type="button" @click="previewImage(realnameBackUrl, '身份证国徽面')">
                  查看大图
                </button>
              </div>
              <button
                v-if="realnameBackUrl"
                class="proof-button"
                type="button"
                @click="previewImage(realnameBackUrl, '身份证国徽面')"
              >
                <img class="proof-img" :src="realnameBackUrl" alt="id-back" />
              </button>
              <div v-else class="proof-empty">暂无身份证国徽面</div>
            </div>
          </div>

          <div class="actions" v-if="profile.realnameVerifyStatus === 1">
            <button class="btn btn-primary" type="button" :disabled="busyKey === 'REALNAME' || !realnameProofReady" @click="onApprove('REALNAME')">
              通过实名
            </button>
            <button class="btn btn-danger" type="button" :disabled="busyKey === 'REALNAME'" @click="openReject('REALNAME')">
              驳回实名
            </button>
          </div>
        </div>

        <div class="card audit-card">
          <div class="section-head">
            <div>
              <div class="section-title">学信网认证审核</div>
              <div class="section-sub">重点核验姓名、学校、学历层次与页面可信特征</div>
            </div>
            <span class="status-pill" :class="statusToneClass(profile.eduVerifyStatus)">
              {{ statusText(profile.eduVerifyStatus) }}
            </span>
          </div>

          <div class="info-grid">
            <div class="kv">
              <div class="k">提交时间</div>
              <div class="v">{{ timeText(profile.eduVerifySubmitTime) }}</div>
            </div>
            <div class="kv">
              <div class="k">审核时间</div>
              <div class="v">{{ timeText(profile.eduVerifyTime) }}</div>
            </div>
            <div class="kv">
              <div class="k">截图数量</div>
              <div class="v">{{ eduUrls.length }} 张</div>
            </div>
            <div class="kv">
              <div class="k">最高学历学校</div>
              <div class="v">{{ profile.highestEduSchool || '-' }}</div>
            </div>
          </div>

          <div v-if="profile.eduVerifyRejectReason" class="audit-note danger">
            驳回原因：{{ profile.eduVerifyRejectReason }}
          </div>
          <div v-if="eduUrls.length === 0" class="audit-note warn">
            当前未获取到学信网截图，无法完成人工审核。
          </div>

          <div v-if="eduUrls.length > 0" class="proof-grid proof-grid-edu">
            <div v-for="(url, index) in eduUrls" :key="url" class="proof-card">
              <div class="proof-head">
                <div class="proof-title">学信截图 {{ index + 1 }}</div>
                <button class="link-btn" type="button" @click="previewImage(url, `学信截图 ${index + 1}`)">查看大图</button>
              </div>
              <button class="proof-button" type="button" @click="previewImage(url, `学信截图 ${index + 1}`)">
                <img class="proof-img" :src="url" alt="edu-proof" />
              </button>
            </div>
          </div>
          <div v-else class="proof-empty large">暂无学信网截图</div>

          <div class="actions" v-if="profile.eduVerifyStatus === 1">
            <button class="btn btn-primary" type="button" :disabled="busyKey === 'EDU' || eduUrls.length === 0" @click="onApprove('EDU')">
              通过学信
            </button>
            <button class="btn btn-danger" type="button" :disabled="busyKey === 'EDU'" @click="openReject('EDU')">
              驳回学信
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <DialogModal v-if="rejectOpen" title="拒绝认证" @close="closeReject">
    <div class="label-text">拒绝原因</div>
    <textarea v-model="rejectReason" class="input textarea" rows="4" placeholder="请输入拒绝原因，并写清需要补充的材料" />
    <div v-if="rejectError" class="error">{{ rejectError }}</div>
    <template #actions>
      <button class="btn btn-muted" type="button" @click="closeReject">取消</button>
      <button class="btn btn-danger" type="button" :disabled="rejectSubmitting" @click="submitReject">
        {{ rejectSubmitting ? '提交中...' : '确认拒绝' }}
      </button>
    </template>
  </DialogModal>

  <DialogModal v-if="previewOpen" :title="previewTitle" @close="closePreview">
    <div class="preview-wrap">
      <img class="preview-img" :src="previewUrl" :alt="previewTitle" />
      <a class="link-btn" :href="previewUrl" target="_blank" rel="noreferrer">新窗口打开原图</a>
    </div>
    <template #actions>
      <button class="btn btn-muted" type="button" @click="closePreview">关闭</button>
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
import { normalizeAssetUrl } from '@/utils/assets'

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

function statusToneClass(v?: number | null) {
  if (v === 1) return 'tone-pending'
  if (v === 2) return 'tone-success'
  if (v === 3) return 'tone-danger'
  return 'tone-default'
}

function realnameMethodText(v?: string | null) {
  if (!v) return '-'
  if (String(v).toUpperCase() === 'ID_PHOTO') return '身份证图片'
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

const realnameFrontUrl = computed(() => normalizeAssetUrl(profile.value?.realnameVerifyIdFrontUrl))
const realnameBackUrl = computed(() => normalizeAssetUrl(profile.value?.realnameVerifyIdBackUrl))
const eduUrls = computed(() => parseJsonStringArray(profile.value?.eduVerifyProofUrls).map((item) => normalizeAssetUrl(item)).filter(Boolean))
const realnameProofReady = computed(() => !!realnameFrontUrl.value && !!realnameBackUrl.value)
const pendingCount = computed(() => {
  let count = 0
  if (profile.value?.realnameVerifyStatus === 1) count += 1
  if (profile.value?.eduVerifyStatus === 1) count += 1
  return count
})

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

const previewOpen = ref(false)
const previewUrl = ref('')
const previewTitle = ref('材料预览')

function previewImage(url: string, title: string) {
  previewUrl.value = normalizeAssetUrl(url)
  previewTitle.value = title
  previewOpen.value = true
}

function closePreview() {
  previewOpen.value = false
  previewUrl.value = ''
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

.right {
  display: flex;
  gap: 10px;
}

.error {
  margin-top: 10px;
  color: var(--danger);
  font-size: 13px;
}

.layout {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 12px;
}

.summary-col,
.detail-col {
  display: grid;
  gap: 12px;
  align-content: start;
}

.summary-card,
.guide-card,
.audit-card {
  padding: 14px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.section-title {
  font-weight: 800;
}

.section-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.hero {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.hero-avatar {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 20px;
  font-weight: 800;
  color: #92400e;
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.22), rgba(245, 158, 11, 0.12));
}

.hero-info {
  min-width: 0;
}

.hero-name {
  font-weight: 800;
  font-size: 16px;
}

.hero-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.metric-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric {
  border-radius: 14px;
  padding: 12px;
  background: rgba(247, 250, 252, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.metric-label {
  color: var(--muted);
  font-size: 12px;
}

.metric-value {
  margin-top: 6px;
  font-weight: 800;
}

.status-stack {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.status-label {
  font-size: 13px;
  font-weight: 700;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
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

.guide-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.95), rgba(248, 250, 252, 0.95));
  color: rgba(15, 23, 42, 0.72);
  font-size: 12px;
  line-height: 1.6;
}

.info-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.kv {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(247, 250, 252, 0.75);
}

.k {
  color: var(--muted);
  font-size: 12px;
}

.v {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 600;
  word-break: break-word;
}

.audit-note {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 1.6;
}

.audit-note.danger {
  color: #be123c;
  background: rgba(244, 63, 94, 0.08);
  border: 1px solid rgba(244, 63, 94, 0.14);
}

.audit-note.warn {
  color: #b45309;
  background: rgba(255, 125, 0, 0.1);
  border: 1px solid rgba(255, 125, 0, 0.16);
}

.proof-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.proof-grid-edu {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.proof-card {
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  padding: 12px;
  background: #fff;
}

.proof-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.proof-title {
  font-weight: 700;
  font-size: 13px;
}

.proof-button {
  margin-top: 10px;
  display: block;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.proof-img {
  width: 100%;
  height: 200px;
  object-fit: contain;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(15, 23, 42, 0.03);
}

.proof-empty {
  margin-top: 10px;
  min-height: 200px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--muted);
  font-size: 12px;
  border-radius: 12px;
  border: 1px dashed rgba(15, 23, 42, 0.14);
  background: rgba(247, 250, 252, 0.7);
}

.proof-empty.large {
  margin-top: 12px;
}

.actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.label-text {
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.textarea {
  width: 100%;
  min-height: 96px;
  resize: vertical;
}

.preview-wrap {
  display: grid;
  gap: 10px;
}

.preview-img {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(15, 23, 42, 0.02);
}

.link-btn {
  border: none;
  padding: 0;
  background: transparent;
  color: rgba(0, 190, 189, 1);
  cursor: pointer;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .proof-grid-edu,
  .proof-grid,
  .info-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .proof-img,
  .proof-empty {
    height: auto;
    min-height: 220px;
  }
}
</style>
