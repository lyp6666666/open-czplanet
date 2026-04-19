package com.ai.tutor.liveclass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "livekit")
public class LiveKitProperties {
    private String apiKey;
    private String apiSecret;
    private String wsUrl;
    private String roomPrefix = "class";
    private long tokenTtlSeconds = 7200L;
}
