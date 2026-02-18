<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import type { HotDemandCardVO, HotServiceCardVO, HotTabsVO, HotTutorCardVO } from '@/api/types'
import { chatApi } from '@/api/chat'
import type { PageState } from '@/stores/home'
import { useAuthStore } from '@/stores/auth'
import { formatBudgetUnit, formatClassMode, formatScheduleText } from '@/utils/present'

const props = defineProps<{
  city: string
  hotTabsService: HotTabsVO | null
  hotTabsDemand: HotTabsVO | null
  serviceTabId: string
  demandTabId: string
  hotServices: PageState<HotServiceCardVO>
  hotDemands: PageState<HotDemandCardVO>
  hotTutors: PageState<HotTutorCardVO>
  showServices?: boolean
  showDemands?: boolean
  showTutors?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:serviceTabId', value: string): void
  (e: 'update:demandTabId', value: string): void
  (e: 'load-more-services'): void
  (e: 'load-more-demands'): void
  (e: 'load-more-tutors'): void
  (e: 'refresh'): void
}>()

const serviceTabs = computed(() => props.hotTabsService?.tabs || [])
const demandTabs = computed(() => props.hotTabsDemand?.tabs || [])
const showServices = computed(() => props.showServices !== false)
const showDemands = computed(() => props.showDemands !== false)
const showTutors = computed(() => props.showTutors !== false)

const router = useRouter()
const auth = useAuthStore()
const canChat = computed(() => auth.isLoggedIn && auth.user?.userType === 1)

function range(n: number) {
  return Array.from({ length: n }, (_, i) => i)
}

const servicePlaceholders = computed(() => range(6))
const demandPlaceholders = computed(() => range(6))
const tutorPlaceholders = computed(() => range(4))

function selectServiceTab(tabId: string) {
  emit('update:serviceTabId', tabId)
  emit('refresh')
}

function selectDemandTab(tabId: string) {
  emit('update:demandTabId', tabId)
  emit('refresh')
}

async function onChatDemand(it: HotDemandCardVO) {
  if (!canChat.value) return
  if (!auth.me) {
    await auth.refreshMe()
  }
  const greeting = auth.me?.teacherProfile?.defaultGreeting ?? null
  const roomId = await chatApi.startRoom(it.parent.userId, greeting)
  await router.push({ name: 'chatRoom', params: { roomId }, query: { otherUid: String(it.parent.userId) } })
}
</script>

<template>
  <section class="hot">
    <div class="head">
      <div class="title">热门推荐</div>
      <div class="meta">{{ city }}</div>
      <button class="btn" type="button" @click="emit('refresh')">换一批</button>
    </div>

    <div v-if="showServices" class="block card">
      <div class="block-head">
        <div class="block-title">热门服务</div>
        <div class="tabs">
          <template v-if="serviceTabs.length">
            <button
              v-for="t in serviceTabs"
              :key="t.tabId"
              class="tab"
              :class="{ active: t.tabId === serviceTabId }"
              type="button"
              @click="selectServiceTab(t.tabId)"
            >
              {{ t.name }}
            </button>
          </template>
          <button v-else class="tab active" type="button" @click="emit('refresh')">推荐</button>
        </div>
      </div>

      <div v-if="hotServices.error" class="hint">{{ hotServices.error }}</div>
      <div class="grid">
        <template v-if="hotServices.loading">
          <article v-for="i in servicePlaceholders" :key="i" class="item">
            <div class="skeleton sk-title skeleton" />
            <div class="sk-line">
              <span class="skeleton sk-pill skeleton" />
              <span class="skeleton sk-pill skeleton" />
              <span class="skeleton sk-pill skeleton" />
            </div>
            <div class="person">
              <div class="avatar skeleton" />
              <div class="info">
                <div class="skeleton sk-name skeleton" />
                <div class="skeleton sk-sub skeleton" />
              </div>
              <div class="skeleton sk-price skeleton" />
            </div>
            <div class="sk-tags">
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
            </div>
          </article>
        </template>

        <template v-else-if="hotServices.list.length">
          <article v-for="it in hotServices.list" :key="it.serviceId" class="item">
            <div class="item-title">{{ it.title }}</div>
            <div class="line">
              <span class="pill">{{ it.subject.name }}</span>
              <span class="muted">{{ formatClassMode(it.mode) }}</span>
              <span class="muted">{{ it.city }}</span>
            </div>
            <div class="person">
              <img class="avatar" :src="it.tutor.avatar" alt="" />
              <div class="info">
                <div class="name">{{ it.tutor.displayName }}</div>
                <div class="sub">{{ it.tutor.education }} · {{ it.tutor.experienceYears }}年</div>
              </div>
              <div class="price">¥{{ it.pricePerHour }}/小时</div>
            </div>
            <div class="tags">
              <span v-for="tag in it.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </article>
        </template>

        <template v-else>
          <article v-for="i in servicePlaceholders" :key="i" class="item empty-item">
            <div class="empty-title">暂无推荐服务</div>
            <div class="empty-sub">稍后再来看看</div>
          </article>
        </template>
      </div>

      <div class="more">
        <button class="btn" type="button" :disabled="hotServices.loading || hotServices.isLast" @click="emit('load-more-services')">
          <span v-if="hotServices.loading">加载中...</span>
          <span v-else-if="hotServices.isLast">没有更多了</span>
          <span v-else>查看更多</span>
        </button>
      </div>
    </div>

    <div v-if="showDemands" class="block card">
      <div class="block-head">
        <div class="block-title">热门需求</div>
        <div class="tabs">
          <template v-if="demandTabs.length">
            <button
              v-for="t in demandTabs"
              :key="t.tabId"
              class="tab"
              :class="{ active: t.tabId === demandTabId }"
              type="button"
              @click="selectDemandTab(t.tabId)"
            >
              {{ t.name }}
            </button>
          </template>
          <button v-else class="tab active" type="button" @click="emit('refresh')">推荐</button>
        </div>
      </div>

      <div v-if="hotDemands.error" class="hint">{{ hotDemands.error }}</div>
      <div class="grid">
        <template v-if="hotDemands.loading">
          <article v-for="i in demandPlaceholders" :key="i" class="item">
            <div class="skeleton sk-title skeleton" />
            <div class="sk-line">
              <span class="skeleton sk-pill skeleton" />
              <span class="skeleton sk-pill skeleton" />
              <span class="skeleton sk-pill skeleton" />
              <span class="skeleton sk-pill skeleton" />
            </div>
            <div class="person">
              <div class="avatar skeleton" />
              <div class="info">
                <div class="skeleton sk-name skeleton" />
                <div class="skeleton sk-sub skeleton" />
              </div>
              <div class="skeleton sk-price skeleton" />
            </div>
            <div class="sk-tags">
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
            </div>
          </article>
        </template>

        <template v-else-if="hotDemands.list.length">
          <article v-for="it in hotDemands.list" :key="it.demandId" class="item">
            <div class="item-title">{{ it.title }}</div>
            <div class="line">
              <span class="pill">{{ it.subject.name }}</span>
              <span class="muted">{{ formatClassMode(it.classMode) }}</span>
              <span class="muted">{{ it.city }}</span>
              <span class="muted">{{ it.addressSimple }}</span>
            </div>
            <div class="person">
              <img class="avatar" :src="it.parent.avatar" alt="" />
              <div class="info">
                <div class="name">{{ it.parent.displayName }}</div>
                <div class="sub">{{ formatScheduleText(it.scheduleText) }}</div>
              </div>
              <div class="price">¥{{ it.budget.min }}-{{ it.budget.max }}/{{ formatBudgetUnit(it.budget.unit) }}</div>
            </div>
            <div class="tags">
              <span v-for="tag in it.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div v-if="canChat" class="ops">
              <button class="btn btn-primary" type="button" @click="onChatDemand(it)">立即沟通</button>
            </div>
          </article>
        </template>

        <template v-else>
          <article v-for="i in demandPlaceholders" :key="i" class="item empty-item">
            <div class="empty-title">暂无推荐需求</div>
            <div class="empty-sub">稍后再来看看</div>
          </article>
        </template>
      </div>

      <div class="more">
        <button class="btn" type="button" :disabled="hotDemands.loading || hotDemands.isLast" @click="emit('load-more-demands')">
          <span v-if="hotDemands.loading">加载中...</span>
          <span v-else-if="hotDemands.isLast">没有更多了</span>
          <span v-else>查看更多</span>
        </button>
      </div>
    </div>

    <div v-if="showTutors" class="block card">
      <div class="block-head">
        <div class="block-title">推荐老师</div>
      </div>

      <div v-if="hotTutors.error" class="hint">{{ hotTutors.error }}</div>
      <div class="grid tutors">
        <template v-if="hotTutors.loading">
          <article v-for="i in tutorPlaceholders" :key="i" class="item tutor-item">
            <div class="person">
              <div class="avatar big skeleton" />
              <div class="info">
                <div class="skeleton sk-name skeleton" />
                <div class="skeleton sk-sub skeleton" />
              </div>
              <div class="skeleton sk-price skeleton" />
            </div>
            <div class="sk-tags">
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
            </div>
            <div class="sk-tags">
              <span class="skeleton sk-tag skeleton" />
              <span class="skeleton sk-tag skeleton" />
            </div>
          </article>
        </template>

        <template v-else-if="hotTutors.list.length">
          <article v-for="it in hotTutors.list" :key="it.userId" class="item tutor-item">
            <div class="person">
              <img class="avatar big" :src="it.avatar" alt="" />
              <div class="info">
                <div class="name">{{ it.displayName }}</div>
                <div class="sub">{{ it.education }} · {{ it.experienceYears }}年 · {{ it.city }}</div>
              </div>
              <div class="price">¥{{ it.ratePerHour }}/小时</div>
            </div>
            <div class="tags">
              <span v-for="tag in it.subjectTags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div class="highlights">
              <span v-for="h in it.highlights" :key="h" class="hl">{{ h }}</span>
            </div>
          </article>
        </template>

        <template v-else>
          <article v-for="i in tutorPlaceholders" :key="i" class="item tutor-item empty-item">
            <div class="empty-title">暂无推荐老师</div>
            <div class="empty-sub">稍后再来看看</div>
          </article>
        </template>
      </div>

      <div class="more">
        <button class="btn" type="button" :disabled="hotTutors.loading || hotTutors.isLast" @click="emit('load-more-tutors')">
          <span v-if="hotTutors.loading">加载中...</span>
          <span v-else-if="hotTutors.isLast">没有更多了</span>
          <span v-else>查看更多</span>
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hot {
  display: grid;
  gap: 16px;
}

.head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 18px;
  font-weight: 900;
}

.meta {
  color: var(--muted);
  font-size: 13px;
}

.block {
  padding: 14px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.block-title {
  font-weight: 800;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tab {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  color: var(--muted);
  font-size: 12px;
}

.tab.active {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-weak);
}

.hint {
  margin-bottom: 10px;
  color: #b42318;
  font-size: 12px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.9);
}

.empty-item {
  display: grid;
  place-items: center;
  text-align: center;
  gap: 6px;
  min-height: 150px;
  background: rgba(255, 255, 255, 0.6);
  border-style: dashed;
}

.empty-title {
  font-weight: 900;
  font-size: 13px;
}

.empty-sub {
  font-size: 12px;
  color: var(--muted);
}

.sk-line {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}

.sk-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.sk-title {
  height: 16px;
  width: 72%;
  border-radius: 8px;
  margin-bottom: 8px;
}

.sk-name {
  height: 14px;
  width: 70%;
  border-radius: 8px;
}

.sk-sub {
  height: 12px;
  width: 92%;
  border-radius: 8px;
  margin-top: 6px;
}

.sk-price {
  height: 14px;
  width: 64px;
  border-radius: 8px;
}

.sk-pill {
  height: 16px;
  width: 52px;
  border-radius: 999px;
}

.sk-tag {
  height: 16px;
  width: 46px;
  border-radius: 999px;
}

.item-title {
  font-weight: 800;
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 8px;
}

.line {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  align-items: center;
}

.pill {
  font-size: 12px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.14);
  padding: 2px 8px;
  border-radius: 999px;
}

.muted {
  font-size: 12px;
  color: var(--muted);
}

.person {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid var(--border);
  background: #fff;
}

.avatar.big {
  width: 46px;
  height: 46px;
}

.name {
  font-weight: 800;
  font-size: 13px;
}

.sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.price {
  font-weight: 900;
  color: var(--text);
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  color: var(--muted);
  border: 1px solid var(--border);
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff;
}

.ops {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.highlights {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.hl {
  font-size: 11px;
  border: 1px dashed rgba(0, 190, 189, 0.5);
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.08);
}

.more {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.tutors {
  grid-template-columns: repeat(2, 1fr);
}
</style>
