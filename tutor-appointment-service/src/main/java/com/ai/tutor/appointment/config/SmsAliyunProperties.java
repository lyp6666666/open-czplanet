package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms.aliyun")
public class SmsAliyunProperties {

    private String accessKeyId;

    private String accessKeySecret;

    private String endpoint = "dypnsapi.aliyuncs.com";

    private String regionId = "cn-hangzhou";

    private String signName;

    private String templateCode;

    private String schemeName;

    private String countryCode = "86";

    private long codeLength = 4L;

    private long validTimeSeconds = 300L;

    private long intervalSeconds = 60L;

    private long duplicatePolicy = 1L;

    private boolean returnVerifyCode = false;

    private String templateParam = "{\"code\":\"##code##\"}";
}
