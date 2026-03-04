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
        System.setProperty("rocketmq.client.logUseSlf4j", "true");
        System.setProperty("rocketmq.client.logRoot", System.getProperty("user.dir") + "/.logs/rocketmq");
        System.setProperty("rocketmq.log.root", System.getProperty("user.dir") + "/.logs/rocketmqlogs");
        SpringApplication.run(TutorStartApplication.class, args);
    }
}
