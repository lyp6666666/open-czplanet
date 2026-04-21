<script setup lang="ts">
import { computed, ref } from 'vue'

import type { BannersVO, SubjectTreeNode } from '@/api/types'

import { Swiper, SwiperSlide } from 'swiper/vue'
import { Autoplay, Pagination } from 'swiper/modules'

import 'swiper/css'
import 'swiper/css/pagination'

const props = defineProps<{
  city: string
  subjectTree: SubjectTreeNode[]
  banners: BannersVO | null
  loading: boolean
}>()

const activeId = ref<number | null>(null)

const fallbackSubjects = [
  { id: -1, parentId: 0, name: '小学', children: [] },
  { id: -2, parentId: 0, name: '初中', children: [] },
  { id: -3, parentId: 0, name: '高中', children: [] },
  { id: -4, parentId: 0, name: '英语', children: [] },
  { id: -5, parentId: 0, name: '数学', children: [] },
  { id: -6, parentId: 0, name: '语文', children: [] },
  { id: -7, parentId: 0, name: '物理', children: [] },
  { id: -8, parentId: 0, name: '编程', children: [] },
] satisfies SubjectTreeNode[]

const topSubjects = computed(() => {
  const list = Array.isArray(props.subjectTree) ? props.subjectTree : []
  const picked = list.slice(0, 8)
  if (picked.length >= 8) return picked
  return [...picked, ...fallbackSubjects.slice(0, 8 - picked.length)]
})

const activeSubject = computed(() => topSubjects.value.find((s) => s.id === activeId.value) || null)

function uniqueNames(names: string[]) {
  return Array.from(new Set(names.filter(Boolean)))
}

function flattenLeafNames(nodes: SubjectTreeNode[] | undefined, limit = 8): string[] {
  const queue = Array.isArray(nodes) ? nodes.slice() : []
  const out: string[] = []

  while (queue.length && out.length < limit) {
    const current = queue.shift()
    if (!current) continue
    if (!current.children?.length) {
      out.push(current.name)
      continue
    }
    queue.push(...current.children)
  }

  return uniqueNames(out).slice(0, limit)
}

function buildPanelGroups(subject: SubjectTreeNode) {
  const groups = (subject.children || [])
    .slice(0, 4)
    .map((child) => ({
      id: child.id,
      title: child.name,
      items: flattenLeafNames(child.children?.length ? child.children : [child], 5),
    }))
    .filter((group) => group.items.length > 0)

  if (groups.length) return groups

  return [
    {
      id: `${subject.id}-popular`,
      title: '热门方向',
      items: uniqueNames([`${subject.name}基础`, `${subject.name}提升`, `${subject.name}冲刺`, '同步辅导']).slice(0, 4),
    },
  ]
}

function buildHighlights(subject: SubjectTreeNode) {
  const name = subject.name
  const highlights = ['同步巩固', '方法梳理']

  if (/数学|物理|化学|编程/.test(name)) highlights.push('思维提升')
  if (/语文|英语|政治|历史|地理/.test(name)) highlights.push('表达强化')
  if (/小学|初中|高中/.test(name)) highlights.push('阶段提分')
  if (/高中|竞赛|奥数|编程/.test(name)) highlights.push('培优冲刺')

  return uniqueNames(highlights).slice(0, 4)
}

const panelGroups = computed(() => (activeSubject.value ? buildPanelGroups(activeSubject.value) : []))

const panelLeafNames = computed(() => uniqueNames(panelGroups.value.flatMap((group) => group.items)))

const panelMetrics = computed(() => {
  if (!activeSubject.value) return []
  return [
    { label: '学习阶段', value: String(activeSubject.value.children?.length || 1) },
    { label: '热门细分', value: String(panelLeafNames.value.length || 1) },
    { label: '推荐路径', value: '2' },
  ]
})

const panelHighlights = computed(() => (activeSubject.value ? buildHighlights(activeSubject.value) : []))

const panelSummary = computed(() => {
  if (!activeSubject.value) return ''
  const leafPreview = panelLeafNames.value.slice(0, 3).join('、')
  const base = leafPreview ? `重点覆盖 ${leafPreview} 等热门方向。` : '覆盖常见学习诉求与阶段提分方向。'
  return `${base} 这里把找老师、看路径、快速了解上课方向放到同一层，帮助用户更快进入后续动作。`
})

const panelQuickActions = computed(() => {
  if (!activeSubject.value) return []
  const focus = panelLeafNames.value[0] || activeSubject.value.name
  return [
    {
      id: 'student',
      audience: 'student' as const,
      eyebrow: '学生入口',
      title: `先看 ${activeSubject.value.name} 找老师路径`,
      desc: '了解如何筛选老师、判断匹配度与快速发起沟通',
      focus,
    },
    {
      id: 'tutor',
      audience: 'tutor' as const,
      eyebrow: '教师入口',
      title: `查看 ${activeSubject.value.name} 接单与展示建议`,
      desc: '从完善资料到接单沟通，快速理解该学科的转化重点',
      focus,
    },
  ]
})

function guideRoute(audience: 'student' | 'tutor', subjectName: string, focus: string) {
  return {
    path: audience === 'student' ? '/guide/student' : '/guide/tutor',
    query: {
      subject: subjectName,
      focus,
    },
  }
}

const carouselItems = computed(() => (Array.isArray(props.banners?.carousel) ? props.banners!.carousel : []))
const carouselAutoplay = computed(() =>
  carouselItems.value.length > 1 ? { delay: 3500, disableOnInteraction: false } : false,
)
const carouselPagination = computed(() => (carouselItems.value.length > 1 ? { clickable: true } : false))

const guideCards = [
  {
    id: 'guide-tutor',
    title: '家教教程',
    subtitle: '从完善资料到接单上课，一次看懂高效路径',
    imageUrl: '/guides/tutor-guide-card.svg',
    to: '/guide/tutor',
  },
  {
    id: 'guide-student',
    title: '学生教程',
    subtitle: '从发布需求到约课成交，少走弯路更高效',
    imageUrl: '/guides/student-guide-card.svg',
    to: '/guide/student',
  },
] as const

type BannerLink = { type?: string; url?: string } | null | undefined

function linkSpec(link: BannerLink) {
  const url = link?.url?.trim()
  if (!url) return { is: 'div', attrs: {} as Record<string, unknown> }
  if (link?.type === 'ROUTE' || url.startsWith('/')) return { is: 'RouterLink', attrs: { to: url } }
  return { is: 'a', attrs: { href: url, target: '_blank', rel: 'noreferrer' } }
}

const swiperModules = [Autoplay, Pagination]
</script>

<template>
  <section class="hero">
    <div class="grid">
      <aside class="subjects card" @mouseleave="activeId = null">
        <div class="title">学科分类</div>

        <div class="body">
          <div v-if="loading" class="skeleton-list">
            <div v-for="i in 8" :key="i" class="skeleton row skeleton" />
          </div>

          <div v-else class="list">
            <button
              v-for="s in topSubjects"
              :key="s.id"
              class="row"
              :class="{ active: activeId === s.id }"
              type="button"
              @mouseenter="activeId = s.id"
            >
              <div class="row-head">
                <div class="name">{{ s.name }}</div>
                <div class="arrow">></div>
              </div>
              <div v-if="s.children?.length" class="meta">{{ s.children?.slice(0, 3).map((c) => c.name).join(' / ') }}</div>
              <div v-else class="meta">热门方向推荐</div>
            </button>
          </div>
        </div>

        <div v-if="activeSubject" class="panel card">
          <div class="panel-hero">
            <div class="panel-copy">
              <div class="panel-kicker">学科导览</div>
              <div class="panel-title">{{ activeSubject.name }}学习地图</div>
              <div class="panel-summary">{{ panelSummary }}</div>
            </div>
            <div class="panel-metrics">
              <div v-for="metric in panelMetrics" :key="metric.label" class="metric">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
              </div>
            </div>
          </div>

          <div class="panel-highlights">
            <span v-for="highlight in panelHighlights" :key="highlight" class="highlight-pill">{{ highlight }}</span>
          </div>

          <div class="panel-layout">
            <div class="panel-groups">
              <div v-for="group in panelGroups" :key="group.id" class="group">
                <div class="group-title">{{ group.title }}</div>
                <div class="group-items">
                  <RouterLink
                    v-for="item in group.items"
                    :key="item"
                    class="tag"
                    :to="guideRoute('student', activeSubject.name, item)"
                  >
                    {{ item }}
                  </RouterLink>
                </div>
              </div>
            </div>

            <div class="panel-actions">
              <RouterLink
                v-for="action in panelQuickActions"
                :key="action.id"
                class="action-card"
                :to="guideRoute(action.audience, activeSubject.name, action.focus)"
              >
                <div class="action-eyebrow">{{ action.eyebrow }}</div>
                <div class="action-title">{{ action.title }}</div>
                <div class="action-desc">{{ action.desc }}</div>
              </RouterLink>
            </div>
          </div>
        </div>
      </aside>

      <div class="main">
        <div v-if="loading" class="carousel card skeleton" />
        <div v-else-if="carouselItems.length" class="carousel card">
          <Swiper
            class="carousel-inner"
            :modules="swiperModules"
            :loop="carouselItems.length > 1"
            :autoplay="carouselAutoplay"
            :pagination="carouselPagination"
          >
            <SwiperSlide v-for="c in carouselItems" :key="c.id">
              <component :is="linkSpec(c.link).is" v-bind="linkSpec(c.link).attrs" class="slide">
                <img class="img" :src="c.imageUrl" :alt="c.title" />
                <div class="mask">
                  <div class="slide-title">{{ c.title }}</div>
                  <div class="slide-sub">{{ c.subtitle }}</div>
                </div>
              </component>
            </SwiperSlide>
          </Swiper>
        </div>
        <div v-else class="carousel card empty">
          <div class="empty-inner">
            <div class="empty-title">为你推荐优质家教</div>
            <div class="empty-sub">运营位暂未配置，可先浏览热门服务与热门需求</div>
          </div>
        </div>

        <div class="cards">
          <template v-if="loading">
            <div class="small card skeleton" />
            <div class="small card skeleton" />
          </template>
          <template v-else>
            <RouterLink v-for="c in guideCards" :key="c.id" :to="c.to" class="small card">
              <img class="img" :src="c.imageUrl" :alt="c.title" />
              <div class="info">
                <div class="small-title">{{ c.title }}</div>
                <div class="small-sub">{{ c.subtitle }}</div>
              </div>
            </RouterLink>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  --hero-h: 339px;
  --hero-gap: 16px;
  margin-bottom: 16px;
}

.grid {
  position: relative;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  align-items: start;
}

.subjects {
  position: relative;
  z-index: 6;
  padding: 16px;
  overflow: visible;
  height: var(--hero-h);
  display: flex;
  flex-direction: column;
  border-radius: 16px;
}

.title {
  font-size: 15px;
  font-weight: 900;
  margin-bottom: 12px;
}

.body {
  flex: 1;
  overflow: hidden;
}

.list {
  display: grid;
  gap: 8px;
}

.row {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  cursor: pointer;
  display: grid;
  gap: 3px;
  position: relative;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.row:hover,
.row.active {
  border-color: var(--primary);
  background: linear-gradient(135deg, rgba(0, 190, 189, 0.12), rgba(0, 190, 189, 0.03));
  transform: translateX(2px);
  box-shadow: 0 12px 24px rgba(0, 190, 189, 0.08);
}

.row:hover::before,
.row.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 999px;
  background: var(--primary);
}

.name {
  font-size: 13px;
  font-weight: 800;
}

.arrow {
  font-size: 12px;
  color: rgba(15, 118, 110, 0.72);
  font-weight: 900;
}

.meta {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel {
  position: absolute;
  left: calc(100% + 12px);
  top: 0;
  z-index: 30;
  width: 560px;
  min-height: var(--hero-h);
  max-height: var(--hero-h);
  padding: 16px;
  border-radius: 20px;
  overflow: auto;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.14), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 251, 251, 0.98));
  box-shadow: 0 22px 56px rgba(14, 22, 32, 0.18);
}

.panel-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 14px;
  align-items: start;
}

.panel-copy {
  display: grid;
  gap: 8px;
}

.panel-kicker {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 11px;
  font-weight: 900;
}

.panel-title {
  font-size: 20px;
  line-height: 1.15;
  font-weight: 900;
  color: #102027;
}

.panel-summary {
  font-size: 13px;
  line-height: 1.65;
  color: rgba(16, 32, 39, 0.76);
}

.panel-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.metric {
  min-height: 78px;
  padding: 12px 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(15, 118, 110, 0.1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.metric-value {
  font-size: 20px;
  line-height: 1;
  font-weight: 900;
  color: #0f766e;
}

.metric-label {
  margin-top: 8px;
  font-size: 11px;
  color: var(--muted);
  font-weight: 700;
}

.panel-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.highlight-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 169, 77, 0.18), rgba(255, 106, 61, 0.08));
  color: #b54708;
  font-size: 12px;
  font-weight: 800;
}

.panel-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 188px;
  gap: 14px;
  margin-top: 16px;
}

.panel-groups {
  display: grid;
  gap: 12px;
  align-content: start;
}

.group-title {
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #102027;
}

.group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: var(--muted);
  border: 1px solid var(--border);
  min-height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  background: #fff;
  text-decoration: none;
  transition: border-color 180ms ease, color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.tag:hover {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(0, 190, 189, 0.1);
}

.panel-actions {
  display: grid;
  gap: 10px;
  align-content: start;
}

.action-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  text-decoration: none;
  border: 1px solid rgba(15, 118, 110, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(239, 249, 248, 0.92)),
    rgba(255, 255, 255, 0.9);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.28);
  box-shadow: 0 16px 28px rgba(15, 118, 110, 0.12);
}

.action-eyebrow {
  font-size: 11px;
  font-weight: 900;
  color: #0f766e;
}

.action-title {
  font-size: 14px;
  line-height: 1.4;
  font-weight: 900;
  color: #102027;
}

.action-desc {
  font-size: 12px;
  line-height: 1.55;
  color: rgba(16, 32, 39, 0.7);
}

.main {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
}

.carousel {
  height: var(--hero-h);
  overflow: hidden;
  border-radius: 16px;
}

.carousel-inner {
  height: 100%;
}

:deep(.swiper) {
  height: 100%;
}

:deep(.swiper-pagination-bullet) {
  background: rgba(255, 255, 255, 0.9);
  opacity: 0.65;
}

:deep(.swiper-pagination-bullet-active) {
  opacity: 1;
}

.slide {
  height: 100%;
  display: block;
  position: relative;
}

.img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.05), rgba(0, 0, 0, 0.55));
  color: #fff;
  display: grid;
  align-content: end;
  padding: 18px;
  gap: 6px;
}

.slide-title {
  font-size: 20px;
  font-weight: 900;
}

.slide-sub {
  font-size: 13px;
  opacity: 0.9;
}

.cards {
  display: grid;
  grid-auto-rows: calc((var(--hero-h) - var(--hero-gap)) / 2);
  gap: var(--hero-gap);
}

.small {
  overflow: hidden;
  position: relative;
  display: block;
  border-radius: 16px;
}

.small .img {
  filter: saturate(1.05);
}

.small:hover .img {
  transform: scale(1.02);
  transition: transform 220ms ease;
}

.info {
  position: absolute;
  inset: auto 0 0 0;
  padding: 14px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.05), rgba(0, 0, 0, 0.55));
  color: #fff;
}

.small-title {
  font-weight: 800;
  font-size: 14px;
}

.small-sub {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
}

.skeleton-list {
  display: grid;
  gap: 8px;
  height: 100%;
}

.skeleton.row {
  height: 52px;
}

.empty {
  background: radial-gradient(1200px 420px at 20% 10%, rgba(0, 190, 189, 0.22), transparent 55%),
    radial-gradient(900px 380px at 80% 70%, rgba(0, 190, 189, 0.14), transparent 60%), #0e1620;
  color: #fff;
  display: grid;
  place-items: center;
}

.empty-inner {
  padding: 18px;
  text-align: center;
  display: grid;
  gap: 6px;
}

.empty-title {
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.2px;
}

.empty-sub {
  font-size: 13px;
  opacity: 0.85;
}

.empty-card {
  overflow: hidden;
  display: grid;
  align-content: end;
  padding: 12px;
  background: radial-gradient(620px 220px at 30% 10%, rgba(0, 190, 189, 0.2), transparent 55%),
    radial-gradient(620px 220px at 70% 90%, rgba(0, 190, 189, 0.14), transparent 58%), #0e1620;
  color: #fff;
}

.empty-card-title {
  font-weight: 900;
  font-size: 14px;
}

.empty-card-sub {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .main {
    grid-template-columns: 1fr;
  }

  .subjects {
    height: auto;
  }

  .carousel {
    height: 220px;
  }

  .cards {
    grid-auto-rows: 112px;
    gap: 16px;
  }

  .panel {
    display: none;
  }
}
</style>
