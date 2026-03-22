package com.ai.tutor.gateway.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewaySignServiceTest {

    @Test
    void shouldGenerateStableSignatureFromKnownVector() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setSecret("sign-secret-123");
        GatewaySignService service = new GatewaySignService(properties);

        String signature = service.sign(123L, 2, 1700000000000L, "POST", "/api/v1/test");

        assertEquals("2b68391d05c6a875eeff78ec9d99317ea8d21b3a0e26ed99f13c1898b4d792d2", signature);
    }

    @Test
    void shouldNormalizeHttpMethodToUppercase() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setSecret("sign-secret-123");
        GatewaySignService service = new GatewaySignService(properties);

        String signature = service.sign(123L, 2, 1700000000000L, "post", "/api/v1/test");

        assertEquals("2b68391d05c6a875eeff78ec9d99317ea8d21b3a0e26ed99f13c1898b4d792d2", signature);
    }
}
