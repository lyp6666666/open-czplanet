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
public class EmailNotificationTask {
    private Long id;
    private String taskKey;
    private String templateCode;
    private String bizType;
    private Long bizId;
    private Long receiverUid;
    private String receiverRole;
    private String emailType;
    private String email;
    private String subject;
    private String payloadJson;
    private LocalDateTime scheduledAt;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String lastError;
    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
