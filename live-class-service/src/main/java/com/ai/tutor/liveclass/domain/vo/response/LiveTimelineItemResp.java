package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveTimelineItemResp {
    private String eventType;
    private String eventSource;
    private Long operatorUid;
    private String payloadJson;
    private LocalDateTime occurredAt;
}
