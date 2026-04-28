package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "sms.spug")
public class SmsSpugProperties {

    private String baseUrl = "https://push.spug.cc";

    private String token;

    private String senderName = "推送助手";
}
