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
public class LiveClassEvent {
    private Long id;
    private Long sessionId;
    private String eventType;
    private String eventSource;
    private Long operatorUid;
    private String payloadJson;
    private LocalDateTime occurredAt;
    private LocalDateTime createTime;
}
