package com.ai.tutor.appointment.integration.email;

import com.ai.tutor.appointment.integration.email.dto.RenderedEmail;

import java.util.Map;

public interface EmailTemplateRenderer {

    /**
     * 按模板编码和 payload 渲染出最终主题与 HTML 正文。
     */
    RenderedEmail render(String templateCode, Map<String, Object> payload);
}
