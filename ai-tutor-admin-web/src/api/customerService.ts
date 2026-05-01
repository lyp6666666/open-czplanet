import { http } from './http'
import type { AdminCustomerServiceConfig } from './types'

export function getCustomerServiceConfig() {
  return http.get<unknown, AdminCustomerServiceConfig>('/api/admin/customer-service/config')
}

export function saveCustomerServiceConfig(payload: AdminCustomerServiceConfig) {
  return http.post<unknown, AdminCustomerServiceConfig>('/api/admin/customer-service/config', payload)
}

export function uploadCustomerServiceQrCode(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<unknown, AdminCustomerServiceConfig>('/api/admin/customer-service/qrcode', form)
}
