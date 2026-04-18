<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { userApi } from '@/api/user'
import type { ChatRoomItemResp, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useChatRealtimeStore } from '@/stores/chatRealtime'
import { normalizeAvatarUrl } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chatRealtime = useChatRealtimeStore()

const loading = ref(false)
const error = ref<string | null>(null)

const rooms = ref<ChatRoomItemResp[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const userMap = ref<Record<number, UserSimpleVO>>({})
const search = ref('')
const avatarBroken = ref<Record<number, boolean>>({})
const lastConsumedMessageSerial = ref(0)

function displayNameOf(user: UserSimpleVO | null | undefined, uid: number): string {
  const realName = user?.realName?.trim()
  if (realName) return realName
  const name = user?.name?.trim()
  if (name) return name
  return `用户${uid}`
}

type StreamMsgEvent = {
  msgId: number
  roomId: number
  fromUid: number
  toUid: number
  sendTime: unknown
  body: unknown
}

function upsertRoomFromEvent(ev: StreamMsgEvent) {
  const myUid = auth.user?.id
  if (!myUid) return
  const existingRoom = rooms.value.find((r) => r.roomId === ev.roomId)
  const otherUid = ev.fromUid === myUid ? ev.toUid : ev.fromUid
  const prevUnread = chatRealtime.roomUnread[ev.roomId] || 0
  const sendTimeMs =
    typeof ev.sendTime === 'number'
      ? ev.sendTime
      : typeof ev.sendTime === 'string'
          ? Number(ev.sendTime)
          : Number.NaN
  const updated: ChatRoomItemResp = {
    roomId: ev.roomId,
    otherUid,
    lastMsgId: ev.msgId,
    lastMsgBody: ev.body,
    myLastReadMsgId: existingRoom?.myLastReadMsgId ?? null,
    peerLastReadMsgId: existingRoom?.peerLastReadMsgId ?? null,
    unreadCount: prevUnread,
    activeTime: Number.isFinite(sendTimeMs) ? new Date(sendTimeMs).toISOString() : new Date().toISOString(),
  }
  const idx = rooms.value.findIndex((r) => r.roomId === ev.roomId)
  if (idx >= 0) {
    const next = rooms.value.slice()
    next.splice(idx, 1)
    rooms.value = [updated, ...next]
  } else {
    rooms.value = [updated, ...rooms.value]
    void enrichUsers([updated])
  }
}

watch(
  () => chatRealtime.roomUnread,
  (map) => {
    rooms.value = rooms.value.map((r) => {
      const u = map[r.roomId]
      if (typeof u === 'number' && u !== r.unreadCount) {
        return { ...r, unreadCount: u }
      }
      return r
    })
  },
  { deep: true },
)

function effectiveUnread(roomId: number, fallback: number) {
  const v = chatRealtime.roomUnread[roomId]
  return typeof v === 'number' ? v : fallback
}

function lastMsgText(raw: unknown): string {
  if (!raw) return ''
  if (typeof raw === 'string') return raw
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    const t = typeof any.type === 'string' ? any.type.trim() : ''
    if (t === 'recall') return '[消息已撤回]'
    if (t === 'image') return '[图片]'
    if (t === 'brokerage_refund_request' || t === 'brokerage_refund_status') return '[退款申请]'
    if (typeof any.content === 'string') return any.content
  }
  try {
    return JSON.stringify(raw)
  } catch {
    return String(raw)
  }
}

function avatarOf(uid: number): string {
  if (avatarBroken.value[uid]) return ''
  const v = userMap.value[uid]?.avatar
  return normalizeAvatarUrl(v)
}

function markAvatarBroken(uid: number) {
  if (!uid) return
  if (avatarBroken.value[uid]) return
  avatarBroken.value = { ...avatarBroken.value, [uid]: true }
}

async function enrichUsers(list: ChatRoomItemResp[]) {
  const ids = Array.from(new Set(list.map((r) => r.otherUid))).filter((n) => Number.isFinite(n))
  if (ids.length === 0) return
  const users = await userApi.batch(ids)
  const next = { ...userMap.value }
  users.forEach((u) => {
    next[u.id] = u
  })
  userMap.value = next
}

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await chatApi.listRooms({ pageSize: 20, cursor: cursor.value })
    const newRooms = page.list || []
    rooms.value = [...rooms.value, ...newRooms]
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast
    await enrichUsers(newRooms)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function openRoom(roomId: number, otherUid: number) {
  const room = rooms.value.find((r) => r.roomId === roomId)
  if (room?.lastMsgId) {
    void chatRealtime.ackRoomRead(roomId, room.lastMsgId)
  }
  // Immediately clear local unread state
  chatRealtime.clearRoomUnread(roomId)
  chatRealtime.setActiveRoom(roomId)
  void router.push({ name: 'chatRoom', params: { roomId }, query: { otherUid: String(otherUid) } })
}

const hasAny = computed(() => rooms.value.length > 0)

const activeRoomId = computed(() => {
  const raw = route.params.roomId
  const v = typeof raw === 'string' ? Number(raw) : Number.NaN
  return Number.isFinite(v) ? v : null
})

const filteredRooms = computed(() => {
  const kw = search.value.trim().toLowerCase()
  if (!kw) return rooms.value
  return rooms.value.filter((r) => {
    const name = displayNameOf(userMap.value[r.otherUid], r.otherUid)
    return name.toLowerCase().includes(kw) || lastMsgText(r.lastMsgBody).toLowerCase().includes(kw)
  })
})

onMounted(() => {
  void loadMore()
  consumePendingMessageEvents()
})

function consumePendingMessageEvents() {
  const pending = chatRealtime.listMessageEventsAfter(lastConsumedMessageSerial.value)
  if (!pending.length) return
  for (const item of pending) {
    upsertRoomFromEvent(item.event)
    lastConsumedMessageSerial.value = item.serial
  }
}

watch(() => chatRealtime.messageEventSerial, consumePendingMessageEvents)
</script>

<template>
  <div class="workbench">
    <aside class="left card">
      <div class="left-head">
        <div class="row">
          <div class="title">消息</div>
        </div>
        <input v-model="search" class="search" placeholder="搜索会话" />
      </div>

      <div v-if="error" class="hint error">{{ error }}</div>

      <div v-if="!hasAny && !loading" class="empty">
        <div class="empty-title">暂无会话</div>
        <div class="empty-desc">发起家教申请后即可进入会话，申请通过并支付后可聊天</div>
      </div>

      <div v-else class="items">
        <button
          v-for="r in filteredRooms"
          :key="r.roomId"
          class="item"
          type="button"
          :class="{ active: activeRoomId === r.roomId }"
          @click="openRoom(r.roomId, r.otherUid)"
        >
          <span v-if="effectiveUnread(r.roomId, r.unreadCount) > 0" class="unread-badge">{{
            effectiveUnread(r.roomId, r.unreadCount) > 99 ? '99+' : effectiveUnread(r.roomId, r.unreadCount)
          }}</span>
          <div class="avatar">
            <img v-if="avatarOf(r.otherUid)" :src="avatarOf(r.otherUid)" alt="" @error="markAvatarBroken(r.otherUid)" />
            <span v-else class="avatar-fallback">{{ displayNameOf(userMap[r.otherUid], r.otherUid).slice(0, 1) }}</span>
          </div>
          <div class="main">
            <div class="row1">
              <div class="name">{{ displayNameOf(userMap[r.otherUid], r.otherUid) }}</div>
              <div class="meta">
                <span class="time">{{ r.activeTime ? String(r.activeTime).slice(0, 19).replace('T', ' ') : '' }}</span>
              </div>
            </div>
            <div class="row2">{{ lastMsgText(r.lastMsgBody) }}</div>
          </div>
        </button>
      </div>

      <div class="footer" v-if="hasAny">
        <button class="btn" type="button" :disabled="loading || isLast" @click="loadMore">
          <span v-if="isLast">没有更多了</span>
          <span v-else>{{ loading ? '加载中...' : '加载更多' }}</span>
        </button>
      </div>
    </aside>

    <section class="right">
      <RouterView v-slot="{ Component }">
        <component :is="Component" v-if="Component" />
        <div v-else class="placeholder card">
          <div class="placeholder-title">选择一个会话开始沟通</div>
          <div class="placeholder-desc">发起家教申请后即可进入会话，申请通过并支付后可聊天</div>
        </div>
      </RouterView>
    </section>
  </div>
</template>

<style scoped>
.workbench {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 12px;
  align-items: stretch;
  height: calc(100vh - var(--app-topbar-height, 56px) - 50px);
  height: calc(100dvh - var(--app-topbar-height, 56px) - 50px);
  min-height: 0;
}

.left {
  padding: 12px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.left-head {
  display: grid;
  gap: 10px;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 16px;
  font-weight: 900;
}

.items {
  display: grid;
  gap: 8px;
  align-content: start;
  overflow-y: auto;
  min-height: 0;
}

.item {
  position: relative;
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 12px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.unread-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 16px;
  min-width: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(255, 77, 79, 0.95);
  color: #fff;
  font-weight: 900;
  font-size: 11px;
  line-height: 16px;
  border: 2px solid #fff;
}

.item.active {
  border-color: rgba(0, 190, 189, 0.45);
  background: rgba(0, 190, 189, 0.06);
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.08);
  display: grid;
  place-items: center;
  font-weight: 900;
  color: var(--text);
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  font-weight: 900;
}

.row1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name {
  font-weight: 900;
  font-size: 14px;
}

.time {
  font-size: 11px;
  color: var(--muted);
}

.row2 {
  margin-top: 6px;
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer {
  display: flex;
  justify-content: center;
}

.empty {
  padding: 28px 10px;
  display: grid;
  gap: 10px;
}

.empty-title {
  font-weight: 900;
}

.empty-desc {
  color: var(--muted);
  font-size: 13px;
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

.search {
  height: 36px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.search:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.right {
  height: 100%;
  min-height: 0;
}

.placeholder {
  height: 100%;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 10px;
  padding: 20px;
  background: #fbfcfe;
}

.placeholder-title {
  font-weight: 900;
}

.placeholder-desc {
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 980px) {
  .workbench {
    grid-template-columns: 1fr;
  }
}
</style>
