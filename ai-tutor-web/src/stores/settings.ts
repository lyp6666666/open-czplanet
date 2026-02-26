import { defineStore } from 'pinia'

import { userApi } from '@/api/user'

export const DEFAULT_APPLICATION_GREETING = '您好，我和岗位的匹配度很高，可以通过详细聊聊吗'

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    applicationGreeting: DEFAULT_APPLICATION_GREETING,
    loaded: false,
  }),
  actions: {
    async load() {
      const res = await userApi.settings()
      this.applicationGreeting = res?.applicationGreeting?.trim() ? res.applicationGreeting.trim() : DEFAULT_APPLICATION_GREETING
      this.loaded = true
      return this.applicationGreeting
    },
    async saveApplicationGreeting(greeting: string) {
      const v = greeting?.trim() ? greeting.trim() : DEFAULT_APPLICATION_GREETING
      const res = await userApi.updateSettings({ applicationGreeting: v })
      this.applicationGreeting = res?.applicationGreeting?.trim() ? res.applicationGreeting.trim() : DEFAULT_APPLICATION_GREETING
      this.loaded = true
      return this.applicationGreeting
    },
    reset() {
      this.applicationGreeting = DEFAULT_APPLICATION_GREETING
    },
  },
})

