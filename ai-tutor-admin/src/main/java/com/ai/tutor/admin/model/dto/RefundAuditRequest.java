package com.ai.tutor.admin.model.dto;

import lombok.Data;

@Data
public class RefundAuditRequest {
    private Long orderId;
    private String reason;
}
