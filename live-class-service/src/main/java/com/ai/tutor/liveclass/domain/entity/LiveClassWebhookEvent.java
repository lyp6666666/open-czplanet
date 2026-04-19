package com.ai.tutor.liveclass.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassWebhookEvent {
    private Long id;
    private String provider;
    private String providerEventId;
    private Long sessionId;
    private String providerRoomName;
    private String eventType;
    private String payloadJson;
    private Boolean processed;
    private LocalDateTime processedAt;
    private LocalDateTime createTime;
}
