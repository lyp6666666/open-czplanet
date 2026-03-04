<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { chatApi } from '@/api/chat'
import { favoritesTutorsApi } from '@/api/favoritesTutors'
import { parentTutorsApi } from '@/api/parentTutors'
import type { ChatMessageResp, ChatRoomItemResp, ParentTutorCardVO, UserCardVO } from '@/api/types'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useCityStore } from '@/stores/city'
import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'
import UserCardModal from '@/ui/user/UserCardModal.vue'
import { SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()
const route = useRoute()
const cityStore = useCityStore()
const auth = useAuthStore()
const settings = useSettingsStore()

const loading = ref(false)
const error = ref<string | null>(null)

const q = ref('')
const mode = ref<string>('')
const subject = ref<string>('')

const city = computed(() => cityStore.city)

const rateMin = ref<number | null>(null)
const rateMax = ref<number | null>(null)
const rateMinInput = ref('')
const rateMaxInput = ref('')

const list = ref<ParentTutorCardVO[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const selectedUid = ref<number | null>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const detail = ref<UserCardVO | null>(null)

const applyBusy = ref(false)
const applyTipOpen = ref(false)
const applyTipText = ref('')

const cardOpen = ref(false)
const cardUid = ref<number | null>(null)

function openCard(uid: number) {
  if (!uid) return
  cardUid.value = uid
  cardOpen.value = true
}

function closeCard() {
  cardOpen.value = false
}

const openKey = ref<'' | 'mode' | 'subject' | 'rate'>('')

const checkedFavoriteIds = new Set<number>()
const favoriteMap = ref<Record<number, boolean>>({})

const modeLabel = computed(() => {
  if (!mode.value) return '不限'
  if (mode.value === 'online') return '线上'
  if (mode.value === 'offline') return '线下'
  if (mode.value === 'both') return '线上/线下'
  return mode.value
})

const subjectLabel = computed(() => {
  if (!subject.value) return '不限'
  return subject.value
})

const rateLabel = computed(() => {
  if (rateMin.value == null && rateMax.value == null) return '不限'
  const left = rateMin.value == null ? '' : String(rateMin.value)
  const right = rateMax.value == null ? '' : String(rateMax.value)
  return `${left}-${right}元/小时`
})

function toggle(key: typeof openKey.value) {
  openKey.value = openKey.value === key ? '' : key
  if (openKey.value === 'rate') {
    rateMinInput.value = rateMin.value == null ? '' : String(rateMin.value)
    rateMaxInput.value = rateMax.value == null ? '' : String(rateMax.value)
  }
}

function closeMenus() {
  openKey.value = ''
}

function selectMode(v: string) {
  mode.value = v
  closeMenus()
}

function selectSubject(v: string) {
  subject.value = v
  closeMenus()
}

function parseNumberInput(raw: string): number | null {
  const v = raw.trim()
  if (!v) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function applyRate() {
  rateMin.value = parseNumberInput(rateMinInput.value)
  rateMax.value = parseNumberInput(rateMaxInput.value)
  closeMenus()
  void refresh()
}

function clearRate() {
  rateMin.value = null
  rateMax.value = null
  rateMinInput.value = ''
  rateMaxInput.value = ''
  closeMenus()
  void refresh()
}

function displayName(it: ParentTutorCardVO): string {
  return it.displayName || `教师${it.userId}`
}

function metaText(it: ParentTutorCardVO): string {
  const parts: string[] = []
  if (it.city) parts.push(it.city)
  const m = (it.teachingMode || '').toLowerCase()
  if (m === 'online') parts.push('线上')
  else if (m === 'offline') parts.push('线下')
  else if (m === 'both') parts.push('线上/线下')
  if (it.highestEduSchool) parts.push(it.highestEduSchool)
  if (it.education) parts.push(it.education)
  if (it.experienceYears != null) parts.push(`${it.experienceYears}年`)
  if (it.subjectTags && it.subjectTags.length) parts.push(it.subjectTags.slice(0, 3).join('、'))
  return parts.join(' · ')
}

async function syncFavorites(ids: number[]) {
  const need = ids.filter((id) => !checkedFavoriteIds.has(id))
  if (!need.length) return
  need.forEach((id) => checkedFavoriteIds.add(id))
  try {
    const favoritedIds = await favoritesTutorsApi.checkTutorFavorites(need)
    const next = { ...favoriteMap.value }
    need.forEach((id) => {
      next[id] = false
    })
    favoritedIds.forEach((id) => {
      next[id] = true
    })
    favoriteMap.value = next
  } catch (e) {
    void e
  }
}

async function loadMore() {
  if (!auth.isLoggedIn) return
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await parentTutorsApi.page({
      pageSize: 20,
      cursor: cursor.value,
      q: q.value.trim() || undefined,
      city: city.value || undefined,
      mode: mode.value || undefined,
      subject: subject.value || undefined,
      rateMin: rateMin.value,
      rateMax: rateMax.value,
    })
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast
    const next = page.list || []
    list.value = [...list.value, ...next]
    if (next.length && auth.user?.userType === 2) {
      await syncFavorites(next.map((it) => it.userId))
    }
    if (selectedUid.value == null) {
      const first = list.value[0]
      if (first) selectedUid.value = first.userId
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function refresh() {
  list.value = []
  cursor.value = null
  isLast.value = false
  selectedUid.value = null
  detail.value = null
  detailError.value = null
  checkedFavoriteIds.clear()
  favoriteMap.value = {}
  await loadMore()
}

function selectTeacher(it: ParentTutorCardVO) {
  selectedUid.value = it.userId
}

async function onToggleFavorite(uid: number) {
  const current = !!favoriteMap.value[uid]
  try {
    if (current) {
      await favoritesTutorsApi.unfavoriteTutor(uid)
    } else {
      await favoritesTutorsApi.favoriteTutor(uid)
    }
    favoriteMap.value = { ...favoriteMap.value, [uid]: !current }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

function genClientRequestId() {
  const g = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : ''
  if (g) return g
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function normalizeMsgBody(raw: unknown): { type: string; content?: string } {
  if (raw && typeof raw === 'object') {
    const any = raw as Record<string, unknown>
    if (typeof any.type === 'string') return any as { type: string; content?: string }
    if (typeof any.content === 'string') return { type: 'text', content: any.content }
  }
  return { type: 'system' }
}

function isChatUnlockedByMessages(list: ChatMessageResp[]) {
  return list.some((m) => {
    const b = normalizeMsgBody(m.message?.body)
    if (b.type === 'contact_unlocked') return true
    return false
  })
}

async function findRoomByOtherUid(otherUid: number): Promise<ChatRoomItemResp | null> {
  let c: number | null = null
  for (let i = 0; i < 10; i++) {
    const page = await chatApi.listRooms({ pageSize: 50, cursor: c })
    const rooms = page.list || []
    const found = rooms.find((r) => r.otherUid === otherUid) || null
    if (found) return found
    c = page.cursor ?? null
    if (page.isLast || rooms.length === 0) break
  }
  return null
}

async function shouldReuseExistingChat(otherUid: number): Promise<{ roomId: number } | null> {
  const found = await findRoomByOtherUid(otherUid)
  if (!found?.roomId) return null
  const page = await chatApi.listMessages({ roomId: found.roomId, pageSize: 20, cursor: null })
  const msgs = page.list || []
  if (!isChatUnlockedByMessages(msgs)) return null
  return { roomId: found.roomId }
}

async function openApply() {
  if (applyBusy.value) return
  const targetUid = detail.value?.user?.id
  const tutorId = detail.value?.teacherProfile?.id
  if (!targetUid || !tutorId) return
  applyBusy.value = true
  try {
    try {
      const reuse = await shouldReuseExistingChat(targetUid)
      if (reuse?.roomId) {
        await router.push({ name: 'chatRoom', params: { roomId: String(reuse.roomId) }, query: { otherUid: String(targetUid) } })
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
      receiverUid: targetUid,
      contextType: 'TUTOR',
      contextId: tutorId,
      content,
      clientRequestId: genClientRequestId(),
    })
    await router.push({ name: 'chatRoom', params: { roomId: String(msg.message.roomId) }, query: { otherUid: String(targetUid) } })
  } catch (e) {
    const msg = e instanceof Error ? e.message : '发送申请失败'
    applyTipText.value = msg
    applyTipOpen.value = true
  } finally {
    applyBusy.value = false
  }
}

watch(
  () => selectedUid.value,
  (uid) => {
    if (!uid) return
    void (async () => {
      detailLoading.value = true
      detailError.value = null
      try {
        detail.value = await userApi.card(uid)
      } catch (e) {
        detailError.value = e instanceof Error ? e.message : '加载详情失败'
        detail.value = null
      } finally {
        detailLoading.value = false
      }
    })()
  },
  { immediate: true },
)

watch([mode, subject], () => {
  void refresh()
})

watch([city], () => {
  void refresh()
})

onMounted(() => {
  const raw = route.query.q
  if (typeof raw === 'string' && raw.trim()) {
    q.value = raw.trim()
  }
  void refresh()
})
</script>

<template>
  <div class="wrap" @click.self="closeMenus">
    <div class="head">
      <div class="search card">
        <input v-model="q" class="search-input" placeholder="搜索教师关键词" @keydown.enter.prevent="refresh" />
        <button class="btn btn-primary" type="button" :disabled="loading" @click="refresh">搜索</button>
      </div>

      <div class="tabs">
        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: !!mode || openKey === 'mode' }" @click.stop="toggle('mode')">
            <span>授课方式</span>
            <span class="val">{{ modeLabel }}</span>
          </button>
          <div v-if="openKey === 'mode'" class="menu card">
            <button class="menu-item" type="button" @click="selectMode('')">不限</button>
            <button class="menu-item" type="button" @click="selectMode('online')">线上</button>
            <button class="menu-item" type="button" @click="selectMode('offline')">线下</button>
            <button class="menu-item" type="button" @click="selectMode('both')">线上/线下</button>
          </div>
        </div>

        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: !!subject || openKey === 'subject' }" @click.stop="toggle('subject')">
            <span>教学科目</span>
            <span class="val">{{ subjectLabel }}</span>
          </button>
          <div v-if="openKey === 'subject'" class="menu card">
            <button class="menu-item" type="button" @click="selectSubject('')">不限</button>
            <button v-for="s in SUBJECT_PRESETS" :key="s" class="menu-item" type="button" @click="selectSubject(s)">
              {{ s }}
            </button>
          </div>
        </div>

        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: rateMin != null || rateMax != null || openKey === 'rate' }" @click.stop="toggle('rate')">
            <span>课时费</span>
            <span class="val">{{ rateLabel }}</span>
          </button>
          <div v-if="openKey === 'rate'" class="menu card budget">
            <div class="budget-row">
              <input v-model="rateMinInput" class="budget-input" inputmode="decimal" placeholder="下限" />
              <div class="dash">—</div>
              <input v-model="rateMaxInput" class="budget-input" inputmode="decimal" placeholder="上限" />
            </div>
            <div class="budget-ops">
              <button class="btn" type="button" @click="clearRate">清空</button>
              <button class="btn btn-primary" type="button" @click="applyRate">确定</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="workbench">
      <aside class="card left">
        <div v-if="list.length === 0 && !loading" class="empty">
          <div class="empty-title">暂无匹配教师</div>
          <div class="empty-desc">换个关键词或筛选条件试试</div>
        </div>

        <div v-else class="items">
          <button
            v-for="it in list"
            :key="it.userId"
            class="item"
            type="button"
            :class="{ active: selectedUid === it.userId }"
            @click="selectTeacher(it)"
          >
            <div class="line1">
              <div class="t">
                <img class="avatar clickable" :src="it.avatar || ''" alt="" @click.stop="openCard(it.userId)" />
                <span class="name clickable" @click.stop="openCard(it.userId)">{{ displayName(it) }}</span>
                <span v-if="it.eduVerifyStatus === 2" class="verified-badge">学信网认证</span>
              </div>
              <div v-if="it.ratePerHour" class="pay">¥{{ it.ratePerHour }}/小时</div>
            </div>
            <div class="meta">{{ metaText(it) }}</div>
            <div v-if="it.introduction" class="desc">{{ it.introduction }}</div>
          </button>
        </div>

        <div class="footer" v-if="list.length > 0">
          <button class="btn" type="button" :disabled="loading || isLast" @click="loadMore">
            <span v-if="isLast">没有更多了</span>
            <span v-else>{{ loading ? '加载中...' : '加载更多' }}</span>
          </button>
        </div>
      </aside>

      <section class="right">
        <div v-if="detailError" class="hint error">{{ detailError }}</div>
        <div v-else-if="detailLoading" class="card detail">
          <div class="d-title skeleton" />
          <div class="d-line skeleton" />
          <div class="d-line skeleton" />
          <div class="d-line skeleton" />
        </div>
        <div v-else-if="detail?.teacherProfile" class="card detail">
          <div class="detail-head">
            <div class="detail-title">
              {{ detail.teacherProfile.realName || detail.user.name || `教师${detail.user.id}` }}
              <span v-if="detail.teacherProfile.eduVerifyStatus === 2" class="verified-badge">学信网认证</span>
            </div>
            <div class="detail-ops">
              <button class="btn" type="button" @click="onToggleFavorite(detail.user.id)">
                {{ favoriteMap[detail.user.id] ? '已收藏' : '收藏' }}
              </button>
              <button class="btn btn-primary" type="button" :disabled="applyBusy" @click="openApply">
                {{ applyBusy ? '发送中...' : '发起申请' }}
              </button>
            </div>
          </div>

          <div class="detail-meta">
            <span v-if="detail.teacherProfile.city">{{ detail.teacherProfile.city }}</span>
            <span v-if="detail.teacherProfile.teachingMode">{{ detail.teacherProfile.teachingMode }}</span>
            <span v-if="detail.teacherProfile.highestEduSchool">{{ detail.teacherProfile.highestEduSchool }}</span>
            <span v-if="detail.teacherProfile.education">{{ detail.teacherProfile.education }}</span>
            <span v-if="detail.teacherProfile.experienceYears != null">{{ detail.teacherProfile.experienceYears }}年</span>
            <span v-if="detail.teacherProfile.ratePerHour">{{ detail.teacherProfile.ratePerHour }}元/小时</span>
          </div>

          <div v-if="detail.teacherProfile.subject" class="detail-block">
            <div class="detail-label">教学科目</div>
            <div class="detail-text">{{ detail.teacherProfile.subject }}</div>
          </div>

          <div class="detail-block">
            <div class="detail-label">教师简介</div>
            <div class="detail-text">{{ detail.teacherProfile.introduction || '—' }}</div>
          </div>
        </div>
        <div v-else class="card detail empty-detail">
          <div class="empty-title">选择一位教师查看详情</div>
          <div class="empty-desc">从左侧列表点击即可预览</div>
        </div>
      </section>
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
}

.search {
  padding: 12px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.search-input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tab-wrap {
  position: relative;
}

.tab {
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 12px;
  background: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  font-size: 12px;
}

.tab.active {
  border-color: rgba(0, 190, 189, 0.45);
  background: rgba(0, 190, 189, 0.06);
}

.val {
  color: var(--muted);
  font-weight: 800;
}

.menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  padding: 10px;
  border-radius: 12px;
  z-index: 20;
  display: grid;
  gap: 6px;
  min-width: 160px;
}

.menu-item {
  text-align: left;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  padding: 8px 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.budget {
  min-width: 220px;
}

.budget-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px;
  align-items: center;
}

.budget-input {
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 0 10px;
  outline: none;
  background: #fff;
}

.dash {
  color: var(--muted);
}

.budget-ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

.workbench {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 12px;
}

.left {
  padding: 12px;
  display: grid;
  gap: 10px;
  align-content: start;
}

.items {
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

.item.active {
  border-color: rgba(0, 190, 189, 0.45);
  box-shadow: 0 0 0 4px rgba(0, 190, 189, 0.08);
}

.line1 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.t {
  font-weight: 900;
  font-size: 14px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar.clickable {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
}

.name.clickable:hover {
  text-decoration: underline;
  cursor: pointer;
}

.pay {
  color: var(--primary);
  font-weight: 900;
  font-size: 13px;
  white-space: nowrap;
}

.meta {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.4;
}

.desc {
  color: #1f2329;
  font-size: 13px;
  line-height: 1.4;
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

.footer {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.right {
  display: grid;
  gap: 10px;
  align-content: start;
}

.detail {
  padding: 14px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-title {
  font-size: 16px;
  font-weight: 900;
}

.detail-ops {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.detail-meta {
  color: var(--muted);
  font-size: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-block {
  display: grid;
  gap: 6px;
}

.detail-label {
  font-weight: 900;
  font-size: 12px;
}

.detail-text {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.5;
  white-space: pre-wrap;
}

.empty-detail {
  padding: 18px;
  text-align: center;
}

.skeleton {
  background: linear-gradient(90deg, rgba(31, 35, 41, 0.06), rgba(31, 35, 41, 0.12), rgba(31, 35, 41, 0.06));
  background-size: 200% 100%;
  animation: sk 1.2s ease-in-out infinite;
  border-radius: 10px;
}

.d-title {
  height: 18px;
  width: 60%;
}

.d-line {
  height: 12px;
  width: 92%;
}

@keyframes sk {
  0% {
    background-position: 0% 0%;
  }
  100% {
    background-position: -200% 0%;
  }
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 60;
}

.modal {
  width: min(520px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.m-title {
  font-weight: 900;
  font-size: 16px;
}

.m-desc {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.m-ops {
  display: flex;
  justify-content: flex-end;
}

.verified-badge {
  display: inline-block;
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 0 4px;
  font-size: 10px;
  line-height: 16px;
  vertical-align: middle;
  margin-left: 6px;
  font-weight: normal;
}
</style>
