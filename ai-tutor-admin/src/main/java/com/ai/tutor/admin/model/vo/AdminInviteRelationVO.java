package com.ai.tutor.admin.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端邀请关系列表视图。
 */
@Data
public class AdminInviteRelationVO {

    private Long id;
    private Long inviterUid;
    private String inviterName;
    private String inviterPhone;
    private Long inviteeUid;
    private String inviteeName;
    private String inviteePhone;
    private Integer inviteeUserType;
    private String inviteCode;
    private String bindSource;
    private String status;
    private Integer riskFlag;
    private LocalDateTime bindTime;
    private LocalDateTime createTime;
}
