<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { chatApi } from '@/api/chat'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { ChatMessageResp, ChatRoomItemResp, DemandViewVO, TutorApplicationVO } from '@/api/types'
import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'
import UserCardModal from '@/ui/user/UserCardModal.vue'
import OrgCardModal from '@/ui/user/OrgCardModal.vue'
import { formatClassMode, formatEducationRequirement, formatScheduleText } from '@/utils/present'

const route = useRoute()
const router = useRouter()
const settings = useSettingsStore()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<DemandViewVO | null>(null)
const avatarLoadFailed = ref(false)
const favorited = ref(false)

const applyBusy = ref(false)
const applyError = ref<string | null>(null)
const applyTipOpen = ref(false)
const applyTipText = ref('')

const applicationLoading = ref(false)
const application = ref<TutorApplicationVO | null>(null)
let applicationPollTimer: number | null = null

const cardOpen = ref(false)
const cardUid = ref<number | null>(null)
const orgCardOpen = ref(false)
const orgCardId = ref<number | null>(null)

function openCard(uid: number) {
  if (!uid) return
  cardUid.value = uid
  cardOpen.value = true
}

function openOrgCard(id: number) {
  if (!id) return
  orgCardId.value = id
  orgCardOpen.value = true
}

function closeCard() {
  cardOpen.value = false
}

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await jobsApi.getDemandView(id.value)
    void loadApplication()
    try {
      const fav = await favoritesApi.checkDemandFavorites([id.value])
      favorited.value = Array.isArray(fav) && fav.some((it) => String(it) === String(id.value))
    } catch {
      favorited.value = false
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

watch(
  () => data.value?.publisher?.avatar,
  () => {
    avatarLoadFailed.value = false
  },
)

function stopApplicationPolling() {
  if (applicationPollTimer != null) {
    clearInterval(applicationPollTimer)
    applicationPollTimer = null
  }
}

async function findLatestSentDemandApplication(receiverUid: number, demandId: number): Promise<TutorApplicationVO | null> {
  let cursor: number | null = null
  let best: TutorApplicationVO | null = null
  for (let i = 0; i < 10; i++) {
    const page = await applicationApi.listSent({ pageSize: 50, cursor })
    const list = page.list || []
    for (const it of list) {
      if (it.contextType !== 'DEMAND') continue
      if (it.contextId !== demandId) continue
      if (it.receiverUid !== receiverUid) continue
      if (!best || it.id > best.id) best = it
    }
    cursor = page.cursor ?? null
    if (page.isLast || list.length === 0) break
  }
  return best
}

async function loadApplication() {
  stopApplicationPolling()
  application.value = null
  if (!data.value) return
  applicationLoading.value = true
  try {
    const app = await findLatestSentDemandApplication(data.value.parentId, data.value.id)
    application.value = app
    if (app?.status === 'PENDING') {
      const appId = app.id
      applicationPollTimer = window.setInterval(async () => {
        if (!application.value) return
        if (application.value.id !== appId) return
        try {
          const latest = await applicationApi.detail(appId)
          application.value = latest
          if (latest.status !== 'PENDING') stopApplicationPolling()
        } catch {
          void 0
        }
      }, 5000)
    }
  } finally {
    applicationLoading.value = false
  }
}

const applyButton = computed(() => {
  if (!data.value) return { text: '发起申请', disabled: true, mode: 'apply' as const }
  const app = application.value
  if (!app || app.status === 'REJECTED') return { text: '发起申请', disabled: false, mode: 'apply' as const }
  if (app.status === 'PENDING') return { text: '已发起申请', disabled: true, mode: 'pending' as const }
  if (app.status === 'ACCEPTED') return { text: '已发起沟通', disabled: false, mode: 'chat' as const }
  return { text: '发起申请', disabled: false, mode: 'apply' as const }
})

async function openChatForCurrentDemand() {
  if (!data.value) return
  const otherUid = data.value.parentId
  const roomId = application.value?.roomId || (await findRoomByOtherUid(otherUid))?.roomId
  if (!roomId) return
  await router.push({ name: 'chatRoom', params: { roomId: String(roomId) }, query: { otherUid: String(otherUid) } })
}

async function onClickApplyButton() {
  if (!data.value) return
  if (applyButton.value.mode === 'chat') {
    await openChatForCurrentDemand()
    return
  }
  if (applyBusy.value) return
  applyBusy.value = true
  applyError.value = null
  try {
    try {
      const otherUid = data.value.parentId
      const reuse = await shouldReuseExistingChat(otherUid)
      if (reuse?.roomId) {
        await router.push({ name: 'chatRoom', params: { roomId: String(reuse.roomId) }, query: { otherUid: String(otherUid) } })
        return
      }
    } catch {
      void 0
    }

    if (!settings.loaded) {
      try {
        await settings.load()
      } catch {
        void 0
      }
    }
    const content = (settings.applicationGreeting || DEFAULT_APPLICATION_GREETING).trim() || DEFAULT_APPLICATION_GREETING
    const msg = await applicationApi.startChat({
      receiverUid: data.value.parentId,
      contextType: 'DEMAND',
      contextId: id.value,
      content,
      clientRequestId: genClientRequestId(),
    })
    const parsed = parseTutorApplicationMsgBody(msg.message?.body)
    if (parsed) {
      application.value = {
        id: parsed.applicationId,
        senderUid: msg.fromUser.uid,
        receiverUid: data.value.parentId,
        senderRole: 'TEACHER',
        receiverRole: 'STUDENT',
        contextType: 'DEMAND',
        contextId: id.value,
        content: parsed.content,
        status: parsed.status,
        chatAccessStatus: 'NONE',
        paymentPayerRole: 'TEACHER',
        orderId: null,
        roomId: msg.message.roomId,
        receiverRead: null,
        decidedAt: null,
        createTime: msg.message.sendTime,
      }
    }
    await router.push({ name: 'chatRoom', params: { roomId: String(msg.message.roomId) }, query: { otherUid: String(data.value.parentId) } })
  } catch (e) {
    const msg = e instanceof Error ? e.message : '发送申请失败'
    applyError.value = msg
    applyTipText.value = msg
    applyTipOpen.value = true
  } finally {
    applyBusy.value = false
  }
}

function normalizeMsgBody(raw: unknown): { type: string; content?: string } {
  if (!raw) return { type: 'text', content: '' }
  if (typeof raw === 'string') return { type: 'text', content: raw }
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    if (typeof any.type === 'string') return any as { type: string; content?: string }
    if (typeof any.content === 'string') return { type: 'text', content: any.content }
  }
  return { type: 'system' }
}

function parseTutorApplicationMsgBody(raw: unknown): { applicationId: number; content: string; status: 'PENDING' | 'ACCEPTED' | 'REJECTED' } | null {
  if (!raw || typeof raw !== 'object') return null
  const b = raw as Record<string, unknown>
  if (b.type !== 'tutor_application') return null
  const applicationId = typeof b.applicationId === 'number' ? b.applicationId : null
  const content = typeof b.content === 'string' ? b.content : ''
  const status = b.status === 'PENDING' || b.status === 'ACCEPTED' || b.status === 'REJECTED' ? b.status : null
  if (applicationId == null || status == null) return null
  return { applicationId, content, status }
}

function isChatUnlockedByMessages(list: ChatMessageResp[]) {
  return list.some((m) => {
    const b = normalizeMsgBody(m.message?.body)
    if (b.type === 'contact_unlocked') return true
    return false
  })
}

async function findRoomByOtherUid(otherUid: number): Promise<ChatRoomItemResp | null> {
  let cursor: number | null = null
  for (let i = 0; i < 10; i++) {
    const page = await chatApi.listRooms({ pageSize: 50, cursor })
    const list = page.list || []
    const found = list.find((r) => r.otherUid === otherUid) || null
    if (found) return found
    cursor = page.cursor ?? null
    if (page.isLast || list.length === 0) break
  }
  return null
}

async function shouldReuseExistingChat(otherUid: number): Promise<{ roomId: number } | null> {
  const found = await findRoomByOtherUid(otherUid)
  if (!found?.roomId) return null
  const page = await chatApi.listMessages({ roomId: found.roomId, pageSize: 20, cursor: null })
  const list = page.list || []
  if (!isChatUnlockedByMessages(list)) return null
  return { roomId: found.roomId }
}

function genClientRequestId() {
  const g = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : ''
  if (g) return g
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function onToggleFavorite() {
  if (!data.value) return
  try {
    if (favorited.value) {
      await favoritesApi.unfavoriteDemand(data.value.id)
      favorited.value = false
    } else {
      await favoritesApi.favoriteDemand(data.value.id)
      favorited.value = true
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

onMounted(() => {
  void load()
})

watch(id, () => {
  void load()
})

onBeforeUnmount(() => {
  stopApplicationPolling()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <button class="btn" type="button" @click="router.back()">返回</button>
      <div class="title">需求详情</div>
      <div class="head-ops">
        <button class="btn" type="button" :disabled="loading || !data" @click="onToggleFavorite">
          {{ favorited ? '已收藏' : '收藏' }}
        </button>
        <button
          class="btn btn-primary"
          type="button"
          :disabled="loading || applicationLoading || applyBusy || applyButton.disabled || !data"
          @click="onClickApplyButton"
        >
          {{ applyButton.text }}
        </button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="data" class="card detail">
      <div class="t">{{ data.title }}</div>
      <div class="meta">
        <span>{{ (data.classMode || '').toLowerCase() === 'online' ? '线上' : data.city || '线下' }}</span>
        <span>{{ formatClassMode(data.classMode) }}</span>
        <span>每周{{ data.frequencyPerWeek || '-' }}次</span>
        <span>{{ formatEducationRequirement(data.educationRequirement) }}</span>
        <span v-if="data.budgetMin || data.budgetMax">{{ data.budgetMin || '-' }}-{{ data.budgetMax || '-' }}/小时</span>
      </div>

      <div v-if="String(data.publisherIdentity || '').toUpperCase() === 'ORGANIZATION'" class="hint notice">
        <div class="n-title">机构单说明</div>
        <div class="n-body">
          机构为需求发布与履约主体，平台提供信息撮合、支付托管与纠纷介入机制；平台不直接保证授课质量与履约结果。
        </div>
      </div>

      <div class="sec">
        <div class="sec-title">需求描述</div>
        <div class="sec-body">{{ data.description || '—' }}</div>
      </div>

      <div v-if="data.publisher" class="sec publisher">
        <img
          v-if="data.publisher.avatar && !avatarLoadFailed"
          class="avatar clickable"
          :src="data.publisher.avatar"
          alt="avatar"
          @error="avatarLoadFailed = true"
          @click="openCard(data.publisher.uid)"
        />
        <div v-else class="avatar fallback clickable" @click="openCard(data.publisher.uid)">{{ (data.publisher.displayName || 'U').slice(0, 1) }}</div>
        <div class="pub-info">
          <div class="pub-name clickable" @click="openCard(data.publisher.uid)">{{ data.publisher.displayName }}</div>
          <div class="pub-tags">
            <div class="pub-tag">{{ data.publisher.identityLabel }}</div>
            <button
              v-if="String(data.publisherIdentity || '').toUpperCase() === 'ORGANIZATION'"
              class="link"
              type="button"
              @click="openOrgCard(data.parentId)"
            >
              查看机构主页
            </button>
          </div>
        </div>
      </div>

      <div class="sec" v-if="data.classMode !== 'online'">
        <div class="sec-title">工作地址</div>
        <div class="sec-body">{{ [data.city, data.address].filter(Boolean).join(' · ') || '—' }}</div>
      </div>

      <div class="sec">
        <div class="sec-title">授课时间</div>
        <div class="sec-body">{{ data.schedule ? formatScheduleText(data.schedule) : '—' }}</div>
      </div>
    </div>

    <div v-if="applyTipOpen" class="mask" @click.self="applyTipOpen = false">
      <div class="modal card">
        <div class="m-title">提示</div>
        <div class="m-desc">{{ applyTipText }}</div>
        <div class="m-ops">
          <button class="btn btn-primary" type="button" @click="applyTipOpen = false">知道了</button>
        </div>
      </div>
    </div>

    <UserCardModal :open="cardOpen" :uid="cardUid" @close="closeCard" />
    <OrgCardModal :open="orgCardOpen" :org-id="orgCardId" @close="orgCardOpen = false" />

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
  align-items: center;
  gap: 12px;
}

.head-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.title {
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.detail {
  padding: 16px;
  display: grid;
  gap: 10px;
}

.t {
  font-weight: 900;
  font-size: 16px;
}

.meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.sec {
  display: grid;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}

.publisher {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  background: rgba(0, 190, 189, 0.08);
}

.pub-info {
  display: grid;
  gap: 4px;
}

.pub-name {
  font-weight: 900;
}

.avatar.clickable {
  cursor: pointer;
}

.avatar.fallback.clickable {
  cursor: pointer;
}

.pub-name.clickable {
  cursor: pointer;
}
.pub-name.clickable:hover {
  text-decoration: underline;
}

.pub-tag {
  font-size: 12px;
  color: var(--muted);
}

.pub-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.link {
  border: 0;
  background: transparent;
  padding: 0 2px;
  color: var(--primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.sec-title {
  font-size: 12px;
  color: var(--muted);
  font-weight: 800;
}

.sec-body {
  font-size: 13px;
  line-height: 1.6;
}

.hint {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: #fff;
}

.hint.error {
  border-color: rgba(255, 0, 0, 0.25);
  background: rgba(255, 0, 0, 0.06);
}

.hint.notice {
  border-color: rgba(255, 170, 0, 0.28);
  background: rgba(255, 170, 0, 0.06);
}

.n-title {
  font-weight: 900;
  margin-bottom: 4px;
}

.n-body {
  color: var(--muted);
  line-height: 1.6;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.38);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 999;
}

.modal {
  width: min(520px, 92vw);
  padding: 14px;
  display: grid;
  gap: 10px;
}

.m-title {
  font-weight: 900;
  font-size: 16px;
}

.m-desc {
  font-size: 13px;
  color: var(--text);
  line-height: 1.45;
}

.m-error {
  color: #ff4d4f;
  font-size: 12px;
  font-weight: 800;
}

.field {
  display: grid;
  gap: 6px;
}

.lab {
  font-size: 12px;
  color: var(--muted);
  font-weight: 800;
}

.txt {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
  resize: vertical;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
