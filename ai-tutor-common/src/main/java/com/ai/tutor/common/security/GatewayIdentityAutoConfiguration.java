package com.ai.tutor.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "security.gateway-identity", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(IdentitySignProperties.class)
public class GatewayIdentityAutoConfiguration {

    @Bean
    public IdentitySignatureUtils identitySignatureUtils(IdentitySignProperties properties) {
        return new IdentitySignatureUtils(properties);
    }

    @Bean
    public GatewayIdentityInterceptor gatewayIdentityInterceptor(IdentitySignProperties properties,
                                                                 IdentitySignatureUtils signatureUtils) {
        return new GatewayIdentityInterceptor(properties, signatureUtils);
    }

    @Bean
    public GatewayIdentityWebMvcConfigurer gatewayIdentityWebMvcConfigurer(GatewayIdentityInterceptor interceptor) {
        return new GatewayIdentityWebMvcConfigurer(interceptor);
    }

    @Bean
    public FeignIdentityRequestInterceptor feignIdentityRequestInterceptor(IdentitySignatureUtils signatureUtils) {
        return new FeignIdentityRequestInterceptor(signatureUtils);
    }
}
