import { http } from './http'

export type BrokerageOrderStatus = 'PENDING' | 'PROOF_SUBMITTED' | 'PAID' | 'REJECTED' | 'CANCELED'
export type BrokeragePayMethod = 'WECHAT' | 'ALIPAY'

export interface BrokerageOrderVO {
  id: number
  proposalId: number | null
  roomId: number | null
  payerUid: number
  amountFen: number
  payMethod: string | null
  status: BrokerageOrderStatus
  proofUrl: string | null
  proofNote: string | null
  paidAt: string | null
}

export const brokerageApi = {
  getOrCreateByProposal(proposalId: number) {
    return http.post<unknown, BrokerageOrderVO>(`/chat/brokerage/order/by-proposal/${proposalId}`)
  },
  getOrder(orderId: number) {
    return http.get<unknown, BrokerageOrderVO>(`/chat/brokerage/order/${orderId}`)
  },
  submitProof(orderId: number, payload: { payMethod?: BrokeragePayMethod | null; proofUrl?: string | null; proofNote?: string | null }) {
    return http.post<unknown, BrokerageOrderVO>(`/chat/brokerage/order/${orderId}/submit-proof`, payload)
  },
  cancel(orderId: number) {
    return http.post<unknown, BrokerageOrderVO>(`/chat/brokerage/order/${orderId}/cancel`)
  },
  adminMarkPaid(orderId: number, adminToken: string) {
    return http.post<unknown, BrokerageOrderVO>(
      `/chat/brokerage/admin/order/${orderId}/mark-paid`,
      {},
      { headers: { 'X-Admin-Token': adminToken } },
    )
  },
}
