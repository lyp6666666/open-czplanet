package com.ai.tutor.liveclass.service;

import cn.hutool.json.JSONUtil;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.LessonPaymentAccessCheckInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.liveclass.config.LiveKitProperties;
import com.ai.tutor.liveclass.domain.entity.LiveClassDeviceReport;
import com.ai.tutor.liveclass.domain.entity.LiveClassEvent;
import com.ai.tutor.liveclass.domain.entity.LiveClassParticipant;
import com.ai.tutor.liveclass.domain.entity.LiveClassSession;
import com.ai.tutor.liveclass.domain.entity.LiveClassWebhookEvent;
import com.ai.tutor.liveclass.domain.vo.request.EndLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.IssueJoinTokenRequest;
import com.ai.tutor.liveclass.domain.vo.request.LeaveLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.LiveDeviceReportRequest;
import com.ai.tutor.liveclass.domain.vo.request.LiveKitWebhookRequest;
import com.ai.tutor.liveclass.domain.vo.request.PrepareLiveSessionRequest;
import com.ai.tutor.liveclass.domain.vo.request.SyncCourseSessionRequest;
import com.ai.tutor.liveclass.domain.vo.response.IssueJoinTokenResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveReminderItemResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiResultResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveAiStateResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveSessionResp;
import com.ai.tutor.liveclass.domain.vo.response.LiveTimelineItemResp;
import com.ai.tutor.liveclass.domain.vo.response.PrepareLiveSessionResp;
import com.ai.tutor.liveclass.integration.feign.AppointmentInternalFeignClient;
import com.ai.tutor.liveclass.mapper.LiveClassDeviceReportMapper;
import com.ai.tutor.liveclass.mapper.LiveClassEventMapper;
import com.ai.tutor.liveclass.mapper.LiveClassParticipantMapper;
import com.ai.tutor.liveclass.mapper.LiveClassSessionMapper;
import com.ai.tutor.liveclass.mapper.LiveClassWebhookEventMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LiveClassService {

    @Resource
    private LiveClassSessionMapper liveClassSessionMapper;
    @Resource
    private LiveClassParticipantMapper liveClassParticipantMapper;
    @Resource
    private LiveClassEventMapper liveClassEventMapper;
    @Resource
    private LiveClassDeviceReportMapper liveClassDeviceReportMapper;
    @Resource
    private LiveClassWebhookEventMapper liveClassWebhookEventMapper;
    @Resource
    private LiveKitTokenService liveKitTokenService;
    @Resource
    private LiveKitProperties liveKitProperties;
    @Resource
    private AppointmentInternalFeignClient appointmentInternalFeignClient;
    @Resource
    private LiveClassAiService liveClassAiService;

    @Transactional
    public LiveSessionResp syncFromCourse(SyncCourseSessionRequest request) {
        ThrowUtils.throwIf(request == null || request.getCourseId() == null, ErrorCode.PARAMS_ERROR);
        LocalDateTime joinOpenAt = request.getScheduledStartAt().minusMinutes(10);
        LiveClassSession session = liveClassSessionMapper.selectByCourseId(request.getCourseId());
        if (session == null) {
            String roomName = buildRoomName(request.getCourseId());
            session = LiveClassSession.builder()
                    .courseId(request.getCourseId())
                    .scheduleEventId(request.getScheduleEventId())
                    .roomId(request.getRoomId())
                    .provider("LIVEKIT")
                    .providerRoomName(roomName)
                    .teacherUid(request.getTeacherUid())
                    .studentUid(request.getStudentUid())
                    .status("CREATED")
                    .joinOpenAt(joinOpenAt)
                    .scheduledStartAt(request.getScheduledStartAt())
                    .scheduledEndAt(request.getScheduledEndAt())
                    .recordPolicy(defaulted(request.getRecordPolicy(), "OFF"))
                    .aiPolicy(defaulted(request.getAiPolicy(), "OFF"))
                    .version(0)
                    .build();
            liveClassSessionMapper.insert(session);
            appendEvent(session.getId(), "SESSION_CREATED", "APP", null, request);
            createParticipant(session.getId(), session.getTeacherUid(), "TEACHER");
            createParticipant(session.getId(), session.getStudentUid(), "STUDENT");
        } else {
            session.setScheduleEventId(request.getScheduleEventId());
            session.setRoomId(request.getRoomId());
            session.setTeacherUid(request.getTeacherUid());
            session.setStudentUid(request.getStudentUid());
            session.setJoinOpenAt(joinOpenAt);
            session.setScheduledStartAt(request.getScheduledStartAt());
            session.setScheduledEndAt(request.getScheduledEndAt());
            session.setRecordPolicy(defaulted(request.getRecordPolicy(), session.getRecordPolicy()));
            session.setAiPolicy(defaulted(request.getAiPolicy(), session.getAiPolicy()));
            liveClassSessionMapper.updateByCourseId(session);
            appendEvent(session.getId(), "SESSION_SYNCED", "APP", null, request);
        }
        return toResp(requireByCourseId(request.getCourseId()), null);
    }

    public LiveSessionResp getByCourseId(Long courseId, Long viewerUid) {
        return toResp(requireByCourseId(courseId), viewerUid);
    }

    public PrepareLiveSessionResp prepare(Long courseId, Long viewerUid, PrepareLiveSessionRequest request) {
        LiveClassSession session = requireByCourseId(courseId);
        try {
            liveClassAiService.ensureRealtimeSession(session);
        } catch (Exception ignored) {
            // AI 初始化失败不影响主课堂准备流程
        }
        String peerName = resolvePeerDisplayName(session, viewerUid);
        boolean joinableNow = joinableNow(session);
        LessonPaymentAccessCheckInfo accessCheck = loadLessonJoinAccess(courseId);
        boolean paymentBlocked = accessCheck != null && Boolean.TRUE.equals(accessCheck.getBlocked());
        boolean canJoin = canJoin(session, viewerUid) && !paymentBlocked;
        return PrepareLiveSessionResp.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .courseTitle("实时课程")
                .peerDisplayName(peerName)
                .canJoin(canJoin)
                .joinableNow(joinableNow)
                .joinBlockedReason(paymentBlocked ? accessCheck.getReason() : resolveJoinBlockedReason(session, joinableNow))
                .blockingPaymentOrderId(paymentBlocked ? accessCheck.getBlockingOrderId() : null)
                .blockingLessonId(paymentBlocked ? accessCheck.getBlockingLessonId() : null)
                .defaultMediaPolicy("AUDIO_VIDEO")
                .deviceCheckRequired(Boolean.TRUE)
                .build();
    }

    public List<LiveReminderItemResp> myReminders(Long uid) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(2);
        LocalDateTime windowEnd = now.plusHours(24);
        return liveClassSessionMapper.listByParticipantUid(uid).stream()
                .filter(session -> session.getScheduledStartAt() != null)
                .filter(session -> !isTerminalStatus(session))
                .filter(session -> {
                    LocalDateTime startAt = session.getScheduledStartAt();
                    return !startAt.isBefore(windowStart) && !startAt.isAfter(windowEnd);
                })
                .sorted(Comparator
                        .comparing((LiveClassSession session) -> Boolean.TRUE.equals(joinableNow(session)) ? 0 : 1)
                        .thenComparing(LiveClassSession::getScheduledStartAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .map(session -> LiveReminderItemResp.builder()
                        .sessionId(session.getId())
                        .courseId(session.getCourseId())
                        .title("课程 #" + session.getCourseId())
                        .status(session.getStatus())
                        .joinableNow(joinableNow(session))
                        .canJoin(canJoin(session, uid))
                        .scheduledStartAt(session.getScheduledStartAt())
                        .scheduledEndAt(session.getScheduledEndAt())
                        .joinOpenAt(session.getJoinOpenAt())
                        .peerDisplayName(resolvePeerDisplayName(session, uid))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueJoinTokenResp issueJoinToken(Long sessionId, Long uid, IssueJoinTokenRequest request) {
        LiveClassSession session = requireById(sessionId);
        ThrowUtils.throwIf(!canJoin(session, uid), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!joinableNow(session), ErrorCode.OPERATION_ERROR, "未到可入会时间");

        String participantName = resolveDisplayName(uid);
        IssueJoinTokenResp resp = liveKitTokenService.issueToken(uid, participantName, session.getProviderRoomName());
        liveClassSessionMapper.markParticipantJoined(sessionId, uid, LocalDateTime.now());
        upsertParticipantJoin(sessionId, uid, session.getTeacherUid(), request);
        appendEvent(sessionId, "JOIN_TOKEN_ISSUED", "APP", uid, Map.of("clientType", request.getClientType(), "joinMode", request.getJoinMode()));
        return resp;
    }

    public LiveSessionResp status(Long sessionId, Long viewerUid) {
        LiveClassSession session = requireById(sessionId);
        try {
            liveClassAiService.ensureRealtimeSession(session);
        } catch (Exception ignored) {
            // AI 初始化失败不影响课堂主流程
        }
        return toResp(session, viewerUid);
    }

    @Transactional
    public void reportDevice(Long sessionId, Long uid, LiveDeviceReportRequest request) {
        requireParticipantSession(sessionId, uid);
        liveClassDeviceReportMapper.insert(LiveClassDeviceReport.builder()
                .sessionId(sessionId)
                .uid(uid)
                .reportStage(request.getReportStage())
                .cameraStatus(request.getCameraStatus())
                .micStatus(request.getMicStatus())
                .speakerStatus(request.getSpeakerStatus())
                .networkLevel(request.getNetworkLevel())
                .browserInfo(request.getBrowserInfo())
                .osInfo(request.getOsInfo())
                .deviceJson(JSONUtil.toJsonStr(request.getDeviceInfo()))
                .build());
        appendEvent(sessionId, "DEVICE_REPORTED", "APP", uid, request);
    }

    @Transactional
    public LiveSessionResp leave(Long sessionId, Long uid, LeaveLiveSessionRequest request) {
        requireParticipantSession(sessionId, uid);
        LiveClassParticipant participant = liveClassParticipantMapper.selectBySessionIdAndUid(sessionId, uid);
        if (participant != null) {
            participant.setLastLeaveAt(LocalDateTime.now());
            participant.setOnlineStatus("LEFT");
            liveClassParticipantMapper.updateLeaveState(participant);
        }
        liveClassSessionMapper.markParticipantLeft(sessionId, uid, LocalDateTime.now());
        appendEvent(sessionId, "PARTICIPANT_LEFT", "APP", uid, request);
        return toResp(requireById(sessionId), uid);
    }

    @Transactional
    public LiveSessionResp end(Long sessionId, Long uid, EndLiveSessionRequest request) {
        LiveClassSession session = requireParticipantSession(sessionId, uid);
        liveClassSessionMapper.markEnded(sessionId, uid, defaulted(request == null ? null : request.getReason(), "USER_END"), LocalDateTime.now());
        appendEvent(sessionId, "CLASS_ENDED", "APP", uid, request);
        try {
            liveClassAiService.finalizeAndNotify(requireById(sessionId), uid);
            appendEvent(sessionId, "CLASS_AI_FINALIZED", "APP", uid, Map.of("courseId", session.getCourseId()));
        } catch (Exception ex) {
            appendEvent(sessionId, "CLASS_AI_FINALIZE_FAILED", "APP", uid, Map.of("courseId", session.getCourseId()));
        }
        return toResp(requireById(sessionId), uid);
    }

    public LiveAiStateResp aiState(Long sessionId, Long uid) {
        LiveClassSession session = requireParticipantSession(sessionId, uid);
        return liveClassAiService.getAiState(session);
    }

    public LiveAiResultResp aiResult(Long sessionId, Long uid) {
        LiveClassSession session = requireParticipantSession(sessionId, uid);
        return liveClassAiService.getAiResult(session);
    }

    public LiveAiResultResp retryAiResult(Long sessionId, Long uid) {
        LiveClassSession session = requireParticipantSession(sessionId, uid);
        LiveAiResultResp result = liveClassAiService.retryResult(session, uid);
        appendEvent(sessionId, "CLASS_AI_RETRY", "APP", uid, Map.of("resultStatus", result.getResultStatus()));
        return result;
    }

    public List<LiveTimelineItemResp> timeline(Long sessionId, Long uid) {
        requireParticipantSession(sessionId, uid);
        return liveClassEventMapper.listBySessionId(sessionId).stream()
                .map(item -> LiveTimelineItemResp.builder()
                        .eventType(item.getEventType())
                        .eventSource(item.getEventSource())
                        .operatorUid(item.getOperatorUid())
                        .payloadJson(item.getPayloadJson())
                        .occurredAt(item.getOccurredAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void consumeLiveKitWebhook(LiveKitWebhookRequest request) {
        ThrowUtils.throwIf(request == null || blank(request.getEventId()) || blank(request.getEventType()), ErrorCode.PARAMS_ERROR);
        LiveClassWebhookEvent existing = liveClassWebhookEventMapper.selectByProviderAndEventId("LIVEKIT", request.getEventId());
        if (existing != null) {
            return;
        }
        LiveClassSession session = blank(request.getRoomName()) ? null : liveClassSessionMapper.selectByProviderRoomName(request.getRoomName());
        LiveClassWebhookEvent event = LiveClassWebhookEvent.builder()
                .provider("LIVEKIT")
                .providerEventId(request.getEventId())
                .sessionId(session == null ? null : session.getId())
                .providerRoomName(request.getRoomName())
                .eventType(request.getEventType())
                .payloadJson(JSONUtil.toJsonStr(request))
                .processed(Boolean.FALSE)
                .build();
        liveClassWebhookEventMapper.insert(event);
        if (session == null) {
            return;
        }

        String normalized = request.getEventType().trim().toUpperCase(Locale.ROOT);
        if ("PARTICIPANT_JOINED".equals(normalized)) {
            Long uid = resolveParticipantUid(request);
            if (uid != null) {
                liveClassSessionMapper.markParticipantJoined(session.getId(), uid, LocalDateTime.now());
                upsertWebhookParticipantJoin(session, uid, request);
                appendEvent(session.getId(), "PARTICIPANT_JOINED", "WEBHOOK", uid, request);
            }
        } else if ("PARTICIPANT_LEFT".equals(normalized)) {
            Long uid = resolveParticipantUid(request);
            if (uid != null) {
                LiveClassParticipant participant = liveClassParticipantMapper.selectBySessionIdAndUid(session.getId(), uid);
                if (participant != null) {
                    participant.setLastLeaveAt(LocalDateTime.now());
                    participant.setOnlineStatus("LEFT");
                    liveClassParticipantMapper.updateLeaveState(participant);
                }
                appendEvent(session.getId(), "PARTICIPANT_LEFT", "WEBHOOK", uid, request);
            }
        } else if ("ROOM_FINISHED".equals(normalized)) {
            liveClassSessionMapper.markEnded(session.getId(), null, "ROOM_FINISHED", LocalDateTime.now());
            appendEvent(session.getId(), "ROOM_FINISHED", "WEBHOOK", null, request);
        }
        liveClassWebhookEventMapper.markProcessed(event.getId());
    }

    private void createParticipant(Long sessionId, Long uid, String role) {
        if (liveClassParticipantMapper.selectBySessionIdAndUid(sessionId, uid) != null) return;
        liveClassParticipantMapper.insert(LiveClassParticipant.builder()
                .sessionId(sessionId)
                .uid(uid)
                .role(role)
                .identityType("HUMAN")
                .joinCount(0)
                .onlineStatus("NOT_JOINED")
                .cameraEnabled(Boolean.FALSE)
                .micEnabled(Boolean.FALSE)
                .build());
    }

    private void upsertParticipantJoin(Long sessionId, Long uid, Long teacherUid, IssueJoinTokenRequest request) {
        LiveClassParticipant participant = liveClassParticipantMapper.selectBySessionIdAndUid(sessionId, uid);
        LocalDateTime now = LocalDateTime.now();
        if (participant == null) {
            participant = LiveClassParticipant.builder()
                    .sessionId(sessionId)
                    .uid(uid)
                    .role(Objects.equals(uid, teacherUid) ? "TEACHER" : "STUDENT")
                    .identityType("HUMAN")
                    .joinCount(1)
                    .firstJoinAt(now)
                    .lastJoinAt(now)
                    .onlineStatus("JOINED")
                    .cameraEnabled(Boolean.TRUE)
                    .micEnabled(Boolean.TRUE)
                    .deviceInfoJson(JSONUtil.toJsonStr(Map.of("joinMode", defaulted(request.getJoinMode(), "AUDIO_VIDEO"), "clientType", request.getClientType())))
                    .build();
            liveClassParticipantMapper.insert(participant);
            return;
        }
        participant.setJoinCount((participant.getJoinCount() == null ? 0 : participant.getJoinCount()) + 1);
        participant.setFirstJoinAt(participant.getFirstJoinAt() == null ? now : participant.getFirstJoinAt());
        participant.setLastJoinAt(now);
        participant.setOnlineStatus("JOINED");
        participant.setCameraEnabled(Boolean.TRUE);
        participant.setMicEnabled(Boolean.TRUE);
        participant.setDeviceInfoJson(JSONUtil.toJsonStr(Map.of("joinMode", defaulted(request.getJoinMode(), "AUDIO_VIDEO"), "clientType", request.getClientType())));
        liveClassParticipantMapper.updateJoinState(participant);
    }

    private void upsertWebhookParticipantJoin(LiveClassSession session, Long uid, LiveKitWebhookRequest request) {
        LiveClassParticipant participant = liveClassParticipantMapper.selectBySessionIdAndUid(session.getId(), uid);
        LocalDateTime now = LocalDateTime.now();
        if (participant == null) {
            participant = LiveClassParticipant.builder()
                    .sessionId(session.getId())
                    .uid(uid)
                    .role(Objects.equals(uid, session.getTeacherUid()) ? "TEACHER" : "STUDENT")
                    .identityType("HUMAN")
                    .joinCount(1)
                    .firstJoinAt(now)
                    .lastJoinAt(now)
                    .onlineStatus("JOINED")
                    .cameraEnabled(Boolean.TRUE.equals(request.getCameraEnabled()))
                    .micEnabled(Boolean.TRUE.equals(request.getMicEnabled()))
                    .build();
            liveClassParticipantMapper.insert(participant);
            return;
        }
        participant.setJoinCount((participant.getJoinCount() == null ? 0 : participant.getJoinCount()) + 1);
        participant.setFirstJoinAt(participant.getFirstJoinAt() == null ? now : participant.getFirstJoinAt());
        participant.setLastJoinAt(now);
        participant.setOnlineStatus("JOINED");
        participant.setCameraEnabled(Boolean.TRUE.equals(request.getCameraEnabled()));
        participant.setMicEnabled(Boolean.TRUE.equals(request.getMicEnabled()));
        liveClassParticipantMapper.updateJoinState(participant);
    }

    private LiveClassSession requireByCourseId(Long courseId) {
        LiveClassSession session = liveClassSessionMapper.selectByCourseId(courseId);
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR, "未找到课堂");
        return session;
    }

    private LiveClassSession requireById(Long sessionId) {
        LiveClassSession session = liveClassSessionMapper.selectById(sessionId);
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR, "未找到课堂");
        return session;
    }

    private LiveClassSession requireParticipantSession(Long sessionId, Long uid) {
        LiveClassSession session = requireById(sessionId);
        ThrowUtils.throwIf(!Objects.equals(session.getTeacherUid(), uid) && !Objects.equals(session.getStudentUid(), uid), ErrorCode.NO_AUTH_ERROR);
        return session;
    }

    private void appendEvent(Long sessionId, String eventType, String source, Long operatorUid, Object payload) {
        liveClassEventMapper.insert(LiveClassEvent.builder()
                .sessionId(sessionId)
                .eventType(eventType)
                .eventSource(source)
                .operatorUid(operatorUid)
                .payloadJson(JSONUtil.toJsonStr(payload))
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private LiveSessionResp toResp(LiveClassSession session, Long viewerUid) {
        List<LiveClassParticipant> participants = liveClassParticipantMapper.listBySessionId(session.getId());
        Long peerUid = viewerUid == null ? null : Objects.equals(viewerUid, session.getTeacherUid()) ? session.getStudentUid() : session.getTeacherUid();
        boolean peerJoined = peerUid != null && participants.stream().anyMatch(it -> Objects.equals(it.getUid(), peerUid) && "JOINED".equals(it.getOnlineStatus()));
        return LiveSessionResp.builder()
                .sessionId(session.getId())
                .courseId(session.getCourseId())
                .status(session.getStatus())
                .joinOpenAt(session.getJoinOpenAt())
                .scheduledStartAt(session.getScheduledStartAt())
                .scheduledEndAt(session.getScheduledEndAt())
                .actualStartAt(session.getActualStartAt())
                .actualEndAt(session.getActualEndAt())
                .teacherUid(session.getTeacherUid())
                .studentUid(session.getStudentUid())
                .roomId(session.getRoomId())
                .provider(session.getProvider())
                .providerRoomName(session.getProviderRoomName())
                .canJoin(viewerUid == null ? null : canJoin(session, viewerUid))
                .joinableNow(joinableNow(session))
                .peerJoined(peerJoined)
                .peerOnline(peerJoined)
                .recordPolicy(session.getRecordPolicy())
                .aiPolicy(session.getAiPolicy())
                .build();
    }

    private boolean canJoin(LiveClassSession session, Long uid) {
        return Objects.equals(session.getTeacherUid(), uid) || Objects.equals(session.getStudentUid(), uid);
    }

    private boolean joinableNow(LiveClassSession session) {
        LocalDateTime now = LocalDateTime.now();
        if (session == null) return false;
        if (isTerminalStatus(session)) return false;
        if (session.getScheduledEndAt() != null && !now.isBefore(session.getScheduledEndAt())) return false;
        return session.getJoinOpenAt() == null || !now.isBefore(session.getJoinOpenAt());
    }

    private boolean isTerminalStatus(LiveClassSession session) {
        String status = session == null ? null : session.getStatus();
        if (blank(status)) return false;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "ENDED".equals(normalized) || "CANCELED".equals(normalized) || "EXPIRED".equals(normalized);
    }

    private String resolveJoinBlockedReason(LiveClassSession session, boolean joinableNow) {
        if (joinableNow) return null;
        if (isTerminalStatus(session)) return "课程已结束";
        LocalDateTime now = LocalDateTime.now();
        if (session.getScheduledEndAt() != null && !now.isBefore(session.getScheduledEndAt())) {
          return "课程已结束";
        }
        return "未到可入会时间";
    }

    private String buildRoomName(Long courseId) {
        return liveKitProperties.getRoomPrefix() + "-" + courseId;
    }

    private String resolvePeerDisplayName(LiveClassSession session, Long viewerUid) {
        Long peerUid = Objects.equals(viewerUid, session.getTeacherUid()) ? session.getStudentUid() : session.getTeacherUid();
        return resolveDisplayName(peerUid);
    }

    private String resolveDisplayName(Long uid) {
        if (uid == null) return "用户";
        List<AppointmentInternalFeignClient.UserSimpleDto> users = appointmentInternalFeignClient.batchUsers(String.valueOf(uid)).getData();
        if (users == null || users.isEmpty()) return "用户" + uid;
        AppointmentInternalFeignClient.UserSimpleDto dto = users.get(0);
        return blank(dto.getRealName()) ? defaulted(dto.getName(), "用户" + uid) : dto.getRealName();
    }

    private LessonPaymentAccessCheckInfo loadLessonJoinAccess(Long lessonId) {
        try {
            BaseResponse<LessonPaymentAccessCheckInfo> resp = appointmentInternalFeignClient.getLessonJoinAccess(lessonId);
            return resp == null ? null : resp.getData();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long resolveParticipantUid(LiveKitWebhookRequest request) {
        if (request.getUid() != null) return request.getUid();
        if (!blank(request.getParticipantIdentity())) {
            try {
                return Long.parseLong(request.getParticipantIdentity().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String defaulted(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
