package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.CollaborationProposalStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.RespondCollaborationProposalReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.BrokerageOrderVO;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.StudentJobPostingLiteMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.common.mapper.ImUserMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CollaborationProposalService {

    private static final long PROPOSAL_EXPIRE_HOURS = 12L;

    @Resource
    private CollaborationProposalMapper collaborationProposalMapper;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private ImUserMapper imUserMapper;
    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Resource
    private TutorApplicationMapper tutorApplicationMapper;
    @Resource
    private StudentJobPostingLiteMapper studentJobPostingLiteMapper;
    @Resource
    private ChatService chatService;
    @Resource
    private BrokerageOrderService brokerageOrderService;
    @Resource
    private CourseEnrollmentService courseEnrollmentService;
    @Resource
    private TutorApplicationService tutorApplicationService;
    @Resource
    private BizKpiMetrics bizKpiMetrics;
    @Autowired(required = false)
    private AppointmentInternalClient appointmentInternalClient;

    public Long createAndSend(CreateCollaborationProposalReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        normalizeTrialProposalReq(req);
        String clientRequestId = trimNullable(req.getClientRequestId());
        if (clientRequestId != null) {
            CollaborationProposal existingByClientRequest = collaborationProposalMapper.selectByFromUidAndClientRequestId(uid, clientRequestId);
            if (existingByClientRequest != null && existingByClientRequest.getId() != null) {
                return sendProposalMessage(existingByClientRequest, uid);
            }
        }
        // #region debug-point
        dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_create_entry\""
                + ",\"uid\":" + uid
                + ",\"roomId\":" + numOrNull(req.getRoomId())
                + ",\"pricePerHour\":\"" + esc(req.getPricePerHour()) + "\""
                + ",\"classTime\":\"" + esc(req.getClassTime()) + "\""
                + ",\"frequencyPerWeek\":" + (req.getFrequencyPerWeek() == null ? "null" : req.getFrequencyPerWeek())
                + "}");
        // #endregion debug-point
        Room room = roomMapper.selectById(req.getRoomId());
        // #region debug-point
        if (room == null) {
            dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_create_guard_fail\""
                    + ",\"stage\":\"room_missing\""
                    + ",\"uid\":" + uid
                    + ",\"roomId\":" + numOrNull(req.getRoomId())
                    + "}");
        } else if (room.getStatus() == null || room.getStatus() != 1) {
            dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_create_guard_fail\""
                    + ",\"stage\":\"room_inactive\""
                    + ",\"uid\":" + uid
                    + ",\"roomId\":" + numOrNull(req.getRoomId())
                    + ",\"roomStatus\":" + numOrNull(room.getStatus() == null ? null : Long.valueOf(room.getStatus()))
                    + ",\"teacherProfileId\":" + numOrNull(room.getTeacherProfileId())
                    + ",\"studentProfileId\":" + numOrNull(room.getStudentProfileId())
                    + "}");
        }
        // #endregion debug-point
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);

        Long teacherUid = resolveUserId(1, room.getTeacherProfileId());
        Long studentUid = resolveUserId(2, room.getStudentProfileId());
        // #region debug-point
        if (teacherUid == null || studentUid == null) {
            dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_create_guard_fail\""
                    + ",\"stage\":\"resolve_user_fail\""
                    + ",\"uid\":" + uid
                    + ",\"roomId\":" + numOrNull(req.getRoomId())
                    + ",\"teacherProfileId\":" + numOrNull(room.getTeacherProfileId())
                    + ",\"studentProfileId\":" + numOrNull(room.getStudentProfileId())
                    + ",\"teacherUid\":" + numOrNull(teacherUid)
                    + ",\"studentUid\":" + numOrNull(studentUid)
                    + "}");
        } else if (!uid.equals(teacherUid) && !uid.equals(studentUid)) {
            dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_create_guard_fail\""
                    + ",\"stage\":\"uid_not_in_room\""
                    + ",\"uid\":" + uid
                    + ",\"roomId\":" + numOrNull(req.getRoomId())
                    + ",\"teacherUid\":" + numOrNull(teacherUid)
                    + ",\"studentUid\":" + numOrNull(studentUid)
                    + "}");
        }
        // #endregion debug-point
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);
        tutorApplicationService.assertRoomReadyForScheduling(req.getRoomId(), uid);

        CollaborationProposal existing = collaborationProposalMapper.selectLatestByRoomId(req.getRoomId());
        if (existing != null) {
            String status = existing.getStatus();
            if (CollaborationProposalStatus.REJECTED.name().equals(status)) {
            } else if (CollaborationProposalStatus.PENDING.name().equals(status)) {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "对方尚未处理上一次提案，可修改提案后重新发送");
            } else if (CollaborationProposalStatus.ACCEPTED.name().equals(status)) {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "该会话已同意过合作提案，无法再次发起");
            } else {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "该会话已发起过合作提案");
            }
        }

        Long toUid = uid.equals(teacherUid) ? studentUid : teacherUid;
        assertTrialScheduleAvailable(uid, toUid, req.getTrialStartAt(), req.getTrialEndAt());
        LocalDateTime now = LocalDateTime.now();
        // 过渡期先将 12 小时有效期写入系统消息，后续表结构迁移后会持久化 expire_at 并由定时任务扫描过期。
        LocalDateTime expireAt = now.plus(PROPOSAL_EXPIRE_HOURS, ChronoUnit.HOURS);

        CollaborationProposal proposal = CollaborationProposal.builder()
                .roomId(req.getRoomId())
                .fromUid(uid)
                .toUid(toUid)
                .pricePerHour(req.getPricePerHour())
                .classTime(req.getClassTime())
                .frequencyPerWeek(req.getFrequencyPerWeek())
                .trialStartAt(toLocalDateTime(req.getTrialStartAt()))
                .trialEndAt(toLocalDateTime(req.getTrialEndAt()))
                .remark(trimTo1024(req.getRemark()))
                .expireAt(expireAt)
                .clientRequestId(clientRequestId)
                .status(CollaborationProposalStatus.PENDING.name())
                .createTime(now)
                .updateTime(now)
                .build();
        collaborationProposalMapper.insert(proposal);
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：试课合作提案只在真正插入成功后计数，命中 clientRequestId 幂等直接返回旧提案时不重复累计，
             * 这样看板上的提案量才等于真实发起量。
             */
            bizKpiMetrics.incTrialProposalCreated(uid.equals(teacherUid) ? "teacher" : "student");
        }
        return sendProposalMessage(proposal, uid);
    }

    public Long updateAndSend(Long proposalId, CreateCollaborationProposalReq req, Long uid) {
        ThrowUtils.throwIf(proposalId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);
        normalizeTrialProposalReq(req);

        CollaborationProposal proposal = collaborationProposalMapper.selectById(proposalId);
        ThrowUtils.throwIf(proposal == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(proposal.getFromUid()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!CollaborationProposalStatus.PENDING.name().equals(proposal.getStatus()), ErrorCode.OPERATION_ERROR, "仅待确认提案可修改");
        ThrowUtils.throwIf(req.getRoomId() == null || !req.getRoomId().equals(proposal.getRoomId()), ErrorCode.PARAMS_ERROR);
        assertTrialScheduleAvailable(uid, proposal.getToUid(), req.getTrialStartAt(), req.getTrialEndAt());

        int updated = collaborationProposalMapper.updateContent(
                proposalId,
                uid,
                req.getPricePerHour(),
                req.getClassTime(),
                req.getFrequencyPerWeek(),
                toLocalDateTime(req.getTrialStartAt()),
                toLocalDateTime(req.getTrialEndAt()),
                trimTo1024(req.getRemark()),
                LocalDateTime.now().plus(PROPOSAL_EXPIRE_HOURS, ChronoUnit.HOURS)
        );
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL");
        body.setEventId(proposalId);
        body.setTitle("家教合作");
        body.setStatus(CollaborationProposalStatus.PENDING.name());
        body.setCreatorUserId(uid);
        body.setPricePerHour(req.getPricePerHour());
        body.setClassTime(req.getClassTime());
        body.setFrequencyPerWeek(req.getFrequencyPerWeek());
        body.setTrialStartAt(req.getTrialStartAt());
        body.setTrialEndAt(req.getTrialEndAt());
        body.setRemark(req.getRemark());
        body.setExpireAt(toEpochMilli(LocalDateTime.now().plus(PROPOSAL_EXPIRE_HOURS, ChronoUnit.HOURS)));

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(proposal.getRoomId())
                .msgType(8)
                .body(body)
                .build();
        return chatService.sendMsg(msgReq, uid);
    }

    @Scheduled(fixedDelayString = "${course.collaboration-proposal.scheduler-delay-ms:60000}")
    public void processExpiredProposals() {
        List<Long> expiredIds = collaborationProposalMapper.selectExpiredPendingIds(LocalDateTime.now(), 100);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return;
        }
        for (Long proposalId : expiredIds) {
            if (proposalId == null) {
                continue;
            }
            int updated = collaborationProposalMapper.updateStatus(
                    proposalId,
                    CollaborationProposalStatus.EXPIRED.name(),
                    0L,
                    LocalDateTime.now()
            );
            if (updated > 0 && bizKpiMetrics != null) {
                /*
                 * 中文注释：试课提案过期指标只在定时任务把待处理提案首次迁移为 EXPIRED 后统计，
                 * 避免任务重复扫描或并发执行时把同一条提案重复累计。
                 */
                bizKpiMetrics.incTrialProposalExpired();
            }
        }
    }

    public Long respondAndSend(Long proposalId, RespondCollaborationProposalReq req, Long uid) {
        ThrowUtils.throwIf(proposalId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);
        // #region debug-point
        dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_respond_entry\""
                + ",\"uid\":" + uid
                + ",\"proposalId\":" + numOrNull(proposalId)
                + ",\"action\":\"" + esc(req.getAction()) + "\""
                + "}");
        // #endregion debug-point
        CollaborationProposal proposal = collaborationProposalMapper.selectById(proposalId);
        // #region debug-point
        if (proposal == null) {
            dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_respond_guard_fail\""
                    + ",\"stage\":\"proposal_missing\""
                    + ",\"uid\":" + uid
                    + ",\"proposalId\":" + numOrNull(proposalId)
                    + "}");
        } else if (!uid.equals(proposal.getToUid())) {
            dbg("{\"ts\":\"" + Instant.now() + "\",\"event\":\"collab_respond_guard_fail\""
                    + ",\"stage\":\"uid_not_receiver\""
                    + ",\"uid\":" + uid
                    + ",\"proposalId\":" + numOrNull(proposalId)
                    + ",\"toUid\":" + numOrNull(proposal.getToUid())
                    + ",\"fromUid\":" + numOrNull(proposal.getFromUid())
                    + ",\"roomId\":" + numOrNull(proposal.getRoomId())
                    + "}");
        }
        // #endregion debug-point
        ThrowUtils.throwIf(proposal == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(proposal.getToUid()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(isExpiredPending(proposal), ErrorCode.OPERATION_ERROR, "该合作提案已超过12小时有效期，请重新发起");

        String action = req.getAction() == null ? "" : req.getAction().trim().toUpperCase();
        CollaborationProposalStatus next;
        if ("ACCEPT".equals(action)) {
            next = CollaborationProposalStatus.ACCEPTED;
        } else if ("REJECT".equals(action)) {
            next = CollaborationProposalStatus.REJECTED;
        } else {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = collaborationProposalMapper.updateStatus(proposalId, next.name(), uid, now);
        if (updated <= 0) {
            CollaborationProposal latest = collaborationProposalMapper.selectById(proposalId);
            ThrowUtils.throwIf(latest == null, ErrorCode.NOT_FOUND_ERROR);
            if (uid.equals(latest.getToUid()) && next.name().equals(latest.getStatus())) {
                return sendStatusMessage(latest, next, uid);
            }
            ThrowUtils.throwIf(!CollaborationProposalStatus.PENDING.name().equals(latest.getStatus()),
                    ErrorCode.OPERATION_ERROR,
                    "该试课提案已被处理，请刷新页面查看最新状态");
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR);
        }
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：提案处理结果只在待处理提案首次成功流转后统计，避免重复点击“同意/拒绝”造成双计数。
             */
            bizKpiMetrics.incTrialProposalDecision(CollaborationProposalStatus.ACCEPTED.equals(next) ? "accepted" : "rejected");
        }

        if (CollaborationProposalStatus.ACCEPTED.equals(next) && proposal.getRoomId() != null) {
            TutorApplication application = tutorApplicationMapper.selectLatestByRoomId(proposal.getRoomId());
            if (application != null && "DEMAND".equalsIgnoreCase(application.getContextType()) && application.getContextId() != null) {
                studentJobPostingLiteMapper.updateBizStatus(application.getContextId(), 4);
            }
            if (courseEnrollmentService != null) {
                courseEnrollmentService.onCollaborationAccepted(proposal.getRoomId(), proposalId);
            }
        }

        return sendStatusMessage(proposal, next, uid);
    }

    private Long sendStatusMessage(CollaborationProposal proposal, CollaborationProposalStatus next, Long uid) {
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL_STATUS");
        body.setEventId(proposal.getId());
        body.setTitle("家教合作");
        body.setStatus(next.name());
        body.setActorUserId(uid);

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(proposal.getRoomId())
                .msgType(8)
                .body(body)
                .build();
        Long statusMsgId = chatService.sendMsg(msgReq, uid);
        if (CollaborationProposalStatus.ACCEPTED.equals(next)) {
            if (brokerageOrderService.hasPaidOrderInRoom(proposal.getRoomId())) {
                return statusMsgId;
            }
            BrokerageOrderVO order = brokerageOrderService.getOrCreateByProposal(proposal.getId(), uid);
            brokerageOrderService.sendBrokerageRequired(proposal.getRoomId(), proposal.getId(), order, uid);
        }
        return statusMsgId;
    }

    private Long resolveUserId(int userType, Long refId) {
        if (refId == null) {
            return null;
        }
        Long userId;
        if (userType == 1) {
            userId = teacherProfileLiteMapper.selectUserIdById(refId);
        } else if (userType == 2) {
            userId = studentProfileLiteMapper.selectUserIdById(refId);
        } else {
            return null;
        }
        if (userId == null) {
            return null;
        }
        ImUser user = imUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            return null;
        }
        return user.getId();
    }

    private static void normalizeTrialProposalReq(CreateCollaborationProposalReq req) {
        ThrowUtils.throwIf(req.getPricePerHour() == null || req.getPricePerHour().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "请填写课时费");
        Long start = req.getTrialStartAt();
        Long end = req.getTrialEndAt();
        if (start != null || end != null) {
            ThrowUtils.throwIf(start == null || end == null || end <= start, ErrorCode.PARAMS_ERROR, "请选择有效的试课时间");
            // 兼容当前数据库仍保存 class_time 的过渡期，后续会迁移为独立 trial_start_at/trial_end_at 字段。
            req.setClassTime(formatTrialTime(start, end));
            if (req.getFrequencyPerWeek() == null || req.getFrequencyPerWeek() <= 0) {
                req.setFrequencyPerWeek(1);
            }
            return;
        }
        ThrowUtils.throwIf(req.getClassTime() == null || req.getClassTime().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "请选择试课时间");
        if (req.getFrequencyPerWeek() == null || req.getFrequencyPerWeek() <= 0) {
            req.setFrequencyPerWeek(1);
        }
    }

    private static boolean isExpiredPending(CollaborationProposal proposal) {
        if (proposal == null || proposal.getCreateTime() == null) {
            return false;
        }
        if (!CollaborationProposalStatus.PENDING.name().equals(proposal.getStatus())) {
            return false;
        }
        LocalDateTime expireAt = proposal.getExpireAt() == null
                ? proposal.getCreateTime().plus(PROPOSAL_EXPIRE_HOURS, ChronoUnit.HOURS)
                : proposal.getExpireAt();
        return LocalDateTime.now().isAfter(expireAt);
    }

    private void assertTrialScheduleAvailable(Long uid, Long otherUid, Long startAt, Long endAt) {
        if (appointmentInternalClient == null || startAt == null || endAt == null) {
            return;
        }
        appointmentInternalClient.assertNoScheduleConflict(uid, otherUid, startAt, endAt);
    }

    private static Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }

    private Long sendProposalMessage(CollaborationProposal proposal, Long uid) {
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL");
        body.setEventId(proposal.getId());
        body.setTitle("试课合作");
        body.setStatus(proposal.getStatus());
        body.setCreatorUserId(proposal.getFromUid());
        body.setPricePerHour(proposal.getPricePerHour());
        body.setClassTime(proposal.getClassTime());
        body.setFrequencyPerWeek(proposal.getFrequencyPerWeek());
        body.setTrialStartAt(toEpochMilli(proposal.getTrialStartAt()));
        body.setTrialEndAt(toEpochMilli(proposal.getTrialEndAt()));
        body.setRemark(proposal.getRemark());
        body.setExpireAt(toEpochMilli(proposal.getExpireAt()));

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(proposal.getRoomId())
                .msgType(8)
                .body(body)
                .build();
        return chatService.sendMsg(msgReq, uid);
    }

    private static LocalDateTime toLocalDateTime(Long epochMs) {
        if (epochMs == null) {
            return null;
        }
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
    }

    private static String trimNullable(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        return s.isEmpty() ? null : s;
    }

    private static String trimTo1024(String v) {
        String s = trimNullable(v);
        if (s == null) {
            return null;
        }
        return s.length() > 1024 ? s.substring(0, 1024) : s;
    }

    private static String formatTrialTime(Long startMs, Long endMs) {
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        java.time.format.DateTimeFormatter dateTime = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        java.time.format.DateTimeFormatter time = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        java.time.ZonedDateTime start = java.time.Instant.ofEpochMilli(startMs).atZone(zone);
        java.time.ZonedDateTime end = java.time.Instant.ofEpochMilli(endMs).atZone(zone);
        return start.format(dateTime) + " - " + end.format(time);
    }

    // #region debug-point
    private static String numOrNull(Long v) {
        return v == null ? "null" : String.valueOf(v);
    }

    private static void dbg(String json) {
        String dbgUrl = System.getProperty("TRAE_DEBUG_URL");
        if (dbgUrl == null || dbgUrl.isBlank()) {
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(dbgUrl.trim()))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpClient.newHttpClient().sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') out.append("\\\\");
            else if (c == '"') out.append("\\\"");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else out.append(c);
        }
        return out.toString();
    }
    // #endregion debug-point
}
