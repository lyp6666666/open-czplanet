import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import HomeHotSection from './HomeHotSection.vue'

const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
  applicationCreate: vi.fn(async () => ({ id: 1 })),
  applicationStartChat: vi.fn(async () => ({ message: { roomId: 1 } })),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
}))

vi.mock('@/api/application', () => ({
  applicationApi: {
    create: mocks.applicationCreate,
    startChat: mocks.applicationStartChat,
  },
}))

vi.mock('@/api/favoritesTutors', () => ({
  favoritesTutorsApi: {
    checkTutorFavorites: vi.fn(async () => []),
    favoriteTutor: vi.fn(async () => undefined),
    unfavoriteTutor: vi.fn(async () => undefined),
  },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, user: { userType: 1 } }),
}))

vi.mock('@/stores/settings', () => ({
  DEFAULT_APPLICATION_GREETING: 'hi',
  useSettingsStore: () => ({ loaded: true, applicationGreeting: 'hello', load: vi.fn(async () => undefined) }),
}))

const toastShow = vi.fn()
vi.mock('@/stores/toast', () => ({
  useToastStore: () => ({ show: toastShow }),
}))

describe('HomeHotSection', () => {
  it('机构单点击发起申请不调用 startChat', async () => {
    const wrapper = mount(HomeHotSection, {
      props: {
        city: '北京',
        hotTabsService: null,
        hotTabsDemand: null,
        serviceTabId: 'recommend',
        demandTabId: 'recommend',
        hotServices: { list: [], loading: false, error: null, isLast: true, nextCursor: null },
        hotDemands: {
          list: [
            {
              demandId: 3021,
              title: '机构单｜初二数学提分',
              subject: { id: 201, name: '初中数学' },
              budget: { min: '180', max: '260', unit: '小时' },
              classMode: 'OFFLINE',
              city: '北京',
              addressSimple: '望京',
              childAge: 14,
              scheduleText: '["Tue 19-21","Sat 10-12"]',
              parent: { userId: 20019, displayName: '示例机构20019', avatar: '' },
              tags: ['["Tue 19-21","Sat 10-12"]'],
              publisherIdentity: 'ORGANIZATION',
            },
          ],
          loading: false,
          error: null,
          isLast: true,
          nextCursor: null,
        },
        hotTutors: { list: [], loading: false, error: null, isLast: true, nextCursor: null },
        showServices: false,
        showDemands: true,
        showTutors: false,
      },
    })

    const btn = wrapper.get('button.btn.btn-primary')
    await btn.trigger('click')
    await Promise.resolve()

    expect(mocks.applicationCreate).toHaveBeenCalledTimes(1)
    expect(mocks.applicationStartChat).toHaveBeenCalledTimes(0)
    expect(mocks.routerPush).toHaveBeenCalledTimes(0)
    expect(toastShow).toHaveBeenCalledWith('申请已发送', 'success')
  })
})
