package com.ai.tutor.gateway.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Objects;

@Service
public class GatewaySignService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;

    private final GatewaySignProperties properties;

    public GatewaySignService(GatewaySignProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public String sign(long uid, int role, long ts, String method, String path) {
        String secret = properties.getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Sign secret not configured");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("Sign secret too short (min 32 bytes)");
        }
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method is required");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path is required");
        }
        if (path.indexOf('\n') >= 0 || path.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Path contains invalid CR/LF characters");
        }
        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        String payload = uid + "\n" + role + "\n" + ts + "\n" + normalizedMethod + "\n" + path;
        return hmacSha256Hex(secret, payload);
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute signature", e);
        }
    }

    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        int index = 0;
        for (byte value : bytes) {
            int v = value & 0xFF;
            chars[index++] = Character.forDigit(v >>> 4, 16);
            chars[index++] = Character.forDigit(v & 0x0F, 16);
        }
        return new String(chars);
    }
}
