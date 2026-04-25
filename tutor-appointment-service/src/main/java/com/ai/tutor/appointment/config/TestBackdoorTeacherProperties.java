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
    public static final String DEFAULT_LOCAL_TEACHER_PHONE = "29999999999";
    public static final String DEFAULT_LOCAL_TEACHER_CODE = "1886";
    public static final String DEFAULT_LOCAL_STUDENT_PHONE = "19999999999";
    public static final String DEFAULT_LOCAL_STUDENT_CODE = "1668";

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
    private String localTeacherPhone = DEFAULT_LOCAL_TEACHER_PHONE;
    private String localTeacherCode = DEFAULT_LOCAL_TEACHER_CODE;
    private Long localTeacherUserId = 667888L;
    private String localStudentPhone = DEFAULT_LOCAL_STUDENT_PHONE;
    private String localStudentCode = DEFAULT_LOCAL_STUDENT_CODE;
    private Long localStudentUserId = 667777L;
    private Long localRedirectRoomId = 667001L;
    private Long localRedirectOtherUid = 667777L;

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

    public String localTeacherPhoneValue() {
        return DEFAULT_LOCAL_TEACHER_PHONE;
    }

    public String localTeacherCodeValue() {
        return DEFAULT_LOCAL_TEACHER_CODE;
    }

    public String localStudentPhoneValue() {
        return DEFAULT_LOCAL_STUDENT_PHONE;
    }

    public String localStudentCodeValue() {
        return DEFAULT_LOCAL_STUDENT_CODE;
    }
}
