package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class EmailNotificationProperties {

    private boolean enabled = true;

    private Verify verify = new Verify();

    private Notification notification = new Notification();

    private Lesson lesson = new Lesson();

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
}
