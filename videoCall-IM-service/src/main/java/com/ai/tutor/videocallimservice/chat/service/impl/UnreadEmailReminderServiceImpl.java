package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.videocallimservice.chat.domain.entity.EmailNotificationTask;
import com.ai.tutor.videocallimservice.chat.domain.entity.EmailSendLog;
import com.ai.tutor.videocallimservice.chat.domain.entity.Message;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.entity.RoomReadState;
import com.ai.tutor.videocallimservice.chat.mapper.EmailNotificationTaskMapper;
import com.ai.tutor.videocallimservice.chat.mapper.EmailSendLogMapper;
import com.ai.tutor.videocallimservice.chat.mapper.MessageMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomReadStateMapper;
import com.ai.tutor.videocallimservice.chat.service.UnreadEmailReminderService;
import com.ai.tutor.videocallimservice.integration.dto.InternalUserEmailsVO;
import com.ai.tutor.videocallimservice.integration.feign.AppointmentInternalFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RefreshScope
public class UnreadEmailReminderServiceImpl implements UnreadEmailReminderService {

    @Resource
    private MessageMapper messageMapper;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private RoomReadStateMapper roomReadStateMapper;
    @Resource
    private EmailNotificationTaskMapper taskMapper;
    @Resource
    private EmailSendLogMapper sendLogMapper;
    @Resource
    private AppointmentInternalFeignClient appointmentInternalFeignClient;
    @Resource
    private UnreadEmailSender unreadEmailSender;

    @Value("${email.notification.unread-delay-minutes:120}")
    private int unreadDelayMinutes;
    @Value("${email.notification.unread-room-daily-limit:1}")
    private int unreadRoomDailyLimit;
    @Value("${email.notification.unread-user-daily-limit:3}")
    private int unreadUserDailyLimit;
    @Value("${email.notification.scheduler-batch-size:50}")
    private int schedulerBatchSize;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessageCreated(Long msgId) {
        if (msgId == null) {
            return;
        }
        Message message = messageMapper.getById(msgId);
        if (!canCreateReminder(message)) {
            return;
        }
        InternalUserEmailsVO.EmailValue email = fetchPrimaryEmail(message.getToUid());
        if (email == null || !Boolean.TRUE.equals(email.getVerified()) || !"NORMAL".equals(email.getBounceStatus())) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgId", message.getId());
        payload.put("roomId", message.getRoomId());
        payload.put("fromUid", message.getFromUid());
        payload.put("receiverName", "用户");
        payload.put("senderName", "站内联系人");
        payload.put("senderRole", "用户");
        payload.put("messageSummary", messageTypeSummary(message));
        payload.put("messageTypeSummary", messageTypeSummary(message));
        EmailNotificationTask task = EmailNotificationTask.builder()
                .taskKey("UNREAD:" + message.getRoomId() + ":" + message.getToUid() + ":" + message.getId())
                .templateCode("UNREAD_MESSAGE_REMINDER")
                .bizType("UNREAD_MESSAGE")
                .bizId(message.getId())
                .receiverUid(message.getToUid())
                .receiverRole("USER")
                .emailType("PRIMARY")
                .email(email.getEmail())
                .payloadJson(toJson(payload))
                .scheduledAt((message.getCreateTime() == null ? LocalDateTime.now() : message.getCreateTime()).plusMinutes(unreadDelayMinutes))
                .status("PENDING")
                .retryCount(0)
                .maxRetryCount(3)
                .build();
        taskMapper.insertIgnore(task);
    }

    @Override
    @Scheduled(fixedDelayString = "${email.notification.scheduler-delay-ms:30000}")
    public void processDueTasks() {
        var tasks = taskMapper.listDuePending(LocalDateTime.now(), schedulerBatchSize);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (EmailNotificationTask task : tasks) {
            processOne(task);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processOne(EmailNotificationTask task) {
        if (taskMapper.updateStatusIfCurrent(task.getId(), "PENDING", "VALIDATING") <= 0) {
            return;
        }
        try {
            if (!validate(task)) {
                taskMapper.markCanceled(task.getId(), "message read or business state invalid");
                return;
            }
            Map<String, Object> payload = parsePayload(task.getPayloadJson());
            UnreadEmailSender.SendResult sendResult = unreadEmailSender.sendUnreadReminder(
                    task.getEmail(),
                    "未读消息待查看",
                    payload,
                    UUID.randomUUID().toString());
            if (!sendResult.success()) {
                throw new IllegalStateException(sendResult.errorCode() + ":" + sendResult.errorMessage());
            }
            sendLogMapper.insert(EmailSendLog.builder()
                    .taskId(task.getId())
                    .provider(sendResult.provider())
                    .providerMessageId(sendResult.providerMessageId())
                    .email(task.getEmail())
                    .sendStatus("SUCCESS")
                    .requestId(sendResult.requestId())
                    .build());
            taskMapper.markSent(task.getId(), LocalDateTime.now(), "未读消息待查看");
        } catch (Exception e) {
            int retry = task.getRetryCount() == null ? 1 : task.getRetryCount() + 1;
            boolean canRetry = retry <= (task.getMaxRetryCount() == null ? 3 : task.getMaxRetryCount());
            taskMapper.markFailedOrRetry(task.getId(), canRetry ? "PENDING" : "FAILED", e.getMessage(), LocalDateTime.now().plusMinutes(5), retry);
            log.warn("unread email reminder failed taskId={}", task.getId(), e);
        }
    }

    private boolean validate(EmailNotificationTask task) {
        Message message = messageMapper.getById(task.getBizId());
        if (!canCreateReminder(message)) {
            return false;
        }
        Room room = roomMapper.selectById(message.getRoomId());
        if (room == null || room.getStatus() == null || room.getStatus() != 1) {
            return false;
        }
        RoomReadState read = roomReadStateMapper.getByRoomAndUid(message.getRoomId(), message.getToUid());
        long lastRead = read == null || read.getLastReadMsgId() == null ? 0L : read.getLastReadMsgId();
        if (lastRead >= message.getId()) {
            return false;
        }
        InternalUserEmailsVO.EmailValue email = fetchPrimaryEmail(message.getToUid());
        if (email == null || !Boolean.TRUE.equals(email.getVerified()) || !"NORMAL".equals(email.getBounceStatus())) {
            return false;
        }
        LocalDateTime dayStart = LocalDateTime.now().minusHours(24);
        int roomSent = taskMapper.countSentSince(message.getToUid(), "UNREAD_MESSAGE_REMINDER", "UNREAD_MESSAGE", message.getId(), dayStart);
        if (roomSent >= unreadRoomDailyLimit) {
            return false;
        }
        int userSent = taskMapper.countSentSince(message.getToUid(), "UNREAD_MESSAGE_REMINDER", null, null, dayStart);
        return userSent < unreadUserDailyLimit;
    }

    private boolean canCreateReminder(Message message) {
        if (message == null || message.getId() == null || message.getToUid() == null) {
            return false;
        }
        if (message.getStatus() != null && message.getStatus() != 0) {
            return false;
        }
        if (Integer.valueOf(2).equals(message.getType())) {
            return false;
        }
        return !Integer.valueOf(8).equals(message.getType()) || !isNoiseSystemMessage(message);
    }

    private boolean isNoiseSystemMessage(Message message) {
        String extra = message.getExtra() == null ? "" : message.getExtra().toUpperCase();
        String content = message.getContent() == null ? "" : message.getContent().toUpperCase();
        return extra.contains("READ") || content.contains("READ") || extra.contains("TYPING") || content.contains("TYPING");
    }

    private String messageTypeSummary(Message message) {
        if (Integer.valueOf(1).equals(message.getType())) {
            return "你收到一条普通消息";
        }
        if (message.getType() != null && message.getType() >= 3 && message.getType() <= 7) {
            return "你收到一条多媒体消息";
        }
        String raw = ((message.getContent() == null ? "" : message.getContent()) + " " + (message.getExtra() == null ? "" : message.getExtra())).toUpperCase();
        if (raw.contains("TUTOR_APPLICATION")) return "你收到一条新的申请消息";
        if (raw.contains("LESSON_REQUEST")) return "你收到一条约课沟通消息";
        if (raw.contains("SCHEDULE") || raw.contains("LESSON_STATUS")) return "你收到一条课程变更消息";
        if (raw.contains("SUMMARY")) return "你收到一条课程结果通知";
        return "你收到一条系统消息";
    }

    private InternalUserEmailsVO.EmailValue fetchPrimaryEmail(Long uid) {
        try {
            BaseResponse<InternalUserEmailsVO> resp = appointmentInternalFeignClient.getUserEmailsById(uid);
            if (resp == null || resp.getData() == null) {
                return null;
            }
            return resp.getData().getPrimaryEmail();
        } catch (Exception e) {
            log.warn("fetch email failed uid={}", uid, e);
            return null;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
