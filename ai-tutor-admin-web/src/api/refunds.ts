import { http } from './http'
import type { DisputeDetailResponse, PageResult, BrokerageOrder } from './types'

export function listRefundDisputes(page = 1, size = 10): Promise<PageResult<BrokerageOrder>> {
  return http.get('/api/admin/refund/disputes', { params: { page, size } })
}

export function getDisputeDetails(orderId: number): Promise<DisputeDetailResponse> {
  return http.get(`/api/admin/refund/details/${orderId}`)
}

export function approveRefund(orderId: number): Promise<boolean> {
  return http.post('/api/admin/refund/approve', { orderId })
}

export function rejectRefund(payload: { orderId: number; reason: string }): Promise<boolean> {
  return http.post('/api/admin/refund/reject', payload)
}

