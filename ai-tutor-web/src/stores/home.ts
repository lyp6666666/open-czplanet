import { defineStore } from 'pinia'

import { homeGuestApi } from '@/api/homeGuest'
import type {
  BannersVO,
  CursorPageResponse,
  FooterLinksVO,
  GeoLocateVO,
  HomeConfigVO,
  HotDemandCardVO,
  HotServiceCardVO,
  HotTabsVO,
  HotTutorCardVO,
  HotWordsVO,
  SubjectTreeNode,
} from '@/api/types'

export interface PageState<T> {
  list: T[]
  nextCursor: number | null
  isLast: boolean
  loading: boolean
  error: string | null
}

function createEmptyPage<T>(): PageState<T> {
  return {
    list: [],
    nextCursor: null,
    isLast: false,
    loading: false,
    error: null,
  }
}

export const useHomeStore = defineStore('home', {
  state: () => ({
    city: '全国',
    config: null as HomeConfigVO | null,
    geo: null as GeoLocateVO | null,
    hotWords: null as HotWordsVO | null,
    subjectTree: [] as SubjectTreeNode[],
    banners: null as BannersVO | null,
    hotTabsService: null as HotTabsVO | null,
    hotTabsDemand: null as HotTabsVO | null,
    footerLinks: null as FooterLinksVO | null,

    selectedServiceTabId: 'recommend',
    selectedDemandTabId: 'recommend',

    hotServices: createEmptyPage<HotServiceCardVO>(),
    hotDemands: createEmptyPage<HotDemandCardVO>(),
    hotTutors: createEmptyPage<HotTutorCardVO>(),

    loading: false,
    error: null as string | null,
  }),
  actions: {
    cityForApi() {
      const v = (this.city || '').trim()
      if (!v || v === '全国') return undefined
      return v
    },

    async initHome() {
      this.loading = true
      this.error = null
      try {
        const [config, geo] = await Promise.all([homeGuestApi.getHomeConfig(), homeGuestApi.geoLocate()])
        this.config = config
        this.geo = geo
        this.city = geo?.city || config?.defaultCity || this.city

        const [hotWords, subjectTree, banners, hotTabsService, hotTabsDemand, footerLinks] = await Promise.all([
          homeGuestApi.getHotWords({ city: this.cityForApi(), limit: 10 }),
          homeGuestApi.getSubjectTree(),
          homeGuestApi.getBanners({ city: this.cityForApi(), scene: 'home' }),
          homeGuestApi.getHotTabs({ type: 'service', city: this.cityForApi(), limit: 12 }),
          homeGuestApi.getHotTabs({ type: 'demand', city: this.cityForApi(), limit: 12 }),
          homeGuestApi.getFooterLinks(),
        ])

        this.hotWords = hotWords
        this.subjectTree = subjectTree
        this.banners = banners
        this.hotTabsService = hotTabsService
        this.hotTabsDemand = hotTabsDemand
        this.footerLinks = footerLinks

        const firstServiceTabId = hotTabsService.tabs?.[0]?.tabId
        if (firstServiceTabId) this.selectedServiceTabId = firstServiceTabId
        const firstDemandTabId = hotTabsDemand.tabs?.[0]?.tabId
        if (firstDemandTabId) this.selectedDemandTabId = firstDemandTabId

        await Promise.all([this.refreshHotServices(), this.refreshHotDemands(), this.refreshHotTutors()])
      } catch (e) {
        this.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.loading = false
      }
    },

    async refreshCityMeta() {
      const [hotWords, banners, hotTabsService, hotTabsDemand] = await Promise.all([
        homeGuestApi.getHotWords({ city: this.cityForApi(), limit: 10 }),
        homeGuestApi.getBanners({ city: this.cityForApi(), scene: 'home' }),
        homeGuestApi.getHotTabs({ type: 'service', city: this.cityForApi(), limit: 12 }),
        homeGuestApi.getHotTabs({ type: 'demand', city: this.cityForApi(), limit: 12 }),
      ])

      this.hotWords = hotWords
      this.banners = banners
      this.hotTabsService = hotTabsService
      this.hotTabsDemand = hotTabsDemand

      const firstServiceTabId = hotTabsService.tabs?.[0]?.tabId
      if (firstServiceTabId) this.selectedServiceTabId = firstServiceTabId
      const firstDemandTabId = hotTabsDemand.tabs?.[0]?.tabId
      if (firstDemandTabId) this.selectedDemandTabId = firstDemandTabId
    },

    async setCity(city: string) {
      if (!city || city === this.city) return
      this.city = city
      this.loading = true
      this.error = null
      try {
        await this.refreshCityMeta()
        await Promise.all([this.refreshHotServices(), this.refreshHotDemands(), this.refreshHotTutors()])
      } catch (e) {
        this.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.loading = false
      }
    },

    async refreshHotServices() {
      this.hotServices = createEmptyPage()
      return this.loadMoreHotServices()
    },

    async loadMoreHotServices() {
      if (this.hotServices.loading || this.hotServices.isLast) return
      this.hotServices.loading = true
      this.hotServices.error = null
      try {
        const page = await homeGuestApi.getHotServices(
          { tabId: this.selectedServiceTabId, city: this.cityForApi(), sort: 'latest' },
          { pageSize: 12, cursor: this.hotServices.nextCursor },
        )
        this.mergePage(this.hotServices, page)
      } catch (e) {
        this.hotServices.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotServices.loading = false
      }
    },

    async refreshHotDemands() {
      this.hotDemands = createEmptyPage()
      return this.loadMoreHotDemands()
    },

    async loadMoreHotDemands() {
      if (this.hotDemands.loading || this.hotDemands.isLast) return
      this.hotDemands.loading = true
      this.hotDemands.error = null
      try {
        const page = await homeGuestApi.getHotDemands(
          { tabId: this.selectedDemandTabId, city: this.cityForApi(), sort: 'latest' },
          { pageSize: 12, cursor: this.hotDemands.nextCursor },
        )
        this.mergePage(this.hotDemands, page)
      } catch (e) {
        this.hotDemands.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotDemands.loading = false
      }
    },

    async refreshHotTutors() {
      this.hotTutors = createEmptyPage()
      return this.loadMoreHotTutors()
    },

    async loadMoreHotTutors() {
      if (this.hotTutors.loading || this.hotTutors.isLast) return
      this.hotTutors.loading = true
      this.hotTutors.error = null
      try {
        const page = await homeGuestApi.getHotTutors(
          { city: this.cityForApi(), sort: 'recommend' },
          { pageSize: 12, cursor: this.hotTutors.nextCursor },
        )
        this.mergePage(this.hotTutors, page)
      } catch (e) {
        this.hotTutors.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotTutors.loading = false
      }
    },

    mergePage<T>(state: PageState<T>, page: CursorPageResponse<T>) {
      const list = Array.isArray(page.list) ? page.list : []
      state.list = [...state.list, ...list]
      state.nextCursor = page.nextCursor ?? null
      state.isLast = !!page.isLast
    },

    async shuffleHotServices() {
      if (this.hotServices.loading) return
      const next = this.hotServices.isLast ? null : this.hotServices.nextCursor
      this.hotServices = createEmptyPage()
      this.hotServices.nextCursor = next
      await this.loadMoreHotServices()
    },

    async shuffleHotDemands() {
      if (this.hotDemands.loading) return
      const next = this.hotDemands.isLast ? null : this.hotDemands.nextCursor
      this.hotDemands = createEmptyPage()
      this.hotDemands.nextCursor = next
      await this.loadMoreHotDemands()
    },

    async shuffleHotTutors() {
      if (this.hotTutors.loading) return
      const next = this.hotTutors.isLast ? null : this.hotTutors.nextCursor
      this.hotTutors = createEmptyPage()
      this.hotTutors.nextCursor = next
      await this.loadMoreHotTutors()
    },
  },
})
