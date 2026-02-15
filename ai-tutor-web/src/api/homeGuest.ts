import { http } from './http'
import type {
  BannersVO,
  CursorPageRequest,
  CursorPageResponse,
  FooterLinksVO,
  GeoLocateVO,
  HomeConfigVO,
  HotDemandCardVO,
  HotServiceCardVO,
  HotTabsVO,
  HotTutorCardVO,
  HotWordsVO,
  SearchSuggestVO,
  SubjectTreeNode,
} from './types'

export const homeGuestApi = {
  getHomeConfig(city?: string) {
    return http.get<unknown, HomeConfigVO>('/api/v1/public/home/config', {
      params: city ? { city } : undefined,
    })
  },

  geoLocate() {
    return http.get<unknown, GeoLocateVO>('/api/v1/public/geo/locate')
  },

  getHotWords(params?: { city?: string; limit?: number }) {
    return http.get<unknown, HotWordsVO>('/api/v1/public/home/hot-words', { params })
  },

  suggest(params: { q: string; city?: string; limit?: number }) {
    return http.get<unknown, SearchSuggestVO>('/api/v1/public/home/search/suggest', { params })
  },

  getSubjectTree() {
    return http.get<unknown, SubjectTreeNode[]>('/api/v1/public/subjects/tree')
  },

  getBanners(params?: { city?: string; scene?: string }) {
    return http.get<unknown, BannersVO>('/api/v1/public/home/banners', { params })
  },

  getHotTabs(params: { type: 'service' | 'demand'; city?: string; limit?: number }) {
    return http.get<unknown, HotTabsVO>('/api/v1/public/home/hot-tabs', { params })
  },

  getHotServices(
    query?: { tabId?: string; subjectId?: number; city?: string; mode?: string; sort?: string },
    page?: CursorPageRequest,
  ) {
    return http.post<unknown, CursorPageResponse<HotServiceCardVO>>(
      '/api/v1/public/home/hot/services',
      {
        pageSize: page?.pageSize ?? 10,
        cursor: page?.cursor ?? null,
      },
      { params: query },
    )
  },

  getHotDemands(
    query?: { tabId?: string; subjectId?: number; city?: string; classMode?: string; sort?: string },
    page?: CursorPageRequest,
  ) {
    return http.post<unknown, CursorPageResponse<HotDemandCardVO>>(
      '/api/v1/public/home/hot/demands',
      {
        pageSize: page?.pageSize ?? 10,
        cursor: page?.cursor ?? null,
      },
      { params: query },
    )
  },

  getHotTutors(query?: { subjectId?: number; city?: string; mode?: string; sort?: string }, page?: CursorPageRequest) {
    return http.post<unknown, CursorPageResponse<HotTutorCardVO>>(
      '/api/v1/public/home/hot/tutors',
      {
        pageSize: page?.pageSize ?? 10,
        cursor: page?.cursor ?? null,
      },
      { params: query },
    )
  },

  getFooterLinks() {
    return http.get<unknown, FooterLinksVO>('/api/v1/public/home/footer-links')
  },
}
