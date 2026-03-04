import { http } from './http'
import type { PageResult, TeacherProfile } from './types'

export type VerificationType = 'REALNAME' | 'EDU'

export function listPendingVerifications(page = 1, size = 10): Promise<PageResult<TeacherProfile>> {
  return http.get('/api/admin/verification/pending', { params: { page, size } })
}

export function getVerificationDetails(userId: number): Promise<TeacherProfile> {
  return http.get(`/api/admin/verification/details/${userId}`)
}

export function approveVerification(payload: { userId: number; type: VerificationType }): Promise<boolean> {
  return http.post('/api/admin/verification/approve', payload)
}

export function rejectVerification(payload: {
  userId: number
  type: VerificationType
  reason: string
}): Promise<boolean> {
  return http.post('/api/admin/verification/reject', payload)
}

