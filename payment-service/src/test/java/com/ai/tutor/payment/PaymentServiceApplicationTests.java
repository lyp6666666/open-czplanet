package com.ai.tutor.payment;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

class PaymentServiceApplicationTests {

    @Test
    void shouldEnableFeignClients() {
        EnableFeignClients annotation = PaymentServiceApplication.class.getAnnotation(EnableFeignClients.class);
        org.assertj.core.api.Assertions.assertThat(annotation).isNotNull();
        org.assertj.core.api.Assertions.assertThat(annotation.basePackages())
                .containsExactly("com.ai.tutor.payment.integration.feign");
    }

    @Test
    void shouldConfigureSpringBootScan() {
        SpringBootApplication annotation = PaymentServiceApplication.class.getAnnotation(SpringBootApplication.class);
        org.assertj.core.api.Assertions.assertThat(annotation).isNotNull();
        org.assertj.core.api.Assertions.assertThat(annotation.scanBasePackages())
                .containsExactlyInAnyOrder("com.ai.tutor.payment", "com.ai.tutor.common");
    }

    @Test
    void shouldScanPaymentMappers() {
        MapperScan mapperScan = PaymentServiceApplication.class.getAnnotation(MapperScan.class);
        org.assertj.core.api.Assertions.assertThat(mapperScan).isNotNull();
        org.assertj.core.api.Assertions.assertThat(mapperScan.value())
                .containsExactly("com.ai.tutor.payment.**.mapper");
    }
}
