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
    private BigDecimal ratePerHour;
    private Integer realnameVerifyStatus;
    private LocalDateTime realnameVerifyTime;
    private String realnameRejectReason;
    private Integer eduVerifyStatus;
    private LocalDateTime eduVerifyTime;
    private String eduRejectReason;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
