import { http } from './http'
import type { AdminInviteRelation, AdminInviteReward, AdminInviteSettlement, AdminInviteSystemConfig, PageResult } from './types'

/**
 * 管理端邀请列表分页参数。
 * 运营与财务列表统一采用页码分页，便于后台人工检索与导出。
 */
export interface AdminInvitePageParams {
  page: number
  size: number
}

/**
 * 管理端邀请关系列表查询参数。
 */
export interface AdminInviteRelationParams extends AdminInvitePageParams {
  inviterUid?: number
  inviteeUid?: number
  status?: string
}

/**
 * 管理端返利明细列表查询参数。
 */
export interface AdminInviteRewardParams extends AdminInviteRelationParams {
  scene?: string
  settlementMonth?: string
}

/**
 * 管理端结算单列表查询参数。
 */
export interface AdminInviteSettlementParams extends AdminInvitePageParams {
  userId?: number
  status?: string
  settlementMonth?: string
}

/**
 * 查询邀请关系。
 * 用于运营核对注册绑定关系与风险状态。
 */
export function listInviteRelations(params: AdminInviteRelationParams) {
  return http.get<unknown, PageResult<AdminInviteRelation>>('/api/admin/invite/relations', { params })
}

/**
 * 查询返利明细。
 * 用于财务核对返利来源、比例与结算月份。
 */
export function listInviteRewards(params: AdminInviteRewardParams) {
  return http.get<unknown, PageResult<AdminInviteReward>>('/api/admin/invite/rewards', { params })
}

/**
 * 查询结算单。
 * 用于财务查看待打款、已打款与失败单据。
 */
export function listInviteSettlements(params: AdminInviteSettlementParams) {
  return http.get<unknown, PageResult<AdminInviteSettlement>>('/api/admin/invite/settlements', { params })
}

/**
 * 标记结算单已打款。
 * 财务确认微信打款完成后调用，后台会同步推进用户端返利明细状态。
 */
export function markInviteSettlementPaid(id: number) {
  return http.post<unknown, boolean>(`/api/admin/invite/settlements/${id}/paid`)
}

/**
 * 标记结算单打款失败。
 * 失败原因会保留在结算单中，便于后续人工追踪与重试。
 */
export function markInviteSettlementFailed(id: number, reason: string) {
  return http.post<unknown, boolean>(`/api/admin/invite/settlements/${id}/failed`, { reason })
}

/**
 * 查询系统邀请码配置。
 * 运营可通过该配置控制推广期系统邀请码是否生效。
 */
export function getInviteSystemConfig() {
  return http.get<unknown, AdminInviteSystemConfig>('/api/admin/invite/system-config')
}

/**
 * 保存系统邀请码配置。
 * 开关关闭后新注册用户不再绑定系统推广权益，历史已绑定用户的权益由后端按规则判断是否继续生效。
 */
export function saveInviteSystemConfig(payload: AdminInviteSystemConfig) {
  return http.post<unknown, AdminInviteSystemConfig>('/api/admin/invite/system-config', payload)
}
