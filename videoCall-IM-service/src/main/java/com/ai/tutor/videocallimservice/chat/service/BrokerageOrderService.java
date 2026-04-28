package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.common.event.InviteBrokeragePaidEvent;
import com.ai.tutor.common.integration.InviteSystemBenefitInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.ApplicationBrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokeragePayMethod;
import com.ai.tutor.videocallimservice.chat.domain.enums.CollaborationProposalStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateDirectBrokerageOrderReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SubmitBrokerageProofReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.BrokerageOrderVO;
import com.ai.tutor.videocallimservice.chat.config.BrokerageInfoFeeHotConfig;
import com.ai.tutor.videocallimservice.chat.mapper.ApplicationBrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.videocallimservice.integration.AppointmentInternalClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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

    @Resource
    private TutorApplicationMapper tutorApplicationMapper;

    @Resource
    private BizKpiMetrics bizKpiMetrics;

    @Resource
    private BrokerageInfoFeeHotConfig brokerageInfoFeeHotConfig;
    @Resource
    private ObjectProvider<AppointmentInternalClient> appointmentInternalClientProvider;

    @Value("${brokerage.amount-fen:19900}")
    private long defaultAmountFen;

    @Value("${brokerage.admin-token:}")
    private String adminToken;

    private static final int LESSON_HOURS = 2;
    private static final String PROMOTION_SYSTEM_INVITE = "SYSTEM_INVITE";
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
        long originalAmountFen = computeInfoFeeAmountFen(proposal);
        PromotionAmount promotionAmount = applySystemInvitePromotion(teacherUid, originalAmountFen);
        BrokerageOrder order = BrokerageOrder.builder()
                .proposalId(proposalId)
                .roomId(proposal.getRoomId())
                .payerUid(teacherUid)
                .amountFen(promotionAmount.amountFen())
                .originalAmountFen(promotionAmount.originalAmountFen())
                .discountAmountFen(promotionAmount.discountAmountFen())
                .promotionType(promotionAmount.promotionType())
                .promotionSnapshotJson(promotionAmount.snapshotJson())
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

    public BrokerageOrderVO createDirectOrder(CreateDirectBrokerageOrderReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        Long amountFen = req.getAmountFen();
        ThrowUtils.throwIf(amountFen == null || amountFen <= 0, ErrorCode.PARAMS_ERROR);

        if (req.getAppointmentId() != null) {
            log.info("Creating BrokerageOrder for Appointment ID: {}", req.getAppointmentId());
        }

        LocalDateTime now = LocalDateTime.now();
        BrokerageOrder order = BrokerageOrder.builder()
                .proposalId(0L) // 0 表示没有关联提案
                .roomId(0L)
                .payerUid(uid)
                .amountFen(amountFen)
                .originalAmountFen(amountFen)
                .discountAmountFen(0L)
                .status(BrokerageOrderStatus.PENDING.name())
                .createTime(now)
                .updateTime(now)
                .proofNote(req.getAppointmentId() != null ? "ApptID:" + req.getAppointmentId() : null)
                .build();
        brokerageOrderMapper.insert(order);
        ThrowUtils.throwIf(order.getId() == null, ErrorCode.OPERATION_ERROR);
        return toVO(order);
    }

    private long computeInfoFeeAmountFen(CollaborationProposal proposal) {
        if (brokerageInfoFeeHotConfig != null && brokerageInfoFeeHotConfig.isUnifiedEnabled()) {
            long v = brokerageInfoFeeHotConfig.getUnifiedAmountFen();
            ThrowUtils.throwIf(v <= 0L, ErrorCode.OPERATION_ERROR, "统一信息费配置不合法");
            return v;
        }
        if (proposal == null) {
            return defaultAmountFen;
        }
        Long pricePerHourFen = InfoFeeCalculator.resolveProposalHourlyPriceFen(proposal.getPricePerHour());
        if (pricePerHourFen == null || pricePerHourFen <= 0L) {
            return defaultAmountFen;
        }
        long amountFen = InfoFeeCalculator.computeFromHourlyPriceFen(pricePerHourFen, proposal.getFrequencyPerWeek(), LESSON_HOURS);
        return amountFen <= 0L ? defaultAmountFen : amountFen;
    }

    PromotionAmount applySystemInvitePromotion(Long teacherUid, long originalAmountFen) {
        if (originalAmountFen <= 0L) {
            return PromotionAmount.none(originalAmountFen);
        }
        AppointmentInternalClient internalClient = appointmentInternalClientProvider == null ? null : appointmentInternalClientProvider.getIfAvailable();
        if (internalClient == null || teacherUid == null) {
            return PromotionAmount.none(originalAmountFen);
        }
        try {
            InviteSystemBenefitInfo benefit = internalClient.getInviteSystemBenefit(teacherUid);
            if (benefit == null || !Boolean.TRUE.equals(benefit.getEnabled()) || !Boolean.TRUE.equals(benefit.getSystemInvited())) {
                return PromotionAmount.none(originalAmountFen);
            }
            double discountRate = benefit.getTutorInfoFeeDiscountRate() == null ? 1D : benefit.getTutorInfoFeeDiscountRate();
            if (discountRate <= 0D || discountRate >= 1D) {
                return PromotionAmount.none(originalAmountFen);
            }
            long amountFen = BigDecimal.valueOf(originalAmountFen)
                    .multiply(BigDecimal.valueOf(discountRate))
                    .setScale(0, RoundingMode.CEILING)
                    .longValue();
            amountFen = Math.max(1L, amountFen);
            long discountAmountFen = Math.max(0L, originalAmountFen - amountFen);
            String snapshot = String.format(
                    "{\"systemInviteCode\":\"%s\",\"discountRate\":%.4f,\"source\":\"appointment-system-benefit\"}",
                    jsonText(benefit.getSystemInviteCode()),
                    discountRate
            );
            /*
             * 企业规范：系统邀请码属于平台推广权益，订单内保留原价、优惠金额和快照，便于财务核对半价来源。
             */
            return new PromotionAmount(originalAmountFen, amountFen, discountAmountFen, PROMOTION_SYSTEM_INVITE, snapshot);
        } catch (Exception ex) {
            log.warn("system_invite_promotion_query_failed teacherUid={} message={}", teacherUid, ex.getMessage());
            return PromotionAmount.none(originalAmountFen);
        }
    }

    public boolean hasPaidOrderInRoom(Long roomId) {
        if (roomId == null) {
            return false;
        }
        BrokerageOrder paid = brokerageOrderMapper.selectPaidByRoomId(roomId);
        return paid != null;
    }

    public BrokerageOrderPayInfo getPayableOrder(Long brokerageOrderId, Long uid) {
        ThrowUtils.throwIf(brokerageOrderId == null || uid == null, ErrorCode.PARAMS_ERROR);

        BrokerageOrder order = brokerageOrderMapper.selectById(brokerageOrderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(order.getPayerUid()), ErrorCode.NO_AUTH_ERROR);

        if (BrokerageOrderStatus.PAID.name().equals(order.getStatus())) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "订单已支付");
        }
        if (BrokerageOrderStatus.CANCELED.name().equals(order.getStatus())) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "订单已取消");
        }
        if (!BrokerageOrderStatus.PENDING.name().equals(order.getStatus())) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "订单当前状态不可支付");
        }

        BrokerageOrderPayInfo info = new BrokerageOrderPayInfo();
        info.setOrderId(order.getId());
        info.setPayerUid(order.getPayerUid());
        info.setAmountFen(order.getAmountFen());
        info.setStatus(order.getStatus());
        info.setSubject("信息费支付");
        info.setBody("对接咨询费支付");
        info.setApplicationId(order.getApplicationId());
        return info;
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
        notifyInviteBrokeragePaid(latest);
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
        log.info("brokerage_payment_success start orderId={} payMethod={} paidAt={}", orderId, payMethod, paidAt);
        BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);

        boolean transitionedToPaid = false;
        if (!BrokerageOrderStatus.PAID.name().equals(order.getStatus())) {
            LocalDateTime now = paidAt == null ? LocalDateTime.now() : paidAt;
            int updated = brokerageOrderMapper.markPaidWithMethod(orderId, now, normalizePayMethod(payMethod));
            transitionedToPaid = updated > 0;
            order = brokerageOrderMapper.selectById(orderId);
            ThrowUtils.throwIf(order == null, ErrorCode.OPERATION_ERROR);
        }

        if (transitionedToPaid && bizKpiMetrics != null) {
            /*
             * Grafana 业务 KPI 指标打点（达成合作次数）。
             * - metric(合作): ai_tutor_biz_collaboration_success_total（按发起方 initiator）
             * - PromQL（按天，合作数）：sum(increase(ai_tutor_biz_collaboration_success_total[1d]))
             *
             * 说明：支付金额类指标已统一交给 payment-service 负责，这里只保留“合作达成”口径，
             * 并且仅在订单首次从非 PAID -> PAID 的状态迁移成功时计入，避免 MQ 重放重复计数。
             */
            bizKpiMetrics.incCollaborationSuccess(resolveInitiatorLower(order));
        }

        afterPaid(order);
        notifyInviteBrokeragePaid(order);
        log.info("brokerage_payment_success done orderId={} status={} applicationId={} roomId={}",
                order.getId(), order.getStatus(), order.getApplicationId(), order.getRoomId());
    }

    private String resolveInitiatorLower(BrokerageOrder order) {
        if (order == null) {
            return "unknown";
        }
        Long applicationId = order.getApplicationId();
        if (applicationId != null && tutorApplicationMapper != null) {
            TutorApplication app = tutorApplicationMapper.selectById(applicationId);
            if (app != null) {
                return toRoleLower(app.getSenderRole());
            }
        }
        return "unknown";
    }

    private static String toRoleLower(String role) {
        if (role == null) {
            return "unknown";
        }
        String v = role.trim().toUpperCase();
        if ("TEACHER".equals(v)) return "teacher";
        if ("STUDENT".equals(v)) return "student";
        if ("ORG".equals(v)) return "org";
        return "unknown";
    }

    public void sendBrokerageRequired(Long roomId, Long proposalId, BrokerageOrderVO order, Long senderUid) {
        ThrowUtils.throwIf(roomId == null || proposalId == null || order == null || senderUid == null, ErrorCode.PARAMS_ERROR);
        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("BROKERAGE_REQUIRED");
        body.setEventId(order.getId());
        body.setProposalId(proposalId);
        body.setTitle("信息费支付");
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
        body.setTitle("聊天功能开启");
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
            return;
        }
    }

    private void notifyInviteBrokeragePaid(BrokerageOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        AppointmentInternalClient internalClient = appointmentInternalClientProvider == null ? null : appointmentInternalClientProvider.getIfAvailable();
        if (internalClient == null) {
            return;
        }
        try {
            InviteBrokeragePaidEvent event = buildInviteBrokeragePaidEvent(order);
            if (event.getTeacherUid() == null || event.getStudentUid() == null) {
                return;
            }
            internalClient.notifyInviteBrokeragePaid(event);
        } catch (Exception ex) {
            // 邀请返利为支付后的异步权益，不阻断订单已支付、聊天解锁等主链路。
            log.warn("invite_brokerage_paid_notify_failed orderId={} message={}", order.getId(), ex.getMessage());
        }
    }

    private InviteBrokeragePaidEvent buildInviteBrokeragePaidEvent(BrokerageOrder order) {
        InviteBrokeragePaidEvent event = new InviteBrokeragePaidEvent();
        event.setBrokerageOrderId(order.getId());
        event.setProposalId(order.getProposalId());
        event.setApplicationId(order.getApplicationId());
        event.setRoomId(order.getRoomId());
        event.setPayerUid(order.getPayerUid());
        event.setAmountFen(order.getAmountFen());
        event.setPayMethod(order.getPayMethod());
        event.setPaidAt(order.getPaidAt());
        event.setSource("videoCall-IM-service");

        if (order.getApplicationId() != null) {
            TutorApplication application = tutorApplicationMapper.selectById(order.getApplicationId());
            fillTeacherStudentFromApplication(event, application);
            return event;
        }
        if (order.getProposalId() != null && order.getProposalId() > 0 && order.getRoomId() != null) {
            fillTeacherStudentFromRoom(event, order.getRoomId());
            return event;
        }
        if (order.getRoomId() != null) {
            fillTeacherStudentFromRoom(event, order.getRoomId());
        }
        return event;
    }

    private void fillTeacherStudentFromApplication(InviteBrokeragePaidEvent event, TutorApplication application) {
        if (application == null) {
            return;
        }
        if ("TEACHER".equalsIgnoreCase(application.getSenderRole())) {
            event.setTeacherUid(application.getSenderUid());
            event.setStudentUid(application.getReceiverUid());
        } else if ("TEACHER".equalsIgnoreCase(application.getReceiverRole())) {
            event.setTeacherUid(application.getReceiverUid());
            event.setStudentUid(application.getSenderUid());
        }
    }

    private void fillTeacherStudentFromRoom(InviteBrokeragePaidEvent event, Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            return;
        }
        event.setTeacherUid(teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId()));
        event.setStudentUid(studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId()));
    }

    private static BrokerageOrderVO toVO(BrokerageOrder order) {
        if (order == null) return null;
        return BrokerageOrderVO.builder()
                .id(order.getId())
                .proposalId(order.getProposalId())
                .roomId(order.getRoomId())
                .payerUid(order.getPayerUid())
                .amountFen(order.getAmountFen())
                .originalAmountFen(order.getOriginalAmountFen())
                .discountAmountFen(order.getDiscountAmountFen())
                .promotionType(order.getPromotionType())
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

    private static String jsonText(String value) {
        return trim(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static class PromotionAmount {
        private final long originalAmountFen;
        private final long amountFen;
        private final long discountAmountFen;
        private final String promotionType;
        private final String snapshotJson;

        PromotionAmount(long originalAmountFen, long amountFen, long discountAmountFen, String promotionType, String snapshotJson) {
            this.originalAmountFen = originalAmountFen;
            this.amountFen = amountFen;
            this.discountAmountFen = discountAmountFen;
            this.promotionType = promotionType;
            this.snapshotJson = snapshotJson;
        }

        long originalAmountFen() {
            return originalAmountFen;
        }

        long amountFen() {
            return amountFen;
        }

        long discountAmountFen() {
            return discountAmountFen;
        }

        String promotionType() {
            return promotionType;
        }

        String snapshotJson() {
            return snapshotJson;
        }

        static PromotionAmount none(long originalAmountFen) {
            return new PromotionAmount(originalAmountFen, originalAmountFen, 0L, null, null);
        }
    }

}
