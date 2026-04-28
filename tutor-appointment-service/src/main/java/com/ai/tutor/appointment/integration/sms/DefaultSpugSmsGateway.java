package com.ai.tutor.appointment.integration.sms;

import com.ai.tutor.appointment.config.SmsSpugProperties;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Component
public class DefaultSpugSmsGateway implements SpugSmsGateway {

    private final SmsSpugProperties properties;
    private final RestTemplate restTemplate;

    public DefaultSpugSmsGateway(SmsSpugProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void sendVerifyCode(String phone, String code) {
        ThrowUtils.throwIf(properties == null || !StringUtils.hasText(properties.getToken()), ErrorCode.OPERATION_ERROR,
                "短信服务未配置：sms.spug.token");

        String base = StringUtils.hasText(properties.getBaseUrl()) ? properties.getBaseUrl().trim() : "https://push.spug.cc";
        String url = base.endsWith("/") ? (base + "sms/" + properties.getToken().trim()) : (base + "/sms/" + properties.getToken().trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "name", properties.getSenderName() == null ? "" : properties.getSenderName(),
                "code", code,
                "to", phone
        );
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            ThrowUtils.throwIf(resp == null || !resp.getStatusCode().is2xxSuccessful(), ErrorCode.OPERATION_ERROR, "短信发送失败");
        } catch (RestClientException e) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "短信发送失败");
        }
    }
}
