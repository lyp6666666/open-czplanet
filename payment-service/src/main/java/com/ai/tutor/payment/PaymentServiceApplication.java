package com.ai.tutor.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.ai.tutor.payment.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.payment", "com.ai.tutor.common"})
@MapperScan("com.ai.tutor.payment.**.mapper")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
