import { beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'

import MePage from './MePage.vue'
import { useToastStore } from '@/stores/toast'

const mocks = vi.hoisted(() => ({
  uploadImage: vi.fn(),
  me: vi.fn(),
  updateUserInfo: vi.fn(),
}))

vi.mock('@/api/assets', () => ({
  assetsApi: {
    uploadImage: mocks.uploadImage,
  },
}))

vi.mock('@/api/user', () => ({
  userApi: {
    me: mocks.me,
    updateUserInfo: mocks.updateUserInfo,
    sendCode: vi.fn(),
    loginOrRegister: vi.fn(),
    batch: vi.fn(),
  },
}))

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [{ path: '/me', name: 'me', component: { template: '<div />' } }],
  })
}

function createLocalStorageMock() {
  const store = new Map<string, string>()
  return {
    getItem: (key: string) => (store.has(key) ? store.get(key)! : null),
    setItem: (key: string, value: string) => {
      store.set(key, String(value))
    },
    removeItem: (key: string) => {
      store.delete(key)
    },
    clear: () => {
      store.clear()
    },
  }
}

describe('MePage avatar upload', () => {
  beforeEach(() => {
    const localStorageMock = createLocalStorageMock()
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
  })

  it('uploads avatar then saves avatar url', async () => {
    localStorage.setItem('ai_tutor_token', 't')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 1001, token: 't', userType: 2, name: '用户', phone: '13800001111', avatar: '', sex: 1 }),
    )

    mocks.me.mockReset()
    mocks.updateUserInfo.mockReset()
    mocks.uploadImage.mockReset()

    mocks.me.mockResolvedValue({ name: '用户', phone: '13800001111', avatar: '', sex: 1, userType: 2, teacherProfile: null, studentProfile: null })
    mocks.uploadImage.mockResolvedValue({ objectKey: 'avatars/1001/x.png', url: 'https://assets.example.com/ai-tutor/avatars/1001/x.png', contentType: 'image/png', size: 3 })
    mocks.updateUserInfo.mockResolvedValue('ok')

    const router = createTestRouter()
    await router.push('/me')
    await router.isReady()

    const wrapper = mount(MePage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    const file = new File([new Uint8Array([1, 2, 3])], 'a.png', { type: 'image/png' })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().trim() === '保存')!.trigger('click')
    await flushPromises()

    expect(mocks.uploadImage).toHaveBeenCalledTimes(1)
    expect(mocks.updateUserInfo).toHaveBeenCalledTimes(1)
    expect(mocks.updateUserInfo.mock.calls[0]![0]).toMatchObject({
      baseUserInfo: { avatar: 'https://assets.example.com/ai-tutor/avatars/1001/x.png' },
    })
  })

  it('uses student real name as base user name when saving student profile', async () => {
    localStorage.setItem('ai_tutor_token', 't')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 1001, token: 't', userType: 2, name: '', phone: '13800001111', avatar: '', sex: 2 }),
    )

    mocks.me.mockReset()
    mocks.updateUserInfo.mockReset()
    mocks.uploadImage.mockReset()

    mocks.me.mockResolvedValue({
      name: '',
      phone: '13800001111',
      avatar: '',
      sex: 2,
      userType: 2,
      teacherProfile: null,
      studentProfile: { realName: '', childAge: null, address: '', demandDescription: '', budget: null },
    })
    mocks.updateUserInfo.mockResolvedValue('ok')

    const router = createTestRouter()
    await router.push('/me')
    await router.isReady()

    const wrapper = mount(MePage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    await wrapper.find('input[placeholder="例如：王女士"]').setValue('陆熠鹏')
    await wrapper.findAll('button').find((b) => b.text().trim() === '保存')!.trigger('click')
    await flushPromises()

    expect(mocks.updateUserInfo).toHaveBeenCalledTimes(1)
    expect(mocks.updateUserInfo.mock.calls[0]![0]).toMatchObject({
      baseUserInfo: { name: '陆熠鹏' },
      studentExtInfo: { realName: '陆熠鹏' },
    })
  })

  it('rejects avatar larger than 4MB before upload', async () => {
    localStorage.setItem('ai_tutor_token', 't')
    localStorage.setItem(
      'ai_tutor_user',
      JSON.stringify({ id: 1001, token: 't', userType: 2, name: '用户', phone: '13800001111', avatar: '', sex: 1 }),
    )

    mocks.me.mockReset()
    mocks.updateUserInfo.mockReset()
    mocks.uploadImage.mockReset()

    mocks.me.mockResolvedValue({ name: '用户', phone: '13800001111', avatar: '', sex: 1, userType: 2, teacherProfile: null, studentProfile: null })

    const router = createTestRouter()
    await router.push('/me')
    await router.isReady()

    const wrapper = mount(MePage, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()

    const file = new File([new Uint8Array(4 * 1024 * 1024 + 1)], 'oversize.png', { type: 'image/png' })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    const toast = useToastStore()
    expect(mocks.uploadImage).not.toHaveBeenCalled()
    expect(toast.message).toBe('头像文件不能超过 4MB')
    expect(toast.type).toBe('error')
  })
})
