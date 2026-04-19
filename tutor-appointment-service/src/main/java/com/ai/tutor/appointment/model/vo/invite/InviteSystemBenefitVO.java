package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统邀请码权益视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteSystemBenefitVO {

    private Boolean enabled;

    private Boolean systemInvited;

    private String systemInviteCode;

    private Double tutorInfoFeeDiscountRate;

    private Double studentRewardRate;
}
