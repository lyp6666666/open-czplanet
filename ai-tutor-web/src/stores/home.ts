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

function shuffleCopy<T>(raw: T[]): T[] {
  const arr = raw.slice()
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const tmp = arr[i]!
    arr[i] = arr[j]!
    arr[j] = tmp
  }
  return arr
}

function mergeUniqueById<T>(base: T[], extra: T[], getId: (it: T) => number): T[] {
  const seen = new Set<number>()
  base.forEach((it) => seen.add(getId(it)))
  const out = base.slice()
  for (const it of extra) {
    const id = getId(it)
    if (seen.has(id)) continue
    seen.add(id)
    out.push(it)
  }
  return out
}

function pickRandomNonOverlapping<T>(pool: T[], excludedIds: Set<number>, limit: number, getId: (it: T) => number): T[] {
  const candidates = pool.filter((it) => !excludedIds.has(getId(it)))
  const shuffled = shuffleCopy(candidates)
  return shuffled.slice(0, Math.max(0, limit))
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

    hotServicesPool: [] as HotServiceCardVO[],
    hotServicesPoolCursor: null as number | null,
    hotServicesPoolIsLast: false,

    hotDemandsPool: [] as HotDemandCardVO[],
    hotDemandsPoolCursor: null as number | null,
    hotDemandsPoolIsLast: false,

    hotTutorsPool: [] as HotTutorCardVO[],
    hotTutorsPoolCursor: null as number | null,
    hotTutorsPoolIsLast: false,

    hotServices: createEmptyPage<HotServiceCardVO>(),
    hotDemands: createEmptyPage<HotDemandCardVO>(),
    hotTutors: createEmptyPage<HotTutorCardVO>(),

    loading: false,
    error: null as string | null,
  }),
  actions: {
    hotServiceDisplaySize() {
      return 6
    },

    hotDemandDisplaySize() {
      return 6
    },

    hotTutorDisplaySize() {
      return 4
    },

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
        const preferred = (this.city || '').trim()
        if (!preferred || preferred === '全国') {
          this.city = geo?.city || config?.defaultCity || this.city
        }

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
      this.hotServicesPool = []
      this.hotServicesPoolCursor = null
      this.hotServicesPoolIsLast = false
      await this.ensureHotServicesPool(Math.max(24, this.hotServiceDisplaySize() * 3))
      await this.shuffleHotServices()
    },

    async loadMoreHotServices() {
      if (this.hotServices.loading) return
      this.hotServices.loading = true
      this.hotServices.error = null
      try {
        const beforeIds = new Set(this.hotServices.list.map((it) => it.serviceId))
        await this.ensureHotServicesPool(this.hotServicesPool.length + 18)
        const more = pickRandomNonOverlapping(this.hotServicesPool, beforeIds, this.hotServiceDisplaySize(), (it) => it.serviceId)
        this.hotServices.list = [...this.hotServices.list, ...more]
        this.hotServices.isLast = this.hotServicesPoolIsLast && this.hotServicesPool.length <= this.hotServices.list.length
      } catch (e) {
        this.hotServices.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotServices.loading = false
      }
    },

    async refreshHotDemands() {
      this.hotDemands = createEmptyPage()
      this.hotDemandsPool = []
      this.hotDemandsPoolCursor = null
      this.hotDemandsPoolIsLast = false
      await this.ensureHotDemandsPool(Math.max(24, this.hotDemandDisplaySize() * 3))
      await this.shuffleHotDemands()
    },

    async loadMoreHotDemands() {
      if (this.hotDemands.loading) return
      this.hotDemands.loading = true
      this.hotDemands.error = null
      try {
        const beforeIds = new Set(this.hotDemands.list.map((it) => it.demandId))
        await this.ensureHotDemandsPool(this.hotDemandsPool.length + 18)
        const more = pickRandomNonOverlapping(this.hotDemandsPool, beforeIds, this.hotDemandDisplaySize(), (it) => it.demandId)
        this.hotDemands.list = [...this.hotDemands.list, ...more]
        this.hotDemands.isLast = this.hotDemandsPoolIsLast && this.hotDemandsPool.length <= this.hotDemands.list.length
      } catch (e) {
        this.hotDemands.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotDemands.loading = false
      }
    },

    async refreshHotTutors() {
      this.hotTutors = createEmptyPage()
      this.hotTutorsPool = []
      this.hotTutorsPoolCursor = null
      this.hotTutorsPoolIsLast = false
      await this.ensureHotTutorsPool(Math.max(20, this.hotTutorDisplaySize() * 4))
      await this.shuffleHotTutors()
    },

    async loadMoreHotTutors() {
      if (this.hotTutors.loading) return
      this.hotTutors.loading = true
      this.hotTutors.error = null
      try {
        const beforeIds = new Set(this.hotTutors.list.map((it) => it.userId))
        await this.ensureHotTutorsPool(this.hotTutorsPool.length + 16)
        const more = pickRandomNonOverlapping(this.hotTutorsPool, beforeIds, this.hotTutorDisplaySize(), (it) => it.userId)
        this.hotTutors.list = [...this.hotTutors.list, ...more]
        this.hotTutors.isLast = this.hotTutorsPoolIsLast && this.hotTutorsPool.length <= this.hotTutors.list.length
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

    async ensureHotServicesPool(minSize: number) {
      let guard = 0
      while (this.hotServicesPool.length < minSize && !this.hotServicesPoolIsLast && guard < 10) {
        const page = await homeGuestApi.getHotServices(
          { tabId: this.selectedServiceTabId, city: this.cityForApi(), sort: 'latest' },
          { pageSize: 36, cursor: this.hotServicesPoolCursor },
        )
        const list = Array.isArray(page.list) ? page.list : []
        this.hotServicesPool = mergeUniqueById(this.hotServicesPool, list, (it) => it.serviceId)
        this.hotServicesPoolCursor = page.nextCursor ?? null
        this.hotServicesPoolIsLast = !!page.isLast
        if (list.length === 0) {
          this.hotServicesPoolIsLast = true
          break
        }
        guard += 1
      }
    },

    async ensureHotDemandsPool(minSize: number) {
      let guard = 0
      while (this.hotDemandsPool.length < minSize && !this.hotDemandsPoolIsLast && guard < 10) {
        const page = await homeGuestApi.getHotDemands(
          { tabId: this.selectedDemandTabId, city: this.cityForApi(), sort: 'latest' },
          { pageSize: 36, cursor: this.hotDemandsPoolCursor },
        )
        const list = Array.isArray(page.list) ? page.list : []
        this.hotDemandsPool = mergeUniqueById(this.hotDemandsPool, list, (it) => it.demandId)
        this.hotDemandsPoolCursor = page.nextCursor ?? null
        this.hotDemandsPoolIsLast = !!page.isLast
        if (list.length === 0) {
          this.hotDemandsPoolIsLast = true
          break
        }
        guard += 1
      }
    },

    async ensureHotTutorsPool(minSize: number) {
      let guard = 0
      while (this.hotTutorsPool.length < minSize && !this.hotTutorsPoolIsLast && guard < 10) {
        const page = await homeGuestApi.getHotTutors(
          { city: this.cityForApi(), sort: 'recommend' },
          { pageSize: 36, cursor: this.hotTutorsPoolCursor },
        )
        const list = Array.isArray(page.list) ? page.list : []
        this.hotTutorsPool = mergeUniqueById(this.hotTutorsPool, list, (it) => it.userId)
        this.hotTutorsPoolCursor = page.nextCursor ?? null
        this.hotTutorsPoolIsLast = !!page.isLast
        if (list.length === 0) {
          this.hotTutorsPoolIsLast = true
          break
        }
        guard += 1
      }
    },

    async shuffleHotServices() {
      if (this.hotServices.loading) return
      this.hotServices.loading = true
      this.hotServices.error = null
      try {
        const need = Math.max(this.hotServiceDisplaySize() * 3, this.hotServicesPool.length)
        await this.ensureHotServicesPool(need)
        const excluded = new Set(this.hotServices.list.map((it) => it.serviceId))
        let picked = pickRandomNonOverlapping(this.hotServicesPool, excluded, this.hotServiceDisplaySize(), (it) => it.serviceId)
        if (picked.length === 0 && this.hotServicesPool.length > 0) {
          picked = shuffleCopy(this.hotServicesPool).slice(0, this.hotServiceDisplaySize())
        }
        this.hotServices.list = picked
        this.hotServices.isLast = this.hotServicesPoolIsLast && this.hotServicesPool.length <= this.hotServices.list.length
      } catch (e) {
        this.hotServices.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotServices.loading = false
      }
    },

    async shuffleHotDemands() {
      if (this.hotDemands.loading) return
      this.hotDemands.loading = true
      this.hotDemands.error = null
      try {
        const need = Math.max(this.hotDemandDisplaySize() * 3, this.hotDemandsPool.length)
        await this.ensureHotDemandsPool(need)
        const excluded = new Set(this.hotDemands.list.map((it) => it.demandId))
        let picked = pickRandomNonOverlapping(this.hotDemandsPool, excluded, this.hotDemandDisplaySize(), (it) => it.demandId)
        if (picked.length === 0 && this.hotDemandsPool.length > 0) {
          picked = shuffleCopy(this.hotDemandsPool).slice(0, this.hotDemandDisplaySize())
        }
        this.hotDemands.list = picked
        this.hotDemands.isLast = this.hotDemandsPoolIsLast && this.hotDemandsPool.length <= this.hotDemands.list.length
      } catch (e) {
        this.hotDemands.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotDemands.loading = false
      }
    },

    async shuffleHotTutors() {
      if (this.hotTutors.loading) return
      this.hotTutors.loading = true
      this.hotTutors.error = null
      try {
        const need = Math.max(this.hotTutorDisplaySize() * 4, this.hotTutorsPool.length)
        await this.ensureHotTutorsPool(need)
        const excluded = new Set(this.hotTutors.list.map((it) => it.userId))
        let picked = pickRandomNonOverlapping(this.hotTutorsPool, excluded, this.hotTutorDisplaySize(), (it) => it.userId)
        if (picked.length === 0 && this.hotTutorsPool.length > 0) {
          picked = shuffleCopy(this.hotTutorsPool).slice(0, this.hotTutorDisplaySize())
        }
        this.hotTutors.list = picked
        this.hotTutors.isLast = this.hotTutorsPoolIsLast && this.hotTutorsPool.length <= this.hotTutors.list.length
      } catch (e) {
        this.hotTutors.error = e instanceof Error ? e.message : '加载失败'
      } finally {
        this.hotTutors.loading = false
      }
    },
  },
})
