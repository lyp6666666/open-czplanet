package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.ApplicationBrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.StudentJobPostingLite;
import com.ai.tutor.videocallimservice.chat.domain.entity.TutorApplication;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.TutorApplicationChatAccessStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.TutorApplicationStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CreateTutorApplicationReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.DecideTutorApplicationReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.TutorApplicationPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.SystemMsgReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatMessageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationEnterResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationUnreadResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.TutorApplicationVO;
import com.ai.tutor.videocallimservice.chat.mapper.ApplicationBrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.StudentJobPostingLiteMapper;
import com.ai.tutor.videocallimservice.chat.mapper.TutorApplicationMapper;
import com.ai.tutor.videocallimservice.chat.service.stream.SseSessionManager;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TutorApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TutorApplicationService.class);
    private static final int DAILY_CREATE_LIMIT = 5;
    private static final int LESSON_HOURS = 2;

    @Resource
    private TutorApplicationMapper tutorApplicationMapper;
    @Resource
    private BrokerageOrderMapper brokerageOrderMapper;
    @Resource
    private ApplicationBrokerageOrderMapper applicationBrokerageOrderMapper;
    @Resource
    private ChatRoomService chatRoomService;
    @Resource
    private ChatService chatService;

    @Resource
    private StudentJobPostingLiteMapper studentJobPostingLiteMapper;

    @Resource
    private SseSessionManager sseSessionManager;

    @Value("${brokerage.amount-fen:19900}")
    private long defaultAmountFen;

    @Value("${tutor-application.skip-payment-check:false}")
    private boolean skipPaymentCheck;

    public TutorApplicationVO create(CreateTutorApplicationReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(req.getReceiverUid() == null || uid.equals(req.getReceiverUid()), ErrorCode.PARAMS_ERROR);
        String content = trim(req.getContent());
        ThrowUtils.throwIf(content.isEmpty() || content.length() > 500, ErrorCode.PARAMS_ERROR);

        Integer role = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
        ThrowUtils.throwIf(role == null, ErrorCode.PARAMS_ERROR);
        String senderRole = normalizeRole(role);
        String receiverRole = Integer.valueOf(1).equals(role) ? "STUDENT" : "TEACHER";

        String contextType = trim(req.getContextType()).toUpperCase();
        ThrowUtils.throwIf(!"DEMAND".equals(contextType) && !"TUTOR".equals(contextType), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(req.getContextId() == null, ErrorCode.PARAMS_ERROR);

        String clientRequestId = trimNullable(req.getClientRequestId());
        if (clientRequestId != null) {
            TutorApplication existing = tutorApplicationMapper.selectBySenderAndClientRequestId(uid, clientRequestId);
            if (existing != null) {
                return toVO(existing, resolveOrderId(existing.getId()));
            }
        }

        TutorApplication latestSameContext = tutorApplicationMapper.selectLatestBySenderReceiverContext(uid, req.getReceiverUid(), contextType, req.getContextId());
        if (latestSameContext != null) {
            String status = latestSameContext.getStatus();
            String chatAccessStatus = latestSameContext.getChatAccessStatus();
            if (TutorApplicationStatus.PENDING.name().equals(status)) {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "对方尚未处理上一次申请，请等待对方回复或被拒绝后再申请");
            }
            if (TutorApplicationStatus.ACCEPTED.name().equals(status) && !TutorApplicationChatAccessStatus.CHAT_ENABLED.name().equals(chatAccessStatus)) {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "该需求申请流程尚未结束，请完成中介费支付后再继续");
            }
        }

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        Long todayCount = tutorApplicationMapper.countCreatedBySenderBetween(uid, dayStart, dayEnd);
        ThrowUtils.throwIf(todayCount != null && todayCount >= DAILY_CREATE_LIMIT, ErrorCode.OPERATION_ERROR, "今日申请已达上限(5次)，请明天再试");

        LocalDateTime now = LocalDateTime.now();
        TutorApplication application = TutorApplication.builder()
                .senderUid(uid)
                .receiverUid(req.getReceiverUid())
                .senderRole(senderRole)
                .receiverRole(receiverRole)
                .contextType(contextType)
                .contextId(req.getContextId())
                .content(content)
                .clientRequestId(clientRequestId)
                .status(TutorApplicationStatus.PENDING.name())
                .chatAccessStatus(TutorApplicationChatAccessStatus.NONE.name())
                .receiverRead(0)
                .receiverReadTime(null)
                .decidedAt(null)
                .roomId(null)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            tutorApplicationMapper.insert(application);
        } catch (DuplicateKeyException e) {
            if (clientRequestId != null) {
                TutorApplication latest = tutorApplicationMapper.selectBySenderAndClientRequestId(uid, clientRequestId);
                ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
                return toVO(latest, resolveOrderId(latest.getId()));
            }
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR);
        }
        ThrowUtils.throwIf(application.getId() == null, ErrorCode.OPERATION_ERROR);
        log.info("tutor_application_created applicationId={} senderUid={} receiverUid={} contextType={} contextId={}",
                application.getId(), application.getSenderUid(), application.getReceiverUid(), application.getContextType(), application.getContextId());
        sseSessionManager.sendToUid(application.getReceiverUid(), "application", Map.of(
                "type", "CREATED",
                "applicationId", application.getId(),
                "status", application.getStatus()
        ));
        return toVO(application, null);
    }

    public ChatMessageResp createAndSendToChat(CreateTutorApplicationReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        Long roomId = chatRoomService.getOrCreateRoomWithUser(req.getReceiverUid(), uid);
        TutorApplicationVO vo = create(req, uid);
        if (vo.getRoomId() == null) {
            tutorApplicationMapper.bindRoom(vo.getId(), roomId);
        }

        SystemMsgReq body = new SystemMsgReq();
        body.setBizType("TUTOR_APPLICATION");
        body.setEventId(vo.getId());
        body.setTitle("家教申请");
        body.setStatus(vo.getStatus());
        body.setCreatorUserId(uid);
        body.setContent(vo.getContent());
        body.setContextType(vo.getContextType());
        body.setContextId(vo.getContextId());

        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(roomId)
                .msgType(8)
                .body(body)
                .build();
        Long msgId = chatService.sendMsg(msgReq, uid);
        return chatService.getMsgResp(msgId, uid);
    }

    public CursorPageResp<TutorApplicationVO> listSent(TutorApplicationPageReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = req.getPageSize() == null ? 20 : req.getPageSize();
        List<TutorApplication> list = tutorApplicationMapper.listBySender(uid, req.getCursor(), pageSize);
        return toPage(list, pageSize);
    }

    public CursorPageResp<TutorApplicationVO> listReceived(TutorApplicationPageReq req, Long uid) {
        ThrowUtils.throwIf(req == null || uid == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = req.getPageSize() == null ? 20 : req.getPageSize();
        List<TutorApplication> list = tutorApplicationMapper.listByReceiver(uid, req.getCursor(), pageSize);
        return toPage(list, pageSize);
    }

    public TutorApplicationUnreadResp unread(Long uid) {
        ThrowUtils.throwIf(uid == null, ErrorCode.PARAMS_ERROR);
        Long count = tutorApplicationMapper.countUnreadByReceiver(uid);
        return new TutorApplicationUnreadResp(count == null ? 0L : count);
    }

    public TutorApplicationVO getDetail(Long applicationId, Long uid) {
        ThrowUtils.throwIf(applicationId == null || uid == null, ErrorCode.PARAMS_ERROR);
        TutorApplication application = tutorApplicationMapper.selectById(applicationId);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(application.getSenderUid()) && !uid.equals(application.getReceiverUid()), ErrorCode.NO_AUTH_ERROR);
        if (uid.equals(application.getReceiverUid())) {
            tutorApplicationMapper.markReceiverRead(applicationId, uid, LocalDateTime.now());
            application = tutorApplicationMapper.selectById(applicationId);
        }
        Long orderId = resolveOrderId(applicationId);
        return toVO(application, orderId);
    }

    @Transactional
    public TutorApplicationVO decide(Long applicationId, DecideTutorApplicationReq req, Long uid) {
        ThrowUtils.throwIf(applicationId == null || req == null || uid == null, ErrorCode.PARAMS_ERROR);
        TutorApplication application = tutorApplicationMapper.selectById(applicationId);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(application.getReceiverUid()), ErrorCode.NO_AUTH_ERROR);

        ensureRoom(application, uid);

        String action = trim(req.getAction()).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        if ("REJECT".equals(action)) {
            int updated = tutorApplicationMapper.decide(applicationId, uid, TutorApplicationStatus.REJECTED.name(), TutorApplicationChatAccessStatus.NONE.name(), now);
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
            TutorApplication latest = tutorApplicationMapper.selectById(applicationId);
            ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
            log.info("tutor_application_decided applicationId={} receiverUid={} status={}", latest.getId(), uid, latest.getStatus());
            sseSessionManager.sendToUid(latest.getSenderUid(), "application", Map.of(
                    "type", "DECIDED",
                    "applicationId", latest.getId(),
                    "status", latest.getStatus()
            ));
            return toVO(latest, null);
        }
        if (!"ACCEPT".equals(action)) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        }

        int updated = tutorApplicationMapper.decide(applicationId, uid, TutorApplicationStatus.ACCEPTED.name(), TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name(), now);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);

        TutorApplication latest = tutorApplicationMapper.selectById(applicationId);
        ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
        ensureRoom(latest, uid);
        Long orderId = getOrCreateBrokerageOrderForApplication(latest);
        log.info("tutor_application_decided applicationId={} receiverUid={} status={} orderId={}", latest.getId(), uid, latest.getStatus(), orderId);
        sseSessionManager.sendToUid(latest.getSenderUid(), "application", Map.of(
                "type", "DECIDED",
                "applicationId", latest.getId(),
                "status", latest.getStatus()
        ));
        return toVO(latest, orderId);
    }

    public ChatMessageResp decideAndSendToChat(Long applicationId, DecideTutorApplicationReq req, Long uid) {
        TutorApplicationVO vo = decide(applicationId, req, uid);
        TutorApplication latest = tutorApplicationMapper.selectById(applicationId);
        ThrowUtils.throwIf(latest == null, ErrorCode.OPERATION_ERROR);
        Long roomId = ensureRoom(latest, uid);

        SystemMsgReq statusBody = new SystemMsgReq();
        statusBody.setBizType("TUTOR_APPLICATION_STATUS");
        statusBody.setEventId(applicationId);
        statusBody.setTitle("家教申请");
        statusBody.setStatus(vo.getStatus());
        statusBody.setActorUserId(uid);

        Long statusMsgId = chatService.sendMsg(ChatMessageReq.builder().roomId(roomId).msgType(8).body(statusBody).build(), uid);

        if (TutorApplicationStatus.ACCEPTED.name().equals(vo.getStatus()) && vo.getOrderId() != null) {
            BrokerageOrder order = brokerageOrderMapper.selectById(vo.getOrderId());
            if (order != null) {
                SystemMsgReq payBody = new SystemMsgReq();
                payBody.setBizType("BROKERAGE_REQUIRED");
                payBody.setEventId(order.getId());
                payBody.setProposalId(applicationId);
                payBody.setAmountFen(order.getAmountFen());
                payBody.setStatus(order.getStatus());
                payBody.setCreatorUserId(order.getPayerUid());
                chatService.sendMsg(ChatMessageReq.builder().roomId(roomId).msgType(8).body(payBody).build(), order.getPayerUid());
            }
        }

        return chatService.getMsgResp(statusMsgId, uid);
    }

    public TutorApplicationEnterResp enterChat(Long applicationId, Long uid) {
        ThrowUtils.throwIf(applicationId == null || uid == null, ErrorCode.PARAMS_ERROR);
        TutorApplication application = tutorApplicationMapper.selectById(applicationId);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(application.getSenderUid()) && !uid.equals(application.getReceiverUid()), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!TutorApplicationStatus.ACCEPTED.name().equals(application.getStatus()), ErrorCode.OPERATION_ERROR, "申请未通过");

        Long orderId = resolveOrderId(applicationId);
        String access = application.getChatAccessStatus();
        if (TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name().equals(access) && !skipPaymentCheck) {
            Long teacherUid = resolveTeacherUid(application);
            if (uid.equals(teacherUid)) {
                return TutorApplicationEnterResp.builder()
                        .paymentRequired(true)
                        .waitingForTeacherPayment(false)
                        .orderId(orderId)
                        .roomId(null)
                        .build();
            }
            return TutorApplicationEnterResp.builder()
                    .paymentRequired(false)
                    .waitingForTeacherPayment(true)
                    .orderId(orderId)
                    .roomId(null)
                    .build();
        }
        if (TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name().equals(access) && skipPaymentCheck) {
            access = TutorApplicationChatAccessStatus.CHAT_ENABLED.name();
        }

        ThrowUtils.throwIf(!TutorApplicationChatAccessStatus.CHAT_ENABLED.name().equals(access), ErrorCode.OPERATION_ERROR);
        Long otherUid = uid.equals(application.getSenderUid()) ? application.getReceiverUid() : application.getSenderUid();
        Long roomId = application.getRoomId();
        if (roomId == null) {
            roomId = chatRoomService.getOrCreateRoomWithUser(otherUid, uid);
            tutorApplicationMapper.bindRoom(applicationId, roomId);
        }
        return TutorApplicationEnterResp.builder()
                .paymentRequired(false)
                .waitingForTeacherPayment(false)
                .orderId(orderId)
                .roomId(roomId)
                .build();
    }

    public void assertNotBlockedByApplication(Long uid, Long targetUid) {
        if (uid == null || targetUid == null || uid.equals(targetUid)) {
            return;
        }
        if (skipPaymentCheck) {
            return;
        }
        TutorApplication application = tutorApplicationMapper.selectLatestAcceptedBetween(uid, targetUid);
        if (application == null) {
            return;
        }
        if (TutorApplicationChatAccessStatus.PAYMENT_REQUIRED.name().equals(application.getChatAccessStatus())) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "请先在申请中完成中介费支付后再进入聊天");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(Long applicationId, String orderNo) {
        if (applicationId == null) {
            return;
        }
        log.info("payment_success_received applicationId={} orderNo={}", applicationId, orderNo);
        onBrokerageOrderPaid(applicationId);
    }

    public void onBrokerageOrderPaid(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        tutorApplicationMapper.updateChatAccessStatus(applicationId, TutorApplicationChatAccessStatus.CHAT_ENABLED.name());
        TutorApplication application = tutorApplicationMapper.selectById(applicationId);
        if (application != null) {
            log.info("tutor_application_paid applicationId={} senderUid={} receiverUid={}", applicationId, application.getSenderUid(), application.getReceiverUid());
            Map<String, Object> payload = Map.of(
                    "type", "PAID",
                    "applicationId", applicationId,
                    "chatAccessStatus", TutorApplicationChatAccessStatus.CHAT_ENABLED.name()
            );
            sseSessionManager.sendToUid(application.getSenderUid(), "application", payload);
            sseSessionManager.sendToUid(application.getReceiverUid(), "application", payload);
            if (application.getRoomId() != null) {
                Long teacherUid = resolveTeacherUid(application);
                SystemMsgReq body = new SystemMsgReq();
                body.setBizType("CONTACT_UNLOCKED");
                body.setEventId(application.getId());
                body.setProposalId(application.getId());
                body.setOrderId(resolveOrderId(applicationId));
                body.setTitle("联系方式已解锁");
                body.setStatus(BrokerageOrderStatus.PAID.name());
                body.setCreatorUserId(teacherUid);
                chatService.sendMsg(ChatMessageReq.builder().roomId(application.getRoomId()).msgType(8).body(body).build(), teacherUid);
            }
        }
    }

    private Long ensureRoom(TutorApplication application, Long uid) {
        ThrowUtils.throwIf(application == null || application.getId() == null || uid == null, ErrorCode.PARAMS_ERROR);
        Long roomId = application.getRoomId();
        if (roomId != null) {
            return roomId;
        }
        Long targetUid = uid.equals(application.getSenderUid()) ? application.getReceiverUid() : application.getSenderUid();
        roomId = chatRoomService.getOrCreateRoomWithUser(targetUid, uid);
        tutorApplicationMapper.bindRoom(application.getId(), roomId);
        application.setRoomId(roomId);
        return roomId;
    }

    private Long getOrCreateBrokerageOrderForApplication(TutorApplication application) {
        ThrowUtils.throwIf(application == null || application.getId() == null, ErrorCode.PARAMS_ERROR);
        ApplicationBrokerageOrder existingRel = applicationBrokerageOrderMapper.selectByApplicationId(application.getId());
        if (existingRel != null && existingRel.getOrderId() != null) {
            return existingRel.getOrderId();
        }

        Long teacherUid = resolveTeacherUid(application);
        LocalDateTime now = LocalDateTime.now();
        long amountFen = computeInfoFeeAmountFenForApplication(application);
        BrokerageOrder order = BrokerageOrder.builder()
                .proposalId(null)
                .applicationId(application.getId())
                .roomId(application.getRoomId())
                .payerUid(teacherUid)
                .amountFen(amountFen)
                .status(BrokerageOrderStatus.PENDING.name())
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            brokerageOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            BrokerageOrder latest = brokerageOrderMapper.selectByApplicationId(application.getId());
            ThrowUtils.throwIf(latest == null || latest.getId() == null, ErrorCode.OPERATION_ERROR);
            order = latest;
        }
        ThrowUtils.throwIf(order.getId() == null, ErrorCode.OPERATION_ERROR);

        ApplicationBrokerageOrder rel = ApplicationBrokerageOrder.builder()
                .applicationId(application.getId())
                .orderId(order.getId())
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            applicationBrokerageOrderMapper.insert(rel);
        } catch (DuplicateKeyException e) {
            ApplicationBrokerageOrder latest = applicationBrokerageOrderMapper.selectByApplicationId(application.getId());
            ThrowUtils.throwIf(latest == null || latest.getOrderId() == null, ErrorCode.OPERATION_ERROR);
            return latest.getOrderId();
        }
        return order.getId();
    }

    private long computeInfoFeeAmountFenForApplication(TutorApplication application) {
        if (application == null) {
            return defaultAmountFen;
        }
        if (!"DEMAND".equals(application.getContextType()) || application.getContextId() == null) {
            return defaultAmountFen;
        }
        StudentJobPostingLite demand = studentJobPostingLiteMapper.selectById(application.getContextId());
        if (demand == null) {
            return defaultAmountFen;
        }
        Integer frequencyPerWeek = demand.getFrequencyPerWeek();
        BigDecimal rate = resolveInfoFeeRate(frequencyPerWeek);
        BigDecimal hourlyCny = demand.getBudgetMax() != null ? demand.getBudgetMax() : demand.getBudgetMin();
        if (hourlyCny == null || hourlyCny.compareTo(BigDecimal.ZERO) <= 0) {
            return defaultAmountFen;
        }
        long lessonsPerWeek = frequencyPerWeek == null || frequencyPerWeek <= 0 ? 1L : frequencyPerWeek.longValue();
        BigDecimal weeklyCourseFeeFen = hourlyCny.multiply(BigDecimal.valueOf(100))
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

    private CursorPageResp<TutorApplicationVO> toPage(List<TutorApplication> list, Integer pageSize) {
        if (list == null) {
            return CursorPageResp.empty();
        }
        List<TutorApplicationVO> out = new ArrayList<>();
        for (TutorApplication a : list) {
            Long orderId = resolveOrderId(a == null ? null : a.getId());
            out.add(toVO(a, orderId));
        }
        Long nextCursor = out.isEmpty() ? null : out.get(out.size() - 1).getId();
        boolean isLast = list.size() < (pageSize == null ? 20 : pageSize);
        return new CursorPageResp<>(nextCursor, isLast, out);
    }

    private Long resolveOrderId(Long applicationId) {
        if (applicationId == null) return null;
        ApplicationBrokerageOrder rel = applicationBrokerageOrderMapper.selectByApplicationId(applicationId);
        return rel == null ? null : rel.getOrderId();
    }

    private static TutorApplicationVO toVO(TutorApplication application, Long orderId) {
        if (application == null) return null;
        Boolean receiverRead = application.getReceiverRead() != null && application.getReceiverRead() == 1;
        return TutorApplicationVO.builder()
                .id(application.getId())
                .senderUid(application.getSenderUid())
                .receiverUid(application.getReceiverUid())
                .senderRole(application.getSenderRole())
                .receiverRole(application.getReceiverRole())
                .contextType(application.getContextType())
                .contextId(application.getContextId())
                .content(application.getContent())
                .status(application.getStatus())
                .chatAccessStatus(application.getChatAccessStatus())
                .paymentPayerRole("TEACHER")
                .orderId(orderId)
                .roomId(application.getRoomId())
                .receiverRead(receiverRead)
                .decidedAt(application.getDecidedAt())
                .createTime(application.getCreateTime())
                .build();
    }

    private static Long resolveTeacherUid(TutorApplication application) {
        ThrowUtils.throwIf(application == null, ErrorCode.PARAMS_ERROR);
        if ("TEACHER".equals(application.getSenderRole())) {
            return application.getSenderUid();
        }
        return application.getReceiverUid();
    }

    private static String normalizeRole(Integer role) {
        if (Integer.valueOf(1).equals(role)) return "TEACHER";
        if (Integer.valueOf(2).equals(role)) return "STUDENT";
        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        return null;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimNullable(String s) {
        String v = trim(s);
        return v.isEmpty() ? null : v;
    }
}
