import { http } from './http'
import type { PageResult, RefundRequestRecord, RefundRequestDetailResponse } from './types'

export function listRefundRequests(params: { page?: number; size?: number; type?: string; status?: string } = {}): Promise<PageResult<RefundRequestRecord>> {
  const { page = 1, size = 10, type, status } = params
  return http.get('/api/admin/refund/requests', { params: { page, size, type, status } })
}

export function getRefundRequestDetails(requestId: number): Promise<RefundRequestDetailResponse> {
  return http.get(`/api/admin/refund/requests/${requestId}`)
}

export function approveRefundRequest(requestId: number, payload?: { note?: string }): Promise<boolean> {
  return http.post(`/api/admin/refund/requests/${requestId}/approve`, payload || {})
}

export function rejectRefundRequest(requestId: number, payload: { reason: string }): Promise<boolean> {
  return http.post(`/api/admin/refund/requests/${requestId}/reject`, payload)
}
