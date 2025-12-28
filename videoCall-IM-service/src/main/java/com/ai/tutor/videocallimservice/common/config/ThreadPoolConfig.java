package com.ai.tutor.videocallimservice.common.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Description: 线程池配置
 * Author: <a href="https://github.com/zongzibinbin">abin</a>
 * Date: 2023-04-09
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {

}
