import { createApp } from 'vue'

import App from './App.vue'
import { setAuthInvalidHandler } from './api/http'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import { createPinia } from 'pinia'
import './styles/global.css'

const pinia = createPinia()
const auth = useAuthStore(pinia)

setAuthInvalidHandler(() => {
  const raw = localStorage.getItem('ai_tutor_user')
  let userType: number | null = null
  if (raw) {
    try {
      const u = JSON.parse(raw) as { userType?: unknown }
      const n = typeof u.userType === 'number' ? u.userType : Number(u.userType)
      userType = Number.isFinite(n) ? n : null
    } catch {
      userType = null
    }
  }

  auth.logout()

  const path = router.currentRoute.value.path
  if (path.startsWith('/auth/')) return
  if (userType === 3) {
    void router.replace('/auth/org')
    return
  }
  void router.replace(userType === 1 ? '/auth/tutor' : '/auth/student')
})

createApp(App).use(pinia).use(router).mount('#app')
