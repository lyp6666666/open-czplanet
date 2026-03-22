package com.ai.tutor.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class JwtClaimsService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final GatewayJwtProperties properties;

    public JwtClaimsService(GatewayJwtProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public JwtIdentity parseToken(String token) {
        Claims claims = parseClaims(token);
        long uid = extractUid(claims);
        int role = extractRole(claims);
        return new JwtIdentity(uid, role);
    }

    public JwtIdentity parseBearerToken(String bearerToken) {
        if (bearerToken == null) {
            throw new JwtClaimsException("Missing JWT token");
        }
        String trimmed = bearerToken.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }
        return parseToken(trimmed);
    }

    private Claims parseClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new JwtClaimsException("Missing JWT token");
        }
        List<String> secrets = properties.getSecrets();
        if (secrets == null || secrets.isEmpty()) {
            throw new JwtClaimsException("JWT secrets not configured");
        }

        JwtException lastException = null;
        String issuer = properties.getIssuer();

        for (String rawSecret : secrets) {
            if (rawSecret == null || rawSecret.trim().isEmpty()) {
                continue;
            }
            try {
                Key key = Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));
                io.jsonwebtoken.JwtParserBuilder builder = Jwts.parserBuilder().setSigningKey(key);
                if (!isBlank(issuer)) {
                    builder.requireIssuer(issuer);
                }
                return builder.build().parseClaimsJws(token).getBody();
            } catch (JwtException e) {
                lastException = e;
            }
        }

        JwtClaimsException exception = new JwtClaimsException("Invalid JWT token");
        if (lastException != null) {
            exception.initCause(lastException);
        }
        throw exception;
    }

    private long extractUid(Claims claims) {
        Object raw = claims.get(CLAIM_USER_ID);
        if (raw == null) {
            raw = claims.get(CLAIM_UID);
        }
        if (raw == null) {
            throw new JwtClaimsException("Missing uid claim");
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            throw new JwtClaimsException("Missing uid claim");
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new JwtClaimsException("Invalid uid claim", e);
        }
    }

    private int extractRole(Claims claims) {
        Object raw = claims.get(CLAIM_ROLE);
        if (raw == null) {
            throw new JwtClaimsException("Missing role claim");
        }
        if (raw instanceof Number) {
            return normalizeRole(((Number) raw).intValue());
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            throw new JwtClaimsException("Missing role claim");
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if ("teacher".equals(normalized)) {
            return 1;
        }
        if ("student".equals(normalized)) {
            return 2;
        }
        if ("org".equals(normalized)) {
            return 3;
        }
        try {
            int value = Integer.parseInt(text);
            return normalizeRole(value);
        } catch (NumberFormatException e) {
            throw new JwtClaimsException("Unknown role: " + text);
        }
    }

    private int normalizeRole(int role) {
        if (role == 1 || role == 2 || role == 3) {
            return role;
        }
        throw new JwtClaimsException("Unknown role: " + role);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class JwtIdentity {
        private final long uid;
        private final int role;

        public JwtIdentity(long uid, int role) {
            this.uid = uid;
            this.role = role;
        }

        public long uid() {
            return uid;
        }

        public int role() {
            return role;
        }
    }

    public static class JwtClaimsException extends RuntimeException {
        public JwtClaimsException(String message) {
            super(message);
        }

        public JwtClaimsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
