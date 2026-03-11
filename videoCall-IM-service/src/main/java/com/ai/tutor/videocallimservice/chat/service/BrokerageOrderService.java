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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final int LESSON_HOURS = 2;
    private static final Pattern PRICE_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

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
        long amountFen = computeInfoFeeAmountFen(proposal);
        BrokerageOrder order = BrokerageOrder.builder()
                .proposalId(proposalId)
                .roomId(proposal.getRoomId())
                .payerUid(teacherUid)
                .amountFen(amountFen)
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

    private long computeInfoFeeAmountFen(CollaborationProposal proposal) {
        if (proposal == null) {
            return defaultAmountFen;
        }
        Integer frequencyPerWeek = proposal.getFrequencyPerWeek();
        BigDecimal rate = resolveInfoFeeRate(frequencyPerWeek);
        Long pricePerHourFen = parsePricePerHourFen(proposal.getPricePerHour());
        if (pricePerHourFen == null || pricePerHourFen <= 0L) {
            return defaultAmountFen;
        }
        long lessonsPerWeek = frequencyPerWeek == null || frequencyPerWeek <= 0 ? 1L : frequencyPerWeek.longValue();
        BigDecimal weeklyCourseFeeFen = BigDecimal.valueOf(pricePerHourFen)
                .multiply(BigDecimal.valueOf(LESSON_HOURS))
                .multiply(BigDecimal.valueOf(lessonsPerWeek));
        BigDecimal infoFeeFen = weeklyCourseFeeFen.multiply(rate).setScale(0, RoundingMode.CEILING);
        long out = infoFeeFen.longValue();
        return out <= 0L ? 1L : out;
    }

    private static BigDecimal resolveInfoFeeRate(Integer frequencyPerWeek) {
        int n = frequencyPerWeek == null ? 1 : frequencyPerWeek;
        if (n <= 1) return BigDecimal.ONE;
        if (n == 2) return new BigDecimal("0.9");
        if (n == 3) return new BigDecimal("0.8");
        if (n == 4) return new BigDecimal("0.7");
        if (n == 5) return new BigDecimal("0.6");
        return new BigDecimal("0.5");
    }

    private static Long parsePricePerHourFen(String pricePerHour) {
        String raw = pricePerHour == null ? "" : pricePerHour.trim();
        if (raw.isEmpty()) {
            return null;
        }
        Matcher m = PRICE_NUMBER.matcher(raw);
        if (!m.find()) {
            return null;
        }
        BigDecimal cny;
        try {
            cny = new BigDecimal(m.group(1));
        } catch (Exception ignored) {
            return null;
        }
        if (cny.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal fen = cny.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        return fen.longValue();
    }

    public boolean hasPaidOrderInRoom(Long roomId) {
        if (roomId == null) {
            return false;
        }
        BrokerageOrder paid = brokerageOrderMapper.selectPaidByRoomId(roomId);
        return paid != null;
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

    /**
     * 付款人撤单（撤销支付）。
     *
     * 允许状态：
     * - PENDING：待支付，可撤单
     * - PROOF_SUBMITTED：已提交凭证，可撤单（用于撤回误提交）
     *
     * 不允许状态：
     * - PAID / REJECTED / CANCELED：不允许重复撤单或撤销已完成交易
     */
    public BrokerageOrderVO cancel(Long orderId, Long uid) {
        ThrowUtils.throwIf(orderId == null || uid == null, ErrorCode.PARAMS_ERROR);
        BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(order.getPayerUid()), ErrorCode.NO_AUTH_ERROR);

        String status = order.getStatus();
        if (BrokerageOrderStatus.CANCELED.name().equals(status)) {
            return toVO(order);
        }
        ThrowUtils.throwIf(
                !BrokerageOrderStatus.PENDING.name().equals(status) && !BrokerageOrderStatus.PROOF_SUBMITTED.name().equals(status),
                ErrorCode.OPERATION_ERROR,
                "当前状态不允许撤单"
        );

        int updated = brokerageOrderMapper.cancel(orderId);
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

    /**
     * 支付成功事件驱动的订单完结逻辑（用于 MQ 消费场景）。
     *
     * <p>与管理端“确认到账”不同：该方法不做管理员 token 校验，而是依赖支付域已完成验签与金额校验，
     * 并通过 MQ 事件驱动业务域最终一致性。</p>
     *
     * <p>幂等要求：
     * - 重复消费同一支付成功事件不得导致状态回退或异常
     * - 若订单已是 PAID，仍需尽力确保后续“解锁/流转”逻辑被触发（避免上一次触发失败）</p>
     *
     * @param orderId 中介费订单ID
     * @param paidAt  支付时间
     * @param payMethod 支付方式（WECHAT/ALIPAY），可为空
     */
    public void onPaymentSuccess(Long orderId, LocalDateTime paidAt, String payMethod) {
        ThrowUtils.throwIf(orderId == null, ErrorCode.PARAMS_ERROR);
        BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);

        if (!BrokerageOrderStatus.PAID.name().equals(order.getStatus())) {
            LocalDateTime now = paidAt == null ? LocalDateTime.now() : paidAt;
            brokerageOrderMapper.markPaidWithMethod(orderId, now, normalizePayMethod(payMethod));
            order = brokerageOrderMapper.selectById(orderId);
            ThrowUtils.throwIf(order == null, ErrorCode.OPERATION_ERROR);
        }

        afterPaid(order);
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

    private void afterPaid(BrokerageOrder latest) {
        if (latest == null) {
            return;
        }
        if (latest.getProposalId() != null && latest.getProposalId() > 0) {
            sendContactUnlocked(latest);
            return;
        }
        if (latest.getApplicationId() != null) {
            tutorApplicationService.onBrokerageOrderPaid(latest.getApplicationId());
            return;
        }
        ApplicationBrokerageOrder rel = applicationBrokerageOrderMapper.selectByOrderId(latest.getId());
        if (rel != null && rel.getApplicationId() != null) {
            tutorApplicationService.onBrokerageOrderPaid(rel.getApplicationId());
        }
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
