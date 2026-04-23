package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class EmailNotificationProperties {

    private boolean enabled = true;

    private Verify verify = new Verify();

    private Notification notification = new Notification();

    private Lesson lesson = new Lesson();

    private Sender sender = new Sender();

    @Data
    public static class Verify {
        private int expireMinutes = 10;
        private int resendCooldownSeconds = 60;
        private int maxTryCount = 5;
    }

    @Data
    public static class Notification {
        private int maxRetryCount = 3;
        private int schedulerBatchSize = 50;
    }

    @Data
    public static class Lesson {
        private List<Integer> reminderMinutes = new ArrayList<>(List.of(30));
        private int summaryBackfillWindowHours = 24;
    }

    @Data
    public static class Sender {
        /**
         * MOCK / TENCENT
         */
        private String provider = "MOCK";
        private String endpoint = "ses.tencentcloudapi.com";
        private String region = "ap-guangzhou";
        private String secretId;
        private String secretKey;
        private String fromEmail;
        private String fromName = "创智星球";
        private String replyToEmail;
        private String templateDir = "email-templates/tencent";
        private Map<String, Long> templateIds = new LinkedHashMap<>();
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;
        private boolean enabled = true;
    }
}
