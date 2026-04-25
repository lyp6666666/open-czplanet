<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { FooterLinksVO } from '@/api/types'
import { BRAND_NAME } from '@/constants/brand'

defineProps<{
  links: FooterLinksVO | null
}>()

function isExternal(url: string) {
  return /^https?:\/\//i.test(url)
}
</script>

<template>
  <footer class="footer">
    <div class="container footer-panel">
      <div class="footer-brand">
        <div class="footer-name">{{ BRAND_NAME }}</div>
        <p>把优质师资、AI学情洞察和家长陪伴整合到一个可信赖的教育平台中。</p>
      </div>

      <div class="footer-links">
        <template v-for="item in links?.links || []" :key="item.url">
          <a v-if="isExternal(item.url)" :href="item.url" target="_blank" rel="noreferrer">{{ item.name }}</a>
          <RouterLink v-else :to="item.url">{{ item.name }}</RouterLink>
        </template>
      </div>

      <div class="footer-copy">© {{ new Date().getFullYear() }} {{ BRAND_NAME }} · All Rights Reserved.</div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  padding: 0 0 34px;
}

.footer-panel {
  display: grid;
  gap: 18px;
  padding: 28px 32px;
  border-radius: 30px;
  border: 1px solid rgba(80, 112, 195, 0.12);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 20px 44px rgba(51, 78, 146, 0.08);
}

.footer-brand {
  display: grid;
  gap: 8px;
}

.footer-name {
  color: #12265f;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.footer-brand p,
.footer-copy {
  margin: 0;
  color: #7485aa;
  font-size: 14px;
  line-height: 1.8;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

.footer-links :deep(a) {
  color: #29458e;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 720px) {
  .footer-panel {
    padding: 22px 18px;
    border-radius: 24px;
  }

  .footer-name {
    font-size: 24px;
  }
}
</style>
