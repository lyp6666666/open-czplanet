import { http } from './http'

export type PayChannel = 'WECHAT' | 'ALIPAY'

export interface PrepayReq {
  contextType: string
  contextId: number
  channel: PayChannel
}

export interface PrepayResp {
  orderNo: string
  amountFen: number
  channel: PayChannel
  qrCodeUrl?: string
  codeUrl?: string
  expireTime?: string
}

export interface PaymentOrderStatusResp {
  orderNo: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED'
  amountFen: number
  channel: PayChannel
  successTime?: string
  expireTime?: string
}

export function prepay(req: PrepayReq) {
  return http.post<unknown, PrepayResp>('/payment/prepay', req)
}

export function getPaymentOrderStatus(orderNo: string) {
  return http.get<unknown, PaymentOrderStatusResp>(`/payment/orders/${encodeURIComponent(orderNo)}`)
}
