package com.ai.tutor.appointment.integration.email;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.integration.email.dto.EmailSendRequest;
import com.ai.tutor.appointment.integration.email.dto.EmailSendResponse;
import com.ai.tutor.appointment.integration.email.impl.DefaultEmailSender;
import com.ai.tutor.common.email.TencentCloudSesClient;
import com.ai.tutor.common.email.TencentCloudSesRequest;
import com.ai.tutor.common.email.TencentCloudSesResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultEmailSenderTest {

    @Test
    void shouldFallbackBackfillTemplateToLessonSummaryTemplateId() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.getSender().setProvider("TENCENT");
        properties.getSender().setEnabled(true);
        properties.getSender().getTemplateIds().put("LESSON_SUMMARY", 173985L);

        EmailTemplateRenderer templateRenderer = mock(EmailTemplateRenderer.class);
        TencentCloudSesClient sesClient = mock(TencentCloudSesClient.class);
        when(sesClient.sendTemplateEmail(any())).thenReturn(TencentCloudSesResponse.builder()
                .success(true)
                .providerMessageId("msg-173985")
                .requestId("req-173985")
                .build());

        DefaultEmailSender sender = new DefaultEmailSender();
        ReflectionTestUtils.setField(sender, "properties", properties);
        ReflectionTestUtils.setField(sender, "templateRenderer", templateRenderer);
        ReflectionTestUtils.setField(sender, "tencentCloudSesClient", sesClient);

        EmailSendResponse response = sender.send(EmailSendRequest.builder()
                .templateCode("LESSON_SUMMARY_BACKFILL")
                .toEmail("user@example.com")
                .subject("你有一份可查看的最新课后总结")
                .requestId("req-local")
                .fromEmail("no-reply@example.com")
                .fromName("创智星球")
                .replyToEmail("support@example.com")
                .templateData(Map.of("courseName", "数学课"))
                .build());

        assertThat(response.isSuccess()).isTrue();
        ArgumentCaptor<TencentCloudSesRequest> requestCaptor = ArgumentCaptor.forClass(TencentCloudSesRequest.class);
        verify(sesClient).sendTemplateEmail(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTemplateId()).isEqualTo(173985L);
    }
}
