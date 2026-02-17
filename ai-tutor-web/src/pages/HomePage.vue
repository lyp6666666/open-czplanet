<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useHomeStore } from '@/stores/home'
import { useAuthStore } from '@/stores/auth'
import HomeFooter from '@/ui/home/HomeFooter.vue'
import HomeHeader from '@/ui/home/HomeHeader.vue'
import HomeHero from '@/ui/home/HomeHero.vue'
import HomeHotSection from '@/ui/home/HomeHotSection.vue'

const router = useRouter()
const home = useHomeStore()
const auth = useAuthStore()
const showDebug = ref(false)
const keyword = ref('')

const isTeacher = computed(() => auth.user?.userType === 1)

async function refreshAll() {
  await Promise.all([home.refreshHotServices(), home.refreshHotDemands(), home.refreshHotTutors()])
}

async function onSearch() {
  const q = keyword.value.trim()
  if (!q) return
  if (isTeacher.value) {
    await router.push({ name: 'tutorJobs', query: { q } })
    return
  }
  await router.push({ name: 'studentPost' })
}

onMounted(() => {
  const storedCity = localStorage.getItem('ai_tutor_city')
  if (storedCity && storedCity.trim()) {
    home.setCity(storedCity.trim())
  }
  void home.initHome()
})
</script>

<template>
  <div>
    <HomeHeader v-if="!auth.isLoggedIn" :city="home.city" :config="home.config" :hot-words="home.hotWords" @city-change="home.setCity" />

    <main class="page">
      <div class="container">
        <div v-if="auth.isLoggedIn" class="search card">
          <input v-model="keyword" class="search-input" placeholder="搜索家教需求关键词" @keydown.enter.prevent="onSearch" />
          <button class="btn btn-primary" type="button" @click="onSearch">搜索</button>
        </div>

        <div v-if="home.error" class="error card">
          <div class="title">首页加载失败</div>
          <div class="desc">{{ home.error }}</div>
          <button class="btn btn-primary" type="button" @click="home.initHome()">重试</button>
        </div>

        <HomeHero
          v-else
          :city="home.city"
          :subject-tree="home.subjectTree"
          :banners="home.banners"
          :loading="home.loading"
        />

        <HomeHotSection
          :city="home.city"
          :hot-tabs-service="home.hotTabsService"
          :hot-tabs-demand="home.hotTabsDemand"
          v-model:service-tab-id="home.selectedServiceTabId"
          v-model:demand-tab-id="home.selectedDemandTabId"
          :hot-services="home.hotServices"
          :hot-demands="home.hotDemands"
          :hot-tutors="home.hotTutors"
          @load-more-services="home.loadMoreHotServices"
          @load-more-demands="home.loadMoreHotDemands"
          @load-more-tutors="home.loadMoreHotTutors"
          @refresh="refreshAll"
        />

        <div class="debug" v-if="showDebug">
          <pre>{{ home }}</pre>
        </div>
      </div>
    </main>

    <HomeFooter :links="home.footerLinks" />
  </div>
</template>

<style scoped>
.page {
  padding: 18px 0 32px;
}

.search {
  padding: 12px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
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

.error {
  padding: 16px;
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
}

.title {
  font-size: 16px;
  font-weight: 700;
}

.desc {
  color: var(--muted);
  font-size: 13px;
}
</style>
