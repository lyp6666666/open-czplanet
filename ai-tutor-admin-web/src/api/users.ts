import { http } from './http'
import type { AdminUserDetail, AdminUserRow, PageResult } from './types'

export function pageTeachers(q: string | null, page = 1, size = 10): Promise<PageResult<AdminUserRow>> {
  return http.get('/api/admin/users/teachers', { params: { q: q || undefined, page, size } })
}

export function pageStudents(q: string | null, page = 1, size = 10): Promise<PageResult<AdminUserRow>> {
  return http.get('/api/admin/users/students', { params: { q: q || undefined, page, size } })
}

export function getUserDetail(id: number): Promise<AdminUserDetail> {
  return http.get(`/api/admin/users/${id}`)
}

export interface AdminUserCreatePayload {
  userType: 1 | 2
  name?: string | null
  phone: string
  avatar?: string | null
  sex?: number | null
  status?: number | null
  activeStatus?: number | null

  teacherRealName?: string | null
  teacherEducation?: string | null
  teacherSubject?: string | null
  teacherCity?: string | null
  teacherRatePerHour?: number | string | null

  studentRealName?: string | null
  studentAge?: number | null
  studentAddress?: string | null
  studentDemandDescription?: string | null
  studentBudget?: number | string | null
}

export function createUser(payload: AdminUserCreatePayload): Promise<number> {
  return http.post('/api/admin/users', payload)
}

export interface AdminUserUpdatePayload {
  name?: string | null
  phone?: string | null
  avatar?: string | null
  sex?: number | null
  status?: number | null
  activeStatus?: number | null

  teacherRealName?: string | null
  teacherEducation?: string | null
  teacherSubject?: string | null
  teacherCity?: string | null
  teacherRatePerHour?: number | string | null
  teacherProfileStatus?: number | null

  studentRealName?: string | null
  studentAge?: number | null
  studentAddress?: string | null
  studentDemandDescription?: string | null
  studentBudget?: number | string | null
  studentProfileStatus?: number | null
}

export function updateUser(id: number, payload: AdminUserUpdatePayload): Promise<boolean> {
  return http.put(`/api/admin/users/${id}`, payload)
}

export function disableUser(id: number): Promise<boolean> {
  return http.delete(`/api/admin/users/${id}`)
}

