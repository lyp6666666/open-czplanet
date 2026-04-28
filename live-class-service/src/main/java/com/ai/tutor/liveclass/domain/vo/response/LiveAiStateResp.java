package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class LiveAiStateResp {
    private Long sessionId;
    private Long courseId;
    private String aiStatus;
    private Boolean realtimeEnabled;
    private String summaryStatus;
    private Boolean asrEnabled;
    private Boolean llmEnabled;
    private Integer segmentCount;
    private Long lastLlmSummaryTs;
    private Integer lastLlmSegmentCount;
    private String currentTopic;
    private String latestStageSummary;
    private List<String> studentQuestions;
    private List<String> homeworkCandidates;
    private List<String> keyPoints;
    private List<Map<String, Object>> minutesOutline;
    private String activeSectionTitle;
    private LocalDateTime updatedAt;
    private Map<String, Object> rawState;
}
