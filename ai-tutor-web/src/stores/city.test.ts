import { beforeEach, describe, expect, it } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'

import { useCityStore } from '@/stores/city'

describe('city store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('defaults to 北京 when no city stored', () => {
    const store = useCityStore()
    expect(store.city).toBe('北京')
  })

  it('persists city to localStorage', () => {
    const store = useCityStore()
    store.setCity('上海')
    expect(store.city).toBe('上海')
    expect(localStorage.getItem('ai_tutor_city')).toBe('上海')
  })
})

