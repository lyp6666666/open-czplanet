package com.ai.tutor.liveclass.integration.ai;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class AiAgentClient {

    private final AiAgentProperties properties;
    private final RestTemplate restTemplate;

    public AiAgentClient(AiAgentProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .build();
    }

    public LiveLessonSessionView createSession(Long lessonId, CreateLiveLessonSessionRequest request) {
        return exchange(
                "/internal/ai/live-lessons/" + lessonId + "/sessions",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<BaseResponse<LiveLessonSessionView>>() {
                },
                "createLiveLessonSession"
        );
    }

    public RealtimeLessonStateView getState(Long lessonId) {
        return exchange(
                "/internal/ai/live-lessons/" + lessonId + "/state",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<BaseResponse<RealtimeLessonStateView>>() {
                },
                "getRealtimeLessonState"
        );
    }

    public RealtimeLessonStateView finalizeLesson(Long lessonId) {
        return exchange(
                "/internal/ai/live-lessons/" + lessonId + "/finalize",
                HttpMethod.POST,
                Map.of(),
                new ParameterizedTypeReference<BaseResponse<RealtimeLessonStateView>>() {
                },
                "finalizeRealtimeLesson"
        );
    }

    public LessonReportView getLessonReport(Long lessonId) {
        return exchange(
                "/internal/ai/lessons/" + lessonId + "/report",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<BaseResponse<LessonReportView>>() {
                },
                "getLessonReport"
        );
    }

    public LessonReportTaskView createLessonReportTask(Long lessonId, CreateLessonReportTaskRequest request) {
        return exchange(
                "/internal/ai/lessons/" + lessonId + "/report-tasks",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<BaseResponse<LessonReportTaskView>>() {
                },
                "createLessonReportTask"
        );
    }

    private <T> T exchange(String path,
                           HttpMethod method,
                           Object body,
                           ParameterizedTypeReference<BaseResponse<T>> type,
                           String action) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI agent is disabled");
        }
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        if (baseUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI agent baseUrl is empty");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getInternalToken() != null && !properties.getInternalToken().isBlank()) {
            headers.set("X-Ai-Agent-Token", properties.getInternalToken().trim());
        }
        try {
            ResponseEntity<BaseResponse<T>> response = restTemplate.exchange(
                    baseUrl + path,
                    method,
                    new HttpEntity<>(body, headers),
                    type
            );
            return unwrap(response.getBody(), action);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI agent call failed: " + action);
        }
    }

    private static <T> T unwrap(BaseResponse<T> response, String action) {
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI agent call failed: " + action + " response is null");
        }
        if (response.getCode() != ErrorCode.SUCCESS.getCode()) {
            String message = response.getMessage();
            if (message == null || message.isBlank()) {
                message = "AI agent call failed: " + action;
            }
            throw new BusinessException(response.getCode(), message);
        }
        return response.getData();
    }

    @Data
    public static class CreateLiveLessonSessionRequest {
        private Long teacherId;
        private Long studentId;
        private String subject;
        private String grade;
        private String courseType;
        private Boolean audioEnabled;
        private String realtimeAiMode;
    }

    @Data
    public static class LiveLessonSessionView {
        private Long lessonId;
        private String sessionId;
        private Boolean asrEnabled;
        private Boolean llmEnabled;
        private String mode;
        private String status;
    }

    @Data
    public static class RealtimeLessonStateView {
        private Long lessonId;
        private String mode;
        private Boolean asrEnabled;
        private Boolean llmEnabled;
        private String currentTopic;
        private String latestStageSummary;
        private List<String> studentQuestions;
        private List<String> homeworkCandidates;
        private List<String> keyPoints;
        private Integer segmentCount;
        private String status;
        private Map<String, Object> rawState;
    }

    @Data
    public static class LessonReportView {
        private Long lessonId;
        private String taskId;
        private String status;
        private Map<String, Object> report;
    }

    @Data
    public static class CreateLessonReportTaskRequest {
        private Long teacherId;
        private Long studentId;
        private String subject;
        private String grade;
        private String lessonTopic;
        private String teacherNotes;
        private String studentPerformance;
        private String homework;
        private String nextPlan;
        private Map<String, Object> extraContext;
        private Boolean forceRegenerate;
    }

    @Data
    public static class LessonReportTaskView {
        private String taskId;
        private String status;
    }
}
