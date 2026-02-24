<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { favoritesTutorsApi } from '@/api/favoritesTutors'
import { userApi } from '@/api/user'
import type { UserCardVO } from '@/api/types'
import UserCardModal from '@/ui/user/UserCardModal.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)

const list = ref<UserCardVO[]>([])
const cursor = ref<number | null>(null)
const isLast = ref(false)

const canUse = computed(() => auth.user?.userType === 2)

const cardOpen = ref(false)
const cardUid = ref<number | null>(null)

function openCard(uid: number) {
  cardUid.value = uid
  cardOpen.value = true
}

function closeCard() {
  cardOpen.value = false
}

async function loadMore() {
  if (!canUse.value) return
  if (loading.value || isLast.value) return
  loading.value = true
  error.value = null
  try {
    const page = await favoritesTutorsApi.pageTutorFavorites({ pageSize: 10, cursor: cursor.value })
    cursor.value = page.nextCursor ?? null
    isLast.value = !!page.isLast

    const ids = (page.list || []).filter((id) => typeof id === 'number' && Number.isFinite(id))
    const settled = await Promise.allSettled(ids.map((id) => userApi.card(id)))
    const cards = settled.flatMap((r) => (r.status === 'fulfilled' && r.value ? [r.value] : []))
    list.value = [...list.value, ...cards]
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function onUnfavorite(uid: number) {
  try {
    await favoritesTutorsApi.unfavoriteTutor(uid)
    list.value = list.value.filter((it) => it.user.id !== uid)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

function displayName(card: UserCardVO): string {
  return card.teacherProfile?.realName || card.user.name || `用户${card.user.id}`
}

function teacherMeta(card: UserCardVO): string {
  const tp = card.teacherProfile
  if (!tp) return ''
  const parts: string[] = []
  if (tp.education) parts.push(tp.education)
  if (tp.experienceYears != null) parts.push(`${tp.experienceYears}年`)
  if (tp.ratePerHour) parts.push(`${tp.ratePerHour}元/小时`)
  return parts.join(' · ')
}

onMounted(() => {
  void loadMore()
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <div class="title">我的收藏</div>
    </div>

    <div v-if="!canUse" class="hint error">当前账号不是学生端，无法查看收藏教师</div>
    <div v-else-if="error" class="hint error">{{ error }}</div>

    <div class="card list">
      <div v-if="canUse && list.length === 0 && !loading" class="empty">
        <div class="empty-title">暂无收藏教师</div>
        <div class="empty-desc">在推荐老师或聊天资料卡中收藏你喜欢的老师</div>
      </div>

      <div v-else class="items">
        <div v-for="it in list" :key="it.user.id" class="item">
          <div class="person" @click="openCard(it.user.id)">
            <img v-if="it.user.avatar" class="avatar" :src="it.user.avatar" alt="" />
            <div v-else class="avatar fallback">{{ displayName(it).slice(0, 1) }}</div>
            <div class="info">
              <div class="name">{{ displayName(it) }}</div>
              <div v-if="teacherMeta(it)" class="sub">{{ teacherMeta(it) }}</div>
            </div>
          </div>
          <div class="ops">
            <button class="btn" type="button" @click="openCard(it.user.id)">查看</button>
            <button class="btn danger" type="button" @click="onUnfavorite(it.user.id)">取消收藏</button>
          </div>
        </div>
      </div>

      <div class="footer" v-if="canUse && list.length > 0">
        <button class="btn" type="button" :disabled="loading || isLast" @click="loadMore">
          <span v-if="isLast">没有更多了</span>
          <span v-else>{{ loading ? '加载中...' : '加载更多' }}</span>
        </button>
      </div>
    </div>

    <UserCardModal :open="cardOpen" :uid="cardUid" @close="closeCard" />
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 900;
}

.list {
  padding: 14px;
}

.empty {
  padding: 28px 10px;
  display: grid;
  gap: 10px;
  justify-items: start;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
}

.person {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  cursor: pointer;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid var(--border);
  object-fit: cover;
  background: #fff;
  flex: 0 0 auto;
}

.avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  color: var(--text);
  background: rgba(31, 35, 41, 0.06);
}

.info {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.name {
  font-weight: 900;
  font-size: 14px;
}

.sub {
  color: var(--muted);
  font-size: 12px;
}

.ops {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.danger {
  border-color: rgba(255, 0, 0, 0.25);
  color: #b42318;
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
</style>

