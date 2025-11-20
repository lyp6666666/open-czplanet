package com.ai.tutor.appointment.utils;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "LypJwtSecretKey123";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24小时

    /**
     * 生成 Token
     * @param phone 用户手机号
     * @param role 用户角色枚举
     * @return JWT字符串
     */
    public String generateToken(String phone, UserRoleEnum role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.getCode());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 解析Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 校验Token是否有效
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
     * 获取用户角色枚举
     */
    public UserRoleEnum getRole(String token) {
        Claims claims = parseToken(token);
        String roleCode = (String) claims.get("role");
        return UserRoleEnum.fromCode(roleCode);
    }
}