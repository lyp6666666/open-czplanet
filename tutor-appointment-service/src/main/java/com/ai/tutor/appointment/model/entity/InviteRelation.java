package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请关系实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteRelation {

    private Long id;

    private Long inviterUid;

    private Long inviteeUid;

    private String inviteCode;

    private String bindSource;

    private String status;

    private Integer riskFlag;

    private String remark;

    private LocalDateTime bindTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
