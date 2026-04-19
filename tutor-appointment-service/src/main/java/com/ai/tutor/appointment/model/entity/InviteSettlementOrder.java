package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请返利结算单实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteSettlementOrder {

    private Long id;

    private Long userId;

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
