package com.ai.tutor.appointment.config;

import com.ai.tutor.appointment.interceptor.CollectorInterceptor;
import com.ai.tutor.appointment.interceptor.JwtInterceptor;
import com.ai.tutor.appointment.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

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

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                // 登录和验证码
                .excludePathPatterns("/user/loginOrRegister", "/user/sendcode")
                // 未登录可访问的首页接口（Guest Home）
                .excludePathPatterns("/api/v1/public/**")
                // 支付回调（第三方平台调用，不携带用户 token）
                .excludePathPatterns("/payment/notify/**")
                // 管理端接口由 ai-tutor-admin 模块单独鉴权
                .excludePathPatterns("/api/admin/**")
                // swagger-ui 静态资源
                .excludePathPatterns(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/favicon.ico",
                "/webjars/**"
                )
                // OpenAPI 文档
                .excludePathPatterns("/v3/api-docs/**", "/swagger-resources/**")
                .excludePathPatterns("/error","/actuator/httpexchanges")
                .order(2);

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .order(3);
    }
}
