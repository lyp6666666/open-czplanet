package com.ai.tutor.liveclass.service;

import com.ai.tutor.liveclass.config.LiveKitProperties;
import com.ai.tutor.liveclass.domain.vo.response.IssueJoinTokenResp;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LiveKitTokenServiceTest {

    @Test
    void shouldIssueLiveKitCompatibleJwtClaims() {
        LiveKitProperties properties = new LiveKitProperties();
        properties.setApiKey("dev-api-key");
        properties.setApiSecret("CHANGE_ME_LIVEKIT_API_SECRET");
        properties.setWsUrl("ws://127.0.0.1:7880");

        LiveKitTokenService service = new LiveKitTokenService();
        ReflectionTestUtils.setField(service, "liveKitProperties", properties);

        IssueJoinTokenResp resp = service.issueToken(1001L, "张老师", "class-66", "wss://huoyue.online/livekit");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(properties.getApiSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(resp.getAccessToken())
                .getBody();

        assertThat(claims.getIssuer()).isEqualTo("dev-api-key");
        assertThat(claims.getSubject()).isEqualTo("1001");
        assertThat(resp.getServerUrl()).isEqualTo("wss://huoyue.online/livekit");
        assertThat(claims.get("name", String.class)).isEqualTo("张老师");
        Map<?, ?> video = (Map<?, ?>) claims.get("video");
        assertThat(video.get("roomJoin")).isEqualTo(true);
        assertThat(video.get("canPublish")).isEqualTo(true);
        assertThat(video.get("canSubscribe")).isEqualTo(true);
        assertThat(video.get("room")).isEqualTo("class-66");
    }
}
