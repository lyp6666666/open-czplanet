package com.ai.tutor.admin.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminOrganizationRowVO {

    private Long orgUserId;

    private String orgName;

    private String username;

    private String contactPhone;

    private Integer userStatus;

    private Integer accountStatus;

    private Integer mustChangePassword;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
