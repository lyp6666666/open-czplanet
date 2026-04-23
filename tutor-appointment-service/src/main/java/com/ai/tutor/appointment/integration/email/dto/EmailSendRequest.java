package com.ai.tutor.appointment.integration.email.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EmailSendRequest {
    private String templateCode;
    private String toEmail;
    private String subject;
    private String htmlBody;
    private String requestId;
    private String fromEmail;
    private String fromName;
    private String replyToEmail;
    private Map<String, Object> templateData;
}
