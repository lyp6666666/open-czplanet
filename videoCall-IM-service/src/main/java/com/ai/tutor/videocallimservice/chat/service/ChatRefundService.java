package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.CourseEnrollment;
import com.ai.tutor.videocallimservice.chat.domain.entity.RefundRequest;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.CollaborationProposalStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.CourseEnrollmentStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.RefundRequestStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.RefundRequestType;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.RefundStateResp;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CourseEnrollmentMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RefundRequestMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChatRefundService {

    private static final String HOVER_CAN_APPLY = "申请退费意味着合作失败，聊天功能会立即关闭，相关费用将在6个小时内退回";
    private static final String HOVER_DISABLE_AFTER_COLLAB = "发起合作后无法再退换信息费用，请尽快完成试课，并前往我的课程中查看详情";

    @Resource
    private RoomMapper roomMapper;
    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Resource
    private BrokerageOrderMapper brokerageOrderMapper;
    @Resource
    private CollaborationProposalMapper collaborationProposalMapper;
    @Resource
    private RefundRequestMapper refundRequestMapper;
    @Resource
    private ChatService chatService;
    @Resource
    private CourseEnrollmentMapper courseEnrollmentMapper;

    public RefundStateResp getRefundState(Long roomId, Long uid) {
        ThrowUtils.throwIf(roomId == null || uid == null, ErrorCode.PARAMS_ERROR);
        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(room.getStatus() == null || room.getStatus() != 1, ErrorCode.OPERATION_ERROR, "聊天已关闭");

        Long teacherUid = teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());
        Long studentUid = studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        BrokerageOrder order = brokerageOrderMapper.selectPaidByRoomId(roomId);
        if (order == null || order.getId() == null) {
            return RefundStateResp.builder()
                    .canApply(false)
                    .disableReasonCode("NOT_PAID")
                    .hoverText("")
                    .build();
        }
        if (!uid.equals(order.getPayerUid())) {
            return RefundStateResp.builder()
                    .canApply(false)
                    .disableReasonCode("ONLY_PAYER")
                    .hoverText("")
                    .build();
        }

        CollaborationProposal proposal = collaborationProposalMapper.selectLatestByRoomId(roomId);
        if (proposal != null && CollaborationProposalStatus.ACCEPTED.name().equals(proposal.getStatus())) {
            return RefundStateResp.builder()
                    .canApply(false)
                    .disableReasonCode("COLLAB_ACCEPTED")
                    .hoverText(HOVER_DISABLE_AFTER_COLLAB)
                    .build();
        }

        RefundRequest pending = refundRequestMapper.selectPendingByBrokerageOrderId(order.getId());
        if (pending != null) {
            return RefundStateResp.builder()
                    .canApply(false)
                    .disableReasonCode("PENDING_REQUEST")
                    .hoverText("退费申请已提交，请等待管理员审核")
                    .build();
        }

        return RefundStateResp.builder()
                .canApply(true)
                .disableReasonCode("")
                .hoverText(HOVER_CAN_APPLY)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatMessageResp applyChatRefund(Long roomId, String reason, Long uid) {
        RefundStateResp state = getRefundState(roomId, uid);
        ThrowUtils.throwIf(!state.isCanApply(), ErrorCode.OPERATION_ERROR, state.getHoverText() == null ? "当前不可申请退费" : state.getHoverText());

        BrokerageOrder order = brokerageOrderMapper.selectPaidByRoomId(roomId);
        ThrowUtils.throwIf(order == null || order.getId() == null, ErrorCode.NOT_FOUND_ERROR);

        RefundRequest pending = refundRequestMapper.selectPendingByBrokerageOrderId(order.getId());
        ThrowUtils.throwIf(pending != null, ErrorCode.OPERATION_ERROR, "退费申请已提交，请等待管理员审核");

        int locked = brokerageOrderMapper.lockForRefund(order.getId(), BrokerageOrderStatus.REFUND_REVIEW.name());
        if (locked <= 0) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "订单状态不可申请退费");
        }

        LocalDateTime now = LocalDateTime.now();
        RefundRequest req = RefundRequest.builder()
                .brokerageOrderId(order.getId())
                .courseId(null)
                .roomId(roomId)
                .applicantUid(uid)
                .applicantRole("TEACHER")
                .type(RefundRequestType.CHAT_INFO_FEE.name())
                .status(RefundRequestStatus.PENDING.name())
                .reason(trimTo1024(reason))
                .evidenceImagesJson(null)
                .refundPercent(100)
                .refundAmountFen(order.getAmountFen())
                .adminUid(null)
                .adminNote(null)
                .decidedAt(null)
                .createTime(now)
                .updateTime(now)
                .build();
        refundRequestMapper.insert(req);
        ThrowUtils.throwIf(req.getId() == null, ErrorCode.OPERATION_ERROR);

        tryUpdateCourseStatusForRoom(roomId, CourseEnrollmentStatus.REFUND_REVIEW.name());

        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("BROKERAGE_REFUND_REQUEST");
        body.setEventId(req.getId());
        body.setTitle("结束沟通");
        body.setStatus("PENDING_REVIEW");
        body.setCreatorUserId(uid);
        body.setOrderId(order.getId());
        body.setProposalId(order.getProposalId());

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(roomId)
                .msgType(8)
                .body(body)
                .build();
        Long msgId = chatService.sendMsg(msgReq, uid);
        return chatService.getMsgResp(msgId, uid);
    }

    private void tryUpdateCourseStatusForRoom(Long roomId, String nextStatus) {
        if (roomId == null) {
            return;
        }
        CourseEnrollment enrollment = courseEnrollmentMapper.selectLatestByRoomId(roomId);
        if (enrollment == null || enrollment.getId() == null || enrollment.getStatus() == null) {
            return;
        }
        courseEnrollmentMapper.updateStatus(enrollment.getId(), enrollment.getStatus(), nextStatus, null, null, null);
    }

    private static String trimTo1024(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= 1024) return v;
        return v.substring(0, 1024);
    }
}
