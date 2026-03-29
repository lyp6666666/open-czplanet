package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "test.backdoor.teacher")
public class TestBackdoorTeacherProperties {
    private boolean enabled = true;
    private String phone = "666888";
    private String code = "1111";
    private Long userId = 666888L;
    private Long redirectRoomId = 666001L;
    private Long redirectOtherUid = 666777L;
}

