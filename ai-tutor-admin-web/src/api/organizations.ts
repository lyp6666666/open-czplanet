import { http } from './http'
import type {
  AdminOrganizationCreateRequest,
  AdminOrganizationCreateResponse,
  AdminOrganizationDetail,
  AdminOrganizationRow,
  AdminOrganizationUpdateRequest,
  PageResult,
} from './types'

export function createOrganization(request: AdminOrganizationCreateRequest) {
  return http.post<unknown, AdminOrganizationCreateResponse>('/api/admin/organizations', request)
}

export function pageOrganizations(q: string | null, page = 1, size = 10): Promise<PageResult<AdminOrganizationRow>> {
  return http.get('/api/admin/organizations', { params: { q: q || undefined, page, size } })
}

export function getOrganizationDetail(orgUserId: number): Promise<AdminOrganizationDetail> {
  return http.get(`/api/admin/organizations/${orgUserId}`)
}

export function updateOrganization(orgUserId: number, payload: AdminOrganizationUpdateRequest): Promise<boolean> {
  return http.put(`/api/admin/organizations/${orgUserId}`, payload)
}

export function disableOrganization(orgUserId: number): Promise<boolean> {
  return http.delete(`/api/admin/organizations/${orgUserId}`)
}
