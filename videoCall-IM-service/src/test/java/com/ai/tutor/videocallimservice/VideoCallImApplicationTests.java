package com.ai.tutor.videocallimservice;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;

class VideoCallImApplicationTests {

    @Test
    void shouldConfigureFeignAndComponentScan() {
        SpringBootApplication appAnnotation = VideoCallImApplication.class.getAnnotation(SpringBootApplication.class);
        assertThat(appAnnotation).isNotNull();
        assertThat(appAnnotation.scanBasePackages())
                .containsExactlyInAnyOrder("com.ai.tutor.videocallimservice", "com.ai.tutor.common");

        EnableFeignClients feignAnnotation = VideoCallImApplication.class.getAnnotation(EnableFeignClients.class);
        assertThat(feignAnnotation).isNotNull();
        assertThat(feignAnnotation.basePackages())
                .containsExactly("com.ai.tutor.videocallimservice.integration.feign");
    }

    @Test
    void shouldScanImMappers() {
        MapperScan mapperScan = VideoCallImApplication.class.getAnnotation(MapperScan.class);
        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.value()).containsExactly("com.ai.tutor.videocallimservice.**.mapper");
    }
}
