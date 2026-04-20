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
public class TutorApplication {
    private Long id;
    private Long senderUid;
    private Long receiverUid;
    private String senderRole;
    private String receiverRole;
    private String contextType;
    private Long contextId;
    private String teachingMode;
    private String content;
    private String clientRequestId;
    private String status;
    private String chatAccessStatus;
    private Long roomId;
    private LocalDateTime decidedAt;
    private Integer receiverRead;
    private LocalDateTime receiverReadTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
