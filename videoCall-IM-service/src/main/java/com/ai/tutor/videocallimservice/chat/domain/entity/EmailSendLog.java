package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendLog {
    private Long id;
    private Long taskId;
    private String provider;
    private String providerMessageId;
    private String email;
    private String sendStatus;
    private String errorCode;
    private String errorMessage;
    private String requestId;
}
