package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.mapper.EmailNotificationTaskMapper;
import com.ai.tutor.appointment.mapper.EmailSendLogMapper;
import com.ai.tutor.appointment.mapper.LessonSummaryMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserEmailMapper;
import com.ai.tutor.appointment.model.entity.EmailNotificationTask;
import com.ai.tutor.appointment.model.entity.EmailSendLog;
import com.ai.tutor.appointment.model.entity.LessonSummary;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.appointment.model.entity.UserEmail;
import com.ai.tutor.appointment.service.EmailNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private static final String PRIMARY = "PRIMARY";
    private static final String SUMMARY_ONLY = "SUMMARY_ONLY";
    private static final String VERIFIED = "VERIFIED";
    private static final String NORMAL = "NORMAL";

    @Resource
    private EmailNotificationTaskMapper taskMapper;
    @Resource
    private EmailSendLogMapper sendLogMapper;
    @Resource
    private UserEmailMapper userEmailMapper;
    @Resource
    private TutorAppointmentMapper tutorAppointmentMapper;
    @Resource
    private LessonSummaryMapper lessonSummaryMapper;
    @Resource
    private EmailNotificationProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createTask(String taskKey, String templateCode, String bizType, Long bizId, Long receiverUid,
                           String receiverRole, String emailType, String email, Map<String, Object> payload,
                           LocalDateTime scheduledAt) {
        if (!properties.isEnabled() || email == null || email.isBlank()) {
            return;
        }
        EmailNotificationTask task = EmailNotificationTask.builder()
                .taskKey(taskKey)
                .templateCode(templateCode)
                .bizType(bizType)
                .bizId(bizId)
                .receiverUid(receiverUid)
                .receiverRole(receiverRole)
                .emailType(emailType)
                .email(email.trim().toLowerCase())
                .payloadJson(toJson(payload))
                .scheduledAt(scheduledAt == null ? LocalDateTime.now() : scheduledAt)
                .status("PENDING")
                .retryCount(0)
                .maxRetryCount(properties.getNotification().getMaxRetryCount())
                .build();
        taskMapper.insertIgnore(task);
    }

    @Override
    public void createLessonStartTasks(TutorAppointment lesson) {
        if (lesson == null || lesson.getId() == null || lesson.getStartTime() == null) {
            return;
        }
        List<Integer> minutesList = properties.getLesson().getReminderMinutes();
        if (minutesList == null || minutesList.isEmpty()) {
            return;
        }
        for (Integer minutes : minutesList) {
            if (minutes == null || minutes < 0) {
                continue;
            }
            createLessonStartTaskFor(lesson, lesson.getTutorId(), "TEACHER", minutes);
            createLessonStartTaskFor(lesson, lesson.getParentId(), "STUDENT", minutes);
        }
    }

    private void createLessonStartTaskFor(TutorAppointment lesson, Long uid, String role, int minutes) {
        UserEmail email = verifiedEmail(uid, PRIMARY);
        if (email == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", lesson.getId());
        payload.put("courseId", lesson.getCourseId());
        payload.put("lessonTitle", safeTitle(lesson));
        payload.put("startTime", lesson.getStartTime().toString());
        payload.put("reminderMinutes", minutes);
        payload.put("link", "/#/courses/" + lesson.getCourseId());
        createTask("LESSON_START:" + lesson.getId() + ":" + uid + ":" + minutes,
                "LESSON_START_REMINDER",
                "LESSON_START",
                lesson.getId(),
                uid,
                role,
                PRIMARY,
                email.getEmail(),
                payload,
                lesson.getStartTime().minusMinutes(minutes));
    }

    @Override
    public void cancelLessonStartTasks(Long lessonId, String reason) {
        if (lessonId == null) {
            return;
        }
        taskMapper.cancelPendingByBiz("LESSON_START", lessonId, reason == null ? "lesson changed" : reason);
    }

    @Override
    public void createLessonSummaryTasks(LessonSummary summary) {
        if (summary == null || summary.getLessonId() == null || !"READY".equals(summary.getSummaryStatus())) {
            return;
        }
        createSummaryTaskFor(summary, summary.getTeacherUid(), "TEACHER", PRIMARY, "LESSON_SUMMARY");
        createSummaryTaskFor(summary, summary.getStudentUid(), "STUDENT", PRIMARY, "LESSON_SUMMARY");
        createSummaryTaskFor(summary, summary.getStudentUid(), "PARENT_SUMMARY", SUMMARY_ONLY, "LESSON_SUMMARY");
    }

    private void createSummaryTaskFor(LessonSummary summary, Long uid, String role, String emailType, String templateCode) {
        UserEmail email = verifiedEmail(uid, emailType);
        if (email == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", summary.getLessonId());
        payload.put("courseId", summary.getCourseId());
        payload.put("title", summary.getTitle());
        payload.put("brief", summary.getSummaryBrief());
        payload.put("homework", summary.getHomework());
        payload.put("link", "/#/courses/" + summary.getCourseId());
        String biz = "LESSON_SUMMARY_BACKFILL".equals(templateCode) ? "LESSON_SUMMARY_BACKFILL" : "LESSON_SUMMARY";
        createTask(templateCode + ":" + summary.getLessonId() + ":" + uid + ":" + emailType,
                templateCode,
                biz,
                summary.getLessonId(),
                uid,
                role,
                emailType,
                email.getEmail(),
                payload,
                LocalDateTime.now());
    }

    @Override
    public void createLatestSummaryBackfillTasks(Long userId, String emailType, String email) {
        if (userId == null || email == null || email.isBlank()) {
            return;
        }
        LocalDateTime since = LocalDateTime.now().minusHours(properties.getLesson().getSummaryBackfillWindowHours());
        LessonSummary summary = lessonSummaryMapper.selectLatestReadyForUser(userId, since);
        if (summary == null) {
            return;
        }
        String role = userId.equals(summary.getTeacherUid()) ? "TEACHER" : SUMMARY_ONLY.equals(emailType) ? "PARENT_SUMMARY" : "STUDENT";
        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", summary.getLessonId());
        payload.put("courseId", summary.getCourseId());
        payload.put("title", summary.getTitle());
        payload.put("brief", summary.getSummaryBrief());
        payload.put("homework", summary.getHomework());
        payload.put("link", "/#/courses/" + summary.getCourseId());
        createTask("LESSON_SUMMARY_BACKFILL:" + summary.getLessonId() + ":" + userId + ":" + emailType,
                "LESSON_SUMMARY_BACKFILL",
                "LESSON_SUMMARY_BACKFILL",
                summary.getLessonId(),
                userId,
                role,
                emailType,
                email,
                payload,
                LocalDateTime.now());
    }

    @Override
    public void createVerificationCodeEmail(Long userId, String emailType, String email, String code, int expireMinutes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("expireMinutes", expireMinutes);
        payload.put("emailType", emailType);
        createTask("EMAIL_VERIFY:" + userId + ":" + emailType + ":" + email + ":" + UUID.randomUUID(),
                "EMAIL_VERIFY_CODE",
                "EMAIL_VERIFY",
                null,
                userId,
                "USER",
                emailType,
                email,
                payload,
                LocalDateTime.now());
    }

    @Override
    @Scheduled(fixedDelayString = "${email.notification.scheduler-delay-ms:30000}")
    public void processDueTasks() {
        if (!properties.isEnabled()) {
            return;
        }
        List<EmailNotificationTask> tasks = taskMapper.listDuePending(LocalDateTime.now(), properties.getNotification().getSchedulerBatchSize());
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
            if (!validateBeforeSend(task)) {
                taskMapper.markCanceled(task.getId(), "business state no longer valid");
                return;
            }
            String subject = renderSubject(task);
            // Mock provider: persist a successful send log. Real provider can replace this call later.
            sendLogMapper.insert(EmailSendLog.builder()
                    .taskId(task.getId())
                    .provider("MOCK")
                    .providerMessageId("mock-" + task.getId())
                    .email(task.getEmail())
                    .sendStatus("SUCCESS")
                    .requestId(UUID.randomUUID().toString())
                    .build());
            taskMapper.markSent(task.getId(), LocalDateTime.now(), subject);
            UserEmail email = task.getReceiverUid() == null ? null : userEmailMapper.selectActiveByUserAndType(task.getReceiverUid(), task.getEmailType());
            if (email != null) {
                userEmailMapper.updateLastNotifyAt(email.getId(), LocalDateTime.now());
            }
        } catch (Exception e) {
            int nextRetry = task.getRetryCount() == null ? 1 : task.getRetryCount() + 1;
            boolean retry = nextRetry <= (task.getMaxRetryCount() == null ? properties.getNotification().getMaxRetryCount() : task.getMaxRetryCount());
            LocalDateTime next = LocalDateTime.now().plusMinutes(nextRetry == 1 ? 1 : nextRetry == 2 ? 5 : 30);
            taskMapper.markFailedOrRetry(task.getId(), retry ? "PENDING" : "FAILED", e.getMessage(), next, nextRetry);
            sendLogMapper.insert(EmailSendLog.builder()
                    .taskId(task.getId())
                    .provider("MOCK")
                    .email(task.getEmail())
                    .sendStatus("FAIL")
                    .errorMessage(e.getMessage())
                    .requestId(UUID.randomUUID().toString())
                    .build());
            log.warn("email task failed taskId={}", task.getId(), e);
        }
    }

    @Override
    public boolean validateBeforeSend(EmailNotificationTask task) {
        if (task == null) {
            return false;
        }
        if ("EMAIL_VERIFY".equals(task.getBizType())) {
            return true;
        }
        if (task.getReceiverUid() != null && task.getEmailType() != null) {
            UserEmail email = verifiedEmail(task.getReceiverUid(), task.getEmailType());
            if (email == null || !email.getEmail().equalsIgnoreCase(task.getEmail())) {
                return false;
            }
        }
        if ("LESSON_START".equals(task.getBizType())) {
            TutorAppointment lesson = tutorAppointmentMapper.selectById(task.getBizId());
            if (lesson == null || lesson.getStatus() == null || lesson.getStatus() != 2 || lesson.getStartTime() == null) {
                return false;
            }
            Map<String, Object> payload = parsePayload(task.getPayloadJson());
            Object startTime = payload.get("startTime");
            return startTime == null || lesson.getStartTime().toString().equals(String.valueOf(startTime));
        }
        if ("LESSON_SUMMARY".equals(task.getBizType()) || "LESSON_SUMMARY_BACKFILL".equals(task.getBizType())) {
            LessonSummary summary = lessonSummaryMapper.selectByLessonId(task.getBizId());
            return summary != null
                    && "READY".equals(summary.getSummaryStatus())
                    && summary.getSummaryContent() != null
                    && !summary.getSummaryContent().isBlank();
        }
        return true;
    }

    private String renderSubject(EmailNotificationTask task) {
        if ("EMAIL_VERIFY_CODE".equals(task.getTemplateCode())) {
            return "邮箱验证码";
        }
        if ("UNREAD_MESSAGE_REMINDER".equals(task.getTemplateCode())) {
            return "你有一条未读消息";
        }
        if ("LESSON_START_REMINDER".equals(task.getTemplateCode())) {
            return "课程即将开始";
        }
        if ("LESSON_SUMMARY_BACKFILL".equals(task.getTemplateCode())) {
            return "你有一份可查看的最新课后总结";
        }
        if ("LESSON_SUMMARY".equals(task.getTemplateCode())) {
            return "本节课课后总结已生成";
        }
        return "平台通知";
    }

    private UserEmail verifiedEmail(Long uid, String emailType) {
        if (uid == null || emailType == null) {
            return null;
        }
        UserEmail email = userEmailMapper.selectActiveByUserAndType(uid, emailType);
        if (email == null || !VERIFIED.equals(email.getVerifyStatus()) || !NORMAL.equals(email.getBounceStatus())) {
            return null;
        }
        return email;
    }

    private String safeTitle(TutorAppointment lesson) {
        return lesson.getTitle() == null || lesson.getTitle().isBlank() ? "课程" : lesson.getTitle();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
