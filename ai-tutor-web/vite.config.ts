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
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/org': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/user': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/chat': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/payment': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/live': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/invite': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
    },
  },
})
