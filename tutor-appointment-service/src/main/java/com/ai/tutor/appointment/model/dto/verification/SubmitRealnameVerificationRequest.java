package com.ai.tutor.appointment.model.dto.verification;

import lombok.Data;

@Data
public class SubmitRealnameVerificationRequest {
    private String method;
    private String realName;
    private String idNo;
    private String idFrontUrl;
    private String idBackUrl;
}

