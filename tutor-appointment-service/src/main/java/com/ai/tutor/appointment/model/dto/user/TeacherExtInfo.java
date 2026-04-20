package com.ai.tutor.appointment.model.dto.user;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师扩展信息
 */
@Data
public class TeacherExtInfo {

    private String realName; // 真实姓名

    private String education; // 学历

    private String subject; // 教授科目

    private Integer experienceYears; // 教学经验年数

    private BigDecimal ratePerHour; // 每小时收费

    private String introduction; // 简介

    private String city;

    private String highestEduSchool;

    private String defaultGreeting;

    private String certificateUrls; // 证书链接（JSON）

    private Integer status; // 状态 1正常 0禁用
}
