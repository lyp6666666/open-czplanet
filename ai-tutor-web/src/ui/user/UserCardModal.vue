<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { favoritesTutorsApi } from '@/api/favoritesTutors'
import { userApi } from '@/api/user'
import type { UserCardVO } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  open: boolean
  uid: number | null
}>()

const emit = defineEmits<{
  close: []
}>()

const loading = ref(false)
const error = ref<string | null>(null)
const card = ref<UserCardVO | null>(null)

const auth = useAuthStore()

const title = computed(() => {
  const user = card.value?.user
  if (!user) return ''
  return user.name || `用户${user.id}`
})

const identityLabel = computed(() => {
  const t = card.value?.user?.userType
  if (t === 1) return '教师'
  if (t === 2) return '学生'
  return ''
})

const canFavoriteTutor = computed(() => auth.user?.userType === 2 && card.value?.user?.userType === 1)
const favorited = ref(false)
const favoriteBusy = ref(false)

function close() {
  emit('close')
}

function fmt(v: unknown): string {
  if (v == null) return ''
  const s = typeof v === 'string' ? v.trim() : String(v)
  return s
}

function fmtBudget(min: string | null, max: string | null) {
  const a = fmt(min)
  const b = fmt(max)
  if (a && b) return `${a} - ${b} 元/小时`
  if (a) return `${a} 元/小时起`
  if (b) return `${b} 元/小时以内`
  return ''
}

watch(
  () => [props.open, props.uid] as const,
  async ([open, uid]) => {
    if (!open || !uid) return
    loading.value = true
    error.value = null
    favorited.value = false
    try {
      card.value = await userApi.card(uid)
      if (auth.user?.userType === 2 && card.value?.user?.userType === 1) {
        const ids = await favoritesTutorsApi.checkTutorFavorites([uid])
        favorited.value = Array.isArray(ids) && ids.includes(uid)
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载失败'
      card.value = null
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

async function onToggleFavoriteTutor() {
  const uid = props.uid
  if (!uid || !canFavoriteTutor.value || favoriteBusy.value) return
  favoriteBusy.value = true
  error.value = null
  try {
    if (favorited.value) {
      await favoritesTutorsApi.unfavoriteTutor(uid)
    } else {
      await favoritesTutorsApi.favoriteTutor(uid)
    }
    favorited.value = !favorited.value
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    favoriteBusy.value = false
  }
}
</script>

<template>
  <div v-if="open" class="mask" @click.self="close">
    <div class="modal card">
      <div class="m-head">
        <div class="u">
          <img v-if="card?.user?.avatar" class="u-avatar" :src="card.user.avatar" alt="" />
          <div v-else class="u-avatar fallback">{{ title.slice(0, 1) }}</div>
          <div class="u-main">
            <div class="u-name">
              <span>{{ title }}</span>
              <span v-if="identityLabel" class="tag">{{ identityLabel }}</span>
            </div>
            <div v-if="card?.user?.id" class="u-sub">UID：{{ card.user.id }}</div>
          </div>
        </div>
        <div class="head-ops">
          <button v-if="canFavoriteTutor" class="btn" type="button" :disabled="favoriteBusy" @click="onToggleFavoriteTutor">
            {{ favorited ? '已收藏' : '收藏' }}
          </button>
          <button class="icon-btn" type="button" @click="close">×</button>
        </div>
      </div>

      <div v-if="loading" class="hint">加载中...</div>
      <div v-else-if="error" class="hint error">{{ error }}</div>

      <template v-else-if="card">
        <div v-if="card.teacherProfile" class="sec">
          <div class="sec-title">教师信息</div>
          <div class="kv">
            <div v-if="fmt(card.teacherProfile.realName)" class="row"><span class="k">姓名</span><span class="v">{{ fmt(card.teacherProfile.realName) }}</span></div>
            <div v-if="fmt(card.teacherProfile.education)" class="row"><span class="k">学历</span><span class="v">{{ fmt(card.teacherProfile.education) }}</span></div>
            <div v-if="fmt(card.teacherProfile.subject)" class="row"><span class="k">科目</span><span class="v">{{ fmt(card.teacherProfile.subject) }}</span></div>
            <div v-if="card.teacherProfile.experienceYears != null" class="row">
              <span class="k">教龄</span><span class="v">{{ card.teacherProfile.experienceYears }} 年</span>
            </div>
            <div v-if="fmt(card.teacherProfile.ratePerHour)" class="row"><span class="k">课时费</span><span class="v">{{ fmt(card.teacherProfile.ratePerHour) }} 元/小时</span></div>
          </div>
          <div v-if="fmt(card.teacherProfile.introduction)" class="desc">{{ fmt(card.teacherProfile.introduction) }}</div>
        </div>

        <div v-if="card.studentProfile" class="sec">
          <div class="sec-title">学生信息</div>
          <div class="kv">
            <div v-if="fmt(card.studentProfile.realName)" class="row"><span class="k">姓名</span><span class="v">{{ fmt(card.studentProfile.realName) }}</span></div>
            <div v-if="card.studentProfile.childAge != null" class="row"><span class="k">孩子年龄</span><span class="v">{{ card.studentProfile.childAge }} 岁</span></div>
            <div v-if="fmt(card.studentProfile.address)" class="row"><span class="k">地址</span><span class="v">{{ fmt(card.studentProfile.address) }}</span></div>
            <div v-if="fmt(card.studentProfile.budget)" class="row"><span class="k">预算</span><span class="v">{{ fmt(card.studentProfile.budget) }}</span></div>
          </div>
          <div v-if="fmt(card.studentProfile.demandDescription)" class="desc">{{ fmt(card.studentProfile.demandDescription) }}</div>
        </div>

        <div v-if="card.jobPosting" class="sec">
          <div class="sec-title">岗位信息</div>
          <div class="kv">
            <div v-if="fmt(card.jobPosting.title)" class="row"><span class="k">标题</span><span class="v">{{ fmt(card.jobPosting.title) }}</span></div>
            <div v-if="fmtBudget(card.jobPosting.budgetMin, card.jobPosting.budgetMax)" class="row">
              <span class="k">预算</span><span class="v">{{ fmtBudget(card.jobPosting.budgetMin, card.jobPosting.budgetMax) }}</span>
            </div>
            <div v-if="fmt(card.jobPosting.classMode)" class="row"><span class="k">授课方式</span><span class="v">{{ fmt(card.jobPosting.classMode) }}</span></div>
            <div v-if="fmt(card.jobPosting.city)" class="row"><span class="k">城市</span><span class="v">{{ fmt(card.jobPosting.city) }}</span></div>
            <div v-if="fmt(card.jobPosting.address)" class="row"><span class="k">地点</span><span class="v">{{ fmt(card.jobPosting.address) }}</span></div>
            <div v-if="fmt(card.jobPosting.availableTime)" class="row"><span class="k">时间</span><span class="v">{{ fmt(card.jobPosting.availableTime) }}</span></div>
            <div v-if="fmt(card.jobPosting.gradeCode)" class="row"><span class="k">年级</span><span class="v">{{ fmt(card.jobPosting.gradeCode) }}</span></div>
          </div>
          <div v-if="fmt(card.jobPosting.description)" class="desc">{{ fmt(card.jobPosting.description) }}</div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 60;
}

.modal {
  width: min(560px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
  max-height: min(78vh, 720px);
  overflow: auto;
}

.m-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.head-ops {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.u {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.u-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: 1px solid var(--border);
  object-fit: cover;
  background: #fff;
  flex: 0 0 auto;
}

.u-avatar.fallback {
  display: grid;
  place-items: center;
  font-weight: 900;
  color: var(--text);
  background: rgba(31, 35, 41, 0.06);
}

.u-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.u-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 900;
  font-size: 16px;
  min-width: 0;
}

.u-sub {
  font-size: 12px;
  color: var(--muted);
}

.tag {
  font-size: 12px;
  color: var(--text);
  border: 1px solid var(--border);
  background: rgba(0, 190, 189, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 900;
  white-space: nowrap;
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

.sec {
  display: grid;
  gap: 10px;
  border-top: 1px solid var(--border);
  padding-top: 12px;
}

.sec-title {
  font-weight: 900;
  font-size: 14px;
}

.kv {
  display: grid;
  gap: 8px;
}

.row {
  display: grid;
  grid-template-columns: 82px 1fr;
  gap: 10px;
  align-items: start;
}

.k {
  font-size: 12px;
  color: var(--muted);
  font-weight: 900;
}

.v {
  font-size: 13px;
  color: var(--text);
  line-height: 1.5;
  word-break: break-word;
}

.desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text);
  background: rgba(31, 35, 41, 0.03);
  border: 1px solid var(--border);
  padding: 10px 12px;
  border-radius: 12px;
  word-break: break-word;
}
</style>
