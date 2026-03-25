package com.ai.tutor.gateway.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySignServiceTest {
    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void shouldGenerateStableSignatureFromKnownVector() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setSecret(STRONG_SECRET);
        GatewaySignService service = new GatewaySignService(properties);

        String signature = service.sign(123L, 2, 1700000000000L, "POST", "/api/v1/test");

        assertEquals("0d0526c6a32e3bc89845e62a19c2abc0cfa2d34019923fb32008e7163ba804d4", signature);
    }

    @Test
    void shouldNormalizeHttpMethodToUppercase() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setSecret(STRONG_SECRET);
        GatewaySignService service = new GatewaySignService(properties);

        String signature = service.sign(123L, 2, 1700000000000L, "post", "/api/v1/test");

        assertEquals("0d0526c6a32e3bc89845e62a19c2abc0cfa2d34019923fb32008e7163ba804d4", signature);
    }

    @Test
    void shouldRejectShortSignSecret() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setSecret("short-secret");
        GatewaySignService service = new GatewaySignService(properties);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.sign(123L, 2, 1700000000000L, "POST", "/api/v1/test")
        );
        assertTrue(exception.getMessage().toLowerCase().contains("too short"));
    }

    @Test
    void shouldRejectPathWithCrLf() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setSecret(STRONG_SECRET);
        GatewaySignService service = new GatewaySignService(properties);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.sign(123L, 2, 1700000000000L, "POST", "/api/v1/test\r\n")
        );
        assertTrue(exception.getMessage().toLowerCase().contains("cr/lf"));
    }

    @Test
    void shouldNormalizeNullWhitelistPathsToEmpty() {
        GatewaySignProperties properties = new GatewaySignProperties();
        properties.setWhitelistPaths(null);

        assertEquals(0, properties.getWhitelistPaths().size());
    }
}
