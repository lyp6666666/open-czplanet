package com.ai.tutor.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfile {
    private Long id;
    private Long userId;
    private String realName;
    private String education;
    private String subject;
    private String city;
    private String highestEduSchool;
    private BigDecimal ratePerHour;
    private Integer realnameVerifyStatus;
    private String realnameVerifyMethod;
    private String realnameVerifyIdFrontUrl;
    private String realnameVerifyIdBackUrl;
    private String realnameVerifyIdnoMasked;
    private String realnameVerifyRejectReason;
    private LocalDateTime realnameVerifySubmitTime;
    private LocalDateTime realnameVerifyTime;
    private Integer eduVerifyStatus;
    private String eduVerifyProofUrls;
    private String eduVerifyRejectReason;
    private LocalDateTime eduVerifySubmitTime;
    private LocalDateTime eduVerifyTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
