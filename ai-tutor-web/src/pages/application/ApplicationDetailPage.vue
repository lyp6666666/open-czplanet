<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import type { TutorApplicationVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = computed(() => {
  const raw = route.params.id
  const v = typeof raw === 'string' ? Number(raw) : Number.NaN
  return Number.isFinite(v) ? v : null
})

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<TutorApplicationVO | null>(null)

const busy = ref(false)
const opError = ref<string | null>(null)

const myUid = computed(() => (auth.user?.id ? Number(auth.user.id) : null))
const isReceiver = computed(() => !!(data.value && myUid.value != null && data.value.receiverUid === myUid.value))
const isTeacher = computed(() => auth.user?.userType === 1)

function statusText(s: TutorApplicationVO['status']): string {
  if (s === 'PENDING') return '待处理'
  if (s === 'ACCEPTED') return '已通过'
  if (s === 'REJECTED') return '已拒绝'
  return s
}

function accessText(v: TutorApplicationVO): string {
  if (v.chatAccessStatus === 'PAYMENT_REQUIRED') return '待教师支付中介费'
  if (v.chatAccessStatus === 'CHAT_ENABLED') return '可进入聊天'
  return ''
}

async function load() {
  if (!id.value) return
  loading.value = true
  error.value = null
  try {
    data.value = await applicationApi.detail(id.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    data.value = null
  } finally {
    loading.value = false
  }
}

async function decide(action: 'ACCEPT' | 'REJECT') {
  if (!id.value || busy.value) return
  busy.value = true
  opError.value = null
  try {
    data.value = await applicationApi.decide(id.value, action)
  } catch (e) {
    opError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    busy.value = false
  }
}

async function enterChat() {
  if (!id.value || busy.value) return
  busy.value = true
  opError.value = null
  try {
    const res = await applicationApi.enterChat(id.value)
    if (res.paymentRequired) {
      if (res.orderId) {
        await router.push({ name: 'brokeragePay', query: { orderId: String(res.orderId), applicationId: String(id.value) } })
      } else {
        opError.value = '需要先支付中介费'
      }
      return
    }
    if (res.waitingForTeacherPayment) {
      opError.value = '请等待教师完成中介费支付'
      return
    }
    if (res.roomId) {
      const otherUid = data.value ? (myUid.value === data.value.senderUid ? data.value.receiverUid : data.value.senderUid) : null
      await router.push({ name: 'chatRoom', params: { roomId: String(res.roomId) }, query: { otherUid: otherUid ? String(otherUid) : undefined } })
      return
    }
    opError.value = '暂无法进入聊天'
  } catch (e) {
    opError.value = e instanceof Error ? e.message : '进入聊天失败'
  } finally {
    busy.value = false
  }
}

function back() {
  router.back()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <button class="btn" type="button" @click="back">返回</button>
      <div class="title">申请详情</div>
      <div />
    </div>

    <div v-if="loading" class="hint">加载中...</div>
    <div v-else-if="error" class="hint error">{{ error }}</div>

    <template v-else-if="data">
      <div class="card panel">
        <div class="row"><span class="k">状态</span><span class="v">{{ statusText(data.status) }}</span></div>
        <div v-if="accessText(data)" class="row"><span class="k">聊天</span><span class="v">{{ accessText(data) }}</span></div>
        <div class="row"><span class="k">上下文</span><span class="v">{{ data.contextType }} · {{ data.contextId }}</span></div>
        <div class="row"><span class="k">创建时间</span><span class="v">{{ String(data.createTime).slice(0, 19).replace('T', ' ') }}</span></div>
        <div v-if="data.decidedAt" class="row"><span class="k">处理时间</span><span class="v">{{ String(data.decidedAt).slice(0, 19).replace('T', ' ') }}</span></div>
      </div>

      <div class="card content">
        <div class="c-title">申请内容</div>
        <div class="c-body">{{ data.content }}</div>
      </div>

      <div v-if="opError" class="hint error">{{ opError }}</div>

      <div class="card ops">
        <template v-if="isReceiver && data.status === 'PENDING'">
          <button class="btn" type="button" :disabled="busy" @click="decide('REJECT')">拒绝</button>
          <button class="btn btn-primary" type="button" :disabled="busy" @click="decide('ACCEPT')">{{ busy ? '处理中...' : '通过' }}</button>
        </template>
        <template v-else-if="data.status === 'ACCEPTED'">
          <button class="btn btn-primary" type="button" :disabled="busy" @click="enterChat">{{ busy ? '处理中...' : '进入聊天' }}</button>
          <button
            v-if="data.chatAccessStatus === 'PAYMENT_REQUIRED' && isTeacher && data.orderId"
            class="btn"
            type="button"
            :disabled="busy"
            @click="router.push({ name: 'brokeragePay', query: { orderId: String(data.orderId), applicationId: String(id) } })"
          >
            去支付
          </button>
        </template>
      </div>
    </template>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}

.head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
}

.title {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}

.panel {
  padding: 12px;
  display: grid;
  gap: 8px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.k {
  color: var(--muted);
  font-weight: 800;
}

.v {
  color: #1f2329;
  font-weight: 800;
}

.content {
  padding: 12px;
  display: grid;
  gap: 8px;
}

.c-title {
  font-weight: 900;
}

.c-body {
  white-space: pre-wrap;
  font-size: 13px;
  color: #1f2329;
}

.ops {
  padding: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
