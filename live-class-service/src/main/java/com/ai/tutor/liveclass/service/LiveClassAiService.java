package com.ai.tutor.liveclass.service;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.liveclass.domain.entity.LiveClassSession;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiResultResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiStateResp;
import com.ai.tutor.liveclass.integration.ai.AiAgentClient;
import com.ai.tutor.liveclass.integration.im.HttpImFacade;
import com.ai.tutor.liveclass.mapper.LiveClassSessionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LiveClassAiService {

    @Resource
    private AiAgentClient aiAgentClient;
    @Resource
    private LiveClassSessionMapper liveClassSessionMapper;
    @Resource
    private HttpImFacade httpImFacade;

    public void ensureRealtimeSession(LiveClassSession session) {
        if (session == null) {
            return;
        }
        Map<String, Object> extra = parseExtra(session.getExtraJson());
        Object sessionCreated = extra.get("aiSessionCreated");
        if (Boolean.TRUE.equals(sessionCreated)) {
            return;
        }

        AiAgentClient.CreateLiveLessonSessionRequest request = new AiAgentClient.CreateLiveLessonSessionRequest();
        request.setTeacherId(session.getTeacherUid());
        request.setStudentId(session.getStudentUid());
        request.setSubject("未配置");
        request.setGrade("未配置");
        request.setCourseType("ONLINE_FORMAL");
        request.setAudioEnabled(Boolean.TRUE);
        request.setRealtimeAiMode(isAiEnabled(session) ? "LIGHT" : "OFF");
        AiAgentClient.LiveLessonSessionView created = aiAgentClient.createSession(session.getCourseId(), request);

        extra.put("aiSessionCreated", Boolean.TRUE);
        extra.put("aiSessionId", created.getSessionId());
        extra.put("aiSessionStatus", created.getStatus());
        extra.put("aiUpdatedAt", LocalDateTime.now().toString());
        session.setExtraJson(JSONUtil.toJsonStr(extra));
        liveClassSessionMapper.updateExtraJsonById(session.getId(), session.getExtraJson());
    }

    public LiveAiStateResp getAiState(LiveClassSession session) {
        if (session == null || !isAiEnabled(session)) {
            return LiveAiStateResp.builder()
                    .sessionId(session == null ? null : session.getId())
                    .courseId(session == null ? null : session.getCourseId())
                    .aiStatus("OFF")
                    .realtimeEnabled(Boolean.FALSE)
                    .summaryStatus("OFF")
                    .studentQuestions(List.of())
                    .homeworkCandidates(List.of())
                    .keyPoints(List.of())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        AiAgentClient.RealtimeLessonStateView state = aiAgentClient.getState(session.getCourseId());
        String aiStatus = resolveAiStatus(state);
        return LiveAiStateResp.builder()
                .sessionId(session.getId())
                .courseId(session.getCourseId())
                .aiStatus(aiStatus)
                .realtimeEnabled(Boolean.TRUE)
                .summaryStatus("ACTIVE".equals(aiStatus) ? "ACTIVE" : aiStatus)
                .currentTopic(state.getCurrentTopic())
                .latestStageSummary(state.getLatestStageSummary())
                .studentQuestions(defaultList(state.getStudentQuestions()))
                .homeworkCandidates(defaultList(state.getHomeworkCandidates()))
                .keyPoints(defaultList(state.getKeyPoints()))
                .updatedAt(LocalDateTime.now())
                .rawState(state.getRawState())
                .build();
    }

    public LiveAiResultResp getAiResult(LiveClassSession session) {
        if (session == null || !isAiEnabled(session)) {
            return LiveAiResultResp.builder()
                    .sessionId(session == null ? null : session.getId())
                    .courseId(session == null ? null : session.getCourseId())
                    .resultStatus("OFF")
                    .reportStatus("OFF")
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        Map<String, Object> extra = parseExtra(session.getExtraJson());
        String resultStatus = String.valueOf(extra.getOrDefault("aiResultStatus", "PENDING"));
        String reportStatus = null;
        Map<String, Object> report = null;
        String preview = null;
        try {
            AiAgentClient.LessonReportView reportView = aiAgentClient.getLessonReport(session.getCourseId());
            reportStatus = reportView.getStatus();
            report = reportView.getReport();
            preview = buildPreview(report);
            resultStatus = "READY";
        } catch (Exception ignored) {
            if ("FINALIZING".equalsIgnoreCase(resultStatus)) {
                resultStatus = "FINALIZING";
            }
        }
        return LiveAiResultResp.builder()
                .sessionId(session.getId())
                .courseId(session.getCourseId())
                .resultStatus(resultStatus)
                .reportStatus(reportStatus)
                .report(report)
                .summary(buildSummaryFromReport(report))
                .preview(preview)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public LiveAiResultResp finalizeAndNotify(LiveClassSession session, Long operatorUid) {
        if (session == null || !isAiEnabled(session)) {
            return LiveAiResultResp.builder()
                    .sessionId(session == null ? null : session.getId())
                    .courseId(session == null ? null : session.getCourseId())
                    .resultStatus("OFF")
                    .reportStatus("OFF")
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        Map<String, Object> extra = parseExtra(session.getExtraJson());
        extra.put("aiResultStatus", "FINALIZING");
        extra.put("aiUpdatedAt", LocalDateTime.now().toString());
        session.setExtraJson(JSONUtil.toJsonStr(extra));
        liveClassSessionMapper.updateExtraJsonById(session.getId(), session.getExtraJson());

        try {
            aiAgentClient.finalizeLesson(session.getCourseId());
            LiveAiResultResp result = getAiResult(session);
            extra.put("aiResultStatus", result.getResultStatus());
            extra.put("aiUpdatedAt", LocalDateTime.now().toString());
            session.setExtraJson(JSONUtil.toJsonStr(extra));
            liveClassSessionMapper.updateExtraJsonById(session.getId(), session.getExtraJson());
            sendAiResultMessage(session, operatorUid, result);
            return result;
        } catch (Exception ex) {
            extra.put("aiResultStatus", "FAILED");
            extra.put("aiUpdatedAt", LocalDateTime.now().toString());
            session.setExtraJson(JSONUtil.toJsonStr(extra));
            liveClassSessionMapper.updateExtraJsonById(session.getId(), session.getExtraJson());
            return LiveAiResultResp.builder()
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .resultStatus("FAILED")
                    .reportStatus(null)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }

    public LiveAiResultResp retryResult(LiveClassSession session, Long operatorUid) {
        return finalizeAndNotify(session, operatorUid);
    }

    private void sendAiResultMessage(LiveClassSession session, Long operatorUid, LiveAiResultResp result) {
        if (session.getRoomId() == null || operatorUid == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("bizType", "LESSON_AI_RESULT");
        payload.put("eventId", session.getCourseId());
        payload.put("title", "本节课 AI 总结已生成");
        payload.put("status", result.getResultStatus());
        payload.put("contextType", "COURSE");
        payload.put("contextId", session.getCourseId());
        payload.put("content", result.getPreview());
        payload.put("reportStatus", result.getReportStatus());
        httpImFacade.sendSystemMessage(operatorUid, session.getRoomId(), payload);
    }

    private static Map<String, Object> parseExtra(String extraJson) {
        if (extraJson == null || extraJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return JSONUtil.toBean(extraJson, HashMap.class);
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private static boolean isAiEnabled(LiveClassSession session) {
        String policy = session.getAiPolicy();
        return policy != null && !"OFF".equalsIgnoreCase(policy.trim());
    }

    private static String resolveAiStatus(AiAgentClient.RealtimeLessonStateView state) {
        if (state == null) {
            return "FAILED";
        }
        if (!Boolean.TRUE.equals(state.getAsrEnabled()) && !Boolean.TRUE.equals(state.getLlmEnabled())) {
            return "OFF";
        }
        if (state.getStatus() == null || !"ACTIVE".equalsIgnoreCase(state.getStatus())) {
            return state.getStatus() == null ? "INITING" : state.getStatus();
        }
        if (!Boolean.TRUE.equals(state.getAsrEnabled())) {
            return "ASR_DEGRADED";
        }
        if (!Boolean.TRUE.equals(state.getLlmEnabled())) {
            return "LLM_DEGRADED";
        }
        return "ACTIVE";
    }

    private static List<String> defaultList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private static String buildPreview(Map<String, Object> report) {
        if (report == null || report.isEmpty()) {
            return null;
        }
        Object summary = report.get("parentSummary");
        String text = summary == null ? "" : String.valueOf(summary).trim();
        if (text.isEmpty()) {
            Object title = report.get("reportTitle");
            text = title == null ? "" : String.valueOf(title).trim();
        }
        if (text.length() > 120) {
            return text.substring(0, 120) + "...";
        }
        return text;
    }

    private static Map<String, Object> buildSummaryFromReport(Map<String, Object> report) {
        if (report == null || report.isEmpty()) {
            return null;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("stageSummary", report.get("parentSummary"));
        summary.put("keyPoints", report.get("knowledgePoints"));
        summary.put("homeworkCandidates", report.get("homework"));
        summary.put("currentTopic", report.get("reportTitle"));
        return summary;
    }
}
