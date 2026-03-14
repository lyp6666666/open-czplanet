package com.ai.tutor.appointment.model.entity;

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

    /** 机构资料id */
    private Long id;

    /** 机构主账号 user_id */
    private Long userId;

    /** 机构名称 */
    private String orgName;

    /** 机构介绍 */
    private String intro;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 机构地址 */
    private String address;

    /** 营业执照号/统一社会信用代码 */
    private String licenseNo;

    /** 平台分成比例（百分比） */
    private Integer splitPlatformPercent;

    /** 机构分成比例（百分比） */
    private Integer splitOrgPercent;

    /** 状态 1正常 0禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
