package com.ai.tutor.appointment.model.vo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统邀请码配置视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteSystemConfigVO {

    private Boolean enabled;

    private String systemInviteCode;

    private String systemInviteLink;

    private Double tutorInfoFeeDiscountRate;

    private Double studentRewardRate;

    private String promoTitle;

    private String promoDesc;
}
