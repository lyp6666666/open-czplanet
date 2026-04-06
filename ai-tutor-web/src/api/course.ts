import { http } from './http'

export type CourseItemVO = {
  courseId: number
  applicationId: number
  roomId?: number | null
  teacherUid: number
  studentUid: number
  status: string
  trialEndAt?: string | null
}

export const courseApi = {
  myCourses(params: { page?: number; size?: number; role?: 'TEACHER' | 'STUDENT' }) {
    return http.get<unknown, CourseItemVO[]>('/courses/my', { params })
  },

  applyTrialRefund(courseId: number, payload: { reason: string; evidenceImageUrls: string[] }) {
    return http.post<unknown, number>(`/courses/${courseId}/trial-refund/apply`, payload)
  },
}

