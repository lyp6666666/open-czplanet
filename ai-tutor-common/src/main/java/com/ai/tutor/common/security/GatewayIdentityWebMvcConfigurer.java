package com.ai.tutor.common.security;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

public class GatewayIdentityWebMvcConfigurer implements WebMvcConfigurer {

    private final GatewayIdentityInterceptor interceptor;

    public GatewayIdentityWebMvcConfigurer(GatewayIdentityInterceptor interceptor) {
        this.interceptor = Objects.requireNonNull(interceptor, "interceptor");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
