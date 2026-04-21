<script setup lang="ts">
import { ref, watch } from 'vue'

import { BRAND_LOGO_LOCAL_PATH, BRAND_LOGO_REMOTE_PATH } from '@/constants/brand'

const props = withDefaults(
  defineProps<{
    alt?: string
    class?: string
    remoteSrc?: string
  }>(),
  {
    alt: '创智星球 logo',
    class: '',
    remoteSrc: BRAND_LOGO_REMOTE_PATH,
  },
)

const currentSrc = ref(props.remoteSrc)

watch(
  () => props.remoteSrc,
  (value) => {
    currentSrc.value = value || BRAND_LOGO_LOCAL_PATH
  },
  { immediate: true },
)

function handleError() {
  if (currentSrc.value !== BRAND_LOGO_LOCAL_PATH) {
    currentSrc.value = BRAND_LOGO_LOCAL_PATH
  }
}
</script>

<template>
  <img :class="props.class" :src="currentSrc" :alt="props.alt" decoding="async" @error="handleError" />
</template>
