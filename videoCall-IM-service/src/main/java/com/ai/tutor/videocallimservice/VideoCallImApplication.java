package com.ai.tutor.videocallimservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.ai.tutor.videocallimservice.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.videocallimservice", "com.ai.tutor.common"})
@MapperScan("com.ai.tutor.videocallimservice.**.mapper")
public class VideoCallImApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoCallImApplication.class, args);
    }
}
