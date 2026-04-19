import { http } from './http'
import type {
  CursorPageResponse,
  InviteOverviewVO,
  InviteReceiverAccountVO,
  InviteRecordVO,
  InviteRewardRecordVO,
  InviteRulesVO,
  InviteSettlementVO,
} from './types'

/**
 * 邀请收款信息保存请求。
 * V1 先以“财务人工/半自动打款”作为落地方案，因此需要用户主动维护收款信息。
 */
export interface SaveInviteReceiverAccountRequest {
  receiverName: string
  wechatNo: string
  phone: string
  remark?: string | null
}

/**
 * 邀请分页查询参数。
 * 统一复用游标分页风格，减少前后端分页口径不一致。
 */
export interface InvitePageParams {
  pageSize?: number
  cursor?: number | null
}

/**
 * 邀请记录分页查询参数。
 */
export interface InviteRecordPageParams extends InvitePageParams {
  status?: string
}

/**
 * 返利明细分页查询参数。
 */
export interface InviteRewardPageParams extends InvitePageParams {
  status?: string
  scene?: string
}

export const inviteApi = {
  /**
   * 获取邀请总览。
   * 用于页面首屏概览、邀请码展示与入口状态判断。
   */
  overview() {
    return http.get<unknown, InviteOverviewVO>('/invite/overview')
  },

  /**
   * 获取邀请记录分页。
   * 用于展示被邀请人的注册与转化情况。
   */
  records(params: InviteRecordPageParams) {
    return http.get<unknown, CursorPageResponse<InviteRecordVO>>('/invite/records', { params })
  },

  /**
   * 获取返利明细分页。
   * 用于展示每一笔返利的金额来源与状态。
   */
  rewards(params: InviteRewardPageParams) {
    return http.get<unknown, CursorPageResponse<InviteRewardRecordVO>>('/invite/rewards', { params })
  },

  /**
   * 获取结算记录分页。
   * 用于展示月度汇总打款结果。
   */
  settlements(params: InvitePageParams) {
    return http.get<unknown, CursorPageResponse<InviteSettlementVO>>('/invite/settlements', { params })
  },

  /**
   * 获取微信收款信息。
   * 页面初始化时调用，决定是否展示“已配置”状态。
   */
  receiverAccount() {
    return http.get<unknown, InviteReceiverAccountVO>('/invite/receiver-account')
  },

  /**
   * 保存微信收款信息。
   * 后端应进行字段格式校验，并在成功后返回最新配置结果。
   */
  saveReceiverAccount(payload: SaveInviteReceiverAccountRequest) {
    return http.post<unknown, InviteReceiverAccountVO>('/invite/receiver-account', payload)
  },

  /**
   * 获取邀请规则。
   * 规则文案与配置通过后端统一下发，避免前端写死比例与结算日。
   */
  rules() {
    return http.get<unknown, InviteRulesVO>('/invite/rules')
  },
}
