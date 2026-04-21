package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class LiveAiResultResp {
    private Long sessionId;
    private Long courseId;
    private String resultStatus;
    private String reportStatus;
    private Map<String, Object> summary;
    private Map<String, Object> report;
    private String preview;
    private LocalDateTime updatedAt;
}
