<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { chatApi } from '@/api/chat'
import { favoritesTutorsApi } from '@/api/favoritesTutors'
import { parentTutorsApi } from '@/api/parentTutors'
import type { ChatMessageResp, ChatRoomItemResp, ParentTutorCardVO, StudentJobPosting, UserCardVO } from '@/api/types'
import { jobsApi } from '@/api/jobs'
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
const LIST_PAGE_SIZE = 6

const canUseStudentActions = computed(() => auth.user?.userType === 2)
const canApplyToTutor = computed(() => auth.user?.userType === 2 || auth.user?.userType === 3)
const isOrg = computed(() => auth.user?.userType === 3)

const q = ref('')
const subject = ref<string>('')
const city = computed(() => cityStore.city)

const rateMin = ref<number | null>(null)
const rateMax = ref<number | null>(null)
const rateMinInput = ref('')
const rateMaxInput = ref('')

const list = ref<ParentTutorCardVO[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)
const listPageIndex = ref(0)
const avatarFailedMap = ref<Record<number, boolean>>({})

const selectedUid = ref<number | null>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const detail = ref<UserCardVO | null>(null)

const applyBusy = ref(false)
const applyTipOpen = ref(false)
const applyTipText = ref('')
const teachingModeModalOpen = ref(false)
const pendingApplyTarget = ref<{ uid: number; tutorId: number } | null>(null)

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

const openKey = ref<'' | 'subject' | 'rate'>('')

const checkedFavoriteIds = new Set<number>()
const favoriteMap = ref<Record<number, boolean>>({})

const orgDemands = ref<StudentJobPosting[]>([])
const orgDemandId = ref<number | null>(null)

const visibleList = computed(() => {
  const start = listPageIndex.value * LIST_PAGE_SIZE
  return list.value.slice(start, start + LIST_PAGE_SIZE)
})

const listPageCount = computed(() => Math.max(1, Math.ceil(list.value.length / LIST_PAGE_SIZE)))

const listRangeText = computed(() => {
  if (!list.value.length) return '暂无结果'
  const start = listPageIndex.value * LIST_PAGE_SIZE + 1
  const end = Math.min(list.value.length, start + LIST_PAGE_SIZE - 1)
  return `${start}-${end} / 已加载 ${list.value.length}`
})

const canPrevListPage = computed(() => listPageIndex.value > 0)
const canNextListPage = computed(() => (listPageIndex.value + 1) * LIST_PAGE_SIZE < list.value.length || !isLast.value)

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

function userInitial(name: string | null | undefined): string {
  const n = String(name || '').trim()
  return n ? n.slice(0, 1) : 'U'
}

function markAvatarFailed(uid: number) {
  if (!uid) return
  if (avatarFailedMap.value[uid]) return
  avatarFailedMap.value = { ...avatarFailedMap.value, [uid]: true }
}

function metaText(it: ParentTutorCardVO): string {
  const parts: string[] = []
  if (it.city) parts.push(it.city)
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
      subject: subject.value || undefined,
      rateMin: rateMin.value,
      rateMax: rateMax.value,
    })
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast
    const next = page.list || []
    list.value = [...list.value, ...next]
    if (next.length && canUseStudentActions.value) {
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

function selectFirstVisibleTeacher() {
  const first = visibleList.value[0]
  if (!first) return
  if (!visibleList.value.some((it) => it.userId === selectedUid.value)) {
    selectedUid.value = first.userId
  }
}

function prevListPage() {
  if (!canPrevListPage.value) return
  listPageIndex.value -= 1
  selectFirstVisibleTeacher()
}

async function nextListPage() {
  if (!canNextListPage.value) return
  const hasLoadedNext = (listPageIndex.value + 1) * LIST_PAGE_SIZE < list.value.length
  if (!hasLoadedNext) {
    const before = list.value.length
    await loadMore()
    if (list.value.length <= before) return
  }
  listPageIndex.value += 1
  selectFirstVisibleTeacher()
}

async function loadOrgDemands() {
  if (!auth.isLoggedIn) return
  if (!isOrg.value) return
  try {
    const page = await jobsApi.mineOrgDemands({ pageSize: 50, cursor: null })
    const items = page.list || []
    orgDemands.value = items
    if (orgDemandId.value == null && items.length > 0) {
      orgDemandId.value = items[0]?.id ?? null
    }
  } catch {
    void 0
  }
}

async function refresh() {
  list.value = []
  cursor.value = null
  isLast.value = false
  listPageIndex.value = 0
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

function formatTeachingMode(raw: string | null | undefined): string {
  const v = String(raw || '').toLowerCase()
  if (v === 'online') return '线上授课'
  if (v === 'offline') return '线下授课'
  return '以需求为准'
}

function splitSubjectTags(raw: string | null | undefined): string[] {
  return String(raw || '')
    .split(/[,，、/\s]+/)
    .map((it) => it.trim())
    .filter(Boolean)
    .slice(0, 8)
}

function formatHistoryTime(raw: string | null | undefined): string {
  const text = String(raw || '').trim()
  if (!text) return '时间待定'
  const parsed = Date.parse(text)
  if (!Number.isFinite(parsed)) return text
  const d = new Date(parsed)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function formatHistoryDuration(minutes: number | null | undefined): string {
  if (!minutes || minutes <= 0) return '时长待定'
  if (minutes < 60) return `${minutes}分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m ? `${h}小时${m}分钟` : `${h}小时`
}

function formatHistoryStatus(status: number | null | undefined): string {
  if (status === 1) return '待确认'
  if (status === 2) return '已确认'
  if (status === 3) return '已完成'
  if (status === 4) return '已取消'
  return '状态待更新'
}

function profileFieldFilled(value: unknown): boolean {
  return String(value ?? '').trim().length > 0
}

const teacherSubjectTags = computed(() => splitSubjectTags(detail.value?.teacherProfile?.subject))

const teacherStats = computed(() => {
  const profile = detail.value?.teacherProfile
  const history = detail.value?.teacherHistory || []
  return [
    { label: '历史课程', value: `${history.length}`, hint: history.length ? '平台记录' : '暂无记录' },
    { label: '教学经验', value: profile?.experienceYears != null ? `${profile.experienceYears}年` : '待补充', hint: '以老师资料为准' },
    { label: '资料完整度', value: `${Math.round(([
      profile?.realName,
      profile?.subject,
      profile?.introduction,
      profile?.highestEduSchool,
      profile?.education,
      profile?.ratePerHour,
    ].filter(profileFieldFilled).length / 6) * 100)}%`, hint: '基础信息' },
  ]
})

async function onToggleFavorite(uid: number) {
  if (!canUseStudentActions.value) return
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
  if (!canApplyToTutor.value) return
  if (applyBusy.value) return
  const targetUid = detail.value?.user?.id
  const tutorId = detail.value?.teacherProfile?.id
  if (!targetUid || !tutorId) return
  pendingApplyTarget.value = { uid: targetUid, tutorId }
  teachingModeModalOpen.value = true
}

async function confirmTeachingMode(teachingMode: 'ONLINE' | 'OFFLINE') {
  const target = pendingApplyTarget.value
  if (!target) return
  teachingModeModalOpen.value = false
  applyBusy.value = true
  try {
    try {
      const reuse = await shouldReuseExistingChat(target.uid)
      if (reuse?.roomId) {
        await router.push({ name: 'chatRoom', params: { roomId: String(reuse.roomId) }, query: { otherUid: String(target.uid) } })
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
    if (isOrg.value && !orgDemandId.value) {
      applyTipText.value = '请先发布一条机构需求后再向教师发起申请'
      applyTipOpen.value = true
      return
    }
    const msg = await applicationApi.startChat({
      receiverUid: target.uid,
      contextType: isOrg.value ? 'ORG_POSTING' : 'TUTOR',
      contextId: isOrg.value ? orgDemandId.value! : target.tutorId,
      content,
      teachingMode,
      clientRequestId: genClientRequestId(),
    })
    await router.push({ name: 'chatRoom', params: { roomId: String(msg.message.roomId) }, query: { otherUid: String(target.uid) } })
  } catch (e) {
    const msg = e instanceof Error ? e.message : '发送申请失败'
    applyTipText.value = msg
    applyTipOpen.value = true
  } finally {
    applyBusy.value = false
    pendingApplyTarget.value = null
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

watch([subject], () => {
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
  void loadOrgDemands()
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
        <div class="list-head">
          <div>
            <div class="list-title">教师列表</div>
            <div class="list-sub">{{ listRangeText }}</div>
          </div>
          <div class="pager-mini">
            <button class="pager-btn" type="button" :disabled="!canPrevListPage" @click="prevListPage">上一页</button>
            <button class="pager-btn" type="button" :disabled="!canNextListPage || loading" @click="nextListPage">
              {{ loading && !isLast ? '加载中' : '下一页' }}
            </button>
          </div>
        </div>

        <div v-if="list.length === 0 && !loading" class="empty">
          <div class="empty-title">暂无匹配教师</div>
          <div class="empty-desc">换个关键词或筛选条件试试</div>
        </div>

        <div v-else class="items">
          <button
            v-for="it in visibleList"
            :key="it.userId"
            class="item"
            type="button"
            :class="{ active: selectedUid === it.userId }"
            @click="selectTeacher(it)"
          >
            <div class="line1">
              <div class="t">
                <img
                  v-if="it.avatar && !avatarFailedMap[it.userId]"
                  class="avatar clickable"
                  :src="it.avatar"
                  alt=""
                  @error="markAvatarFailed(it.userId)"
                  @click.stop="openCard(it.userId)"
                />
                <div v-else class="avatar clickable fallback" @click.stop="openCard(it.userId)">{{ userInitial(displayName(it)) }}</div>
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
          <span class="footer-note">第 {{ listPageIndex + 1 }} / {{ listPageCount }} 页</span>
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
            <div>
              <div class="detail-title">
                {{ detail.teacherProfile.realName || detail.user.name || `教师${detail.user.id}` }}
                <span v-if="detail.teacherProfile.eduVerifyStatus === 2" class="verified-badge">学信网认证</span>
              </div>
              <div class="detail-subtitle">
                {{ [detail.teacherProfile.city, detail.teacherProfile.highestEduSchool].filter(Boolean).join(' · ') }}
              </div>
            </div>
            <div v-if="canApplyToTutor" class="detail-ops">
              <button v-if="canUseStudentActions" class="btn" type="button" @click="onToggleFavorite(detail.user.id)">
                {{ favoriteMap[detail.user.id] ? '已收藏' : '收藏' }}
              </button>
              <select v-if="isOrg" v-model.number="orgDemandId" class="input sm">
                <option v-if="orgDemands.length === 0" :value="null">暂无需求</option>
                <option v-for="d in orgDemands" :key="d.id" :value="d.id">{{ d.title }}</option>
              </select>
              <button class="btn btn-primary" type="button" :disabled="applyBusy" @click="openApply">
                {{ applyBusy ? '发送中...' : '发起聊天申请' }}
              </button>
            </div>
          </div>

          <div class="stat-grid">
            <div v-for="stat in teacherStats" :key="stat.label" class="stat-card">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-hint">{{ stat.hint }}</div>
            </div>
          </div>

          <div v-if="canApplyToTutor" class="apply-note">
            <div class="apply-note-title">沟通前提示</div>
            <div class="apply-note-text">发起的是聊天申请。教师通过后仍由教师支付平台信息费，支付完成后双方才进入可沟通状态。</div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">教学资料</div>
              <div class="section-sub">快速判断老师是否匹配你的需求</div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span>授课形式</span>
                <strong>发起申请时选择，创建后不可修改</strong>
              </div>
              <div class="info-item">
                <span>课时费</span>
                <strong>{{ detail.teacherProfile.ratePerHour ? `${detail.teacherProfile.ratePerHour}元/小时` : '待沟通' }}</strong>
              </div>
              <div class="info-item">
                <span>学历背景</span>
                <strong>{{ [detail.teacherProfile.highestEduSchool, detail.teacherProfile.education].filter(Boolean).join(' · ') || '待补充' }}</strong>
              </div>
              <div class="info-item">
                <span>所在城市</span>
                <strong>{{ detail.teacherProfile.city || '待补充' }}</strong>
              </div>
            </div>
            <div class="subject-tags">
              <span v-if="!teacherSubjectTags.length" class="soft-empty">老师暂未填写教学科目</span>
              <span v-for="s in teacherSubjectTags" :key="s" class="subject-tag">{{ s }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">教师简介</div>
              <div class="section-sub">教学风格、擅长方向和可沟通重点</div>
            </div>
            <div class="detail-text rich-text">{{ detail.teacherProfile.introduction || '老师暂未补充详细简介，可先发起沟通了解教学经验、课程安排和匹配度。' }}</div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">历史课程</div>
              <div class="section-sub">平台内可见的预约/课程记录</div>
            </div>
            <div v-if="detail.teacherHistory?.length" class="history-list">
              <div v-for="h in detail.teacherHistory.slice(0, 4)" :key="h.id" class="history-card">
                <div class="history-title">{{ h.title || '课程记录' }}</div>
                <div class="history-meta">
                  <span>{{ h.city || '城市待定' }}</span>
                  <span>{{ formatTeachingMode(h.classMode) }}</span>
                  <span>{{ formatHistoryDuration(h.durationMinutes) }}</span>
                </div>
                <div class="history-foot">
                  <span>{{ formatHistoryTime(h.startTime) }}</span>
                  <span>{{ formatHistoryStatus(h.status) }}</span>
                </div>
              </div>
            </div>
            <div v-else class="placeholder-card">
              <div class="placeholder-title">暂无历史课程记录</div>
              <div class="placeholder-desc">老师可能是新入驻，或历史课程暂未对外展示。</div>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">历史评价</div>
              <div class="section-sub">评价能力预留区，后续接入真实评价后自动展示</div>
            </div>
            <div class="placeholder-card">
              <div class="placeholder-title">暂无公开评价</div>
              <div class="placeholder-desc">平台暂未开放评价数据，可先关注老师资料完整度、认证状态与沟通反馈。</div>
            </div>
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

    <div v-if="teachingModeModalOpen" class="mask" @click.self="teachingModeModalOpen = false">
      <div class="modal card">
        <div class="m-title">请选择授课形式</div>
        <div class="m-desc">授课形式创建后不可修改。线下只做撮合，线上会进入平台课程与支付流程。</div>
        <div class="mode-actions">
          <button class="mode-card" type="button" :disabled="applyBusy" @click="confirmTeachingMode('ONLINE')">
            <strong>线上授课</strong>
            <span>推荐，在平台内上课并使用后续课程与支付服务</span>
          </button>
          <button class="mode-card" type="button" :disabled="applyBusy" @click="confirmTeachingMode('OFFLINE')">
            <strong>线下授课</strong>
            <span>平台只负责帮你联系老师，后续自行沟通安排</span>
          </button>
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

.mode-actions {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.mode-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: #fff;
  display: grid;
  gap: 6px;
  text-align: left;
  cursor: pointer;
}

.mode-card strong {
  font-size: 15px;
}

.mode-card span {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
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
  grid-template-columns: minmax(320px, 390px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.left {
  height: min(760px, calc(100vh - 178px));
  min-height: 620px;
  padding: 14px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 12px;
  overflow: hidden;
  border-radius: 18px;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 2px;
}

.list-title {
  font-size: 15px;
  font-weight: 900;
}

.list-sub {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

.pager-mini {
  display: flex;
  gap: 6px;
  flex: 0 0 auto;
}

.pager-btn {
  height: 30px;
  padding: 0 9px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
}

.pager-btn:disabled {
  cursor: not-allowed;
  color: var(--muted);
  background: rgba(31, 35, 41, 0.04);
}

.items {
  display: grid;
  gap: 10px;
  align-content: start;
  overflow: auto;
  padding: 2px 4px 2px 0;
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
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease, background 160ms ease;
}

.item.active {
  border-color: rgba(0, 190, 189, 0.45);
  background: linear-gradient(135deg, rgba(0, 190, 189, 0.08), #fff 62%);
  box-shadow: 0 0 0 4px rgba(0, 190, 189, 0.08);
}

.item:hover {
  transform: translateY(-1px);
  border-color: rgba(0, 190, 189, 0.28);
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

.avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 800;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
  padding-top: 2px;
}

.footer-note {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.right {
  align-self: start;
}

.detail {
  height: min(760px, calc(100vh - 178px));
  min-height: 620px;
  padding: 18px;
  border-radius: 20px;
  display: grid;
  gap: 14px;
  align-content: start;
  overflow: auto;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.08), transparent 34%),
    #fff;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-title {
  font-size: 22px;
  line-height: 1.2;
  font-weight: 900;
}

.detail-subtitle {
  margin-top: 7px;
  color: var(--muted);
  font-size: 13px;
}

.detail-ops {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.stat-card {
  min-height: 86px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(15, 118, 110, 0.1);
  background: rgba(246, 251, 251, 0.86);
}

.stat-value {
  color: #0f766e;
  font-size: 22px;
  line-height: 1;
  font-weight: 900;
}

.stat-label {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 900;
}

.stat-hint {
  margin-top: 4px;
  color: var(--muted);
  font-size: 11px;
}

.apply-note {
  padding: 12px 14px;
  border-radius: 16px;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.12), transparent 32%),
    rgba(240, 253, 250, 0.82);
  border: 1px solid rgba(0, 190, 189, 0.16);
}

.apply-note-title {
  color: #0f766e;
  font-size: 13px;
  font-weight: 900;
}

.apply-note-text {
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
}

.detail-section {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.section-head {
  display: grid;
  gap: 4px;
}

.section-title {
  font-weight: 900;
  font-size: 15px;
}

.section-sub {
  color: var(--muted);
  font-size: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-item {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.035);
}

.info-item span {
  color: var(--muted);
  font-size: 12px;
}

.info-item strong {
  color: var(--text);
  font-size: 13px;
  line-height: 1.45;
}

.subject-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.subject-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.09);
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
}

.soft-empty {
  color: var(--muted);
  font-size: 13px;
}

.detail-text {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.7;
  white-space: pre-wrap;
}

.rich-text {
  padding: 12px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.035);
}

.history-list {
  display: grid;
  gap: 10px;
}

.history-card,
.placeholder-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(31, 35, 41, 0.025);
}

.history-title,
.placeholder-title {
  font-size: 13px;
  font-weight: 900;
}

.history-meta,
.history-foot {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.history-foot {
  justify-content: space-between;
}

.placeholder-desc {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}

.empty-detail {
  padding: 18px;
  text-align: center;
  place-items: center;
  align-content: center;
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
