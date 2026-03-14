import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const visible = ref(false)
  const message = ref('')
  const type = ref<'success' | 'error' | 'info'>('info')
  let timer: number | null = null

  function show(msg: string, t: 'success' | 'error' | 'info' = 'info', duration = 3000) {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    message.value = msg
    type.value = t
    visible.value = true

    if (duration > 0) {
      timer = window.setTimeout(() => {
        visible.value = false
        timer = null
      }, duration)
    }
  }

  function hide() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    visible.value = false
  }

  return { visible, message, type, show, hide }
})
