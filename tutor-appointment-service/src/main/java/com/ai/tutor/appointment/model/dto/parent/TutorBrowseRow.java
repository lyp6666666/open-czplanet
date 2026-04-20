package com.ai.tutor.appointment.model.dto.parent;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TutorBrowseRow {
    private Long id;
    private Long userId;
    private String userName;
    private String avatar;
    private String realName;
    private String city;
    private String education;
    private Integer experienceYears;
    private BigDecimal ratePerHour;
    private String subject;
    private String highestEduSchool;
    private Integer realnameVerifyStatus;
    private Integer eduVerifyStatus;
    private String introduction;
}
