package com.ai.tutor.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentitySignatureUtilsTest {
    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void shouldSignAndVerifyDeterministically() {
        IdentitySignProperties properties = new IdentitySignProperties();
        properties.setSecret(STRONG_SECRET);
        IdentitySignatureUtils utils = new IdentitySignatureUtils(properties);

        String signature = utils.sign(123L, 2, 1700000000000L, "POST", "/api/v1/test");

        assertEquals("0d0526c6a32e3bc89845e62a19c2abc0cfa2d34019923fb32008e7163ba804d4", signature);
        assertTrue(utils.verify(123L, 2, 1700000000000L, "POST", "/api/v1/test", signature));
        assertFalse(utils.verify(123L, 2, 1700000000000L, "GET", "/api/v1/test", signature));
    }
}
