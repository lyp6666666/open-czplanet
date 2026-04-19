package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请返利记录实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteRewardRecord {

    private Long id;

    private Long inviterUid;

    private Long inviteeUid;

    private String rewardScene;

    private String bizOrderType;

    private Long bizOrderId;

    private Long paymentOrderId;

    private Long baseAmountFen;

    private Double rewardRate;

    private Long rewardAmountFen;

    private String status;

    private String freezeReason;

    private String settlementMonth;

    private String configSnapshotJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
