package com.ai.tutor.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "security.gateway-identity", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(IdentitySignProperties.class)
public class GatewayIdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdentitySignatureUtils identitySignatureUtils(IdentitySignProperties properties) {
        return new IdentitySignatureUtils(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public GatewayIdentityInterceptor gatewayIdentityInterceptor(IdentitySignProperties properties,
                                                                 IdentitySignatureUtils signatureUtils) {
        return new GatewayIdentityInterceptor(properties, signatureUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public GatewayIdentityWebMvcConfigurer gatewayIdentityWebMvcConfigurer(GatewayIdentityInterceptor interceptor) {
        return new GatewayIdentityWebMvcConfigurer(interceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public FeignIdentityRequestInterceptor feignIdentityRequestInterceptor(IdentitySignatureUtils signatureUtils) {
        return new FeignIdentityRequestInterceptor(signatureUtils);
    }
}
