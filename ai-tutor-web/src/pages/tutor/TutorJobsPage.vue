<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { applicationApi } from '@/api/application'
import { chatApi } from '@/api/chat'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { ChatMessageResp, ChatRoomItemResp, DemandViewVO, StudentJobPosting } from '@/api/types'
import { DEFAULT_APPLICATION_GREETING, useSettingsStore } from '@/stores/settings'
import { useCityStore } from '@/stores/city'
import { formatClassMode, formatEducationRequirement } from '@/utils/present'
import { SUBJECT_OTHER_VALUE, SUBJECT_PRESETS } from '@/utils/subjects'

const router = useRouter()
const route = useRoute()
const settings = useSettingsStore()
const cityStore = useCityStore()

const loading = ref(false)
const error = ref<string | null>(null)

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
const selectedId = ref<number | null>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const detail = ref<DemandViewVO | null>(null)
const pendingHighlightId = ref<number | null>(null)

const applyBusy = ref(false)
const applyError = ref<string | null>(null)
const applyTipOpen = ref(false)
const applyTipText = ref('')

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
  list.value = []
  cursor.value = null
  isLast.value = false
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

function openDetail(it: StudentJobPosting) {
  selectedId.value = it.id
}

function genClientRequestId() {
  const g = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : ''
  if (g) return g
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
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

async function openApply(it: StudentJobPosting) {
  if (applyBusy.value) return
  applyError.value = null
  applyBusy.value = true
  try {
    try {
      const reuse = await shouldReuseExistingChat(it.parentId)
      if (reuse?.roomId) {
        await router.push({ name: 'chatRoom', params: { roomId: String(reuse.roomId) }, query: { otherUid: String(it.parentId) } })
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
      receiverUid: it.parentId,
      contextType: 'DEMAND',
      contextId: it.id,
      content,
      clientRequestId: genClientRequestId(),
    })
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

function renderLocation(it: Pick<StudentJobPosting, 'classMode' | 'city'>): string {
  const mode = (it.classMode || '').toLowerCase()
  if (mode === 'online') return '线上'
  return it.city || '线下'
}

function renderFrequency(it: Pick<StudentJobPosting, 'frequencyPerWeek'>): string {
  return it.frequencyPerWeek ? `每周${it.frequencyPerWeek}次` : '每周-次'
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

watch(
  () => cityStore.city,
  () => {
    void refresh()
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
        <div v-if="list.length === 0 && !loading" class="empty">
          <div class="empty-title">暂无匹配需求</div>
          <div class="empty-desc">换个关键词或筛选条件试试</div>
        </div>

        <div v-else class="items">
          <button
            v-for="it in list"
            :key="it.id"
            class="item"
            type="button"
            :id="`demand-item-${it.id}`"
            :class="{ active: selectedId === it.id }"
            @click="openDetail(it)"
          >
            <div class="line1">
              <div class="t">{{ it.title }}</div>
              <div v-if="it.budgetMin || it.budgetMax" class="pay">{{ it.budgetMin || '-' }}-{{ it.budgetMax || '-' }}/小时</div>
            </div>
            <div class="meta">
              <span>{{ renderLocation(it) }}</span>
              <span>{{ formatClassMode(it.classMode) }}</span>
              <span>{{ renderFrequency(it) }}</span>
              <span>{{ formatEducationRequirement(it.educationRequirement) }}</span>
            </div>
            <div v-if="it.description" class="desc">{{ it.description }}</div>
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
        <div v-else-if="detail" class="card detail">
          <div class="detail-head">
            <div class="detail-title">{{ detail.title }}</div>
            <div class="detail-ops">
              <button class="btn" type="button" @click="onToggleFavorite(detail)">{{ favoriteMap[detail.id] ? '已收藏' : '收藏' }}</button>
              <button class="btn btn-primary" type="button" @click="openApply(detail)">发起申请</button>
            </div>
          </div>

          <div class="detail-meta">
            <span>{{ renderLocation(detail) }}</span>
            <span>{{ formatClassMode(detail.classMode) }}</span>
            <span>{{ renderFrequency(detail) }}</span>
            <span>{{ formatEducationRequirement(detail.educationRequirement) }}</span>
          </div>

          <div class="detail-block">
            <div class="detail-label">需求描述</div>
            <div class="detail-text">{{ detail.description || '—' }}</div>
          </div>

          <div v-if="detail.availableTime || detail.schedule" class="detail-block">
            <div class="detail-label">可上课时间</div>
            <div class="detail-text">{{ detail.availableTime || detail.schedule }}</div>
          </div>

          <div v-if="detail.teacherRequirementDetail" class="detail-block">
            <div class="detail-label">对教员的详细要求</div>
            <div class="detail-text">{{ detail.teacherRequirementDetail }}</div>
          </div>

          <div v-if="detail.publisher" class="publisher">
            <img v-if="detail.publisher.avatar" class="p-avatar" :src="detail.publisher.avatar" alt="avatar" />
            <div v-else class="p-avatar fallback">{{ (detail.publisher.displayName || 'U').slice(0, 1) }}</div>
            <div class="p-info">
              <div class="p-name">{{ detail.publisher.displayName }}</div>
              <div class="p-tags">
                <span class="tag">{{ detail.publisher.identityLabel }}</span>
              </div>
            </div>
          </div>

          <div v-if="detail.classMode !== 'online'" class="detail-block">
            <div class="detail-label">工作地址</div>
            <div class="detail-text">{{ [detail.city, detail.address].filter(Boolean).join(' · ') || '—' }}</div>
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
  grid-template-columns: 360px 1fr;
  gap: 12px;
  align-items: start;
}

.left {
  padding: 12px;
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 12px;
  overflow: hidden;
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
}

.item.active {
  border-color: rgba(0, 190, 189, 0.45);
  background: rgba(0, 190, 189, 0.06);
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

.footer {
  display: flex;
  justify-content: center;
}

.right {
  align-self: start;
}

.detail {
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 12px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-title {
  font-weight: 900;
  font-size: 18px;
}

.detail-ops {
  display: flex;
  gap: 10px;
  align-items: center;
}

.detail-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.detail-block {
  display: grid;
  gap: 8px;
}

.detail-label {
  font-weight: 900;
  font-size: 13px;
}

.detail-text {
  color: var(--text);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.publisher {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 6px;
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
