import { http } from './http'

export type CustomerServiceChannelType = 'WECHAT_PERSONAL' | 'WECHAT_WORK'

export type CustomerServiceConfigResp = {
  enabled: boolean
  channelType: CustomerServiceChannelType
  displayName: string
  wechatNo?: string | null
  qqNo?: string | null
  qrCodeUrl?: string | null
  serviceTime: string
  description?: string | null
}

export function getCustomerServiceConfig() {
  return http.get<unknown, CustomerServiceConfigResp>('/api/v1/public/customer-service/config')
}
