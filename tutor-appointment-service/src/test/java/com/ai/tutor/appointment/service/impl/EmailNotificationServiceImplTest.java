package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.integration.email.EmailSender;
import com.ai.tutor.appointment.integration.email.dto.EmailSendResponse;
import com.ai.tutor.appointment.mapper.EmailNotificationTaskMapper;
import com.ai.tutor.appointment.mapper.EmailSendLogMapper;
import com.ai.tutor.appointment.mapper.LessonSummaryMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserEmailMapper;
import com.ai.tutor.appointment.model.entity.EmailNotificationTask;
import com.ai.tutor.appointment.model.entity.EmailSendLog;
import com.ai.tutor.appointment.model.entity.UserEmail;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationServiceImplTest {

    @Test
    void processOneShouldSendViaConfiguredSenderAndPersistLog() {
        EmailNotificationTaskMapper taskMapper = mock(EmailNotificationTaskMapper.class);
        EmailSendLogMapper sendLogMapper = mock(EmailSendLogMapper.class);
        UserEmailMapper userEmailMapper = mock(UserEmailMapper.class);
        TutorAppointmentMapper tutorAppointmentMapper = mock(TutorAppointmentMapper.class);
        LessonSummaryMapper lessonSummaryMapper = mock(LessonSummaryMapper.class);
        EmailSender emailSender = mock(EmailSender.class);
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.getSender().setFromEmail("no-reply@example.com");
        properties.getSender().setFromName("创智星球");

        EmailNotificationServiceImpl service = new EmailNotificationServiceImpl();
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "sendLogMapper", sendLogMapper);
        ReflectionTestUtils.setField(service, "userEmailMapper", userEmailMapper);
        ReflectionTestUtils.setField(service, "tutorAppointmentMapper", tutorAppointmentMapper);
        ReflectionTestUtils.setField(service, "lessonSummaryMapper", lessonSummaryMapper);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "emailSender", emailSender);

        EmailNotificationTask task = EmailNotificationTask.builder()
                .id(1L)
                .templateCode("EMAIL_VERIFY_CODE")
                .bizType("EMAIL_VERIFY")
                .receiverUid(1001L)
                .emailType("PRIMARY")
                .email("user@example.com")
                .payloadJson("{\"code\":\"123456\",\"expireMinutes\":10}")
                .retryCount(0)
                .maxRetryCount(3)
                .build();

        when(taskMapper.updateStatusIfCurrent(1L, "PENDING", "VALIDATING")).thenReturn(1);
        when(emailSender.send(any())).thenReturn(EmailSendResponse.builder()
                .success(true)
                .provider("TENCENT")
                .providerMessageId("msg-1")
                .requestId("req-1")
                .build());
        when(userEmailMapper.selectActiveByUserAndType(1001L, "PRIMARY")).thenReturn(UserEmail.builder()
                .id(9L)
                .userId(1001L)
                .email("user@example.com")
                .verifyStatus("VERIFIED")
                .bounceStatus("NORMAL")
                .build());

        service.processOne(task);

        verify(taskMapper).markSent(org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq("邮箱验证通知"));
        ArgumentCaptor<EmailSendLog> logCaptor = ArgumentCaptor.forClass(EmailSendLog.class);
        verify(sendLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo("TENCENT");
        assertThat(logCaptor.getValue().getProviderMessageId()).isEqualTo("msg-1");
        verify(userEmailMapper).updateLastNotifyAt(org.mockito.ArgumentMatchers.eq(9L), any(LocalDateTime.class));
    }
}
