<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { useHomeStore } from '@/stores/home'
import HomeFooter from '@/ui/home/HomeFooter.vue'
import HomeHeader from '@/ui/home/HomeHeader.vue'
import HomeHero from '@/ui/home/HomeHero.vue'
import HomeHotSection from '@/ui/home/HomeHotSection.vue'

const home = useHomeStore()
const showDebug = ref(false)

async function refreshAll() {
  await Promise.all([home.refreshHotServices(), home.refreshHotDemands(), home.refreshHotTutors()])
}

onMounted(() => {
  void home.initHome()
})
</script>

<template>
  <div>
    <HomeHeader :city="home.city" :config="home.config" :hot-words="home.hotWords" @city-change="home.setCity" />

    <main class="page">
      <div class="container">
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
