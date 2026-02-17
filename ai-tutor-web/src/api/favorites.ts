import { http } from './http'
import type { CursorPageResponse } from './types'

export const favoritesApi = {
  favoriteDemand(demandId: number) {
    return http.post<unknown, string>(`/api/v1/tutor/favorites/demands/${demandId}`)
  },

  unfavoriteDemand(demandId: number) {
    return http.delete<unknown, string>(`/api/v1/tutor/favorites/demands/${demandId}`)
  },

  checkDemandFavorites(ids: number[]) {
    return http.get<unknown, number[]>('/api/v1/tutor/favorites/demands/check', { params: { ids } })
  },

  pageDemandFavorites(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResponse<number>>('/api/v1/tutor/favorites/demands/page', { params })
  },
}

