import { defineStore } from 'pinia'

const STORAGE_CITY_KEY = 'ai_tutor_city'

function readInitialCity(): string {
  const raw = localStorage.getItem(STORAGE_CITY_KEY)
  const v = typeof raw === 'string' ? raw.trim() : ''
  return v || '北京'
}

export const useCityStore = defineStore('city', {
  state: () => ({
    city: readInitialCity(),
  }),
  actions: {
    setCity(next: string) {
      const v = String(next || '').trim()
      if (!v || v === this.city) return
      this.city = v
      localStorage.setItem(STORAGE_CITY_KEY, v)
    },
  },
})

