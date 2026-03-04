package com.ai.tutor.admin.model.dto;

import lombok.Data;

@Data
public class RejectJobRequest {
    private Long id;
    private String reason;
}
