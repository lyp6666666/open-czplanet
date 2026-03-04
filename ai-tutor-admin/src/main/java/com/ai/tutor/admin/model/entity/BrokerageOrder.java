package com.ai.tutor.admin.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
