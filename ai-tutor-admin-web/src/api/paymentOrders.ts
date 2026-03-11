import { http } from './http'
import type { PageResult } from './types'

export type PaymentChannel = 'WECHAT' | 'ALIPAY'
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED'

export interface PaymentOrderRecord {
  id: number
  orderNo: string
  userId: number
  amount: number
  currency: string
  channel: PaymentChannel
  provider: string
  status: PaymentStatus
  transactionId: string | null
  providerOrderNo: string | null
  contextId: number
  contextType: string
  subject: string
  body: string | null
  clientIp: string | null
  payData: string | null
  notifyCount: number | null
  lastNotifyTime: string | null
  notifyVerified: number | null
  successTime: string | null
  expireTime: string | null
  createTime: string
  updateTime: string
}

export function listPaymentOrders(params: {
  page: number
  size: number
  orderNo?: string
  userId?: number
  contextType?: string
  contextId?: number
  channel?: PaymentChannel
  status?: PaymentStatus
  startTime?: string
  endTime?: string
}) {
  return http.get<unknown, PageResult<PaymentOrderRecord>>('/api/admin/payment/orders', { params })
}

export function getPaymentOrderDetail(orderNo: string) {
  return http.get<unknown, PaymentOrderRecord>(`/api/admin/payment/orders/${encodeURIComponent(orderNo)}`)
}

