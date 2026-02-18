package com.ai.tutor.appointment.model.dto.verification;

import lombok.Data;

@Data
public class OpsTeacherVerificationAuditRequest {
    private Long userId;
    private String type;
    private String reason;
}

