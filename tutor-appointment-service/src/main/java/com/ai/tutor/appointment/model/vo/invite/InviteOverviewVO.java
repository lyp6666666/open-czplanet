package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请总览视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteOverviewVO {

    private String myInviteCode;

    private Integer totalInviteCount;

    private Integer effectiveInviteCount;

    private Long totalRewardAmountFen;

    private Long pendingSettlementAmountFen;

    private Long estimatedCurrentMonthAmountFen;

    private Double teacherRewardRate;

    private Double studentRewardRate;

    private Integer settlementDay;

    private Boolean receiverConfigured;

    private InviteSystemConfigVO systemInviteConfig;
}
