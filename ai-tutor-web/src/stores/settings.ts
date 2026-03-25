import { defineStore } from 'pinia'

import { userApi } from '@/api/user'

export const DEFAULT_APPLICATION_GREETING = '您好，我这边有一个家教需求，方便聊聊吗？'
export const LEGACY_DEFAULT_APPLICATION_GREETING = '您好，我和岗位的匹配度很高，可以通过详细聊聊吗'

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    applicationGreeting: DEFAULT_APPLICATION_GREETING,
    loaded: false,
  }),
  actions: {
    async load() {
      const res = await userApi.settings()
      const v = res?.applicationGreeting?.trim() ? res.applicationGreeting.trim() : ''
      this.applicationGreeting =
        v && v !== LEGACY_DEFAULT_APPLICATION_GREETING ? v : DEFAULT_APPLICATION_GREETING
      this.loaded = true
      return this.applicationGreeting
    },
    async saveApplicationGreeting(greeting: string) {
      const v = greeting?.trim() ? greeting.trim() : DEFAULT_APPLICATION_GREETING
      const res = await userApi.updateSettings({ applicationGreeting: v })
      const saved = res?.applicationGreeting?.trim() ? res.applicationGreeting.trim() : ''
      this.applicationGreeting =
        saved && saved !== LEGACY_DEFAULT_APPLICATION_GREETING ? saved : DEFAULT_APPLICATION_GREETING
      this.loaded = true
      return this.applicationGreeting
    },
    reset() {
      this.applicationGreeting = DEFAULT_APPLICATION_GREETING
    },
  },
})
