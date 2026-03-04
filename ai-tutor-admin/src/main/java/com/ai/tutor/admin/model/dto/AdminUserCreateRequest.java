package com.ai.tutor.admin.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminUserCreateRequest {

    private Integer userType;

    private String name;

    private String phone;

    private String avatar;

    private Integer sex;

    private Integer status;

    private Integer activeStatus;

    private String teacherRealName;

    private String teacherEducation;

    private String teacherSubject;

    private String teacherCity;

    private BigDecimal teacherRatePerHour;

    private String studentRealName;

    private Integer studentAge;

    private String studentAddress;

    private String studentDemandDescription;

    private BigDecimal studentBudget;
}

