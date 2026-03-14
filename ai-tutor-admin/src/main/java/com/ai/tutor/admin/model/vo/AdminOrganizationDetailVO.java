package com.ai.tutor.admin.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminOrganizationDetailVO {

    private Long orgUserId;

    private String orgName;

    private String username;

    private Integer accountStatus;

    private Integer mustChangePassword;

    private LocalDateTime lastLoginTime;

    private Integer userStatus;

    private String contactName;

    private String contactPhone;

    private String address;

    private String intro;

    private String licenseNo;

    private Integer splitPlatformPercent;

    private Integer splitOrgPercent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
