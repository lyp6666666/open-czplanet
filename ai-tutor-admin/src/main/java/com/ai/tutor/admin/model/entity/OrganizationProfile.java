package com.ai.tutor.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationProfile {
    private Long id;
    private Long userId;
    private String orgName;
    private String intro;
    private String contactName;
    private String contactPhone;
    private String address;
    private String licenseNo;
    private Integer splitPlatformPercent;
    private Integer splitOrgPercent;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
