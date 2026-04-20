package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentJobPostingLite {
    private Long id;
    private String classMode;
    private Integer frequencyPerWeek;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
}
