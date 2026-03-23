package com.ai.tutor.appointment;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

class TutorAppointmentApplicationTests {

    @Test
    void shouldEnableFeignClients() {
        EnableFeignClients annotation = TutorAppointmentApplication.class.getAnnotation(EnableFeignClients.class);
        org.assertj.core.api.Assertions.assertThat(annotation).isNotNull();
        org.assertj.core.api.Assertions.assertThat(annotation.basePackages())
                .containsExactly("com.ai.tutor.appointment.integration.feign");
    }

    @Test
    void shouldConfigureSpringBootScan() {
        SpringBootApplication annotation = TutorAppointmentApplication.class.getAnnotation(SpringBootApplication.class);
        org.assertj.core.api.Assertions.assertThat(annotation).isNotNull();
        org.assertj.core.api.Assertions.assertThat(annotation.scanBasePackages())
                .containsExactlyInAnyOrder("com.ai.tutor.appointment", "com.ai.tutor.common");
    }

    @Test
    void shouldScanAppointmentMappers() {
        MapperScan mapperScan = TutorAppointmentApplication.class.getAnnotation(MapperScan.class);
        org.assertj.core.api.Assertions.assertThat(mapperScan).isNotNull();
        org.assertj.core.api.Assertions.assertThat(mapperScan.value())
                .containsExactly("com.ai.tutor.appointment.**.mapper");
    }
}
