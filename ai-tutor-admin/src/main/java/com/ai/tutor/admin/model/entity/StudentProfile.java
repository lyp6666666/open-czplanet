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
public class StudentProfile {
    private Long id;
    private Long userId;
    private String realName;
    private Integer age;
    private String address;
    private String demandDescription;
    private BigDecimal budget;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
