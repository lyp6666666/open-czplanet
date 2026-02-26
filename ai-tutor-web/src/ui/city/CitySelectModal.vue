<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CITY_ENTRIES, DEFAULT_HOT_CITIES, type CityEntry } from './cities'

const props = defineProps<{
  open: boolean
  modelValue: string
  hotCities?: string[]
  allowNational?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update:modelValue', city: string): void
}>()

const q = ref('')
// Controls expansion of each letter row
const expanded = ref<Record<string, boolean>>({})

const allLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

// Dedup and sort cities
const entries = computed(() => {
  const dedup = new Map<string, CityEntry>()
  for (const it of CITY_ENTRIES) {
    const name = String(it.name || '').trim()
    if (!name) continue
    if (props.allowNational === false && name === '全国') continue
    if (dedup.has(name)) continue
    dedup.set(name, {
      name,
      pinyin: String(it.pinyin || '').trim().toLowerCase(),
      firstLetter: String(it.firstLetter || '').trim().toUpperCase() || '#',
    })
  }
  const list = Array.from(dedup.values())
  list.sort((a, b) =>
    a.firstLetter === b.firstLetter
      ? a.pinyin.localeCompare(b.pinyin)
      : a.firstLetter.localeCompare(b.firstLetter),
  )
  return list
})

const hot = computed(() => {
  // Use passed hotCities or default, ensuring uniqueness
  const base = [...(props.hotCities || []), ...DEFAULT_HOT_CITIES]
  const list = Array.from(new Set(base)).filter(Boolean)
  if (props.allowNational === false) {
    return list.filter((x) => x !== '全国')
  }

  const nationalIdx = list.indexOf('全国')
  if (nationalIdx > -1) list.splice(nationalIdx, 1)
  list.unshift('全国')
  return list
})

// Group by letter
const groups = computed(() => {
  const map = new Map<string, CityEntry[]>()
  for (const letter of allLetters) map.set(letter, [])
  for (const it of entries.value) {
    const l = allLetters.includes(it.firstLetter) ? it.firstLetter : '#'
    if (!map.has(l)) map.set(l, [])
    map.get(l)!.push(it)
  }
  return map
})

const normalizedQuery = computed(() => q.value.trim().toLowerCase())

const searchResults = computed(() => {
  const query = normalizedQuery.value
  if (!query) return []
  const parts = query.split(/\s+/).filter(Boolean)
  return entries.value
    .filter((it) => {
      return parts.every(
        (p) => it.name.includes(p) || it.pinyin.includes(p) || it.firstLetter.toLowerCase() === p,
      )
    })
    .slice(0, 80)
})

function close() {
  emit('close')
}

function selectCity(city: string) {
  const v = String(city || '').trim()
  if (!v) return
  emit('update:modelValue', v)
  close()
}

function scrollTo(letter: string) {
  if (!props.open) return
  const el = document.getElementById(`city-group-${letter}`)
  if (el) {
    el.scrollIntoView({ block: 'start', behavior: 'smooth' })
  }
}

function toggleExpand(letter: string) {
  expanded.value[letter] = !expanded.value[letter]
}

function isExpanded(letter: string) {
  return !!expanded.value[letter]
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

watch(
  () => props.open,
  (open) => {
    if (!open) return
    q.value = ''
    expanded.value = {}
  },
)
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="mask" @click.self="close" @keydown="onKeydown">
      <div class="modal" role="dialog" aria-modal="true" @click.stop>
        <!-- Header Area with Cyan Gradient -->
        <div class="modal-header">
          <div class="header-content">
            <div class="header-group search-group">
              <span class="header-label">直接搜索</span>
              <div class="search-box">
                <input v-model="q" class="search-input" type="text" placeholder="城市名称" />
                <svg class="search-icon" viewBox="0 0 1024 1024" width="16" height="16">
                   <path d="M192 448c0-141.152 114.848-256 256-256s256 114.848 256 256-114.848 256-256 256-256-114.848-256-256z m710.624 409.376l-206.88-206.88A318.784 318.784 0 0 0 768 448c0-176.736-143.264-320-320-320S128 271.264 128 448s143.264 320 320 320a318.784 318.784 0 0 0 202.496-72.256l206.88 206.88 45.248-45.248z" fill="currentColor"/>
                </svg>
              </div>
            </div>
          </div>

        <button class="close-btn" type="button" @click="close">
           <svg viewBox="0 0 1024 1024" width="16" height="16">
             <path d="M563.8 512l262.5-312.9c4.4-5.2 0.7-13.1-6.1-13.1h-79.8c-4.7 0-9.2 2.1-12.3 5.7L511.6 449.8 295.1 191.7c-3-3.6-7.5-5.7-12.3-5.7H203c-6.8 0-10.5 7.9-6.1 13.1L459.4 512 196.9 824.9A7.95 7.95 0 0 0 203 838h79.8c4.7 0 9.2-2.1 12.3-5.7l216.5-258.1 216.5 258.1c3 3.6 7.5 5.7 12.3 5.7h79.8c6.8 0 10.5-7.9 6.1-13.1L563.8 512z" fill="currentColor"/>
           </svg>
        </button>
      </div>

      <div class="modal-body">
        <!-- Search Results -->
        <div v-if="normalizedQuery" class="search-results-section">
           <div v-if="!searchResults.length" class="empty-state">未找到相关城市</div>
           <div v-else class="hot-cities-list">
              <button
                v-for="city in searchResults"
                :key="city.name"
                class="city-tag"
                @click="selectCity(city.name)"
              >
                {{ city.name }}
              </button>
           </div>
        </div>

        <template v-else>
          <!-- Hot Cities -->
          <div class="section">
            <div class="section-title">热门城市</div>
            <div class="hot-cities-list">
              <button class="city-tag" v-if="modelValue && !hot.includes(modelValue)" @click="selectCity(modelValue)">{{ modelValue }}</button>
              <button
                v-for="city in hot"
                :key="city"
                class="city-tag"
                :class="{ active: city === modelValue }"
                @click="selectCity(city)"
              >
                {{ city }}
              </button>
            </div>
          </div>

          <!-- Alphabet Navigation -->
          <div class="section">
            <div class="alphabet-header">
              <span class="section-title" style="margin-bottom:0; margin-right: 12px;">按字母选择：</span>
              <div class="alphabet-list">
                 <button
                   v-for="l in allLetters"
                   :key="l"
                   class="alpha-item"
                   :disabled="!groups.get(l)?.length"
                   @click="scrollTo(l)"
                 >
                   {{ l }}
                 </button>
              </div>
            </div>

            <!-- City Groups -->
            <div class="city-groups">
              <div
                v-for="l in allLetters"
                :key="l"
                :id="`city-group-${l}`"
                class="city-group-row"
                v-show="groups.get(l)?.length"
              >
                <div class="group-letter">{{ l }}</div>
                
                <div class="group-cities-wrapper" :class="{ collapsed: !isExpanded(l) }">
                  <div class="group-cities-inner">
                    <button
                      v-for="city in groups.get(l)"
                      :key="city.name"
                      class="city-link"
                      :class="{ active: city.name === modelValue }"
                      @click="selectCity(city.name)"
                    >
                      {{ city.name }}
                    </button>
                  </div>
                </div>

                <button v-if="groups.get(l) && groups.get(l)!.length > 12" class="expand-btn" @click="toggleExpand(l)">
                   {{ isExpanded(l) ? '收起' : '更多' }}
                   <svg class="arrow" :class="{ up: isExpanded(l) }" viewBox="0 0 1024 1024" width="10" height="10">
                      <path d="M831.872 340.864 512 652.672 192.128 340.864a30.592 30.592 0 0 0-42.752 0 29.12 29.12 0 0 0 0 41.6L489.664 714.24a32 32 0 0 0 44.672 0l340.288-331.712a29.12 29.12 0 0 0 0-41.728 30.592 30.592 0 0 0-42.752 0z" fill="currentColor"/>
                   </svg>
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
  </Teleport>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  width: 820px;
  max-width: 95vw;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}

/* Header */
.modal-header {
  background: linear-gradient(90deg, #4ce2ce 0%, #00bebd 100%);
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 32px;
}

.header-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-label {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.search-box {
  position: relative;
  width: 200px;
  height: 32px;
}

.search-input {
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 4px;
  padding: 0 32px 0 12px;
  color: #fff;
  font-size: 13px;
  outline: none;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.8);
}

.search-icon {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #fff;
  pointer-events: none;
}

.close-btn {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.close-btn:hover {
  opacity: 1;
}

/* Body */
.modal-body {
  padding: 24px 32px;
  overflow-y: auto;
  flex: 1;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 16px;
}

/* Hot Cities */
.hot-cities-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.city-tag {
  min-width: 72px;
  height: 32px;
  background: #fff;
  border: 1px solid #dee0e3;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #646a73;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0 16px;
}

.city-tag:hover {
  color: #00bebd;
  border-color: #00bebd;
}

.city-tag.active {
  color: #fff;
  background: #00bebd;
  border-color: #00bebd;
}

/* Alphabet Nav */
.alphabet-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 16px;
}

.alphabet-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.alpha-item {
  background: none;
  border: none;
  color: #8f959e;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
  font-family: inherit;
}

.alpha-item:hover {
  color: #00bebd;
}

.alpha-item:disabled {
  color: #dee0e3;
  cursor: not-allowed;
}

/* City Rows */
.city-groups {
  display: flex;
  flex-direction: column;
}

.city-group-row {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}

.city-group-row:last-child {
  border-bottom: none;
}

.group-letter {
  width: 40px;
  font-size: 18px;
  color: #8f959e;
  font-family: Arial, sans-serif;
  padding-top: 4px;
  flex-shrink: 0;
}

.group-cities-wrapper {
  flex: 1;
  overflow: hidden;
  /* Height for one line. Assuming 32px height items + gap. */
  /* If items wrap, they will be hidden if collapsed */
}

.group-cities-wrapper.collapsed {
  max-height: 30px; /* Adjust based on line-height */
}

.group-cities-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.city-link {
  background: none;
  border: none;
  color: #1f2329;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
  white-space: nowrap;
}

.city-link:hover {
  color: #00bebd;
}

.city-link.active {
  color: #00bebd;
  font-weight: 600;
}

.expand-btn {
  background: none;
  border: none;
  color: #8f959e;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 0 4px 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.expand-btn:hover {
  color: #00bebd;
}

.arrow {
  transition: transform 0.2s;
}

.arrow.up {
  transform: rotate(180deg);
}

.empty-state {
  text-align: center;
  color: #8f959e;
  padding: 40px 0;
}
</style>
