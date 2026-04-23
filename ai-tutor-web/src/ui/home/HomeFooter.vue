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
    <div class="inner">
      <div class="links">
        <template
          v-for="l in links?.links || []"
          :key="l.url"
        >
          <a
            v-if="isExternal(l.url)"
            :href="l.url"
            target="_blank"
            rel="noreferrer"
          >{{ l.name }}</a>
          <RouterLink
            v-else
            :to="l.url"
          >
            {{ l.name }}
          </RouterLink>
        </template>
      </div>
      <div class="copy">
        © {{ new Date().getFullYear() }} {{ BRAND_NAME }}
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 40px;
  padding: 0 32px 28px;
}

.inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  width: min(100%, 1600px);
  margin: 0 auto;
  min-height: 96px;
  padding: 28px 32px;
  border-top: 1px solid rgba(31, 35, 41, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.92)),
    linear-gradient(90deg, rgba(0, 190, 189, 0.08), rgba(0, 190, 189, 0));
  box-shadow: 0 -12px 28px rgba(31, 35, 41, 0.04);
}

.links {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.links :deep(a) {
  position: relative;
  color: var(--muted);
  font-size: 14px;
  transition:
    color 0.2s ease,
    transform 0.2s ease;
}

.links :deep(a)::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -6px;
  height: 2px;
  border-radius: 999px;
  background: var(--primary);
  transform: scaleX(0);
  transform-origin: left center;
  transition: transform 0.2s ease;
}

.links :deep(a:hover) {
  color: var(--text);
  transform: translateY(-1px);
}

.links :deep(a:hover)::after {
  transform: scaleX(1);
}

.copy {
  color: var(--muted);
  font-size: 14px;
}

@media (max-width: 768px) {
  .footer {
    padding: 0 16px 20px;
  }

  .inner {
    min-height: auto;
    padding: 20px 18px;
  }

  .links {
    gap: 14px;
  }

  .links :deep(a),
  .copy {
    font-size: 13px;
  }
}
</style>
