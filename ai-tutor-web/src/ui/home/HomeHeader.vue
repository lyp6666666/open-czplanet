<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { homeGuestApi } from '@/api/homeGuest'
import type { HomeConfigVO, HotWordsVO, SearchSuggestVO } from '@/api/types'
import { BRAND_NAME } from '@/constants/brand'
import { useAuthStore } from '@/stores/auth'
import CitySelectModal from '@/ui/city/CitySelectModal.vue'
import BrandLogoMark from '@/ui/common/BrandLogoMark.vue'
import { debounce } from '@/utils/debounce'

const props = defineProps<{
  city: string
  config: HomeConfigVO | null
  hotWords: HotWordsVO | null
}>()

const emit = defineEmits<{
  (e: 'city-change', city: string): void
}>()

const router = useRouter()
const auth = useAuthStore()

const userMenuOpen = ref(false)
const cityModalOpen = ref(false)
const avatarLoadFailed = ref(false)
const keyword = ref('')
const suggestOpen = ref(false)
const suggestLoading = ref(false)
const suggestError = ref<string | null>(null)
const suggestList = ref<SearchSuggestVO['list']>([])

const cities = computed(() => {
  const base = [props.city, '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.filter(Boolean)))
})

const cityModel = computed({
  get: () => props.city,
  set: (value: string) => {
    const next = String(value || '').trim()
    if (!next) return
    emit('city-change', next)
  },
})

const placeholder = computed(() => props.config?.search?.placeholder || '搜索科目 / 年级 / 老师，例如：初中数学')

const navItems = computed(() => {
  return [
    { key: 'home', name: '首页', link: '/' },
    { key: 'student', name: '找老师', link: '/student/tutors' },
    { key: 'demand', name: '找需求', link: '/student/post' },
    { key: 'ai', name: 'AI学习', link: '/guide/student' },
    { key: 'tutor', name: '师资中心', link: '/tutor/jobs' },
    { key: 'about', name: '关于我们', link: '/about' },
  ]
})

const userInitial = computed(() => {
  const name = auth.user?.name?.trim()
  return name?.slice(0, 1) || 'U'
})

const isTeacher = computed(() => auth.user?.userType === 1)

async function runSearch() {
  const q = keyword.value.trim()
  if (!q) return

  if (!auth.isLoggedIn) {
    window.alert('请先登录后再使用搜索功能')
    return
  }

  if (isTeacher.value) {
    await router.push({ name: 'tutorJobs', query: { q } })
    return
  }

  await router.push({ name: 'studentTutors', query: { q } })
}

const fetchSuggest = debounce(async () => {
  const q = keyword.value.trim()
  suggestError.value = null

  if (!q) {
    suggestList.value = []
    suggestOpen.value = false
    return
  }

  suggestLoading.value = true
  suggestOpen.value = true
  try {
    const result = await homeGuestApi.suggest({ q, city: props.city, limit: 8 })
    suggestList.value = result.list || []
  } catch (error) {
    suggestError.value = error instanceof Error ? error.message : '搜索联想失败'
    suggestList.value = []
  } finally {
    suggestLoading.value = false
  }
}, 220)

function onKeywordInput() {
  void fetchSuggest()
}

function closeSuggest() {
  window.setTimeout(() => {
    suggestOpen.value = false
  }, 150)
}

function useHotWord(word: string) {
  keyword.value = word
  void runSearch()
}

async function goAuth(role: 'TEACHER' | 'STUDENT') {
  if (auth.isLoggedIn) return
  await router.push({ name: role === 'TEACHER' ? 'authTutor' : 'authStudent' })
}

async function goMenu(path: string) {
  userMenuOpen.value = false
  await router.push(path)
}

async function logout() {
  auth.logout()
  userMenuOpen.value = false
  await router.replace({ name: 'home' })
}

watch(
  () => auth.user?.avatar,
  () => {
    avatarLoadFailed.value = false
  },
)
</script>

<template>
  <header class="header-shell">
    <div class="container header">
      <RouterLink class="brand" to="/">
        <BrandLogoMark class="brand-mark" />
        <div class="brand-copy">
          <div class="brand-name">{{ BRAND_NAME }}</div>
          <div class="brand-tag">用数据联动孩子成长</div>
        </div>
      </RouterLink>

      <button class="city-trigger" type="button" @click="cityModalOpen = true">
        <span class="city-dot"></span>
        <span>{{ city }}</span>
        <span class="city-switch">切换</span>
      </button>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.key"
          class="nav-link"
          :to="item.link"
        >
          {{ item.name }}
        </RouterLink>
      </nav>

      <div class="search-area">
        <div class="search-box">
          <input
            v-model="keyword"
            class="search-input"
            type="text"
            :placeholder="placeholder"
            @input="onKeywordInput"
            @focus="onKeywordInput"
            @blur="closeSuggest"
            @keydown.enter.prevent="runSearch"
          />
          <button class="search-btn" type="button" @click="runSearch">搜索</button>
        </div>

        <div v-if="hotWords?.list?.length" class="hot-words">
          <span class="hot-label">热搜</span>
          <button
            v-for="item in hotWords.list.slice(0, 3)"
            :key="item.word"
            class="hot-chip"
            type="button"
            @click="useHotWord(item.word)"
          >
            {{ item.word }}
          </button>
        </div>

        <div v-if="suggestOpen" class="suggest-panel">
          <div v-if="suggestLoading" class="suggest-empty">加载中...</div>
          <div v-else-if="suggestError" class="suggest-empty">{{ suggestError }}</div>
          <template v-else>
            <div v-if="!suggestList.length" class="suggest-empty">暂无建议</div>
            <button
              v-for="(item, index) in suggestList"
              :key="`${item.type}-${index}`"
              class="suggest-item"
              type="button"
              @mousedown.prevent="keyword = item.title"
              @click="runSearch"
            >
              <div class="suggest-main">{{ item.title }}</div>
              <div v-if="item.subtitle" class="suggest-sub">{{ item.subtitle }}</div>
              <div class="suggest-tag">{{ item.type }}</div>
            </button>
          </template>
        </div>
      </div>

      <div class="actions">
        <template v-if="auth.isLoggedIn && auth.user">
          <button class="ghost-btn" type="button" @click="goMenu(isTeacher ? '/me' : '/student/jobs/mine')">
            {{ isTeacher ? '我的简历' : '我的学习' }}
          </button>

          <div class="user" @mouseenter="userMenuOpen = true" @mouseleave="userMenuOpen = false">
            <img
              v-if="auth.user.avatar && !avatarLoadFailed"
              class="avatar"
              :src="auth.user.avatar"
              alt="avatar"
              @error="avatarLoadFailed = true"
            />
            <div v-else class="avatar avatar-fallback">{{ userInitial }}</div>

            <div v-if="userMenuOpen" class="user-menu">
              <button class="menu-item" type="button" @click="goMenu('/me')">个人中心</button>
              <button class="menu-item" type="button" @click="goMenu(isTeacher ? '/tutor/favorites' : '/student/favorites')">我的收藏</button>
              <button class="menu-item" type="button" @click="goMenu('/invite')">邀请有礼</button>
              <button class="menu-item danger" type="button" @click="logout">退出登录</button>
            </div>
          </div>
        </template>

        <template v-else>
          <button class="ghost-btn" type="button" @click="goAuth('TEACHER')">我要当老师</button>
          <button class="primary-btn" type="button" @click="goAuth('STUDENT')">我找学生家教</button>
        </template>
      </div>
    </div>

    <CitySelectModal :open="cityModalOpen" v-model="cityModel" :hot-cities="cities" @close="cityModalOpen = false" />
  </header>
</template>

<style scoped>
.header-shell {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(248, 250, 255, 0.92);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(42, 75, 155, 0.08);
}

.header {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) minmax(320px, 460px) auto;
  align-items: center;
  gap: 14px;
  min-height: 86px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 14px;
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-name {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #0d1b48;
}

.brand-tag {
  font-size: 12px;
  color: #7585ab;
}

.city-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(69, 108, 198, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #30437d;
  cursor: pointer;
}

.city-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #16c7b5, #2f6dff);
}

.city-switch {
  color: #90a0c7;
  font-size: 12px;
}

.nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.nav::-webkit-scrollbar {
  display: none;
}

.nav-link {
  padding: 10px 14px;
  border-radius: 999px;
  color: #31406e;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.nav-link:hover,
.router-link-active.nav-link {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.09);
  transform: translateY(-1px);
}

.search-area {
  position: relative;
}

.search-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid rgba(92, 124, 200, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 32px rgba(52, 78, 141, 0.08);
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 16px;
  border: 0;
  outline: none;
  background: transparent;
  color: #16275d;
  font-size: 14px;
}

.search-btn,
.primary-btn,
.ghost-btn {
  height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.search-btn,
.primary-btn {
  border: 0;
  color: #fff;
  background: linear-gradient(135deg, #19c2bf, #2d62f2);
  box-shadow: 0 14px 28px rgba(43, 98, 234, 0.24);
}

.ghost-btn {
  border: 1px solid rgba(81, 112, 198, 0.16);
  color: #28407e;
  background: rgba(255, 255, 255, 0.92);
}

.search-btn:hover,
.primary-btn:hover,
.ghost-btn:hover {
  transform: translateY(-1px);
}

.hot-words {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px 0;
  color: #7f8fb3;
  font-size: 12px;
  overflow: hidden;
}

.hot-label {
  flex: none;
}

.hot-chip {
  padding: 0;
  border: 0;
  background: transparent;
  color: #4f649f;
  font-size: 12px;
  cursor: pointer;
}

.suggest-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 8px);
  display: grid;
  gap: 4px;
  padding: 10px;
  border: 1px solid rgba(92, 124, 200, 0.16);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 28px 48px rgba(40, 64, 126, 0.16);
}

.suggest-item,
.suggest-empty {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 18px;
}

.suggest-item {
  border: 0;
  text-align: left;
  background: rgba(245, 248, 255, 0.92);
  cursor: pointer;
}

.suggest-main {
  color: #17285b;
  font-size: 14px;
  font-weight: 700;
}

.suggest-sub,
.suggest-empty {
  color: #7f8fb3;
  font-size: 12px;
}

.suggest-tag {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.user {
  position: relative;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 22px rgba(41, 70, 143, 0.14);
}

.avatar-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2d62f2, #19c2bf);
  color: #fff;
  font-weight: 700;
}

.user-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 12px);
  display: grid;
  gap: 4px;
  width: 180px;
  padding: 10px;
  border: 1px solid rgba(92, 124, 200, 0.14);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 24px 48px rgba(34, 56, 116, 0.14);
}

.menu-item {
  padding: 10px 12px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: #24396f;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
}

.menu-item:hover {
  background: rgba(37, 99, 235, 0.08);
}

.menu-item.danger {
  color: #d14343;
}

@media (max-width: 1280px) {
  .header {
    grid-template-columns: auto auto minmax(0, 1fr);
    grid-template-areas:
      'brand city actions'
      'nav nav nav'
      'search search search';
    padding: 14px 0 18px;
  }

  .brand {
    grid-area: brand;
  }

  .city-trigger {
    grid-area: city;
    justify-self: start;
  }

  .nav {
    grid-area: nav;
    justify-content: flex-start;
  }

  .search-area {
    grid-area: search;
  }

  .actions {
    grid-area: actions;
    justify-self: end;
  }
}

@media (max-width: 768px) {
  .header {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'brand actions'
      'city city'
      'nav nav'
      'search search';
    gap: 12px;
    min-height: auto;
    padding: 12px 0 16px;
  }

  .brand-name {
    font-size: 20px;
  }

  .brand-tag {
    display: none;
  }

  .brand-mark {
    width: 42px;
    height: 42px;
  }

  .actions {
    gap: 8px;
  }

  .ghost-btn,
  .primary-btn {
    height: 38px;
    padding: 0 12px;
    font-size: 13px;
  }

  .hot-words {
    padding-left: 4px;
  }
}
</style>
