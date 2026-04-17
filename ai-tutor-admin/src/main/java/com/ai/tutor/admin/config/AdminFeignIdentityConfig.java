package com.ai.tutor.admin.config;

import com.ai.tutor.common.security.FeignIdentityRequestInterceptor;
import com.ai.tutor.common.security.IdentitySignProperties;
import com.ai.tutor.common.security.IdentitySignatureUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdentitySignProperties.class)
public class AdminFeignIdentityConfig {

    @Bean
    @ConditionalOnMissingBean
    public IdentitySignatureUtils adminIdentitySignatureUtils(IdentitySignProperties properties) {
        return new IdentitySignatureUtils(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FeignIdentityRequestInterceptor adminFeignIdentityRequestInterceptor(IdentitySignatureUtils signatureUtils) {
        return new FeignIdentityRequestInterceptor(signatureUtils);
    }
}
