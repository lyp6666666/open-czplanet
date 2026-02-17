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

const carouselItems = computed(() => (Array.isArray(props.banners?.carousel) ? props.banners!.carousel : []))
const cardItems = computed(() => (Array.isArray(props.banners?.cards) ? props.banners!.cards : []))

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
      <aside class="subjects card">
        <div class="title">学科分类</div>

        <div class="body" @mouseleave="activeId = null">
          <div v-if="loading" class="skeleton-list">
            <div v-for="i in 8" :key="i" class="skeleton row skeleton" />
          </div>

          <div v-else class="list">
            <button
              v-for="s in topSubjects"
              :key="s.id"
              class="row"
              type="button"
              @mouseenter="activeId = s.id"
            >
              <div class="name">{{ s.name }}</div>
              <div v-if="s.children?.length" class="meta">{{ s.children?.slice(0, 3).map((c) => c.name).join(' / ') }}</div>
              <div v-else class="meta">热门学科推荐</div>
            </button>
          </div>
        </div>

        <div v-if="activeSubject" class="panel card">
          <div class="panel-title">{{ activeSubject.name }}</div>
          <div class="panel-groups">
            <div v-for="child in activeSubject.children" :key="child.id" class="group">
              <div class="group-title">{{ child.name }}</div>
              <div class="group-items">
                <a v-for="leaf in child.children" :key="leaf.id" class="tag" href="#">
                  {{ leaf.name }}
                </a>
              </div>
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
            :autoplay="{ delay: 3500, disableOnInteraction: false }"
            :pagination="{ clickable: true }"
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
            <component
              v-for="c in cardItems"
              :key="c.id"
              :is="linkSpec(c.link).is"
              v-bind="linkSpec(c.link).attrs"
              class="small card"
            >
              <img class="img" :src="c.imageUrl" :alt="c.title" />
              <div class="info">
                <div class="small-title">{{ c.title }}</div>
                <div class="small-sub">{{ c.subtitle }}</div>
              </div>
            </component>
            <div v-if="!cardItems.length" class="small card empty-card">
              <div class="empty-card-title">新手指南</div>
              <div class="empty-card-sub">快速了解发布需求与挑选家教</div>
            </div>
            <div v-if="!cardItems.length" class="small card empty-card">
              <div class="empty-card-title">在线沟通</div>
              <div class="empty-card-sub">先聊再约课，匹配更高效</div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  --hero-h: 320px;
  --hero-gap: 16px;
  margin-bottom: 16px;
}

.grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  align-items: start;
}

.subjects {
  position: relative;
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
}

.row:hover {
  border-color: var(--primary);
  background: var(--primary-weak);
}

.row:hover::before {
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
  top: 12px;
  width: 520px;
  padding: 12px;
  border-radius: 16px;
}

.panel-title {
  font-weight: 800;
  margin-bottom: 10px;
}

.panel-groups {
  display: grid;
  gap: 12px;
}

.group-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
}

.group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  font-size: 12px;
  color: var(--muted);
  border: 1px solid var(--border);
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff;
}

.tag:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.main {
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
