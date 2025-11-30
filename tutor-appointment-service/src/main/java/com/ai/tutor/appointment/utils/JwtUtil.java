package com.ai.tutor.appointment.utils;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import io.jsonwebtoken.Claims;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "LypJwtSecretKey123LypJwtSecretKey123"; // >= 32 字节
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 小时

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     */
    public String generateToken(String phone, UserRoleEnum role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.getCode());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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
     * 获取用户角色
     */
    public UserRoleEnum getRole(String token) {
        Claims claims = parseToken(token);
        String roleCode = (String) claims.get("role");
        return UserRoleEnum.fromCode(roleCode);
    }
}