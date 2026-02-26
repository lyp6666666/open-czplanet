package com.ai.tutor.appointment.utils;

import com.ai.tutor.appointment.config.JwtProperties;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * JWT Claims Key：用户id
     */
    private static final String CLAIM_USER_ID = "userId";

    /**
     * JWT Claims Key：用户角色
     */
    private static final String CLAIM_ROLE = "role";

    private Key getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String phone, UserRoleEnum role) {
        List<String> secrets = jwtProperties.getSecrets();
        if (secrets == null || secrets.isEmpty() || secrets.get(0) == null || secrets.get(0).isBlank()) {
            throw new IllegalStateException("jwt.secrets 未配置");
        }
        if (secrets.get(0).getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secrets[0] 长度不足（HMAC-SHA256 至少 32 字节）");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_ROLE, role.getCode());

        long expirationMillis = (jwtProperties.getExpiration() == null ? java.time.Duration.ofHours(24) : jwtProperties.getExpiration()).toMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setIssuer(jwtProperties.getIssuer())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .serializeToJsonWith(new JacksonSerializer<>(objectMapper))
                .signWith(getSigningKey(secrets.get(0)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 兼容旧签名：只传手机号与角色时，userId claim 会缺失（不建议继续使用）
     */
    public String generateToken(String phone, UserRoleEnum role) {
        return generateToken(null, phone, role);
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        List<String> secrets = jwtProperties.getSecrets();
        if (secrets == null || secrets.isEmpty()) {
            throw new IllegalStateException("jwt.secrets 未配置");
        }
        JwtException last = null;
        for (String secret : secrets) {
            if (secret == null || secret.isBlank()) {
                continue;
            }
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(getSigningKey(secret))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (JwtException e) {
                last = e;
            }
        }
        throw last == null ? new JwtException("token 解析失败") : last;
    }

    /**
     * 校验 Token
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取手机号
     */
    public String getPhone(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 获取用户id
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object raw = claims.get(CLAIM_USER_ID);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        return Long.parseLong(String.valueOf(raw));
    }

    /**
     * 获取用户角色
     */
    public UserRoleEnum getRole(String token) {
        Claims claims = parseToken(token);
        String roleCode = String.valueOf(claims.get(CLAIM_ROLE));
        return UserRoleEnum.fromCode(roleCode);
    }
}
