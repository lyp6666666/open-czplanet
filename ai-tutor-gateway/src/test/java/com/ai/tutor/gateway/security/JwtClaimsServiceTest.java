package com.ai.tutor.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtClaimsServiceTest {

    private static final String SECRET = "test-jwt-secret-1234567890-test-jwt-secret-1234567890";
    private static final String ISSUER = "ai-tutor";

    private JwtClaimsService newService() {
        GatewayJwtProperties properties = new GatewayJwtProperties();
        properties.setSecrets(Arrays.asList(SECRET));
        properties.setIssuer(ISSUER);
        return new JwtClaimsService(properties);
    }

    private String tokenWithClaims(Map<String, Object> claims) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(ISSUER)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void shouldParseUidAndRoleFromJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 123L);
        claims.put("role", 2);
        String token = tokenWithClaims(claims);

        JwtClaimsService.JwtIdentity identity = newService().parseToken(token);

        assertEquals(123L, identity.uid());
        assertEquals(2, identity.role());
    }

    @Test
    void shouldParseBearerToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 456L);
        claims.put("role", "teacher");
        String token = tokenWithClaims(claims);

        JwtClaimsService.JwtIdentity identity = newService().parseBearerToken("Bearer " + token);

        assertEquals(456L, identity.uid());
        assertEquals(1, identity.role());
    }

    @Test
    void shouldRejectMissingUidOrRole() {
        Map<String, Object> missingUid = new HashMap<>();
        missingUid.put("role", 2);
        String tokenMissingUid = tokenWithClaims(missingUid);

        JwtClaimsService.JwtClaimsException uidException = assertThrows(
                JwtClaimsService.JwtClaimsException.class,
                () -> newService().parseToken(tokenMissingUid)
        );
        assertTrue(uidException.getMessage().toLowerCase().contains("uid"));

        Map<String, Object> missingRole = new HashMap<>();
        missingRole.put("userId", 123L);
        String tokenMissingRole = tokenWithClaims(missingRole);

        JwtClaimsService.JwtClaimsException roleException = assertThrows(
                JwtClaimsService.JwtClaimsException.class,
                () -> newService().parseToken(tokenMissingRole)
        );
        assertTrue(roleException.getMessage().toLowerCase().contains("role"));
    }

    @Test
    void shouldRejectUnknownRoleString() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 789L);
        claims.put("role", "admin");
        String token = tokenWithClaims(claims);

        JwtClaimsService.JwtClaimsException exception = assertThrows(
                JwtClaimsService.JwtClaimsException.class,
                () -> newService().parseToken(token)
        );
        assertTrue(exception.getMessage().toLowerCase().contains("unknown role"));
    }
}
