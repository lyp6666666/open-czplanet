package com.ai.tutor.appointment.integration.email.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailSendResponse {
    private boolean success;
    private String provider;
    private String providerMessageId;
    private String requestId;
    private String errorCode;
    private String errorMessage;
}
