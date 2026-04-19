import { beforeEach, describe, expect, it } from 'vitest'

import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'

import HomeHeader from './HomeHeader.vue'

function createStorage(): Storage {
  const store = new Map<string, string>()
  return {
    get length() {
      return store.size
    },
    clear() {
      store.clear()
    },
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    key(index: number) {
      return Array.from(store.keys())[index] ?? null
    },
    removeItem(key: string) {
      store.delete(key)
    },
    setItem(key: string, value: string) {
      store.set(key, String(value))
    },
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/me', name: 'me', component: { template: '<div />' } },
      { path: '/invite', name: 'inviteReward', component: { template: '<div />' } },
      { path: '/tutor/favorites', name: 'tutorFavorites', component: { template: '<div />' } },
      { path: '/student/favorites', name: 'studentFavorites', component: { template: '<div />' } },
      { path: '/auth/tutor', name: 'authTutor', component: { template: '<div />' } },
      { path: '/auth/student', name: 'authStudent', component: { template: '<div />' } },
    ],
  })
}

describe('HomeHeader', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.clear()
  })

  it('shows two entry buttons when unauthenticated', async () => {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomeHeader, {
      props: { city: '北京', config: null, hotWords: null },
      global: {
        plugins: [createPinia(), router],
      },
    })

    expect(wrapper.text()).toContain('我要当家教')
    expect(wrapper.text()).toContain('我要找家教')

    const tutorBtn = wrapper.findAll('button').find((b) => b.text() === '我要当家教')
    expect(tutorBtn).toBeTruthy()
    await tutorBtn!.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('authTutor')
  })

  it('shows avatar and supports logout when authenticated', async () => {
    localStorage.setItem('ai_tutor_token', 'mock.teacher.token')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({
        id: 1001,
        name: '教师0000',
        phone: '13800138000',
        avatar: 'https://example.com/tutor.png',
        sex: null,
        userType: 1,
        token: 'mock.teacher.token',
      }),
    )

    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomeHeader, {
      props: { city: '北京', config: null, hotWords: null },
      global: {
        plugins: [createPinia(), router],
      },
    })

    expect(wrapper.find('img.avatar').exists()).toBe(true)

    await wrapper.find('.user').trigger('mouseenter')
    expect(wrapper.text()).toContain('收藏')
    expect(wrapper.text()).toContain('邀请有礼')
    expect(wrapper.text()).toContain('退出登录')

    await wrapper.findAll('button').find((b) => b.text() === '退出登录')?.trigger('click')
    expect(localStorage.getItem('ai_tutor_token')).toBeNull()
  })
})
