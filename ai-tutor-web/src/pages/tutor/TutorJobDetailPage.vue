<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { chatApi } from '@/api/chat'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { ChatMessageResp, ChatRoomItemResp, DemandViewVO } from '@/api/types'
import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'
import { formatClassMode, formatEducationRequirement, formatScheduleText } from '@/utils/present'

const route = useRoute()
const router = useRouter()
const settings = useSettingsStore()

const id = computed(() => Number(route.params.id))

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<DemandViewVO | null>(null)
const favorited = ref(false)

const applyBusy = ref(false)
const applyError = ref<string | null>(null)
const applyTipOpen = ref(false)
const applyTipText = ref('')

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await jobsApi.getDemandView(id.value)
    try {
      const fav = await favoritesApi.checkDemandFavorites([id.value])
      favorited.value = Array.isArray(fav) && fav.includes(id.value)
    } catch {
      favorited.value = false
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function openApply() {
  if (!data.value) return
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

function isChatUnlockedByMessages(list: ChatMessageResp[]) {
  return list.some((m) => {
    const b = normalizeMsgBody(m.message?.body)
    if (b.type === 'contact_unlocked' || b.type === 'brokerage_required') return true
    if (b.type === 'text' && typeof b.content === 'string' && b.content.trim().length > 0) return true
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
        <button class="btn btn-primary" type="button" :disabled="loading || !data" @click="openApply">发起申请</button>
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

      <div class="sec">
        <div class="sec-title">需求描述</div>
        <div class="sec-body">{{ data.description || '—' }}</div>
      </div>

      <div v-if="data.publisher" class="sec publisher">
        <img v-if="data.publisher.avatar" class="avatar" :src="data.publisher.avatar" alt="avatar" />
        <div v-else class="avatar fallback">{{ (data.publisher.displayName || 'U').slice(0, 1) }}</div>
        <div class="pub-info">
          <div class="pub-name">{{ data.publisher.displayName }}</div>
          <div class="pub-tag">{{ data.publisher.identityLabel }}</div>
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

.pub-tag {
  font-size: 12px;
  color: var(--muted);
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
