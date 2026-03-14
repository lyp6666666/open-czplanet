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
public class OrganizationAccount {

    /** 机构账号id */
    private Long id;

    /** 机构主账号 user_id */
    private Long orgUserId;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    /** 是否首次登录强制改密 1是 0否 */
    private Integer mustChangePassword;

    /** 状态 1正常 0禁用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
