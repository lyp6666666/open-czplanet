package com.ai.tutor.admin.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserRowVO {

    private Long id;

    private String name;

    private String phone;

    private String avatar;

    private Integer sex;

    private Integer status;

    private Integer activeStatus;

    private Integer userType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String teacherRealName;

    private String teacherEducation;

    private String teacherSubject;

    private String teacherCity;

    private BigDecimal teacherRatePerHour;

    private Integer teacherRealnameVerifyStatus;

    private Integer teacherEduVerifyStatus;

    private Integer teacherProfileStatus;

    private Integer teacherHomeStarTeacher;

    private String studentRealName;

    private Integer studentAge;

    private String studentAddress;

    private String studentDemandDescription;

    private BigDecimal studentBudget;

    private Integer studentProfileStatus;
}
