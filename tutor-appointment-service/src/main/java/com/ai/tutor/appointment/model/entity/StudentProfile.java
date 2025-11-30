package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 家长资料表实体类
 * 对应表：parent_profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {

    /** 学生资料id */
    private Long id;

    /** 用户id（逻辑外键） */
    private Long userId;

    /** 学生姓名 */
    private String realName;

    private Integer age;

    /** 孩子年龄 */
    private Integer childAge;

    /** 上课地址 */
    private String address;

    /** 家教需求描述 */
    private String demandDescription;

    /** 预算（每小时或每次） */
    private BigDecimal budget;

    /** 状态 1正常 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}

