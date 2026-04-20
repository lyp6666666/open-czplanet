package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPaymentOrder {
    private Long id;
    private Long lessonId;
    private Long courseId;
    private Long studentUid;
    private Long teacherUid;
    private String lessonType;
    private Long totalAmountFen;
    private Integer platformFeeRate;
    private Long platformFeeAmountFen;
    private Long teacherIncomeAmountFen;
    private String status;
    private String paymentOrderNo;
    private LocalDateTime paidAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
