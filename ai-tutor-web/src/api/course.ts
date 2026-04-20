import { http } from './http'
import type { CourseDetailVO, CourseItemVO } from './types'

export const courseApi = {
  myCourses(params: { page?: number; size?: number; role?: 'TEACHER' | 'STUDENT' }) {
    return http.get<unknown, CourseItemVO[]>('/courses/my', { params })
  },

  detail(courseId: number) {
    return http.get<unknown, CourseDetailVO>(`/courses/${courseId}`)
  },

  byRoom(roomId: number) {
    return http.get<unknown, CourseDetailVO>(`/courses/by-room/${roomId}`)
  },

  applyTrialRefund(courseId: number, payload: { reason: string; evidenceImageUrls: string[]; evidenceVideoUrl: string; evidenceVideoDurationSeconds: number }) {
    return http.post<unknown, number>(`/courses/${courseId}/trial-refund/apply`, payload)
  },
}
