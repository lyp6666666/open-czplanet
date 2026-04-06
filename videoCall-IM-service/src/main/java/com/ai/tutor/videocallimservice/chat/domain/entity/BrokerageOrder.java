package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerageOrder {
    private Long id;
    private Long proposalId;
    private Long applicationId;
    private Long roomId;
    private Long payerUid;
    private Long amountFen;
    private String payMethod;
    private String status;
    private String proofUrl;
    private String proofNote;
    private LocalDateTime paidAt;
    private Integer refundLocked;
    private Long refundedAmountFen;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
