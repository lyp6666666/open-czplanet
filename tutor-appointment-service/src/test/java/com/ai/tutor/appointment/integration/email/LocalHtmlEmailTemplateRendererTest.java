package com.ai.tutor.appointment.integration.email;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.integration.email.dto.RenderedEmail;
import com.ai.tutor.appointment.integration.email.impl.LocalHtmlEmailTemplateRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalHtmlEmailTemplateRendererTest {

    @Test
    void shouldRenderVerifyTemplateFromLocalHtml() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.getSender().setTemplateDir("email-templates/tencent");

        LocalHtmlEmailTemplateRenderer renderer = new LocalHtmlEmailTemplateRenderer();
        ReflectionTestUtils.setField(renderer, "properties", properties);

        RenderedEmail rendered = renderer.render("EMAIL_VERIFY_CODE", Map.of(
                "code", "123456",
                "expireMinutes", 10
        ));

        assertThat(rendered.getSubject()).isEqualTo("邮箱验证通知");
        assertThat(rendered.getHtmlBody()).contains("123456");
        assertThat(rendered.getHtmlBody()).doesNotContain("{{code}}");
    }
}
