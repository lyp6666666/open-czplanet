package com.ai.tutor.appointment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.ai.tutor.appointment.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.appointment", "com.ai.tutor.common"})
@MapperScan("com.ai.tutor.appointment.**.mapper")
public class TutorAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutorAppointmentApplication.class, args);
    }
}
