package com.ai.tutor.appointment.model.dto.user;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学生扩展信息
 */

@Data
public class StudentExtInfo {
    private String realName; // 学生姓名

    private Integer age; // 年龄

    private Integer childAge; // 孩子年龄

    private String address; // 上课地址

    private String demandDescription; // 需求描述

    private BigDecimal budget; // 预算

    private Integer status; // 状态 1正常 0禁用
}