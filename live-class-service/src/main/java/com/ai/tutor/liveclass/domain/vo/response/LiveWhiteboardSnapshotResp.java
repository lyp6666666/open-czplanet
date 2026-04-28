package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class LiveWhiteboardSnapshotResp {
    private Long whiteboardId;
    private Long sessionId;
    private Long courseId;
    private Long scheduleEventId;
    private Long sceneVersion;
    private Map<String, Object> scene;
    private Boolean finalized;
    private LocalDateTime updatedAt;
}
