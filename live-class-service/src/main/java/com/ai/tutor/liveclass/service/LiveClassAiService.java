package com.ai.tutor.liveclass.service;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.liveclass.domain.entity.LiveClassSession;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiResultResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiStateResp;
import com.ai.tutor.liveclass.integration.ai.AiAgentClient;
import com.ai.tutor.liveclass.integration.feign.AppointmentInternalFeignClient;
import com.ai.tutor.liveclass.integration.im.HttpImFacade;
import com.ai.tutor.liveclass.mapper.LiveClassSessionMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LiveClassAiService {

    @Resource
    private AiAgentClient aiAgentClient;
    @Resource
    private LiveClassSessionMapper liveClassSessionMapper;
    @Resource
    private HttpImFacade httpImFacade;
    @Resource
    private AppointmentInternalFeignClient appointmentInternalFeignClient;

    public void ensureRealtimeSession(LiveClassSession session) {
        if (session == null) {
            return;
        }
        Map<String, Object> extra = parseExtra(session.getExtraJson());
        Object sessionCreated = extra.get("aiSessionCreated");
        if (Boolean.TRUE.equals(sessionCreated) || Boolean.TRUE.equals(extra.get("aiSessionCreating"))) {
            return;
        }
        if (liveClassSessionMapper.tryMarkAiSessionCreating(session.getId()) <= 0) {
            return;
        }

        try {
            AiAgentClient.CreateLiveLessonSessionRequest request = new AiAgentClient.CreateLiveLessonSessionRequest();
            request.setTeacherId(session.getTeacherUid());
            request.setStudentId(session.getStudentUid());
            request.setSubject("未配置");
            request.setGrade("未配置");
            request.setCourseType("ONLINE_FORMAL");
            request.setAudioEnabled(Boolean.TRUE);
            request.setRealtimeAiMode(isAiEnabled(session) ? "LIGHT" : "OFF");
            AiAgentClient.LiveLessonSessionView created = aiAgentClient.createSession(session.getCourseId(), request);

            LiveClassSession latest = liveClassSessionMapper.selectById(session.getId());
            Map<String, Object> latestExtra = parseExtra(latest == null ? session.getExtraJson() : latest.getExtraJson());
            latestExtra.remove("aiSessionCreating");
            latestExtra.put("aiSessionCreated", Boolean.TRUE);
            latestExtra.put("aiSessionId", created.getSessionId());
            latestExtra.put("aiSessionStatus", created.getStatus());
            latestExtra.put("aiUpdatedAt", LocalDateTime.now().toString());
            session.setExtraJson(JSONUtil.toJsonStr(latestExtra));
            liveClassSessionMapper.updateExtraJsonById(session.getId(), session.getExtraJson());
        } catch (Exception ex) {
            liveClassSessionMapper.clearAiSessionCreating(session.getId());
            throw ex;
        }
    }

    public LiveAiStateResp getAiState(LiveClassSession session) {
        if (session == null || !isAiEnabled(session)) {
            return LiveAiStateResp.builder()
                    .sessionId(session == null ? null : session.getId())
                    .courseId(session == null ? null : session.getCourseId())
                    .aiStatus("OFF")
                    .realtimeEnabled(Boolean.FALSE)
                    .summaryStatus("OFF")
                    .asrEnabled(Boolean.FALSE)
                    .llmEnabled(Boolean.FALSE)
                    .segmentCount(0)
                    .studentQuestions(List.of())
                    .homeworkCandidates(List.of())
                    .keyPoints(List.of())
                    .minutesOutline(List.of())
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
                .asrEnabled(state.getAsrEnabled())
                .llmEnabled(state.getLlmEnabled())
                .segmentCount(state.getSegmentCount())
                .lastLlmSummaryTs(resolveLong(state.getLastLlmSummaryTs(), state.getRawState(), "lastLlmSummaryTs"))
                .lastLlmSegmentCount(resolveInteger(state.getLastLlmSegmentCount(), state.getRawState(), "lastLlmSegmentCount"))
                .currentTopic(state.getCurrentTopic())
                .latestStageSummary(state.getLatestStageSummary())
                .studentQuestions(defaultList(state.getStudentQuestions()))
                .homeworkCandidates(defaultList(state.getHomeworkCandidates()))
                .keyPoints(defaultList(state.getKeyPoints()))
                .minutesOutline(defaultMapList(state.getMinutesOutline()))
                .activeSectionTitle(state.getActiveSectionTitle())
                .updatedAt(LocalDateTime.now())
                .rawState(state.getRawState())
                .build();
    }

    public void acceptAudioChunk(LiveClassSession session, AiAgentClient.AudioChunkRequest request) {
        if (session == null || !isAiEnabled(session)) {
            return;
        }
        aiAgentClient.acceptAudioChunk(session.getCourseId(), request);
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
            syncLessonSummary(session, report, preview);
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
            queueLessonReportTask(session);
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

    private void queueLessonReportTask(LiveClassSession session) {
        if (session == null || session.getId() == null || session.getCourseId() == null) {
            return;
        }
        if (liveClassSessionMapper.tryMarkAiReportTaskQueued(session.getId()) <= 0) {
            return;
        }
        try {
            LiveClassSession latest = liveClassSessionMapper.selectById(session.getId());
            LiveClassSession source = latest == null ? session : latest;
            AiAgentClient.CreateLessonReportTaskRequest request = new AiAgentClient.CreateLessonReportTaskRequest();
            request.setTeacherId(source.getTeacherUid());
            request.setStudentId(source.getStudentUid());
            request.setSubject("未配置");
            request.setGrade("未配置");
            request.setLessonTopic("课程 #" + session.getCourseId() + " 实时课堂总结");
            request.setTeacherNotes("课程已结束，请基于实时课堂转写、阶段摘要和课堂状态生成课后报告。");
            request.setStudentPerformance("请结合实时课堂中的提问、总结与互动表现生成学生学习情况。");
            request.setHomework("请结合课堂内容自动给出课后作业与复习建议。");
            request.setNextPlan("请结合课堂表现生成下节课建议。");
            request.setForceRegenerate(Boolean.FALSE);
            Map<String, Object> extraContext = new HashMap<>();
            extraContext.put("sessionId", session.getId());
            extraContext.put("providerRoomName", source.getProviderRoomName());
            Map<String, Object> sessionExtra = parseExtra(source.getExtraJson());
            Object aiSessionId = sessionExtra.get("aiSessionId");
            if (aiSessionId != null) {
                extraContext.put("aiSessionId", aiSessionId);
            }
            request.setExtraContext(extraContext);

            AiAgentClient.LessonReportTaskView created = aiAgentClient.createLessonReportTask(session.getCourseId(), request);
            LiveClassSession latestAfterQueue = liveClassSessionMapper.selectById(session.getId());
            Map<String, Object> latestExtra = parseExtra(latestAfterQueue == null ? source.getExtraJson() : latestAfterQueue.getExtraJson());
            latestExtra.put("aiReportTaskQueued", Boolean.TRUE);
            latestExtra.put("aiReportTaskId", created.getTaskId());
            latestExtra.put("aiUpdatedAt", LocalDateTime.now().toString());
            session.setExtraJson(JSONUtil.toJsonStr(latestExtra));
            liveClassSessionMapper.updateExtraJsonById(session.getId(), session.getExtraJson());
        } catch (Exception ex) {
            liveClassSessionMapper.clearAiReportTaskQueued(session.getId());
            throw ex;
        }
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
        if (policy != null && !"OFF".equalsIgnoreCase(policy.trim())) {
            return true;
        }
        Map<String, Object> extra = parseExtra(session.getExtraJson());
        return Boolean.TRUE.equals(extra.get("realtimeSummaryEnabled"))
                || Boolean.TRUE.equals(extra.get("postClassSummaryEnabled"));
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

    private static List<Map<String, Object>> defaultMapList(List<Map<String, Object>> value) {
        return value == null ? List.of() : value;
    }

    private static Long resolveLong(Long typedValue, Map<String, Object> rawState, String key) {
        if (typedValue != null) {
            return typedValue;
        }
        Object value = rawState == null ? null : rawState.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer resolveInteger(Integer typedValue, Map<String, Object> rawState, String key) {
        if (typedValue != null) {
            return typedValue;
        }
        Object value = rawState == null ? null : rawState.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private void syncLessonSummary(LiveClassSession session, Map<String, Object> report, String preview) {
        if (session == null || session.getScheduleEventId() == null || report == null || report.isEmpty()) {
            return;
        }
        try {
            appointmentInternalFeignClient.upsertLessonSummary(AppointmentInternalFeignClient.UpsertLessonSummaryRequest.builder()
                    .lessonId(session.getScheduleEventId())
                    .title(stringValue(report.get("reportTitle")))
                    .summaryBrief(preview)
                    .summaryContent(buildLessonSummaryContent(report))
                    .homework(buildHomeworkText(report.get("homework")))
                    .build());
        } catch (Exception ex) {
            log.warn("sync lesson summary failed, sessionId={}, lessonId={}", session.getId(), session.getScheduleEventId(), ex);
        }
    }

    private String buildLessonSummaryContent(Map<String, Object> report) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "标题", stringValue(report.get("reportTitle")));
        appendSection(builder, "课堂总结", stringValue(report.get("parentSummary")));
        appendSection(builder, "知识点", buildListText(report.get("knowledgePoints")));
        appendSection(builder, "学生表现", buildStudentPerformanceText(report.get("studentPerformance")));
        appendSection(builder, "课后作业", buildHomeworkText(report.get("homework")));
        appendSection(builder, "下节课建议", stringValue(report.get("nextLessonPlan")));
        appendSection(builder, "老师建议", stringValue(report.get("teacherSuggestion")));
        return builder.toString().trim();
    }

    private void appendSection(StringBuilder builder, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(title).append("：\n").append(value.trim());
    }

    private String buildStudentPerformanceText(Object value) {
        if (!(value instanceof Map<?, ?> performance)) {
            return stringValue(value);
        }
        StringBuilder builder = new StringBuilder();
        appendInline(builder, "表现总结", stringValue(performance.get("summary")));
        appendInline(builder, "优势", buildListText(performance.get("strengths")));
        appendInline(builder, "待提升", buildListText(performance.get("problems")));
        return builder.toString().trim();
    }

    private void appendInline(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(label).append("：").append(value.trim());
    }

    private String buildHomeworkText(Object value) {
        return buildListText(value);
    }

    private String buildListText(Object value) {
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder();
            for (Object item : iterable) {
                String text = stringValue(item);
                if (text == null || text.isBlank()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("- ").append(text.trim());
            }
            return builder.toString();
        }
        return stringValue(value);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
