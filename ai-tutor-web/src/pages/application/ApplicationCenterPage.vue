<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { userApi } from '@/api/user'
import type { CursorPageResp, TutorApplicationVO, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

type TabKey = 'received' | 'sent'

const router = useRouter()
const auth = useAuthStore()
const isTeacher = computed(() => auth.user?.userType === 1)

const tab = ref<TabKey>('received')

const loading = ref(false)
const error = ref<string | null>(null)

const sent = ref<TutorApplicationVO[]>([])
const sentCursor = ref<number | null>(null)
const sentIsLast = ref(false)

const received = ref<TutorApplicationVO[]>([])
const receivedCursor = ref<number | null>(null)
const receivedIsLast = ref(false)

const unreadCount = ref(0)

const userMap = ref<Record<number, UserSimpleVO>>({})

const streamAbort = ref<AbortController | null>(null)

function normalizeBaseUrl(raw: unknown) {
  const s = typeof raw === 'string' ? raw.trim() : ''
  return s.length > 0 ? s : ''
}

async function startStream() {
  if (!auth.isLoggedIn || !auth.token) return
  if (streamAbort.value) return
  const controller = new AbortController()
  streamAbort.value = controller

  const baseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
  const url = `${baseUrl}/chat/stream`
  const res = await fetch(url, {
    method: 'GET',
    headers: { Authorization: `Bearer ${auth.token}` },
    signal: controller.signal,
  })
  if (!res.ok || !res.body) {
    streamAbort.value = null
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const lines = part.split('\n').map((l) => l.trimEnd())
      let event = 'message'
      const dataLines: string[] = []
      for (const line of lines) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      const dataRaw = dataLines.join('\n')
      if (!dataRaw) continue
      if (event !== 'application') continue
      try {
        const ev = JSON.parse(dataRaw) as { type?: string; applicationId?: number }
        if (!ev || !ev.applicationId) continue
        await loadUnread()
        if (tab.value === 'received') {
          received.value = []
          receivedCursor.value = null
          receivedIsLast.value = false
          await loadPage('received')
        }
        if (tab.value === 'sent') {
          sent.value = []
          sentCursor.value = null
          sentIsLast.value = false
          await loadPage('sent')
        }
      } catch {
        void 0
      }
    }
  }
  streamAbort.value = null
}

function stopStream() {
  streamAbort.value?.abort()
  streamAbort.value = null
}

async function enrichUsers(list: TutorApplicationVO[]) {
  const ids = Array.from(new Set(list.flatMap((it) => [it.senderUid, it.receiverUid]))).filter((n) => Number.isFinite(n))
  if (ids.length === 0) return
  const users = await userApi.batch(ids)
  const next = { ...userMap.value }
  users.forEach((u) => {
    next[u.id] = u
  })
  userMap.value = next
}

function otherUid(it: TutorApplicationVO): number {
  const mine = auth.user?.id ? Number(auth.user.id) : 0
  if (mine && it.senderUid === mine) return it.receiverUid
  if (mine && it.receiverUid === mine) return it.senderUid
  return it.senderUid
}

function otherName(it: TutorApplicationVO): string {
  const uid = otherUid(it)
  return userMap.value[uid]?.name || `用户${uid}`
}

function statusText(it: TutorApplicationVO): string {
  if (it.status === 'PENDING') return '待处理'
  if (it.status === 'ACCEPTED') return '已通过'
  if (it.status === 'REJECTED') return '已拒绝'
  return it.status
}

async function loadUnread() {
  try {
    const res = await applicationApi.unread()
    unreadCount.value = typeof res.unreadCount === 'number' ? res.unreadCount : 0
  } catch {
    unreadCount.value = 0
  }
}

async function loadPage(kind: TabKey) {
  if (loading.value) return
  loading.value = true
  error.value = null
  try {
    let page: CursorPageResp<TutorApplicationVO>
    if (kind === 'sent') {
      if (sentIsLast.value) return
      page = await applicationApi.listSent({ pageSize: 20, cursor: sentCursor.value })
      const list = page.list || []
      sent.value = [...sent.value, ...list]
      sentCursor.value = page.cursor ?? null
      sentIsLast.value = !!page.isLast
      await enrichUsers(list)
    } else {
      if (receivedIsLast.value) return
      page = await applicationApi.listReceived({ pageSize: 20, cursor: receivedCursor.value })
      const list = page.list || []
      received.value = [...received.value, ...list]
      receivedCursor.value = page.cursor ?? null
      receivedIsLast.value = !!page.isLast
      await enrichUsers(list)
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function openDetail(id: number) {
  void router.push({ name: 'applicationDetail', params: { id: String(id) } })
}

function exitCenter() {
  if (isTeacher.value) {
    void router.push({ name: 'tutorJobs' })
  } else {
    void router.push({ name: 'studentPost' })
  }
}

const list = computed(() => (tab.value === 'sent' ? sent.value : received.value))
const isLast = computed(() => (tab.value === 'sent' ? sentIsLast.value : receivedIsLast.value))

watch(
  () => tab.value,
  () => {
    if (tab.value === 'sent' && sent.value.length === 0) void loadPage('sent')
    if (tab.value === 'received' && received.value.length === 0) void loadPage('received')
  },
)

onMounted(() => {
  void loadUnread()
  void loadPage('received')
  void startStream()
})

onBeforeUnmount(() => {
  stopStream()
})
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <div class="head-top">
        <div class="title">申请中心</div>
        <button class="btn" type="button" @click="exitCenter">退出</button>
      </div>
      <div class="tabs">
        <button class="tab" type="button" :class="{ active: tab === 'received' }" @click="tab = 'received'">
          我收到的申请<span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
        </button>
        <button class="tab" type="button" :class="{ active: tab === 'sent' }" @click="tab = 'sent'">我发出的申请</button>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div v-if="list.length === 0 && !loading" class="card empty">
      <div class="empty-title">暂无申请</div>
      <div class="empty-desc">从需求页/教员页发起申请后，会在这里展示</div>
    </div>

    <div v-else class="card list">
      <button v-for="it in list" :key="it.id" class="item" type="button" @click="openDetail(it.id)">
        <div class="row1">
          <div class="name">{{ otherName(it) }}</div>
          <div class="status">{{ statusText(it) }}</div>
        </div>
        <div class="row2">{{ it.content }}</div>
        <div class="row3">
          <span>{{ it.contextType === 'DEMAND' ? '需求' : '教员' }} · {{ it.contextId }}</span>
          <span>{{ String(it.createTime).slice(0, 19).replace('T', ' ') }}</span>
        </div>
      </button>

      <div class="footer">
        <button class="btn" type="button" :disabled="loading || isLast" @click="loadPage(tab)">
          <span v-if="isLast">没有更多了</span>
          <span v-else>{{ loading ? '加载中...' : '加载更多' }}</span>
        </button>
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
  gap: 10px;
  padding: 12px;
}

.head-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 18px;
  font-weight: 900;
}

.tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tab {
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 6px 10px;
  background: #fff;
  cursor: pointer;
  font-weight: 800;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab.active {
  border-color: rgba(0, 190, 189, 0.45);
  background: rgba(0, 190, 189, 0.06);
}

.badge {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 77, 79, 0.15);
  color: rgba(255, 77, 79, 0.95);
  font-weight: 900;
  line-height: 18px;
  text-align: center;
}

.empty {
  padding: 18px;
  text-align: center;
}

.empty-title {
  font-weight: 900;
}

.empty-desc {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}

.list {
  padding: 12px;
  display: grid;
  gap: 10px;
}

.item {
  text-align: left;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  display: grid;
  gap: 8px;
  cursor: pointer;
}

.row1 {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.name {
  font-weight: 900;
}

.status {
  font-size: 12px;
  color: var(--muted);
}

.row2 {
  font-size: 13px;
  color: #1f2329;
}

.row3 {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--muted);
}

.footer {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}
</style>
