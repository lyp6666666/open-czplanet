package com.ai.tutor;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = "com.ai.tutor"
)
@MapperScan("com.ai.tutor.**.mapper")
public class TutorStartApplication
{
    public static void main(String[] args) {
        SpringApplication.run(TutorStartApplication.class, args);
    }
}
