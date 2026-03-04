import { http } from './http'
import type { DashboardStatsResponse } from './types'

export function getDashboardStats(): Promise<DashboardStatsResponse> {
  return http.get('/api/admin/dashboard/stats')
}

