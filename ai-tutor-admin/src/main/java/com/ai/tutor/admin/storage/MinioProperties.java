package com.ai.tutor.admin.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.minio")
public class MinioProperties {
    private boolean enabled = false;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket = "ai-tutor-assets";
    private String publicBaseUrl;
    private boolean autoCreateBucket = true;
}
