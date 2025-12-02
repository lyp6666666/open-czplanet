package com.ai.tutor.appointment.model.dto.user;

import lombok.Data;

/**
 * 基础用户信息（User表通用字段）
 */

@Data
public class BaseUserInfo {
    private String name; // 用户昵称

    private String avatar; // 用户头像

    private Integer sex; // 性别 1男 2女

    private Integer activeStatus; // 在线状态 1在线 2离线

    private Long itemId; // 佩戴徽章id

    private Integer status; // 使用状态 0正常 1拉黑

    private String ipInfo;// ip信息
}