package com.ai.tutor.admin.model.dto;

import lombok.Data;

/**
 * 管理端系统邀请码配置请求。
 */
@Data
public class AdminInviteSystemConfigRequest {

    private Boolean enabled;

    private String systemInviteCode;

    private String systemInviteLink;

    private Double tutorInfoFeeDiscountRate;

    private Double studentRewardRate;

    private String promoTitle;

    private String promoDesc;
}
