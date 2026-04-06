package com.ai.tutor.videocallimservice.chat.service;

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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class CollaborationProposalService {

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

    public Long createAndSend(CreateCollaborationProposalReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
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
        LocalDateTime now = LocalDateTime.now();

        CollaborationProposal proposal = CollaborationProposal.builder()
                .roomId(req.getRoomId())
                .fromUid(uid)
                .toUid(toUid)
                .pricePerHour(req.getPricePerHour())
                .classTime(req.getClassTime())
                .frequencyPerWeek(req.getFrequencyPerWeek())
                .status(CollaborationProposalStatus.PENDING.name())
                .createTime(now)
                .updateTime(now)
                .build();
        collaborationProposalMapper.insert(proposal);

        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL");
        body.setEventId(proposal.getId());
        body.setTitle("家教合作");
        body.setStatus(CollaborationProposalStatus.PENDING.name());
        body.setCreatorUserId(uid);
        body.setPricePerHour(req.getPricePerHour());
        body.setClassTime(req.getClassTime());
        body.setFrequencyPerWeek(req.getFrequencyPerWeek());

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(req.getRoomId())
                .msgType(8)
                .body(body)
                .build();
        return chatService.sendMsg(msgReq, uid);
    }

    public Long updateAndSend(Long proposalId, CreateCollaborationProposalReq req, Long uid) {
        ThrowUtils.throwIf(proposalId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);

        CollaborationProposal proposal = collaborationProposalMapper.selectById(proposalId);
        ThrowUtils.throwIf(proposal == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(proposal.getFromUid()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!CollaborationProposalStatus.PENDING.name().equals(proposal.getStatus()), ErrorCode.OPERATION_ERROR, "仅待确认提案可修改");
        ThrowUtils.throwIf(req.getRoomId() == null || !req.getRoomId().equals(proposal.getRoomId()), ErrorCode.PARAMS_ERROR);

        int updated = collaborationProposalMapper.updateContent(
                proposalId,
                uid,
                req.getPricePerHour(),
                req.getClassTime(),
                req.getFrequencyPerWeek()
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

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(proposal.getRoomId())
                .msgType(8)
                .body(body)
                .build();
        return chatService.sendMsg(msgReq, uid);
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
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        if (CollaborationProposalStatus.ACCEPTED.equals(next) && proposal.getRoomId() != null) {
            TutorApplication application = tutorApplicationMapper.selectLatestByRoomId(proposal.getRoomId());
            if (application != null && "DEMAND".equalsIgnoreCase(application.getContextType()) && application.getContextId() != null) {
                studentJobPostingLiteMapper.updateBizStatus(application.getContextId(), 4);
            }
            if (courseEnrollmentService != null) {
                courseEnrollmentService.onCollaborationAccepted(proposal.getRoomId(), proposalId);
            }
        }

        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("COLLAB_PROPOSAL_STATUS");
        body.setEventId(proposalId);
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
            BrokerageOrderVO order = brokerageOrderService.getOrCreateByProposal(proposalId, uid);
            brokerageOrderService.sendBrokerageRequired(proposal.getRoomId(), proposalId, order, uid);
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
