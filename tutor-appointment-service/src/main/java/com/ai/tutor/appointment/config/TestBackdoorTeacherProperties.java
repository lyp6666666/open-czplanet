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
    public static final String DEFAULT_TEACHER_PHONE = "28888888888";
    public static final String DEFAULT_TEACHER_CODE = "1888";
    public static final String DEFAULT_STUDENT_PHONE = "26666666666";
    public static final String DEFAULT_STUDENT_CODE = "1666";

    private boolean enabled = true;
    /**
     * 凭据固定走代码常量，避免被旧的 Nacos 配置覆盖后失效。
     * 这里保留字段只是为了兼容已有配置结构，不再作为运行时匹配依据。
     */
    private String phone = DEFAULT_TEACHER_PHONE;
    private String code = DEFAULT_TEACHER_CODE;
    private Long userId = 666888L;
    private String studentPhone = DEFAULT_STUDENT_PHONE;
    private String studentCode = DEFAULT_STUDENT_CODE;
    private Long studentUserId = 666777L;
    private Long redirectRoomId = 666001L;
    private Long redirectOtherUid = 666777L;

    public String teacherPhone() {
        return DEFAULT_TEACHER_PHONE;
    }

    public String teacherCode() {
        return DEFAULT_TEACHER_CODE;
    }

    public String studentPhoneValue() {
        return DEFAULT_STUDENT_PHONE;
    }

    public String studentCodeValue() {
        return DEFAULT_STUDENT_CODE;
    }
}
