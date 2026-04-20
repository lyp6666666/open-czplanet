package com.ai.tutor.admin.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefundRequestRecord {
    private Long id;
    private Long brokerageOrderId;
    private Long courseId;
    private Long roomId;
    private Long applicantUid;
    private String applicantRole;
    private String type;
    private String status;
    private String reason;
    private String evidenceImagesJson;
    private String evidenceVideoUrl;
    private Integer evidenceVideoDurationSeconds;
    private String evidenceVideoDeleteStatus;
    private LocalDateTime evidenceVideoDeletedAt;
    private Integer refundPercent;
    private Long refundAmountFen;
    private Long adminUid;
    private String adminNote;
    private LocalDateTime decidedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
