import { http } from './http'
import type { CursorPageResponse } from './types'

export const favoritesTutorsApi = {
  favoriteTutor(tutorId: number) {
    return http.post<unknown, string>(`/api/v1/parent/favorites/tutors/${tutorId}`)
  },

  unfavoriteTutor(tutorId: number) {
    return http.delete<unknown, string>(`/api/v1/parent/favorites/tutors/${tutorId}`)
  },

  checkTutorFavorites(ids: number[]) {
    return http.get<unknown, number[]>('/api/v1/parent/favorites/tutors/check', { params: { ids } })
  },

  pageTutorFavorites(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResponse<number>>('/api/v1/parent/favorites/tutors/page', { params })
  },
}

