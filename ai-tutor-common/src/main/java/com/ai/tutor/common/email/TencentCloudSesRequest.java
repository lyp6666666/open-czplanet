package com.ai.tutor.common.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TencentCloudSesRequest {
    private String endpoint;
    private String region;
    private String secretId;
    private String secretKey;
    private String fromEmail;
    private String fromName;
    private String replyToEmail;
    private String toEmail;
    private String subject;
    private Long templateId;
    private String templateDataJson;
    private int connectTimeoutMs;
    private int readTimeoutMs;
}
