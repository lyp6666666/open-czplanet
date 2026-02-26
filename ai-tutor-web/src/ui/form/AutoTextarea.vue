<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useAttrs, watch } from 'vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    modelValue: string | null | undefined
    rows?: number | string
    maxHeight?: number | string
  }>(),
  {
    modelValue: '',
    rows: 4,
    maxHeight: undefined,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
}>()

const attrs = useAttrs()
const el = ref<HTMLTextAreaElement | null>(null)

const normalizedRows = computed(() => {
  const n = Number(props.rows)
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 4
})

const normalizedMaxHeight = computed(() => {
  if (props.maxHeight == null) return null
  const n = Number(props.maxHeight)
  return Number.isFinite(n) && n > 0 ? n : null
})

const forwardedAttrs = computed(() => {
  const { style, ...rest } = attrs as Record<string, unknown>
  return rest
})

const forwardedStyle = computed(() => (attrs as Record<string, any>).style as any)

const baseStyle = computed(() => {
  return {
    resize: 'none',
    overflowY: 'hidden',
  }
})

const mergedStyle = computed<any>(() => [forwardedStyle.value, baseStyle.value])

function applyMinHeight() {
  const ta = el.value
  if (!ta) return
  const s = window.getComputedStyle(ta)
  const lineHeight = Number.parseFloat(s.lineHeight)
  const paddingTop = Number.parseFloat(s.paddingTop)
  const paddingBottom = Number.parseFloat(s.paddingBottom)
  const borderTop = Number.parseFloat(s.borderTopWidth)
  const borderBottom = Number.parseFloat(s.borderBottomWidth)

  const lh = Number.isFinite(lineHeight) ? lineHeight : 20
  const pt = Number.isFinite(paddingTop) ? paddingTop : 0
  const pb = Number.isFinite(paddingBottom) ? paddingBottom : 0
  const bt = Number.isFinite(borderTop) ? borderTop : 0
  const bb = Number.isFinite(borderBottom) ? borderBottom : 0

  ta.style.minHeight = `${lh * normalizedRows.value + pt + pb + bt + bb}px`
}

function resize() {
  const ta = el.value
  if (!ta) return

  ta.style.height = 'auto'

  const s = window.getComputedStyle(ta)
  const borderTop = Number.parseFloat(s.borderTopWidth) || 0
  const borderBottom = Number.parseFloat(s.borderBottomWidth) || 0
  const minH = Number.parseFloat(s.minHeight) || 0

  const maxH = normalizedMaxHeight.value
  const next = Math.max(ta.scrollHeight + borderTop + borderBottom, minH)

  if (maxH != null && next > maxH) {
    ta.style.height = `${maxH}px`
    ta.style.overflowY = 'auto'
    return
  }

  ta.style.height = `${next}px`
  ta.style.overflowY = 'hidden'
}

function onInput(e: Event) {
  const v = (e.target as HTMLTextAreaElement).value
  emit('update:modelValue', v)
  resize()
}

let ro: ResizeObserver | null = null

onMounted(() => {
  applyMinHeight()
  nextTick(resize)

  if (typeof window !== 'undefined' && 'ResizeObserver' in window && el.value) {
    ro = new ResizeObserver(() => resize())
    ro.observe(el.value)
  }
})

onBeforeUnmount(() => {
  ro?.disconnect()
  ro = null
})

watch(
  () => props.modelValue,
  () => nextTick(resize),
  { flush: 'post' },
)

watch(
  () => props.rows,
  () => nextTick(() => (applyMinHeight(), resize())),
  { flush: 'post' },
)
</script>

<template>
  <textarea
    ref="el"
    v-bind="forwardedAttrs"
    :rows="normalizedRows"
    :value="modelValue ?? ''"
    :style="mergedStyle"
    @input="onInput"
  />
</template>
