<script setup lang="ts">
import { computed, ref } from 'vue'

import type { BannersVO, SubjectTreeNode } from '@/api/types'

const props = defineProps<{
  city: string
  subjectTree: SubjectTreeNode[]
  banners: BannersVO | null
  loading: boolean
}>()

const activeId = ref<number | null>(null)

const topSubjects = computed(() => {
  return Array.isArray(props.subjectTree) ? props.subjectTree.slice(0, 12) : []
})

const activeSubject = computed(() => topSubjects.value.find((s) => s.id === activeId.value) || null)

const firstCarousel = computed(() => props.banners?.carousel?.[0] || null)
</script>

<template>
  <section class="hero">
    <div class="grid">
      <aside class="subjects card">
        <div class="title">学科分类</div>

        <div v-if="loading" class="skeleton-list">
          <div v-for="i in 10" :key="i" class="skeleton row skeleton" />
        </div>

        <div v-else class="list" @mouseleave="activeId = null">
          <button
            v-for="s in topSubjects"
            :key="s.id"
            class="row"
            type="button"
            @mouseenter="activeId = s.id"
          >
            <div class="name">{{ s.name }}</div>
            <div class="meta">{{ s.children?.slice(0, 3).map((c) => c.name).join(' / ') }}</div>
          </button>
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
        <div class="carousel card" v-if="firstCarousel">
          <div class="carousel-inner">
            <a class="slide" :href="firstCarousel.link.url">
              <img class="img" :src="firstCarousel.imageUrl" :alt="firstCarousel.title" />
              <div class="mask">
                <div class="slide-title">{{ firstCarousel.title }}</div>
                <div class="slide-sub">{{ firstCarousel.subtitle }}</div>
              </div>
            </a>
          </div>
        </div>
        <div v-else class="carousel card skeleton" />

        <div class="cards">
          <a v-for="c in banners?.cards || []" :key="c.id" class="small card" :href="c.link.url">
            <img class="img" :src="c.imageUrl" :alt="c.title" />
            <div class="info">
              <div class="small-title">{{ c.title }}</div>
              <div class="small-sub">{{ c.subtitle }}</div>
            </div>
          </a>
          <div v-if="!banners?.cards?.length" class="small card skeleton" />
          <div v-if="!banners?.cards?.length" class="small card skeleton" />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
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
  padding: 14px;
  overflow: visible;
}

.title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
}

.list {
  display: grid;
  gap: 6px;
}

.row {
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 10px;
  padding: 10px 10px;
  cursor: pointer;
  display: grid;
  gap: 3px;
}

.row:hover {
  border-color: var(--primary);
  background: var(--primary-weak);
}

.name {
  font-size: 13px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: var(--muted);
}

.panel {
  position: absolute;
  left: calc(100% + 12px);
  top: 12px;
  width: 520px;
  padding: 12px;
  border-radius: 12px;
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
  height: 240px;
  overflow: hidden;
}

.carousel-inner {
  height: 100%;
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
  padding: 14px;
  gap: 4px;
}

.slide-title {
  font-size: 18px;
  font-weight: 800;
}

.slide-sub {
  font-size: 13px;
  opacity: 0.9;
}

.cards {
  display: grid;
  grid-auto-rows: 112px;
  gap: 16px;
}

.small {
  overflow: hidden;
  position: relative;
  display: block;
}

.small .img {
  filter: saturate(1.05);
}

.info {
  position: absolute;
  inset: auto 0 0 0;
  padding: 10px;
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
}

.skeleton.row {
  height: 44px;
}
</style>
