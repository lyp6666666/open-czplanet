package com.ai.tutor.appointment.model.vo.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailTaskRowVO {
    private Long id;
    private String taskKey;
    private String templateCode;
    private String bizType;
    private Long bizId;
    private Long receiverUid;
    private String receiverName;
    private String receiverRole;
    private String emailType;
    private String email;
    private String subject;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String lastError;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
