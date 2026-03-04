package com.ai.tutor.admin.model.dto;

import lombok.Data;

@Data
public class VerificationAuditRequest {
    private Long userId;
    private String type; // REALNAME or EDU
    private String reason;
}
