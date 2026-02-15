<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { chatApi } from '@/api/chat'
import { jobsApi } from '@/api/jobs'
import { homeGuestApi } from '@/api/homeGuest'
import type { StudentJobPosting, SubjectTreeNode } from '@/api/types'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)

const subjects = ref<SubjectTreeNode[]>([])
const subjectId = ref<number | null>(null)

const q = ref('')
const classMode = ref<string>('')
const city = ref('北京')

const list = ref<StudentJobPosting[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const subjectOptions = computed(() => {
  const out: Array<{ id: number; label: string }> = [{ id: 0, label: '全部科目' }]
  function walk(node: SubjectTreeNode, prefix: string) {
    const label = prefix ? `${prefix} / ${node.name}` : node.name
    out.push({ id: node.id, label })
    node.children?.forEach((c) => walk(c, label))
  }
  subjects.value.forEach((n) => walk(n, ''))
  return out
})

async function loadSubjects() {
  subjects.value = await homeGuestApi.getSubjectTree()
}

async function refresh() {
  list.value = []
  cursor.value = null
  isLast.value = false
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
      subjectId: subjectId.value && subjectId.value > 0 ? subjectId.value : undefined,
      classMode: classMode.value || undefined,
      city: classMode.value && classMode.value !== 'online' ? city.value.trim() || undefined : undefined,
      q: q.value.trim() || undefined,
      sort: 'latest',
    })
    list.value = [...list.value, ...(page.list || [])]
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast
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

watch([subjectId, classMode], () => {
  void refresh()
})

onMounted(() => {
  void loadSubjects()
  void refresh()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">需求广场</div>
      <div class="filters card">
        <input v-model="q" class="input" placeholder="搜索标题/描述关键词" @keydown.enter.prevent="refresh" />
        <select v-model.number="subjectId" class="input">
          <option v-for="o in subjectOptions" :key="o.id" :value="o.id">{{ o.label }}</option>
        </select>
        <select v-model="classMode" class="input">
          <option value="">全部方式</option>
          <option value="online">线上</option>
          <option value="offline">线下</option>
          <option value="both">均可</option>
        </select>
        <input v-if="classMode && classMode !== 'online'" v-model="city" class="input" placeholder="城市" />
        <button class="btn btn-primary" type="button" :disabled="loading" @click="refresh">搜索</button>
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
              <span v-if="it.classMode">{{ it.classMode }}</span>
              <span v-if="it.budgetMin || it.budgetMax">
                {{ it.budgetMin || '-' }}-{{ it.budgetMax || '-' }}/h
              </span>
            </div>
            <div v-if="it.description" class="desc">{{ it.description }}</div>
          </button>
          <div class="ops">
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

.title {
  font-size: 18px;
  font-weight: 900;
}

.filters {
  padding: 12px;
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr 0.8fr auto;
  gap: 10px;
  align-items: center;
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

.list {
  padding: 14px;
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
