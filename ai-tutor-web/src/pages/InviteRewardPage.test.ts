import { beforeEach, describe, expect, it } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import InviteRewardPage from './InviteRewardPage.vue'

function createStorage() {
  const store = new Map<string, string>()
  return {
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    setItem(key: string, value: string) {
      store.set(key, String(value))
    },
    removeItem(key: string) {
      store.delete(key)
    },
    clear() {
      store.clear()
    },
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [{ path: '/invite', name: 'inviteReward', component: InviteRewardPage }],
  })
}

describe('InviteRewardPage', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.setItem('ai_tutor_token', 'mock.token')
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: new URL('http://localhost/#/invite'),
    })
  })

  it('renders invite code and generated invite link instead of placeholder dashes', async () => {
    const router = createTestRouter()
    await router.push('/invite')
    await router.isReady()

    const wrapper = mount(InviteRewardPage, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('ABC123')
    expect(wrapper.text()).toContain('/auth/student?inviteCode=ABC123')
    /*
     * 企业规范：系统推广码属于运营投放物料，不在用户个人邀请页展示，避免和个人邀请码产生认知冲突。
     */
    expect(wrapper.text()).not.toContain('CHUANGZHI')
    expect(wrapper.text()).not.toContain('创智推广专属福利')
    expect(wrapper.text()).not.toContain('邀请码生成中')
  })
})
