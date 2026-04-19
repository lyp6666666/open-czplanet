import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    allowedHosts: ['huoyue.online', 'www.huoyue.online'],
    proxy: {
      '/api': {
        target: 'http://localhost:18084',
        changeOrigin: true,
      },
    },
  },
})
