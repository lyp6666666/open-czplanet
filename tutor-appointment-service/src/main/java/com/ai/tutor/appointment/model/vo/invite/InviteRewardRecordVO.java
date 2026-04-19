package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请返利明细视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteRewardRecordVO {

    private Long id;

    private Long inviteeUid;

    private String inviteeDisplayName;

    private String rewardScene;

    private String bizOrderType;

    private Long bizOrderId;

    private Long baseAmountFen;

    private Double rewardRate;

    private Long rewardAmountFen;

    private String status;

    private String createdAt;
}
