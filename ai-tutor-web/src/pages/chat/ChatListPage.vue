<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { userApi } from '@/api/user'
import type { ChatRoomItemResp, UserSimpleVO } from '@/api/types'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)

const rooms = ref<ChatRoomItemResp[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const userMap = ref<Record<number, UserSimpleVO>>({})

function lastMsgText(raw: unknown): string {
  if (!raw) return ''
  if (typeof raw === 'string') return raw
  if (typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    if (typeof any.content === 'string') return any.content
  }
  try {
    return JSON.stringify(raw)
  } catch {
    return String(raw)
  }
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
  void router.push({ name: 'chatRoom', params: { roomId }, query: { otherUid: String(otherUid) } })
}

const hasAny = computed(() => rooms.value.length > 0)

onMounted(() => {
  void loadMore()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">消息</div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card list">
      <div v-if="!hasAny && !loading" class="empty">
        <div class="empty-title">暂无会话</div>
        <div class="empty-desc">从需求详情点击“立即沟通”即可开始聊天</div>
      </div>

      <div v-else class="items">
        <button v-for="r in rooms" :key="r.roomId" class="item" type="button" @click="openRoom(r.roomId, r.otherUid)">
          <div class="avatar" />
          <div class="main">
            <div class="row1">
              <div class="name">{{ userMap[r.otherUid]?.name || `用户${r.otherUid}` }}</div>
              <div class="time">{{ r.activeTime ? String(r.activeTime).slice(0, 19).replace('T', ' ') : '' }}</div>
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
  font-weight: 900;
}

.list {
  padding: 14px;
}

.items {
  display: grid;
  gap: 10px;
}

.item {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.08);
}

.row1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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
  margin-top: 14px;
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
</style>
