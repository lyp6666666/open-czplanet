package com.ai.tutor.liveclass.integration.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "integration.ai-agent")
public class AiAgentProperties {
    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:18086";
    private String internalToken;
    private int timeoutMillis = 5000;
}
