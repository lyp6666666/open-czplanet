package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.ApplicationBrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokeragePayMethod;
import com.ai.tutor.videocallimservice.chat.domain.enums.CollaborationProposalStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SubmitBrokerageProofReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.BrokerageOrderVO;
import com.ai.tutor.videocallimservice.chat.mapper.ApplicationBrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BrokerageOrderService {

    @Resource
    private BrokerageOrderMapper brokerageOrderMapper;
    @Resource
    private CollaborationProposalMapper collaborationProposalMapper;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Resource
    private ChatService chatService;

    @Resource
    private ApplicationBrokerageOrderMapper applicationBrokerageOrderMapper;

    @Resource
    private TutorApplicationService tutorApplicationService;

    @Value("${brokerage.amount-fen:19900}")
    private long defaultAmountFen;

    @Value("${brokerage.admin-token:}")
    private String adminToken;

    public BrokerageOrderVO getOrCreateByProposal(Long proposalId, Long uid) {
        ThrowUtils.throwIf(proposalId == null || uid == null, ErrorCode.PARAMS_ERROR);
        CollaborationProposal proposal = collaborationProposalMapper.selectById(proposalId);
        ThrowUtils.throwIf(proposal == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!CollaborationProposalStatus.ACCEPTED.name().equals(proposal.getStatus()), ErrorCode.OPERATION_ERROR, "合作未同意");

        Room room = roomMapper.selectById(proposal.getRoomId());
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);
        Long teacherUid = teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());
        Long studentUid = studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid) && !uid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        BrokerageOrder existing = brokerageOrderMapper.selectByProposalId(proposalId);
        if (existing != null) {
            return toVO(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        BrokerageOrder order = BrokerageOrder.builder()
                .proposalId(proposalId)
                .roomId(proposal.getRoomId())
                .payerUid(teacherUid)
                .amountFen(defaultAmountFen)
                .status(BrokerageOrderStatus.PENDING.name())
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            brokerageOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            BrokerageOrder latest = brokerageOrderMapper.selectByProposalId(proposalId);
            ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
            return toVO(latest);
        }
        ThrowUtils.throwIf(order.getId() == null, ErrorCode.OPERATION_ERROR);
        return toVO(order);
    }

    public BrokerageOrderVO getById(Long orderId, Long uid) {
        ThrowUtils.throwIf(orderId == null || uid == null, ErrorCode.PARAMS_ERROR);
        BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(order.getPayerUid()), ErrorCode.NO_AUTH_ERROR);
        return toVO(order);
    }

    public BrokerageOrderVO submitProof(Long orderId, SubmitBrokerageProofReq req, Long uid) {
        ThrowUtils.throwIf(orderId == null || uid == null, ErrorCode.PARAMS_ERROR);
        BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(order.getPayerUid()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!BrokerageOrderStatus.PENDING.name().equals(order.getStatus()), ErrorCode.OPERATION_ERROR);

        String payMethod = normalizePayMethod(req == null ? null : req.getPayMethod());
        String proofUrl = trim(req == null ? null : req.getProofUrl());
        String proofNote = trim(req == null ? null : req.getProofNote());

        int updated = brokerageOrderMapper.submitProof(orderId, payMethod, proofUrl, proofNote);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
        BrokerageOrder latest = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
        return toVO(latest);
    }

    public BrokerageOrderVO markPaid(Long orderId, String token) {
        ThrowUtils.throwIf(orderId == null, ErrorCode.PARAMS_ERROR);
        String expected = adminToken == null ? "" : adminToken.trim();
        ThrowUtils.throwIf(expected.isEmpty() || token == null || !expected.equals(token.trim()), ErrorCode.NO_AUTH_ERROR);

        BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        LocalDateTime now = LocalDateTime.now();
        int updated = brokerageOrderMapper.markPaid(orderId, now);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        BrokerageOrder latest = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
        if (latest.getProposalId() != null && latest.getProposalId() > 0) {
            sendContactUnlocked(latest);
        } else {
            if (latest.getApplicationId() != null) {
                tutorApplicationService.onBrokerageOrderPaid(latest.getApplicationId());
            } else {
                ApplicationBrokerageOrder rel = applicationBrokerageOrderMapper.selectByOrderId(orderId);
                if (rel != null && rel.getApplicationId() != null) {
                    tutorApplicationService.onBrokerageOrderPaid(rel.getApplicationId());
                }
            }
        }
        return toVO(latest);
    }

    public void sendBrokerageRequired(Long roomId, Long proposalId, BrokerageOrderVO order, Long senderUid) {
        ThrowUtils.throwIf(roomId == null || proposalId == null || order == null || senderUid == null, ErrorCode.PARAMS_ERROR);
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("BROKERAGE_REQUIRED");
        body.setEventId(order.getId());
        body.setProposalId(proposalId);
        body.setTitle("中介费支付");
        body.setStatus(order.getStatus());
        body.setCreatorUserId(order.getPayerUid());
        body.setAmountFen(order.getAmountFen());

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(roomId)
                .msgType(8)
                .body(body)
                .build();
        chatService.sendMsg(msgReq, senderUid);
    }

    private void sendContactUnlocked(BrokerageOrder order) {
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("CONTACT_UNLOCKED");
        body.setEventId(order.getProposalId());
        body.setOrderId(order.getId());
        body.setProposalId(order.getProposalId());
        body.setTitle("联系方式已解锁");
        body.setStatus(BrokerageOrderStatus.PAID.name());
        body.setCreatorUserId(order.getPayerUid());

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(order.getRoomId())
                .msgType(8)
                .body(body)
                .build();
        chatService.sendMsg(msgReq, order.getPayerUid());
    }

    private static BrokerageOrderVO toVO(BrokerageOrder order) {
        if (order == null) return null;
        return BrokerageOrderVO.builder()
                .id(order.getId())
                .proposalId(order.getProposalId())
                .roomId(order.getRoomId())
                .payerUid(order.getPayerUid())
                .amountFen(order.getAmountFen())
                .payMethod(order.getPayMethod())
                .status(order.getStatus())
                .proofUrl(order.getProofUrl())
                .proofNote(order.getProofNote())
                .paidAt(order.getPaidAt())
                .build();
    }

    private static String normalizePayMethod(String raw) {
        String s = trim(raw);
        if (s.isEmpty()) return null;
        String u = s.toUpperCase();
        if (BrokeragePayMethod.WECHAT.name().equals(u)) return BrokeragePayMethod.WECHAT.name();
        if (BrokeragePayMethod.ALIPAY.name().equals(u)) return BrokeragePayMethod.ALIPAY.name();
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        return null;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

}
