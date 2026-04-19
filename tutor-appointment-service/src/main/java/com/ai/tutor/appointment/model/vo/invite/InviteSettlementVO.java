package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请结算记录视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteSettlementVO {

    private Long id;

    private String settlementMonth;

    private Long totalAmountFen;

    private String status;

    private String payTime;

    private String failReason;
}
