<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { courseApi, type CourseItemVO } from '@/api/course'
import { liveApi, type LiveSessionResp } from '@/api/live'
import { assetsApi } from '@/api/assets'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const isTeacher = computed(() => auth.user?.userType === 1)
const role = computed<'TEACHER' | 'STUDENT'>(() => (isTeacher.value ? 'TEACHER' : 'STUDENT'))

const loading = ref(false)
const error = ref<string | null>(null)
const list = ref<CourseItemVO[]>([])
const liveMap = ref<Record<number, LiveSessionResp>>({})

const modalOpen = ref(false)
const modalCourseId = ref<number | null>(null)
const modalReason = ref('')
const modalFiles = ref<File[]>([])
const modalBusy = ref(false)
const modalErr = ref<string | null>(null)

function statusText(s: string): string {
  const v = String(s || '').trim().toUpperCase()
  if (v === 'APPLYING') return '申请中'
  if (v === 'WAIT_PAY') return '待支付'
  if (v === 'COMMUNICATING') return '沟通中'
  if (v === 'REFUND_REVIEW') return '退费审批中'
  if (v === 'REFUNDED') return '已退费'
  if (v === 'TRIALING') return '试课中'
  if (v === 'TEACHING') return '开课中'
  if (v === 'FINISHED') return '已结课'
  if (v === 'TRIAL_REFUND_REVIEW') return '试课退费审批中'
  return v || '未知'
}

function trialExpired(trialEndAt?: string | null): boolean {
  if (!trialEndAt) return false
  const t = Date.parse(trialEndAt)
  return Number.isFinite(t) ? Date.now() > t : false
}

function canApplyTrialRefund(it: CourseItemVO): boolean {
  if (!isTeacher.value) return false
  const s = String(it.status || '').trim().toUpperCase()
  if (s !== 'TRIALING') return false
  if (trialExpired(it.trialEndAt)) return false
  return true
}

function openTrialRefund(courseId: number) {
  modalCourseId.value = courseId
  modalReason.value = ''
  modalFiles.value = []
  modalErr.value = null
  modalOpen.value = true
}

function closeTrialRefund() {
  if (modalBusy.value) return
  modalOpen.value = false
}

function onPickFiles(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files ? Array.from(input.files) : []
  modalFiles.value = files
}

async function submitTrialRefund() {
  if (modalBusy.value) return
  if (!modalCourseId.value) return
  const reason = modalReason.value.trim()
  if (!reason) {
    modalErr.value = '请填写试课不通过说明'
    return
  }
  if (modalFiles.value.length < 1) {
    modalErr.value = '请至少上传 1 张证据图片'
    return
  }

  modalBusy.value = true
  modalErr.value = null
  try {
    const urls: string[] = []
    for (const f of modalFiles.value) {
      const uploaded = await assetsApi.uploadImage(f, 'trial_refund')
      if (uploaded?.url) urls.push(uploaded.url)
    }
    if (urls.length < 1) {
      modalErr.value = '图片上传失败'
      return
    }
    await courseApi.applyTrialRefund(modalCourseId.value, { reason, evidenceImageUrls: urls })
    modalOpen.value = false
    await load()
  } catch (e) {
    modalErr.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    modalBusy.value = false
  }
}

function goChat(roomId?: number | null) {
  if (!roomId) return
  void router.push({ name: 'chatRoom', params: { roomId: String(roomId) } })
}

function goLivePrepare(courseId: number) {
  void router.push({ name: 'livePrepare', params: { courseId: String(courseId) } })
}

async function load() {
  loading.value = true
  error.value = null
  try {
    list.value = await courseApi.myCourses({ page: 1, size: 50, role: role.value })
    const liveEntries = await Promise.all(
      list.value.map(async (it) => {
        try {
          const live = await liveApi.getByCourse(it.courseId)
          return [it.courseId, live] as const
        } catch {
          return null
        }
      }),
    )
    liveMap.value = Object.fromEntries(liveEntries.filter(Boolean) as Array<readonly [number, LiveSessionResp]>)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">我的课程</div>
      <button class="btn" type="button" :disabled="loading" @click="load">{{ loading ? '加载中...' : '刷新' }}</button>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card list">
      <div v-if="list.length === 0 && !loading" class="empty">
        <div class="empty-title">暂无课程</div>
        <div class="empty-desc">从聊天达成合作后，会在这里展示试课与课程状态</div>
      </div>

      <div v-else class="items">
        <div v-for="it in list" :key="it.courseId" class="item">
          <div class="main">
            <div class="trow">
              <div class="t">课程 #{{ it.courseId }}</div>
              <div class="tag">{{ statusText(it.status) }}</div>
            </div>
            <div class="meta">
              <span>教师：{{ it.teacherUid }}</span>
              <span>学生：{{ it.studentUid }}</span>
              <span v-if="it.trialEndAt">试课截止：{{ it.trialEndAt }}</span>
              <span v-if="liveMap[it.courseId]?.joinableNow">课堂已开放</span>
            </div>
          </div>
          <div class="ops">
            <button class="btn" type="button" :disabled="!it.roomId" @click="goChat(it.roomId)">进入聊天</button>
            <button class="btn" type="button" :disabled="!liveMap[it.courseId]" @click="goLivePrepare(it.courseId)">进入课堂</button>
            <button v-if="canApplyTrialRefund(it)" class="btn btn-primary" type="button" @click="openTrialRefund(it.courseId)">试课不通过</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="modalOpen" class="mask" @click.self="closeTrialRefund">
      <div class="modal card">
        <div class="m-title">试课不通过</div>
        <div class="m-desc">提交后将进入管理员审核，审核通过后原路退回信息费的 60%</div>
        <div class="m-form">
          <textarea v-model="modalReason" class="textarea" rows="4" placeholder="请填写试课不通过说明"></textarea>
          <input class="file" type="file" accept="image/*" multiple @change="onPickFiles" />
          <div class="m-hint">已选择 {{ modalFiles.length }} 张</div>
        </div>
        <div v-if="modalErr" class="m-error">{{ modalErr }}</div>
        <div class="m-ops">
          <button class="btn" type="button" :disabled="modalBusy" @click="closeTrialRefund">取消</button>
          <button class="btn btn-primary" type="button" :disabled="modalBusy" @click="submitTrialRefund">
            {{ modalBusy ? '提交中...' : '提交申请' }}
          </button>
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
  font-weight: 700;
}

.list {
  padding: 12px;
}

.items {
  display: grid;
  gap: 10px;
}

.item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
}

.trow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.t {
  font-weight: 700;
}

.tag {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.6);
  font-size: 12px;
}

.ops {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty {
  display: grid;
  gap: 8px;
  padding: 22px 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
}

.empty-title {
  font-weight: 700;
  color: rgba(0, 0, 0, 0.85);
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 50;
}

.modal {
  width: min(520px, 100%);
  padding: 14px;
}

.m-title {
  font-weight: 800;
  font-size: 16px;
}

.m-desc {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.m-form {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.textarea {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  outline: none;
}

.file {
  width: 100%;
}

.m-hint {
  color: rgba(0, 0, 0, 0.6);
  font-size: 12px;
}

.m-error {
  margin-top: 8px;
  color: #d93026;
  font-size: 13px;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}
</style>
