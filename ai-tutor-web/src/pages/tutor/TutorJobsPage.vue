<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { favoritesApi } from '@/api/favorites'
import { jobsApi } from '@/api/jobs'
import type { StudentJobPosting } from '@/api/types'
import { formatClassMode } from '@/utils/present'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref<string | null>(null)

const q = ref('')
const classMode = ref<string>('')
const stageCode = ref<string>('')
const educationRequirement = ref<string>('')

const city = ref(localStorage.getItem('ai_tutor_city') || '北京')
watch(city, (v) => localStorage.setItem('ai_tutor_city', v))

const budgetMin = ref<number | null>(null)
const budgetMax = ref<number | null>(null)
const budgetMinInput = ref('')
const budgetMaxInput = ref('')

const list = ref<StudentJobPosting[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const openKey = ref<'' | 'type' | 'budget' | 'stage' | 'edu'>('')

const stageOptions = [
  { value: '', label: '不限' },
  { value: 'PRESCHOOL', label: '幼教育' },
  { value: 'PRIMARY', label: '小学' },
  { value: 'JUNIOR', label: '初中' },
  { value: 'SENIOR', label: '高中' },
  { value: 'OTHER', label: '其他' },
]

const eduOptions = [
  { value: '', label: '不限' },
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

const stageLabel = computed(() => stageOptions.find((o) => o.value === stageCode.value)?.label ?? '不限')

const eduLabel = computed(() => eduOptions.find((o) => o.value === educationRequirement.value)?.label ?? '不限')

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

function selectStage(v: string) {
  stageCode.value = v
  closeMenus()
}

function selectEdu(v: string) {
  educationRequirement.value = v
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
  } catch {}
}

async function refresh() {
  list.value = []
  cursor.value = null
  isLast.value = false
  checkedFavoriteIds.clear()
  favoriteMap.value = {}
  await loadMore()
}

async function loadMore() {
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await jobsApi.feedDemands({
      pageSize: 10,
      cursor: cursor.value,
      classMode: classMode.value || undefined,
      city: classMode.value && classMode.value !== 'online' ? city.value.trim() || undefined : undefined,
      stageCode: stageCode.value || undefined,
      educationRequirement: educationRequirement.value || undefined,
      budgetMin: budgetMin.value ?? undefined,
      budgetMax: budgetMax.value ?? undefined,
      q: q.value.trim() || undefined,
      sort: 'latest',
    })
    list.value = [...list.value, ...(page.list || [])]
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast
    await syncFavorites((page.list || []).map((it) => it.id))
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function openDetail(it: StudentJobPosting) {
  void router.push({ name: 'tutorJobDetail', params: { id: it.id } })
}

async function onChat(it: StudentJobPosting) {
  try {
    const roomId = await chatApi.getOrCreateRoom(it.parentId)
    await router.push({ name: 'chatRoom', params: { roomId }, query: { otherUid: String(it.parentId) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发起沟通失败'
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

watch([classMode, stageCode, educationRequirement], () => {
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
      </div>
    </div>

    <div v-if="error" class="hint error">{{ error }}</div>

    <div class="card list">
      <div v-if="list.length === 0 && !loading" class="empty">
        <div class="empty-title">暂无匹配需求</div>
        <div class="empty-desc">换个关键词或筛选条件试试</div>
      </div>

      <div v-else class="items">
        <div v-for="it in list" :key="it.id" class="item">
          <button class="main" type="button" @click="openDetail(it)">
            <div class="t">{{ it.title }}</div>
            <div class="meta">
              <span v-if="it.city">{{ it.city }}</span>
              <span v-if="it.classMode">{{ formatClassMode(it.classMode) }}</span>
              <span v-if="it.stageCode">{{ stageOptions.find((o) => o.value === it.stageCode)?.label || it.stageCode }}</span>
              <span v-if="it.educationRequirement">{{ eduOptions.find((o) => o.value === it.educationRequirement)?.label || it.educationRequirement }}</span>
              <span v-if="it.budgetMin || it.budgetMax">
                {{ it.budgetMin || '-' }}-{{ it.budgetMax || '-' }}/小时
              </span>
            </div>
            <div v-if="it.description" class="desc">{{ it.description }}</div>
          </button>
          <div class="ops">
            <button class="btn" type="button" @click="onToggleFavorite(it)">{{ favoriteMap[it.id] ? '已收藏' : '收藏' }}</button>
            <button class="btn btn-primary" type="button" @click="onChat(it)">立即沟通</button>
          </div>
        </div>
      </div>

      <div class="footer" v-if="list.length > 0">
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


.list {
  padding: 14px;

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

.items {
  display: grid;
  gap: 10px;
}

.item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
}

.main {
  border: none;
  background: transparent;
  text-align: left;
  padding: 0;
  cursor: pointer;
}

.t {
  font-weight: 900;
  font-size: 14px;
}

.meta {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.desc {
  margin-top: 8px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.ops {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.footer {
  display: flex;
  justify-content: center;
  margin-top: 14px;
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

@media (max-width: 980px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
