package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.entity.EmailNotificationTask;
import com.ai.tutor.appointment.model.entity.LessonSummary;
import com.ai.tutor.appointment.model.entity.TutorAppointment;

import java.time.LocalDateTime;
import java.util.Map;

public interface EmailNotificationService {
    void createTask(String taskKey,
                    String templateCode,
                    String bizType,
                    Long bizId,
                    Long receiverUid,
                    String receiverRole,
                    String emailType,
                    String email,
                    Map<String, Object> payload,
                    LocalDateTime scheduledAt);

    void createLessonStartTasks(TutorAppointment lesson);

    void cancelLessonStartTasks(Long lessonId, String reason);

    void createLessonSummaryTasks(LessonSummary summary);

    void createLatestSummaryBackfillTasks(Long userId, String emailType, String email);

    void createVerificationCodeEmail(Long userId, String emailType, String email, String code, int expireMinutes);

    void processDueTasks();

    boolean validateBeforeSend(EmailNotificationTask task);
}
