package com.ai.tutor.liveclass.service;

import com.ai.tutor.liveclass.config.LiveKitProperties;
import com.ai.tutor.liveclass.domain.vo.response.IssueJoinTokenResp;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class LiveKitTokenService {

    @Resource
    private LiveKitProperties liveKitProperties;

    public IssueJoinTokenResp issueToken(Long uid, String participantName, String roomName) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusSeconds(Math.max(300L, liveKitProperties.getTokenTtlSeconds()));

        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("roomJoin", true);
        videoGrant.put("room", roomName);
        videoGrant.put("canPublish", true);
        videoGrant.put("canSubscribe", true);

        Map<String, Object> claims = new HashMap<>();
        claims.put("video", videoGrant);
        claims.put("name", participantName);

        String token = Jwts.builder()
                .setIssuer(liveKitProperties.getApiKey())
                .setSubject(String.valueOf(uid))
                .setAudience("livekit")
                .setClaims(claims)
                .setIssuedAt(Date.from(now.toInstant(ZoneOffset.ofHours(8))))
                .setExpiration(Date.from(expireAt.toInstant(ZoneOffset.ofHours(8))))
                .signWith(resolveKey(), SignatureAlgorithm.HS256)
                .compact();

        return IssueJoinTokenResp.builder()
                .provider("LIVEKIT")
                .serverUrl(liveKitProperties.getWsUrl())
                .roomName(roomName)
                .participantName(participantName)
                .participantIdentity(String.valueOf(uid))
                .accessToken(token)
                .expireAt(expireAt)
                .build();
    }

    private SecretKey resolveKey() {
        String raw = String.valueOf(liveKitProperties.getApiSecret() == null ? "" : liveKitProperties.getApiSecret()).trim();
        byte[] bytes;
        if (raw.matches("^[A-Za-z0-9+/=]+$") && raw.length() >= 32) {
            try {
                bytes = Decoders.BASE64.decode(raw);
            } catch (IllegalArgumentException ex) {
                bytes = raw.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            bytes = raw.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            bytes = String.format("%-32s", raw).replace(' ', '0').getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
