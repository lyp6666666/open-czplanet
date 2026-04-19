package com.ai.tutor.admin.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端邀请返利明细视图。
 */
@Data
public class AdminInviteRewardVO {

    private Long id;
    private Long inviterUid;
    private String inviterName;
    private String inviterPhone;
    private Long inviteeUid;
    private String inviteeName;
    private String inviteePhone;
    private String rewardScene;
    private String bizOrderType;
    private Long bizOrderId;
    private Long baseAmountFen;
    private Double rewardRate;
    private Long rewardAmountFen;
    private String status;
    private String settlementMonth;
    private LocalDateTime createTime;
}
