import { http } from './http'
import type { CursorPageRequest, CursorPageResponse, ParentTutorCardVO } from './types'

export const parentTutorsApi = {
  page(params: CursorPageRequest & { q?: string; city?: string; mode?: string; subject?: string; rateMin?: number | null; rateMax?: number | null }) {
    return http.get<unknown, CursorPageResponse<ParentTutorCardVO>>('/api/v1/parent/tutors/page', { params })
  },
}
