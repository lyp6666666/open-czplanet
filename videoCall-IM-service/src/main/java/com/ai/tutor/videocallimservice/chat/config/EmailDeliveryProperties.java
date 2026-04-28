package com.ai.tutor.videocallimservice.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "email")
public class EmailDeliveryProperties {

    private Sender sender = new Sender();

    @Data
    public static class Sender {
        private String provider = "MOCK";
        private boolean enabled = true;
        private String endpoint = "ses.tencentcloudapi.com";
        private String region = "ap-guangzhou";
        private String secretId;
        private String secretKey;
        private String fromEmail;
        private String fromName = "创智星球";
        private String replyToEmail;
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;
        private Map<String, Long> templateIds = new LinkedHashMap<>();
    }
}
