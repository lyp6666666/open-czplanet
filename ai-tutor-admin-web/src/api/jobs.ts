import { http } from './http'
import type { PageResult, StudentJobPosting } from './types'

export function listPendingJobs(page = 1, size = 10): Promise<PageResult<StudentJobPosting>> {
  return http.get('/api/admin/jobs/pending', { params: { page, size } })
}

export function approveJob(id: number): Promise<boolean> {
  return http.post(`/api/admin/jobs/approve/${id}`)
}

export function rejectJob(payload: { id: number; reason: string }): Promise<boolean> {
  return http.post('/api/admin/jobs/reject', payload)
}

