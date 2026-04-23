package com.ai.tutor.common.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TencentCloudSesResponse {
    private boolean success;
    private String providerMessageId;
    private String requestId;
    private String errorCode;
    private String errorMessage;
}
