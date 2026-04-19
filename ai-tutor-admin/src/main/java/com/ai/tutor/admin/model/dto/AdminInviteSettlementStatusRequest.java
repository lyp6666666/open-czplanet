package com.ai.tutor.admin.model.dto;

import lombok.Data;

/**
 * 管理端邀请返利结算单状态更新请求。
 */
@Data
public class AdminInviteSettlementStatusRequest {

    /**
     * 失败原因或财务备注。
     */
    private String reason;
}
