<script setup lang="ts">
import AppTopBar from './AppTopBar.vue'
</script>

<template>
  <div class="shell">
    <AppTopBar />
    <main class="main">
      <div class="container">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.main {
  padding: 18px 0 32px;
}
</style>

