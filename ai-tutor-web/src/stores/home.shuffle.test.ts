import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createPinia, setActivePinia } from 'pinia'

import { useHomeStore } from '@/stores/home'

const mocks = vi.hoisted(() => ({
  getHotServices: vi.fn(),
  getHotDemands: vi.fn(),
  getHotTutors: vi.fn(),
}))

vi.mock('@/api/homeGuest', () => ({
  homeGuestApi: {
    getHotServices: mocks.getHotServices,
    getHotDemands: mocks.getHotDemands,
    getHotTutors: mocks.getHotTutors,
    getHomeConfig: vi.fn(),
    geoLocate: vi.fn(),
    getHotWords: vi.fn(),
    getSubjectTree: vi.fn(),
    getBanners: vi.fn(),
    getHotTabs: vi.fn(),
    getFooterLinks: vi.fn(),
    suggest: vi.fn(),
  },
}))

function makeServices(n: number) {
  return Array.from({ length: n }, (_, i) => ({
    serviceId: i + 1,
    title: `服务${i + 1}`,
    subject: { id: 1, name: '数学' },
    pricePerHour: '100',
    mode: 'ONLINE',
    city: '北京',
    tutor: { userId: 100 + i, displayName: `老师${i + 1}`, avatar: '', education: '本科', experienceYears: 1, ratePerHour: '100' },
    tags: [],
  }))
}

function makeDemands(n: number) {
  return Array.from({ length: n }, (_, i) => ({
    demandId: i + 1,
    title: `需求${i + 1}`,
    subject: { id: 1, name: '数学' },
    budget: { min: '100', max: '150', unit: '小时' },
    classMode: 'OFFLINE',
    city: '北京',
    addressSimple: '',
    childAge: 12,
    scheduleText: '',
    parent: { userId: 200 + i, displayName: '家长', avatar: '' },
    tags: [],
  }))
}

function makeTutors(n: number) {
  return Array.from({ length: n }, (_, i) => ({
    userId: i + 1,
    displayName: `老师${i + 1}`,
    avatar: '',
    city: i % 2 === 0 ? '北京' : '上海',
    education: '本科',
    experienceYears: 2,
    ratePerHour: '120',
    subjectTags: [],
    highlights: [],
    representativeServices: [],
  }))
}

describe('home store shuffle', () => {
  beforeEach(() => {
    mocks.getHotServices.mockReset()
    mocks.getHotDemands.mockReset()
    mocks.getHotTutors.mockReset()
    setActivePinia(createPinia())
  })

  it('shuffles demands without overlapping consecutive batches when pool is sufficient', async () => {
    mocks.getHotDemands.mockResolvedValue({ nextCursor: null, isLast: true, list: makeDemands(30) })
    const home = useHomeStore()
    home.city = '北京'
    await home.refreshHotDemands()
    const first = new Set(home.hotDemands.list.map((x) => x.demandId))
    await home.shuffleHotDemands()
    const second = new Set(home.hotDemands.list.map((x) => x.demandId))
    const overlap = Array.from(first).filter((id) => second.has(id))
    expect(overlap.length).toBe(0)
  })

  it('shuffles tutors without overlapping consecutive batches when pool is sufficient', async () => {
    mocks.getHotTutors.mockResolvedValue({ nextCursor: null, isLast: true, list: makeTutors(30) })
    const home = useHomeStore()
    home.city = '北京'
    await home.refreshHotTutors()
    const first = new Set(home.hotTutors.list.map((x) => x.userId))
    await home.shuffleHotTutors()
    const second = new Set(home.hotTutors.list.map((x) => x.userId))
    const overlap = Array.from(first).filter((id) => second.has(id))
    expect(overlap.length).toBe(0)
  })

  it('shuffles services without overlapping consecutive batches when pool is sufficient', async () => {
    mocks.getHotServices.mockResolvedValue({ nextCursor: null, isLast: true, list: makeServices(30) })
    const home = useHomeStore()
    home.city = '北京'
    await home.refreshHotServices()
    const first = new Set(home.hotServices.list.map((x) => x.serviceId))
    await home.shuffleHotServices()
    const second = new Set(home.hotServices.list.map((x) => x.serviceId))
    const overlap = Array.from(first).filter((id) => second.has(id))
    expect(overlap.length).toBe(0)
  })
})

