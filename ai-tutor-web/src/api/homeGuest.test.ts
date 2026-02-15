import { describe, expect, it } from 'vitest'

import { homeGuestApi } from './homeGuest'

describe('homeGuestApi', () => {
  it('fetches home config', async () => {
    const res = await homeGuestApi.getHomeConfig()
    expect(res.defaultCity).toBe('北京')
    expect(res.nav.length).toBeGreaterThan(0)
  })

  it('fetches geo locate', async () => {
    const res = await homeGuestApi.geoLocate()
    expect(res.city).toBe('北京')
  })

  it('fetches hot words', async () => {
    const res = await homeGuestApi.getHotWords({ city: '北京', limit: 10 })
    expect(res.list.length).toBeGreaterThan(0)
  })

  it('fetches search suggest', async () => {
    const res = await homeGuestApi.suggest({ q: '数学', city: '北京', limit: 10 })
    expect(res.q).toBe('数学')
    expect(res.list.length).toBeGreaterThan(0)
  })

  it('fetches subject tree', async () => {
    const res = await homeGuestApi.getSubjectTree()
    expect(res.length).toBeGreaterThan(0)
    expect(res[0]?.name).toBe('数学')
  })

  it('fetches banners', async () => {
    const res = await homeGuestApi.getBanners({ city: '北京', scene: 'home' })
    expect(res.carousel.length).toBeGreaterThan(0)
    expect(res.cards.length).toBeGreaterThan(0)
  })

  it('fetches hot tabs', async () => {
    const res = await homeGuestApi.getHotTabs({ type: 'service', city: '北京', limit: 12 })
    expect(res.type).toBe('service')
    expect(res.tabs.length).toBeGreaterThan(0)
  })

  it('fetches hot services', async () => {
    const res = await homeGuestApi.getHotServices({ tabId: 'recommend', city: '北京' }, { pageSize: 12, cursor: null })
    expect(res.list.length).toBeGreaterThan(0)
    expect(res.isLast).toBe(true)
  })

  it('fetches hot demands', async () => {
    const res = await homeGuestApi.getHotDemands({ tabId: 'recommend', city: '北京' }, { pageSize: 12, cursor: null })
    expect(res.list.length).toBeGreaterThan(0)
    expect(res.isLast).toBe(true)
  })

  it('fetches hot tutors', async () => {
    const res = await homeGuestApi.getHotTutors({ city: '北京' }, { pageSize: 12, cursor: null })
    expect(res.list.length).toBeGreaterThan(0)
    expect(res.isLast).toBe(true)
  })

  it('fetches footer links', async () => {
    const res = await homeGuestApi.getFooterLinks()
    expect(res.links.length).toBeGreaterThan(0)
  })
})
