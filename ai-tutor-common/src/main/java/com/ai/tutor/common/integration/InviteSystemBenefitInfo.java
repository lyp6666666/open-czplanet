package com.ai.tutor.common.integration;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统邀请码权益信息。
 */
@Data
public class InviteSystemBenefitInfo implements Serializable {

    /**
     * 系统邀请码配置是否启用。
     */
    private Boolean enabled;

    /**
     * 当前用户是否通过系统邀请码注册。
     */
    private Boolean systemInvited;

    /**
     * 系统邀请码码值。
     */
    private String systemInviteCode;

    /**
     * 教师信息费折扣比例，0.5 表示半价。
     */
    private Double tutorInfoFeeDiscountRate;

    /**
     * 学生信息费返现比例。
     */
    private Double studentRewardRate;
}
