<script setup lang="ts">
import type { FooterLinksVO } from '@/api/types'
import { BRAND_NAME } from '@/constants/brand'

defineProps<{
  links: FooterLinksVO | null
}>()
</script>

<template>
  <footer class="footer">
    <div class="container inner">
      <div class="links">
        <a v-for="l in links?.links || []" :key="l.url" :href="l.url" target="_blank" rel="noreferrer">{{ l.name }}</a>
      </div>
      <div class="copy">© {{ new Date().getFullYear() }} {{ BRAND_NAME }}</div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 24px;
  border-top: 1px solid var(--border);
  padding: 16px 0 24px;
  background: rgba(255, 255, 255, 0.6);
}

.inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.links a {
  color: var(--muted);
  font-size: 12px;
}

.copy {
  color: var(--muted);
  font-size: 12px;
}
</style>
