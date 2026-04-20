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
public class RefundRequest {
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
