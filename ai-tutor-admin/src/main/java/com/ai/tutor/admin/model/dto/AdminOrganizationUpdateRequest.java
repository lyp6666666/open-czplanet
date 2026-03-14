package com.ai.tutor.admin.model.dto;

import lombok.Data;

@Data
public class AdminOrganizationUpdateRequest {

    private String orgName;

    private String username;

    private String initialPassword;

    private Integer accountStatus;

    private String contactName;

    private String contactPhone;

    private String address;

    private String intro;

    private String licenseNo;

    private Integer splitPlatformPercent;

    private Integer splitOrgPercent;
}
