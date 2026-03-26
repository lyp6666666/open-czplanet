package com.ai.tutor.admin.model.dto.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentExtInfo {
    private String realName;
    private Integer age;
    private Integer childAge;
    private String address;
    private String demandDescription;
    private BigDecimal budget;
    private Integer status;
}
