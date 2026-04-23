package com.ai.tutor.videocallimservice.chat.service.impl;

import com.ai.tutor.common.email.TencentCloudSesClient;
import com.ai.tutor.common.email.TencentCloudSesRequest;
import com.ai.tutor.common.email.TencentCloudSesResponse;
import com.ai.tutor.videocallimservice.chat.config.EmailDeliveryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnreadEmailSender {

    @Resource
    private EmailDeliveryProperties properties;
    @Resource
    private TencentCloudSesClient tencentCloudSesClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SendResult sendUnreadReminder(String toEmail, String subject, Map<String, Object> templateData, String requestId) {
        String provider = properties.getSender().getProvider() == null ? "MOCK" : properties.getSender().getProvider().trim().toUpperCase();
        if (!properties.getSender().isEnabled() || "MOCK".equals(provider)) {
            return new SendResult(true, "MOCK", "mock-" + requestId, requestId, null, null);
        }
        Long templateId = properties.getSender().getTemplateIds().get("UNREAD_MESSAGE_REMINDER");
        if (templateId == null || templateId <= 0) {
            return new SendResult(false, "TENCENT", null, requestId, "TEMPLATE_ID_MISSING", "missing template id for UNREAD_MESSAGE_REMINDER");
        }
        try {
            String dataJson = objectMapper.writeValueAsString(templateData == null ? Map.of() : templateData);
            TencentCloudSesResponse response = tencentCloudSesClient.sendTemplateEmail(TencentCloudSesRequest.builder()
                    .endpoint(properties.getSender().getEndpoint())
                    .region(properties.getSender().getRegion())
                    .secretId(properties.getSender().getSecretId())
                    .secretKey(properties.getSender().getSecretKey())
                    .fromEmail(properties.getSender().getFromEmail())
                    .fromName(properties.getSender().getFromName())
                    .replyToEmail(properties.getSender().getReplyToEmail())
                    .toEmail(toEmail)
                    .subject(subject)
                    .templateId(templateId)
                    .templateDataJson(dataJson)
                    .connectTimeoutMs(properties.getSender().getConnectTimeoutMs())
                    .readTimeoutMs(properties.getSender().getReadTimeoutMs())
                    .build());
            return new SendResult(response.isSuccess(), "TENCENT", response.getProviderMessageId(),
                    response.getRequestId() == null ? requestId : response.getRequestId(),
                    response.getErrorCode(), response.getErrorMessage());
        } catch (Exception e) {
            return new SendResult(false, "TENCENT", null, requestId, "SERIALIZE_TEMPLATE_DATA_ERROR", e.getMessage());
        }
    }

    public record SendResult(boolean success,
                             String provider,
                             String providerMessageId,
                             String requestId,
                             String errorCode,
                             String errorMessage) {
    }
}
