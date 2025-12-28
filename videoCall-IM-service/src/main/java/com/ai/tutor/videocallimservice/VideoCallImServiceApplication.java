package com.ai.tutor.videocallimservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
@MapperScan("com.ai.tutor.videocallimservice.chat.mapper")
public class VideoCallImServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoCallImServiceApplication.class, args);
    }
}
