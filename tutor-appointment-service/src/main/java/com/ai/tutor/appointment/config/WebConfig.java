package com.ai.tutor.appointment.config;

import com.ai.tutor.appointment.interceptor.CollectorInterceptor;
import com.ai.tutor.appointment.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CollectorInterceptor collectorInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 必须先执行 CollectorInterceptor，负责设置 RequestHolder
        registry.addInterceptor(collectorInterceptor)
                .addPathPatterns("/**")
                .order(1);  // 优先级最高

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }
}
