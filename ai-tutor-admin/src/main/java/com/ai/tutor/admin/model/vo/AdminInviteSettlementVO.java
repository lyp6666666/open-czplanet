package com.ai.tutor.admin.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端邀请返利结算单视图。
 */
@Data
public class AdminInviteSettlementVO {

    private Long id;
    private Long userId;
    private String userName;
    private String userPhone;
    private String settlementMonth;
    private Long totalAmountFen;
    private Long paidAmountFen;
    private String status;
    private String receiverSnapshotJson;
    private String failReason;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
