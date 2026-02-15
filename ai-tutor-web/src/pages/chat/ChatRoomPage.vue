<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { userApi } from '@/api/user'
import type { ChatMessageResp, UserSimpleVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const roomId = computed(() => Number(route.params.roomId))
const otherUid = computed(() => {
  const raw = route.query.otherUid
  const v = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(v) ? v : null
})

const otherUser = ref<UserSimpleVO | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

const sending = ref(false)
const input = ref('')

const cursor = ref<string | null>(null)
const isLast = ref(false)
const messages = ref<ChatMessageResp[]>([])

const myUid = computed(() => auth.user?.id ?? 0)

function msgText(raw: unknown): string {
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

const sortedMessages = computed(() => {
  const list = messages.value.slice()
  list.sort((a, b) => (a.message?.id || 0) - (b.message?.id || 0))
  return list
})

async function loadOtherUser() {
  if (!otherUid.value) return
  const users = await userApi.batch([otherUid.value])
  otherUser.value = users[0] || null
}

function mergeMessages(incoming: ChatMessageResp[], mode: 'prepend' | 'append') {
  const byId = new Map<number, ChatMessageResp>()
  messages.value.forEach((m) => byId.set(m.message.id, m))
  incoming.forEach((m) => byId.set(m.message.id, m))
  const merged = Array.from(byId.values())
  merged.sort((a, b) => a.message.id - b.message.id)
  messages.value = merged
  if (mode === 'prepend') {
    return
  }
}

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await chatApi.listMessages({ roomId: roomId.value, pageSize: 20, cursor: cursor.value })
    const list = page.list || []
    mergeMessages(list, cursor.value ? 'prepend' : 'append')
    cursor.value = page.cursor ?? null
    isLast.value = !!page.isLast
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onSend() {
  const text = input.value.trim()
  if (!text) return
  if (sending.value) return
  sending.value = true
  error.value = null
  try {
    const msg = await chatApi.sendText(roomId.value, text)
    mergeMessages([msg], 'append')
    input.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  void loadOtherUser()
  void loadMore()
})
</script>

<template>
  <div class="wrap">
    <div class="head card">
      <button class="btn" type="button" @click="router.back()">返回</button>
      <div class="name">{{ otherUser?.name || (otherUid ? `用户${otherUid}` : `会话 ${roomId}`) }}</div>
      <div />
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card panel">
      <div class="top">
        <button class="btn" type="button" :disabled="loading || isLast" @click="loadMore">
          <span v-if="isLast">没有更多历史消息</span>
          <span v-else>{{ loading ? '加载中...' : '加载历史' }}</span>
        </button>
      </div>

      <div class="msgs">
        <div v-for="m in sortedMessages" :key="m.message.id" class="msg" :class="{ me: m.fromUser.uid === myUid }">
          <div class="bubble">{{ msgText(m.message.body) }}</div>
        </div>
      </div>

      <div class="send">
        <input
          v-model="input"
          class="input"
          placeholder="请输入消息"
          @keydown.enter.prevent="onSend"
        />
        <button class="btn btn-primary" type="button" :disabled="sending" @click="onSend">
          {{ sending ? '发送中...' : '发送' }}
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
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: 10px 12px;
}

.name {
  text-align: center;
  font-weight: 900;
}

.panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 520px;
  overflow: hidden;
}

.top {
  padding: 12px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: center;
}

.msgs {
  padding: 14px;
  display: grid;
  gap: 10px;
  background: #fbfcfe;
}

.msg {
  display: flex;
  justify-content: flex-start;
}

.msg.me {
  justify-content: flex-end;
}

.bubble {
  max-width: min(560px, 80%);
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #fff;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.msg.me .bubble {
  border-color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
}

.send {
  padding: 12px;
  border-top: 1px solid var(--border);
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  background: #fff;
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
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

