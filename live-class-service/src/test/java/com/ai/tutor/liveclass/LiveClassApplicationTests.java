package com.ai.tutor.liveclass;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;

class LiveClassApplicationTests {

    @Test
    void shouldConfigureFeignAndComponentScan() {
        SpringBootApplication appAnnotation = LiveClassApplication.class.getAnnotation(SpringBootApplication.class);
        assertThat(appAnnotation).isNotNull();
        assertThat(appAnnotation.scanBasePackages())
                .containsExactlyInAnyOrder("com.ai.tutor.liveclass", "com.ai.tutor.common");

        EnableFeignClients feignAnnotation = LiveClassApplication.class.getAnnotation(EnableFeignClients.class);
        assertThat(feignAnnotation).isNotNull();
        assertThat(feignAnnotation.basePackages()).containsExactly("com.ai.tutor.liveclass.integration.feign");
    }

    @Test
    void shouldScanLiveClassMappers() {
        MapperScan mapperScan = LiveClassApplication.class.getAnnotation(MapperScan.class);
        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.value()).containsExactly("com.ai.tutor.liveclass.**.mapper");
    }
}
