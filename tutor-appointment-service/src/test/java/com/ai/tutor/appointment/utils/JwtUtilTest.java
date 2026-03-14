package com.ai.tutor.appointment.utils;

import com.ai.tutor.appointment.config.JwtProperties;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generateTokenShouldWorkWithExplicitSerializer() {
        JwtUtil jwtUtil = new JwtUtil();
        JwtProperties props = new JwtProperties();
        props.setSecrets(List.of("01234567890123456789012345678901"));
        props.setIssuer("test");

        ReflectionTestUtils.setField(jwtUtil, "jwtProperties", props);
        ReflectionTestUtils.setField(jwtUtil, "objectMapper", new ObjectMapper());

        String token = jwtUtil.generateToken(1001L, "13800000000", UserRoleEnum.TEACHER);
        assertThat(token).isNotBlank();

        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("13800000000");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(1001L);
        assertThat(jwtUtil.getRole(token)).isEqualTo(UserRoleEnum.TEACHER);
    }
}

