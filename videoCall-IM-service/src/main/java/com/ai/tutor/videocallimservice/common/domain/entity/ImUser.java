package com.ai.tutor.videocallimservice.common.domain.entity;

import lombok.Data;

/**
 * IM 服务使用的用户最小信息映射（来自 user 表）。
 * 仅保留 room 创建/校验所需字段，避免与预约服务的 User 实体耦合。
 */
@Data
public class ImUser {

    private Long id;

    /**
     * 用户类型：1教师 2家长
     */
    private Integer userType;

    /**
     * 逻辑外键：指向 teacher_profile.id 或 student_profile.id
     */
    private Long refId;

    /**
     * 使用状态：0正常 1拉黑
     */
    private Integer status;
}

