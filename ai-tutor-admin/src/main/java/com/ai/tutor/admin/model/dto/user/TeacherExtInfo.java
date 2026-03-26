package com.ai.tutor.admin.model.dto.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeacherExtInfo {
    private String realName;
    private String education;
    private String subject;
    private Integer experienceYears;
    private BigDecimal ratePerHour;
    private String introduction;
    private String city;
    private String highestEduSchool;
    private String teachingMode;
    private String defaultGreeting;
    private String certificateUrls;
    private Integer status;
}
