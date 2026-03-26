package com.ai.tutor.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ai.tutor.admin", "com.ai.tutor.common"})
@MapperScan("com.ai.tutor.admin.mapper")
public class AiTutorAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTutorAdminApplication.class, args);
    }
}
