<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { liveApi, type LiveSessionResp } from '@/api/live'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const loading = ref(false)
const error = ref<string | null>(null)
const session = ref<LiveSessionResp | null>(null)
const micOn = ref(true)
const camOn = ref(true)
const sidebar = ref<'chat' | 'info' | 'tech'>('info')

let pollTimer: number | null = null

async function load() {
  loading.value = true
  error.value = null
  try {
    const live = await liveApi.getByCourse(courseId.value)
    session.value = live
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课堂失败'
  } finally {
    loading.value = false
  }
}

async function refreshStatus() {
  if (!session.value?.sessionId) return
  try {
    session.value = await liveApi.status(session.value.sessionId)
  } catch {
    // 课堂状态轮询失败时保留现状，避免频繁打断用户。
  }
}

async function leaveClass() {
  if (!session.value?.sessionId) return
  await liveApi.leave(session.value.sessionId, { leaveReason: 'USER_BACK', connectionState: 'CONNECTED' })
  await router.push({ name: 'myCourses' })
}

async function endClass() {
  if (!session.value?.sessionId) return
  await liveApi.end(session.value.sessionId, { reason: 'MANUAL_END', confirm: true })
  await router.push({ name: 'myCourses' })
}

onMounted(async () => {
  await load()
  pollTimer = window.setInterval(() => {
    void refreshStatus()
  }, 8000)
})

onUnmounted(() => {
  if (pollTimer != null) window.clearInterval(pollTimer)
})
</script>

<template>
  <div class="live-room">
    <header class="topbar card">
      <div>
        <div class="room-title">实时课堂</div>
        <div class="room-meta">
          <span>课程 #{{ courseId }}</span>
          <span v-if="session?.status">状态：{{ session.status }}</span>
          <span v-if="session?.peerJoined">对方已加入</span>
          <span v-else>正在等待对方</span>
        </div>
      </div>
      <div class="top-actions">
        <button class="btn" type="button" @click="leaveClass">离开课堂</button>
        <button class="btn btn-danger" type="button" @click="endClass">结束课程</button>
      </div>
    </header>

    <div class="stage-grid">
      <section class="stage card">
        <div class="stage-screen">
          <div v-if="!session?.peerJoined" class="waiting">
            <div class="waiting-title">已进入课堂，正在等待对方加入</div>
            <div class="waiting-desc">你可以先确认设备状态，或回到聊天提醒对方进入。</div>
          </div>
          <div v-else class="peer-tile">对方画面</div>
        </div>
        <div class="self-tile">我的画面</div>
      </section>

      <aside class="sidebar card">
        <div class="side-tabs">
          <button class="tab" :class="{ active: sidebar === 'info' }" type="button" @click="sidebar = 'info'">课程信息</button>
          <button class="tab" :class="{ active: sidebar === 'tech' }" type="button" @click="sidebar = 'tech'">技术状态</button>
          <button class="tab" :class="{ active: sidebar === 'chat' }" type="button" @click="sidebar = 'chat'">课中聊天</button>
        </div>

        <div v-if="sidebar === 'info'" class="side-section">
          <div class="row"><span>课堂状态</span><strong>{{ session?.status || '—' }}</strong></div>
          <div class="row"><span>房间</span><strong>{{ session?.providerRoomName || '—' }}</strong></div>
          <div class="row"><span>录制策略</span><strong>{{ session?.recordPolicy || 'OFF' }}</strong></div>
          <div class="row"><span>AI 观察</span><strong>{{ session?.aiPolicy || 'OFF' }}</strong></div>
        </div>

        <div v-else-if="sidebar === 'tech'" class="side-section">
          <div class="row"><span>麦克风</span><strong>{{ micOn ? '开启' : '关闭' }}</strong></div>
          <div class="row"><span>摄像头</span><strong>{{ camOn ? '开启' : '关闭' }}</strong></div>
          <div class="row"><span>网络</span><strong>{{ error ? '异常' : '稳定' }}</strong></div>
        </div>

        <div v-else class="side-section chat-panel">
          <div class="chat-empty">课中聊天将在下一轮补齐为实时消息区，这里先保留信息位。</div>
        </div>
      </aside>
    </div>

    <footer class="controls card">
      <button class="ctl" :class="{ off: !micOn }" type="button" @click="micOn = !micOn">{{ micOn ? '关闭麦克风' : '打开麦克风' }}</button>
      <button class="ctl" :class="{ off: !camOn }" type="button" @click="camOn = !camOn">{{ camOn ? '关闭摄像头' : '打开摄像头' }}</button>
      <button class="ctl" type="button" @click="sidebar = 'tech'">切换设备</button>
      <button class="ctl" type="button" @click="sidebar = 'chat'">课中聊天</button>
      <button class="ctl primary" type="button" @click="endClass">结束课程</button>
    </footer>
  </div>
</template>

<style scoped>
.live-room {
  display: grid;
  gap: 14px;
}

.topbar,
.controls,
.stage,
.sidebar {
  border-radius: 22px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.room-title {
  font-size: 22px;
  font-weight: 800;
}

.room-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.58);
}

.top-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 14px;
}

.stage {
  position: relative;
  min-height: 520px;
  padding: 16px;
  background: linear-gradient(160deg, #08191d 0%, #10262c 100%);
}

.stage-screen {
  height: 100%;
  min-height: 488px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.peer-tile {
  font-size: 28px;
  font-weight: 700;
}

.waiting {
  text-align: center;
  max-width: 420px;
}

.waiting-title {
  font-size: 28px;
  font-weight: 800;
}

.waiting-desc {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.self-tile {
  position: absolute;
  right: 28px;
  bottom: 28px;
  width: 188px;
  height: 128px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.sidebar {
  padding: 14px;
}

.side-tabs {
  display: flex;
  gap: 8px;
}

.tab {
  flex: 1 1 0;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
}

.tab.active {
  background: rgba(0, 190, 189, 0.1);
  border-color: rgba(0, 190, 189, 0.24);
}

.side-section {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.chat-empty {
  padding: 16px;
  border-radius: 16px;
  background: rgba(0, 190, 189, 0.08);
  color: rgba(0, 0, 0, 0.66);
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  padding: 14px;
}

.ctl {
  min-width: 120px;
  padding: 12px 16px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
}

.ctl.off {
  color: #b42318;
}

.ctl.primary,
.btn-danger {
  background: #0f766e;
  color: #fff;
  border-color: #0f766e;
}

@media (max-width: 1080px) {
  .stage-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: -1;
  }
}
</style>
