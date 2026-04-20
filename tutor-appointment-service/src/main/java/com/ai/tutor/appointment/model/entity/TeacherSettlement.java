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
public class TeacherSettlement {
    private Long id;
    private Long lessonPaymentOrderId;
    private Long teacherUid;
    private Long settlementAmountFen;
    private Long platformFeeAmountFen;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
