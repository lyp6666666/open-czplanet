package com.ai.tutor.liveclass;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.ai.tutor.liveclass.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.liveclass", "com.ai.tutor.common"})
@MapperScan("com.ai.tutor.liveclass.**.mapper")
public class LiveClassApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveClassApplication.class, args);
    }
}
