package com.ai.tutor.appointment.integration.email.impl;

import com.ai.tutor.appointment.config.EmailNotificationProperties;
import com.ai.tutor.appointment.integration.email.EmailSender;
import com.ai.tutor.appointment.integration.email.EmailTemplateRenderer;
import com.ai.tutor.appointment.integration.email.dto.EmailSendRequest;
import com.ai.tutor.appointment.integration.email.dto.EmailSendResponse;
import com.ai.tutor.common.email.TencentCloudSesClient;
import com.ai.tutor.common.email.TencentCloudSesRequest;
import com.ai.tutor.common.email.TencentCloudSesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DefaultEmailSender implements EmailSender {

    @Resource
    private EmailNotificationProperties properties;
    @Resource
    private EmailTemplateRenderer templateRenderer;
    @Resource
    private TencentCloudSesClient tencentCloudSesClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public EmailSendResponse send(EmailSendRequest request) {
        String provider = properties.getSender().getProvider() == null ? "MOCK" : properties.getSender().getProvider().trim().toUpperCase();
        if (!properties.getSender().isEnabled() || "MOCK".equals(provider)) {
            templateRenderer.render(request.getTemplateCode(), request.getTemplateData());
            return EmailSendResponse.builder()
                    .success(true)
                    .provider("MOCK")
                    .providerMessageId("mock-" + request.getRequestId())
                    .requestId(request.getRequestId())
                    .build();
        }
        if (!"TENCENT".equals(provider)) {
            return EmailSendResponse.builder()
                    .success(false)
                    .provider(provider)
                    .requestId(request.getRequestId())
                    .errorCode("UNSUPPORTED_PROVIDER")
                    .errorMessage("unsupported email provider: " + provider)
                    .build();
        }
        Long templateId = properties.getSender().getTemplateIds().get(request.getTemplateCode());
        if (templateId == null || templateId <= 0) {
            return EmailSendResponse.builder()
                    .success(false)
                    .provider("TENCENT")
                    .requestId(request.getRequestId())
                    .errorCode("TEMPLATE_ID_MISSING")
                    .errorMessage("missing tencent template id for " + request.getTemplateCode())
                    .build();
        }
        try {
            String templateDataJson = objectMapper.writeValueAsString(request.getTemplateData() == null ? java.util.Map.of() : request.getTemplateData());
            TencentCloudSesResponse response = tencentCloudSesClient.sendTemplateEmail(TencentCloudSesRequest.builder()
                    .endpoint(properties.getSender().getEndpoint())
                    .region(properties.getSender().getRegion())
                    .secretId(properties.getSender().getSecretId())
                    .secretKey(properties.getSender().getSecretKey())
                    .fromEmail(request.getFromEmail())
                    .fromName(request.getFromName())
                    .replyToEmail(request.getReplyToEmail())
                    .toEmail(request.getToEmail())
                    .subject(request.getSubject())
                    .templateId(templateId)
                    .templateDataJson(templateDataJson)
                    .connectTimeoutMs(properties.getSender().getConnectTimeoutMs())
                    .readTimeoutMs(properties.getSender().getReadTimeoutMs())
                    .build());
            return EmailSendResponse.builder()
                    .success(response.isSuccess())
                    .provider("TENCENT")
                    .providerMessageId(response.getProviderMessageId())
                    .requestId(response.getRequestId() == null ? request.getRequestId() : response.getRequestId())
                    .errorCode(response.getErrorCode())
                    .errorMessage(response.getErrorMessage())
                    .build();
        } catch (Exception e) {
            return EmailSendResponse.builder()
                    .success(false)
                    .provider("TENCENT")
                    .requestId(request.getRequestId())
                    .errorCode("SERIALIZE_TEMPLATE_DATA_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
