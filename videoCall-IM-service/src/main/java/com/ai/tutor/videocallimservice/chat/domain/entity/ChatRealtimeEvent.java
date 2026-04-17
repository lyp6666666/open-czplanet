package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEvent {
    private Long eventId;
    private Long targetUid;
    private String eventType;
    private String bizType;
    private Long roomId;
    private Long msgId;
    private LocalDateTime occurredAt;
    private String payloadJson;
    private LocalDateTime createTime;
}
