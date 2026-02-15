<script setup lang="ts">
import { computed, ref } from 'vue'

import { homeGuestApi } from '@/api/homeGuest'
import type { HomeConfigVO, HotWordsVO, SearchSuggestVO } from '@/api/types'
import { debounce } from '@/utils/debounce'

const props = defineProps<{
  city: string
  config: HomeConfigVO | null
  hotWords: HotWordsVO | null
}>()

const emit = defineEmits<{
  (e: 'city-change', city: string): void
}>()

const cities = computed(() => {
  const base = [props.city, '北京', '上海', '广州', '深圳', '杭州']
  return Array.from(new Set(base.filter(Boolean)))
})

const keyword = ref('')
const suggestOpen = ref(false)
const suggestLoading = ref(false)
const suggestError = ref<string | null>(null)
const suggestList = ref<SearchSuggestVO['list']>([])

const placeholder = computed(() => props.config?.search?.placeholder || '搜索科目/老师/需求，例如：初中数学')

const runSuggest = debounce(async () => {
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
    const res = await homeGuestApi.suggest({ q, city: props.city, limit: 10 })
    suggestList.value = res.list || []
  } catch (e) {
    suggestError.value = e instanceof Error ? e.message : '搜索联想失败'
    suggestList.value = []
  } finally {
    suggestLoading.value = false
  }
}, 250)

function onKeywordInput() {
  runSuggest()
}

function useHotWord(word: string) {
  keyword.value = word
  runSuggest()
}

function closeSuggest() {
  window.setTimeout(() => {
    suggestOpen.value = false
  }, 150)
}
</script>

<template>
  <header class="header">
    <div class="container bar">
      <div class="left">
        <div class="logo">家教直聘</div>

        <label class="city">
          <select class="city-select" :value="city" @change="emit('city-change', ($event.target as HTMLSelectElement).value)">
            <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>
      </div>

      <div class="search">
        <div class="search-box">
          <input
            v-model="keyword"
            class="search-input"
            type="text"
            :placeholder="placeholder"
            @input="onKeywordInput"
            @focus="onKeywordInput"
            @blur="closeSuggest"
          />
          <button class="btn btn-primary search-btn" type="button">搜索</button>
        </div>

        <div class="hot-words" v-if="hotWords?.list?.length">
          <span class="label">热搜</span>
          <button v-for="w in hotWords.list" :key="w.word" class="chip" type="button" @click="useHotWord(w.word)">
            {{ w.word }}
          </button>
        </div>

        <div v-if="suggestOpen" class="suggest card">
          <div v-if="suggestLoading" class="suggest-row muted">加载中...</div>
          <div v-else-if="suggestError" class="suggest-row muted">{{ suggestError }}</div>
          <template v-else>
            <div v-if="!suggestList.length" class="suggest-row muted">暂无建议</div>
            <button v-for="(it, idx) in suggestList" :key="idx" class="suggest-row" type="button">
              <div class="title">{{ it.title }}</div>
              <div class="sub" v-if="it.subtitle">{{ it.subtitle }}</div>
              <div class="tag">{{ it.type }}</div>
            </button>
          </template>
        </div>
      </div>

      <div class="right">
        <a class="btn" :href="config?.authEntry?.link || '#/login'">{{ config?.authEntry?.loginText || '登录/注册' }}</a>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(246, 247, 251, 0.88);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.bar {
  display: grid;
  grid-template-columns: 260px 1fr 140px;
  gap: 14px;
  align-items: start;
  padding: 14px 0;
}

.left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--text);
}

.city-select {
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  padding: 0 10px;
  background: #fff;
  outline: none;
}

.search {
  position: relative;
}

.search-box {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.search-input {
  height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  outline: none;
  background: #fff;
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.search-btn {
  height: 36px;
}

.hot-words {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.chip {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: var(--muted);
}

.suggest {
  position: absolute;
  left: 0;
  right: 0;
  top: 78px;
  padding: 6px;
  display: grid;
  gap: 4px;
}

.suggest-row {
  width: 100%;
  text-align: left;
  padding: 10px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
}

.suggest-row:hover {
  border-color: var(--border);
  background: rgba(255, 255, 255, 0.6);
}

.title {
  font-size: 13px;
  font-weight: 600;
}

.sub {
  grid-column: 1 / -1;
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.tag {
  font-size: 11px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.14);
  padding: 2px 8px;
  border-radius: 999px;
}

.muted {
  color: var(--muted);
  cursor: default;
}

.right {
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}
</style>
