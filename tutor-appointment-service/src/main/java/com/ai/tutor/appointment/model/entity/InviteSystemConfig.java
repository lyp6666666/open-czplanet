package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统邀请码配置实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteSystemConfig {

    private Long id;

    private Integer enabled;

    private String systemInviteCode;

    private String systemInviteLink;

    private Double tutorInfoFeeDiscountRate;

    private Double studentRewardRate;

    private String promoTitle;

    private String promoDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
