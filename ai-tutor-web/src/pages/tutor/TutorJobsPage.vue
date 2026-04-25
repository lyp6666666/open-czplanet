<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { DemandViewVO, StudentJobPosting, TutorApplicationVO } from '@/api/types'
import { useChatRealtimeStore } from '@/stores/chatRealtime'
import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'
import { useCityStore } from '@/stores/city'
import { useToastStore } from '@/stores/toast'
import OrgCardModal from '@/ui/user/OrgCardModal.vue'
import { formatClassMode, formatEducationRequirement, formatScheduleText } from '@/utils/present'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()
const route = useRoute()
const chatRealtime = useChatRealtimeStore()
const settings = useSettingsStore()
const cityStore = useCityStore()
const toast = useToastStore()

const loading = ref(false)
const error = ref<string | null>(null)
const LIST_PAGE_SIZE = 6

const q = ref('')
const subject = ref<string>('')
const classMode = ref<string>('')
const stageCode = ref<string>('')
const educationRequirement = ref<string>('')
const frequencyPerWeek = ref<number | null>(null)
const teacherGenderPreference = ref<string>('')

const city = computed(() => cityStore.city)

const budgetMin = ref<number | null>(null)
const budgetMax = ref<number | null>(null)
const budgetMinInput = ref('')
const budgetMaxInput = ref('')

const list = ref<StudentJobPosting[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)
const listPageIndex = ref(0)
const selectedId = ref<number | null>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const detail = ref<DemandViewVO | null>(null)
const publisherAvatarFailed = ref(false)
const pendingHighlightId = ref<number | null>(null)

const detailApplicationLoading = ref(false)
const detailApplication = ref<TutorApplicationVO | null>(null)
let detailApplicationPollTimer: number | null = null

const applyBusy = ref(false)
const applyError = ref<string | null>(null)
const applyTipOpen = ref(false)
const applyTipText = ref('')

const orgCardOpen = ref(false)
const orgCardId = ref<number | null>(null)

function openOrgCard(id: number) {
  if (!id) return
  orgCardId.value = id
  orgCardOpen.value = true
}

const openKey = ref<'' | 'type' | 'subject' | 'budget' | 'stage' | 'edu' | 'freq' | 'tGender'>('')

const stageOptions = [
  { value: '', label: '不限' },
  { value: 'PRESCHOOL', label: '幼教育' },
  { value: 'PRIMARY', label: '小学' },
  { value: 'JUNIOR', label: '初中' },
  { value: 'SENIOR', label: '高中' },
  { value: 'OTHER', label: '其他' },
]

const eduOptions = [
  { value: 'UNLIMITED', label: '不限' },
  { value: 'TOP2', label: 'top2' },
  { value: 'C985', label: '985' },
  { value: 'C211', label: '211' },
  { value: 'DOUBLE_FIRST_CLASS', label: '双一流' },
  { value: 'FIRST_TIER', label: '一本' },
  { value: 'BACHELOR', label: '本科' },
  { value: 'OVERSEAS', label: '海归' },
  { value: 'QS50', label: 'QS前50' },
]

const checkedFavoriteIds = new Set<number>()
const favoriteMap = ref<Record<number, boolean>>({})

const maxListPageIndex = computed(() => Math.max(0, Math.ceil(list.value.length / LIST_PAGE_SIZE) - 1))

const visibleList = computed(() => {
  const safePageIndex = Math.min(listPageIndex.value, maxListPageIndex.value)
  const start = safePageIndex * LIST_PAGE_SIZE
  return list.value.slice(start, start + LIST_PAGE_SIZE)
})

const listPageCount = computed(() => Math.max(1, Math.ceil(list.value.length / LIST_PAGE_SIZE)))

const listRangeText = computed(() => {
  if (!list.value.length) return '暂无结果'
  const safePageIndex = Math.min(listPageIndex.value, maxListPageIndex.value)
  const start = safePageIndex * LIST_PAGE_SIZE + 1
  const end = Math.min(list.value.length, start + LIST_PAGE_SIZE - 1)
  return `${start}-${end} / 已加载 ${list.value.length}`
})

const canPrevListPage = computed(() => listPageIndex.value > 0)
const canNextListPage = computed(() => listPageIndex.value < maxListPageIndex.value || !isLast.value)

const typeLabel = computed(() => {
  if (!classMode.value) return '不限'
  if (classMode.value === 'online') return '线上'
  if (classMode.value === 'offline') return '线下'
  return classMode.value
})

const subjectLabel = computed(() => {
  if (!subject.value) return '不限'
  if (subject.value === SUBJECT_OTHER_VALUE) return '其他'
  return subject.value
})

const stageLabel = computed(() => stageOptions.find((o) => o.value === stageCode.value)?.label ?? '不限')

const eduLabel = computed(() => eduOptions.find((o) => o.value === educationRequirement.value)?.label ?? '不限')

const teacherGenderLabel = computed(() => {
  if (!teacherGenderPreference.value) return '不限'
  if (teacherGenderPreference.value === 'male') return '男'
  if (teacherGenderPreference.value === 'female') return '女'
  return '均可'
})

const freqLabel = computed(() => {
  if (!frequencyPerWeek.value) return '不限'
  return `每周${frequencyPerWeek.value}次`
})

const budgetLabel = computed(() => {
  if (budgetMin.value == null && budgetMax.value == null) return '不限'
  const left = budgetMin.value == null ? '' : String(budgetMin.value)
  const right = budgetMax.value == null ? '' : String(budgetMax.value)
  return `${left}-${right}元/小时`
})

function toggle(key: typeof openKey.value) {
  openKey.value = openKey.value === key ? '' : key
  if (openKey.value === 'budget') {
    budgetMinInput.value = budgetMin.value == null ? '' : String(budgetMin.value)
    budgetMaxInput.value = budgetMax.value == null ? '' : String(budgetMax.value)
  }
}

function closeMenus() {
  openKey.value = ''
}

function selectType(v: string) {
  classMode.value = v
  closeMenus()
}

function selectSubject(v: string) {
  subject.value = v
  closeMenus()
}

function selectStage(v: string) {
  stageCode.value = v
  closeMenus()
}

function selectEdu(v: string) {
  educationRequirement.value = v
  closeMenus()
}

function selectTeacherGender(v: string) {
  teacherGenderPreference.value = v
  closeMenus()
}

function selectFreq(v: number | null) {
  frequencyPerWeek.value = v
  closeMenus()
}

function parseBudgetInput(raw: string): number | null {
  const v = raw.trim()
  if (!v) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function commitBudgetInputs() {
  const minRaw = budgetMinInput.value.trim()
  const maxRaw = budgetMaxInput.value.trim()
  if (!minRaw && !maxRaw) return
  budgetMin.value = parseBudgetInput(minRaw)
  budgetMax.value = parseBudgetInput(maxRaw)
}

function applyBudget() {
  budgetMin.value = parseBudgetInput(budgetMinInput.value)
  budgetMax.value = parseBudgetInput(budgetMaxInput.value)
  closeMenus()
  void refresh()
}

function clearBudget() {
  budgetMin.value = null
  budgetMax.value = null
  budgetMinInput.value = ''
  budgetMaxInput.value = ''
  closeMenus()
  void refresh()
}

async function syncFavorites(ids: number[]) {
  const need = ids.filter((id) => !checkedFavoriteIds.has(id))
  if (!need.length) return
  need.forEach((id) => checkedFavoriteIds.add(id))
  try {
    const favoritedIds = await favoritesApi.checkDemandFavorites(need)
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

async function refresh() {
  commitBudgetInputs()
  list.value = []
  cursor.value = null
  isLast.value = false
  listPageIndex.value = 0
  checkedFavoriteIds.clear()
  favoriteMap.value = {}
  selectedId.value = null
  detail.value = null
  detailError.value = null
  await loadMore()
  await tryHighlight()
}

async function tryHighlight() {
  const id = pendingHighlightId.value
  if (id == null) return
  let guard = 0
  while (!list.value.some((it) => it.id === id) && !isLast.value && guard < 6) {
    await loadMore()
    guard += 1
  }
  if (list.value.some((it) => it.id === id)) {
    selectedId.value = id
    await nextTick()
    const el = document.getElementById(`demand-item-${id}`)
    if (el) {
      el.scrollIntoView({ block: 'center', behavior: 'smooth' })
    }
  }
  pendingHighlightId.value = null
}

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const cityFilter = city.value.trim()
    const page = await jobsApi.feedDemands({
      pageSize: 10,
      cursor: cursor.value,
      subject: subject.value && subject.value !== SUBJECT_OTHER_VALUE ? subject.value : undefined,
      subjectOther: subject.value === SUBJECT_OTHER_VALUE ? true : undefined,
      classMode: classMode.value || undefined,
      city: classMode.value && classMode.value !== 'online' && cityFilter && cityFilter !== '全国' ? cityFilter : undefined,
      stageCode: stageCode.value || undefined,
      educationRequirement: educationRequirement.value || undefined,
      frequencyPerWeek: frequencyPerWeek.value ?? undefined,
      teacherGenderPreference: teacherGenderPreference.value || undefined,
      budgetMin: budgetMin.value ?? undefined,
      budgetMax: budgetMax.value ?? undefined,
      q: q.value.trim() || undefined,
      sort: 'latest',
    })
    list.value = [...list.value, ...(page.list || [])]
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast
    await syncFavorites((page.list || []).map((it) => it.id))
    if (selectedId.value == null && list.value.length > 0) {
      selectedId.value = list.value[0]?.id ?? null
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function selectFirstVisibleDemand() {
  const first = visibleList.value[0]
  if (!first) return
  if (!visibleList.value.some((it) => it.id === selectedId.value)) {
    selectedId.value = first.id
  }
}

function prevListPage() {
  if (!canPrevListPage.value) return
  listPageIndex.value -= 1
  selectFirstVisibleDemand()
}

async function nextListPage() {
  if (!canNextListPage.value) return
  const hasLoadedNext = listPageIndex.value < maxListPageIndex.value
  if (!hasLoadedNext) {
    const before = list.value.length
    await loadMore()
    if (list.value.length <= before) {
      listPageIndex.value = Math.min(listPageIndex.value, maxListPageIndex.value)
      return
    }
  }
  listPageIndex.value = Math.min(listPageIndex.value + 1, maxListPageIndex.value)
  selectFirstVisibleDemand()
}

function openDetail(it: StudentJobPosting) {
  selectedId.value = it.id
}

function formatGradeCode(raw: string | null | undefined): string {
  const map: Record<string, string> = {
    PRESCHOOL: '幼儿',
    GRADE1: '一年级',
    GRADE2: '二年级',
    GRADE3: '三年级',
    GRADE4: '四年级',
    GRADE5: '五年级',
    GRADE6: '六年级',
    JUNIOR1: '初一',
    JUNIOR2: '初二',
    JUNIOR3: '初三',
    SENIOR1: '高一',
    SENIOR2: '高二',
    SENIOR3: '高三',
    SELF_EXAM: '自考生',
    COLLEGE1: '大一',
    COLLEGE2: '大二',
    COLLEGE3: '大三',
    COLLEGE4: '大四',
    ADULT: '成人',
  }
  const key = String(raw || '').trim()
  return map[key] || key || '待补充'
}

function formatGender(raw: string | null | undefined): string {
  const v = String(raw || '').toLowerCase()
  if (v === 'male') return '男'
  if (v === 'female') return '女'
  if (v === 'both') return '均可'
  return raw || '不限'
}

function formatBudgetRange(it: Pick<StudentJobPosting, 'budgetMin' | 'budgetMax'>): string {
  if (!it.budgetMin && !it.budgetMax) return '预算待沟通'
  return `${it.budgetMin || '-'}-${it.budgetMax || '-'}元/小时`
}

function formatSubjectName(it: Pick<StudentJobPosting, 'subjectName' | 'subjectIsOther'>): string {
  if (it.subjectIsOther) return it.subjectName || '其他科目'
  return it.subjectName || '科目待补充'
}

const demandStats = computed(() => {
  const d = detail.value
  if (!d) return []
  return [
    { label: '预算', value: formatBudgetRange(d), hint: '按小时计费' },
    { label: '频次', value: renderFrequency(d), hint: '可沟通调整' },
    { label: '授课方式', value: formatClassMode(d.classMode), hint: renderLocation(d) },
  ]
})

const demandCompleteness = computed(() => {
  const d = detail.value
  if (!d) return 0
  const filled = [
    d.title,
    d.description,
    d.subjectName,
    d.availableTime || d.schedule,
    d.teacherRequirementDetail,
    d.city || d.classMode === 'online',
    d.budgetMin || d.budgetMax,
    d.gradeCode,
  ].filter((it) => String(it ?? '').trim()).length
  return Math.round((filled / 8) * 100)
})

function genClientRequestId() {
  const g = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : ''
  if (g) return g
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
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

function stopDetailApplicationPolling() {
  if (detailApplicationPollTimer != null) {
    clearInterval(detailApplicationPollTimer)
    detailApplicationPollTimer = null
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

async function loadDetailApplication() {
  stopDetailApplicationPolling()
  detailApplication.value = null
  if (!detail.value) return
  detailApplicationLoading.value = true
  try {
    const app = await findLatestSentDemandApplication(detail.value.parentId, detail.value.id)
    detailApplication.value = app
    if (app?.status === 'PENDING') {
      detailApplicationPollTimer = window.setInterval(() => {
        void reloadDetailApplication('polling')
      }, 5000)
    }
  } finally {
    detailApplicationLoading.value = false
  }
}

async function reloadDetailApplication(reason: 'realtime' | 'polling') {
  const appId = detailApplication.value?.id
  if (!(appId && appId > 0)) return
  try {
    const latest = await applicationApi.detail(appId)
    detailApplication.value = latest
    if (latest.status !== 'PENDING') {
      stopDetailApplicationPolling()
      return
    }
    if (reason === 'realtime' && detailApplicationPollTimer == null) {
      // 列表页详情侧栏优先走实时刷新，但保留轮询兜底，避免丢事件后状态长期卡住。
      detailApplicationPollTimer = window.setInterval(() => {
        void reloadDetailApplication('polling')
      }, 5000)
    }
  } catch {
    void 0
  }
}

const applyButton = computed(() => {
  const d = detail.value
  if (!d) return { text: '发起申请', disabled: true, mode: 'apply' as const }
  const app = detailApplication.value
  if (app?.status === 'ACCEPTED') return { text: '已发起沟通', disabled: false, mode: 'chat' as const }
  if (app?.status === 'PENDING') return { text: '已发起申请', disabled: true, mode: 'pending' as const }
  if (isDemandClosed(d)) return { text: '已关闭', disabled: true, mode: 'closed' as const }
  if (!app || app.status === 'REJECTED') return { text: '发起申请', disabled: false, mode: 'apply' as const }
  return { text: '发起申请', disabled: false, mode: 'apply' as const }
})

async function openChatForCurrentDetail() {
  if (!detail.value) return
  const roomId = detailApplication.value?.roomId
  if (!roomId) return
  await router.push({ name: 'chatRoom', params: { roomId: String(roomId) }, query: { otherUid: String(detail.value.parentId) } })
}

async function onClickApplyButton() {
  if (!detail.value) return
  if (applyButton.value.mode === 'chat') {
    await openChatForCurrentDetail()
    return
  }
  await openApply(detail.value)
}

async function openApply(it: StudentJobPosting) {
  if (applyBusy.value) return
  applyError.value = null
  applyBusy.value = true
  try {
    if (!settings.loaded) {
      try {
        await settings.load()
      } catch {
        void 0
      }
    }
    const content = (settings.applicationGreeting || DEFAULT_APPLICATION_GREETING).trim() || DEFAULT_APPLICATION_GREETING
    const msg = await applicationApi.startChat({
      receiverUid: it.parentId,
      contextType: 'DEMAND',
      contextId: it.id,
      content,
      clientRequestId: genClientRequestId(),
    })
    const parsed = parseTutorApplicationMsgBody(msg.message?.body)
    if (parsed) {
      detailApplication.value = {
        id: parsed.applicationId,
        senderUid: msg.fromUser.uid,
        receiverUid: it.parentId,
        senderRole: 'TEACHER',
        receiverRole: 'STUDENT',
        contextType: 'DEMAND',
        contextId: it.id,
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
    await router.push({ name: 'chatRoom', params: { roomId: String(msg.message.roomId) }, query: { otherUid: String(it.parentId) } })
  } catch (e) {
    const msg = e instanceof Error ? e.message : '发送申请失败'
    applyError.value = msg
    applyTipText.value = msg
    applyTipOpen.value = true
  } finally {
    applyBusy.value = false
  }
}

async function onToggleFavorite(it: StudentJobPosting) {
  const current = !!favoriteMap.value[it.id]
  try {
    if (current) {
      await favoritesApi.unfavoriteDemand(it.id)
    } else {
      await favoritesApi.favoriteDemand(it.id)
    }
    favoriteMap.value = { ...favoriteMap.value, [it.id]: !current }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

watch([subject, classMode, stageCode, educationRequirement, teacherGenderPreference], () => {
  void refresh()
})

watch([frequencyPerWeek], () => {
  void refresh()
})

watch(
  () => selectedId.value,
  (id) => {
    if (id == null) return
    void (async () => {
      detailLoading.value = true
      detailError.value = null
      try {
        detail.value = await jobsApi.getDemandView(id)
        await loadDetailApplication()
      } catch (e) {
        detailError.value = e instanceof Error ? e.message : '加载详情失败'
        detail.value = null
        stopDetailApplicationPolling()
        detailApplication.value = null
      } finally {
        detailLoading.value = false
      }
    })()
  },
  { immediate: true },
)

watch(
  () => detail.value?.publisher?.avatar,
  () => {
    publisherAvatarFailed.value = false
  },
)

function renderLocation(it: Pick<StudentJobPosting, 'classMode' | 'city'>): string {
  const mode = (it.classMode || '').toLowerCase()
  if (mode === 'online') return '线上'
  return it.city || '线下'
}

function renderFrequency(it: Pick<StudentJobPosting, 'frequencyPerWeek'>): string {
  return it.frequencyPerWeek ? `每周${it.frequencyPerWeek}次` : '每周-次'
}

function isDemandClosed(it: Pick<StudentJobPosting, 'status' | 'bizStatus'> | null | undefined): boolean {
  if (!it) return false
  return it.status !== 1 || (it.bizStatus != null && Number(it.bizStatus) !== 1)
}

function buildDemandShareLink(id: number): string {
  const href = router.resolve({ name: 'tutorJobDetail', params: { id: String(id) } }).href
  return new URL(href, window.location.origin).toString()
}

async function onShareDemand(id: number) {
  const link = buildDemandShareLink(id)
  try {
    if (!(navigator.clipboard && typeof navigator.clipboard.writeText === 'function')) {
      throw new Error('clipboard-unavailable')
    }
    await navigator.clipboard.writeText(link)
    toast.show('链接已复制，可转发给其他老师查看', 'success')
  } catch {
    toast.show('复制失败，请手动复制', 'error')
  }
}

onMounted(() => {
  const raw = route.query.q
  if (typeof raw === 'string' && raw.trim()) {
    q.value = raw.trim()
  }
  const demandId = route.query.demandId
  if (typeof demandId === 'string') {
    const n = Number(demandId)
    if (Number.isFinite(n)) {
      pendingHighlightId.value = n
    }
  }
  void refresh()
})

onBeforeUnmount(() => {
  stopDetailApplicationPolling()
})

watch(
  () => chatRealtime.lastApplicationEvent,
  (ev) => {
    if (!ev?.applicationId) return
    if (ev.applicationId !== detailApplication.value?.id) return
    // 仅在当前右侧详情绑定的申请发生变化时刷新，避免其他申请事件影响当前浏览体验。
    void reloadDetailApplication('realtime')
  },
)

watch(
  () => cityStore.city,
  () => {
    void refresh()
  },
)

watch(
  () => list.value.length,
  () => {
    listPageIndex.value = Math.min(listPageIndex.value, maxListPageIndex.value)
  },
)
</script>

<template>
  <div class="wrap" @click.self="closeMenus">
    <div class="head">
      <div class="search card">
        <input v-model="q" class="search-input" placeholder="搜索家教需求关键词" @keydown.enter.prevent="refresh" />
        <button class="btn btn-primary" type="button" :disabled="loading" @click="refresh">搜索</button>
      </div>

      <div class="tabs">
        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: !!classMode || openKey === 'type' }" @click.stop="toggle('type')">
            <span>需求类型</span>
            <span class="val">{{ typeLabel }}</span>
          </button>
          <div v-if="openKey === 'type'" class="menu card">
            <button class="menu-item" type="button" @click="selectType('')">不限</button>
            <button class="menu-item" type="button" @click="selectType('online')">线上</button>
            <button class="menu-item" type="button" @click="selectType('offline')">线下</button>
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
            <button class="menu-item" type="button" @click="selectSubject(SUBJECT_OTHER_VALUE)">其他</button>
          </div>
        </div>

        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: budgetMin != null || budgetMax != null || openKey === 'budget' }" @click.stop="toggle('budget')">
            <span>薪资待遇</span>
            <span class="val">{{ budgetLabel }}</span>
          </button>
          <div v-if="openKey === 'budget'" class="menu card budget">
            <div class="budget-row">
              <input v-model="budgetMinInput" class="budget-input" inputmode="decimal" placeholder="下限" />
              <div class="dash">—</div>
              <input v-model="budgetMaxInput" class="budget-input" inputmode="decimal" placeholder="上限" />
            </div>
            <div class="budget-ops">
              <button class="btn" type="button" @click="clearBudget">清空</button>
              <button class="btn btn-primary" type="button" @click="applyBudget">确定</button>
            </div>
          </div>
        </div>

        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: !!stageCode || openKey === 'stage' }" @click.stop="toggle('stage')">
            <span>授课学段</span>
            <span class="val">{{ stageLabel }}</span>
          </button>
          <div v-if="openKey === 'stage'" class="menu card">
            <button v-for="o in stageOptions" :key="o.value" class="menu-item" type="button" @click="selectStage(o.value)">
              {{ o.label }}
            </button>
          </div>
        </div>

        <div class="tab-wrap">
          <button
            class="tab"
            type="button"
            :class="{ active: !!teacherGenderPreference || openKey === 'tGender' }"
            @click.stop="toggle('tGender')"
          >
            <span>教师性别</span>
            <span class="val">{{ teacherGenderLabel }}</span>
          </button>
          <div v-if="openKey === 'tGender'" class="menu card">
            <button class="menu-item" type="button" @click="selectTeacherGender('')">不限</button>
            <button class="menu-item" type="button" @click="selectTeacherGender('male')">男</button>
            <button class="menu-item" type="button" @click="selectTeacherGender('female')">女</button>
            <button class="menu-item" type="button" @click="selectTeacherGender('both')">均可</button>
          </div>
        </div>

        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: !!educationRequirement || openKey === 'edu' }" @click.stop="toggle('edu')">
            <span>学历要求</span>
            <span class="val">{{ eduLabel }}</span>
          </button>
          <div v-if="openKey === 'edu'" class="menu card">
            <button v-for="o in eduOptions" :key="o.value" class="menu-item" type="button" @click="selectEdu(o.value)">
              {{ o.label }}
            </button>
          </div>
        </div>

        <div class="tab-wrap">
          <button class="tab" type="button" :class="{ active: !!frequencyPerWeek || openKey === 'freq' }" @click.stop="toggle('freq')">
            <span>授课频次</span>
            <span class="val">{{ freqLabel }}</span>
          </button>
          <div v-if="openKey === 'freq'" class="menu card">
            <button class="menu-item" type="button" @click="selectFreq(null)">不限</button>
            <button v-for="n in 7" :key="n" class="menu-item" type="button" @click="selectFreq(n)">每周 {{ n }} 次</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="workbench">
      <aside class="card left">
        <div class="list-head">
          <div>
            <div class="list-title">需求列表</div>
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
          <div class="empty-title">暂无匹配需求</div>
          <div class="empty-desc">换个关键词或筛选条件试试</div>
        </div>

        <div v-else class="items">
          <div
            v-for="it in visibleList"
            :key="it.id"
            class="item"
            :id="`demand-item-${it.id}`"
            :class="{ active: selectedId === it.id }"
            role="button"
            tabindex="0"
            @click="openDetail(it)"
            @keydown.enter.prevent="openDetail(it)"
            @keydown.space.prevent="openDetail(it)"
          >
            <div class="line1">
              <div class="t">
                <span>{{ it.title }}</span>
                <span v-if="String(it.publisherIdentity || '').toUpperCase() === 'ORGANIZATION'" class="org-tag">机构发布</span>
              </div>
              <div v-if="it.budgetMin || it.budgetMax" class="pay">{{ it.budgetMin || '-' }}-{{ it.budgetMax || '-' }}/小时</div>
            </div>
            <div class="meta">
              <span>{{ renderLocation(it) }}</span>
              <span>{{ formatClassMode(it.classMode) }}</span>
              <span>{{ renderFrequency(it) }}</span>
              <span>{{ formatEducationRequirement(it.educationRequirement) }}</span>
            </div>
            <div v-if="it.description" class="desc">{{ it.description }}</div>
            <div class="item-ops">
              <button class="btn btn-small" type="button" @click.stop="onShareDemand(it.id)">分享需求</button>
            </div>
          </div>
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
        <div v-else-if="detail" class="card detail">
          <div class="detail-head">
            <div>
              <div class="detail-title">{{ detail.title }}</div>
              <div class="detail-subtitle">
                {{ [formatSubjectName(detail), formatGradeCode(detail.gradeCode), renderLocation(detail)].filter(Boolean).join(' · ') }}
              </div>
              <div v-if="isDemandClosed(detail)" class="closed-tip-inline">已关闭，当前不再公开招募，仅支持通过分享链接查看。</div>
            </div>
            <div class="detail-ops">
              <button class="btn" type="button" @click="onToggleFavorite(detail)">{{ favoriteMap[detail.id] ? '已收藏' : '收藏' }}</button>
              <button class="btn" type="button" @click="onShareDemand(detail.id)">分享需求</button>
              <button
                class="btn btn-primary"
                type="button"
                :disabled="applyBusy || detailLoading || detailApplicationLoading || applyButton.disabled"
                @click="onClickApplyButton"
              >
                {{ applyButton.text }}
              </button>
            </div>
          </div>

          <div class="stat-grid">
            <div v-for="stat in demandStats" :key="stat.label" class="stat-card">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-hint">{{ stat.hint }}</div>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">需求概览</div>
              <div class="section-sub">资料完整度 {{ demandCompleteness }}%，越完整越利于判断是否接单</div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span>科目</span>
                <strong>{{ formatSubjectName(detail) }}</strong>
              </div>
              <div class="info-item">
                <span>学生年级</span>
                <strong>{{ formatGradeCode(detail.gradeCode) }}</strong>
              </div>
              <div class="info-item">
                <span>学生性别</span>
                <strong>{{ formatGender(detail.studentGender) }}</strong>
              </div>
              <div class="info-item">
                <span>教师性别偏好</span>
                <strong>{{ formatGender(detail.teacherGenderPreference) }}</strong>
              </div>
              <div class="info-item">
                <span>学历要求</span>
                <strong>{{ formatEducationRequirement(detail.educationRequirement) }}</strong>
              </div>
              <div class="info-item">
                <span>孩子年龄</span>
                <strong>{{ detail.childAge ? `${detail.childAge}岁` : '待补充' }}</strong>
              </div>
            </div>
          </div>

          <div v-if="String(detail.publisherIdentity || '').toUpperCase() === 'ORGANIZATION'" class="hint notice">
            <div class="n-title">机构单说明</div>
            <div class="n-body">
              机构为需求发布与履约主体，平台提供信息撮合、支付托管与纠纷介入机制；平台不直接保证授课质量与履约结果。
            </div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">需求描述</div>
              <div class="section-sub">家长/机构对学习情况和目标的说明</div>
            </div>
            <div class="detail-text rich-text">{{ detail.description || '发布者暂未补充详细描述，可先通过申请沟通了解学生基础、目标和上课节奏。' }}</div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">上课安排</div>
              <div class="section-sub">时间、频次、地点和授课方式</div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span>可上课时间</span>
                <strong>{{ detail.availableTime || (detail.schedule ? formatScheduleText(detail.schedule) : '待沟通') }}</strong>
              </div>
              <div class="info-item">
                <span>上课频次</span>
                <strong>{{ renderFrequency(detail) }}</strong>
              </div>
              <div class="info-item">
                <span>授课方式</span>
                <strong>{{ formatClassMode(detail.classMode) }}</strong>
              </div>
              <div class="info-item">
                <span>{{ detail.classMode === 'online' ? '授课地点' : '工作地址' }}</span>
                <strong>{{ detail.classMode === 'online' ? '线上授课' : [detail.city, detail.address].filter(Boolean).join(' · ') || '地址待沟通' }}</strong>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">对教员的详细要求</div>
              <div class="section-sub">接单前重点核对是否符合</div>
            </div>
            <div class="detail-text rich-text">{{ detail.teacherRequirementDetail || '发布者暂未填写额外要求，可在申请沟通时确认教学经验、性别偏好、试讲安排等信息。' }}</div>
          </div>

          <div class="detail-section">
            <div class="section-head">
              <div class="section-title">发布者信息</div>
              <div class="section-sub">用于判断发布主体与后续沟通对象</div>
            </div>
            <div v-if="detail.publisher" class="publisher">
              <img
                v-if="detail.publisher.avatar && !publisherAvatarFailed"
                class="p-avatar"
                :src="detail.publisher.avatar"
                alt="avatar"
                @error="publisherAvatarFailed = true"
              />
              <div v-else class="p-avatar fallback">{{ (detail.publisher.displayName || 'U').slice(0, 1) }}</div>
              <div class="p-info">
                <div class="p-name">{{ detail.publisher.displayName }}</div>
                <div class="p-tags">
                  <span class="tag">{{ detail.publisher.identityLabel }}</span>
                  <button
                    v-if="String(detail.publisherIdentity || '').toUpperCase() === 'ORGANIZATION'"
                    class="link"
                    type="button"
                    @click="openOrgCard(detail.parentId)"
                  >
                    查看机构主页
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="placeholder-card">
              <div class="placeholder-title">发布者资料暂不可见</div>
              <div class="placeholder-desc">可以先从需求信息判断是否适合，再发起申请沟通。</div>
            </div>
          </div>
        </div>
        <div v-else class="card detail empty-detail">
          <div class="empty-title">选择一条需求查看详情</div>
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
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.tab-wrap {
  position: relative;
}

.tab {
  height: 36px;
  border-radius: 18px;
  border-radius: 12px;
  background: #fff;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 800;
  color: var(--muted);
}

.tab.active {
  color: var(--text);
  border-color: rgba(0, 190, 189, 0.35);
  background: rgba(0, 190, 189, 0.06);
}

.val {
  color: var(--text);
  font-weight: 900;
  font-size: 12px;
  background: #fff;
}
.menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 180px;
  padding: 8px;
  z-index: 20;
  box-shadow: 0 0 0 4px var(--primary-weak);
}
.menu-item {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 10px 10px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 700;
}

.menu-item:hover {
  background: rgba(31, 35, 41, 0.06);
}

.budget {
  min-width: 260px;
}

.budget-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 10px;
  align-items: center;
  padding: 6px 2px 10px;
}

.budget-input {
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 0 10px;
  outline: none;
}

.budget-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.dash {
  color: var(--muted);
  font-weight: 900;
}

.budget-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 2px 2px;
}


.workbench {
  display: grid;
  grid-template-columns: minmax(320px, 390px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.left {
  height: min(760px, calc(100vh - 218px));
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
  gap: 8px;
  align-content: start;
  overflow: auto;
  padding-right: 4px;
}

.item {
  width: 100%;
  text-align: left;
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #fff;
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.t {
  font-weight: 900;
  font-size: 14px;
  line-height: 1.4;
  flex: 1 1 auto;
}

.org-tag {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 12px;
  color: #d46b08;
  border: 1px solid rgba(255, 170, 0, 0.35);
  background: rgba(255, 170, 0, 0.08);
  margin-left: 6px;
}

.pay {
  flex: 0 0 auto;
  color: #ff4d4f;
  font-weight: 900;
  font-size: 13px;
  white-space: nowrap;
}

.meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.desc {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-ops {
  display: flex;
  justify-content: flex-end;
  margin-top: 2px;
}

.btn-small {
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
}

.footer {
  display: flex;
  justify-content: center;
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
  height: min(760px, calc(100vh - 218px));
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
  font-weight: 900;
  font-size: 22px;
  line-height: 1.2;
}

.detail-subtitle {
  margin-top: 7px;
  color: var(--muted);
  font-size: 13px;
}

.detail-ops {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.closed-tip-inline {
  margin-top: 8px;
  font-size: 12px;
  color: #b54708;
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
  font-size: 17px;
  line-height: 1.25;
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

.detail-text {
  color: var(--text);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.rich-text {
  padding: 12px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.035);
}

.publisher {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(31, 35, 41, 0.035);
}

.p-avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.p-avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  color: var(--text);
  background: rgba(0, 190, 189, 0.08);
}

.p-info {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.p-name {
  font-weight: 900;
  font-size: 13px;
}

.p-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--muted);
  background: rgba(31, 35, 41, 0.06);
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

.placeholder-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(31, 35, 41, 0.025);
}

.placeholder-title {
  font-size: 13px;
  font-weight: 900;
}

.placeholder-desc {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
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

.empty-detail {
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}

.d-title {
  height: 18px;
  width: 70%;
  border-radius: 10px;
}

.d-line {
  height: 14px;
  width: 100%;
  border-radius: 10px;
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

@media (max-width: 980px) {
  .workbench {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .left {
    max-height: 420px;
  }

  .right {
    min-height: auto;
  }
}
</style>
