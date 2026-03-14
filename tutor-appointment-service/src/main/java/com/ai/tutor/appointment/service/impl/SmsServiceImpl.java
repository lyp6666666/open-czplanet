package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.SmsProperties;
import com.ai.tutor.appointment.config.SmsSpugProperties;
import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SmsSpugProperties smsSpugProperties;

    @Resource
    private Environment environment;

    @Resource
    private SmsProperties smsProperties;

    private final RestTemplate restTemplate;

    private static final ConcurrentHashMap<String, CodeEntry> LOCAL_CODES = new ConcurrentHashMap<>();
    private static final long CODE_TTL_SECONDS = 5 * 60L;

    public SmsServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    private static class CodeEntry {
        private final String code;
        private final long expireAtMs;

        private CodeEntry(String code, long expireAtMs) {
            this.code = code;
            this.expireAtMs = expireAtMs;
        }
    }

    public String sendCode(String phone, String prefix) {
        String code = String.format("%04d", new Random().nextInt(10000));
        // 构造Key
        String key = prefix + phone;
        // 存入Redis，设置过期时间 5 分钟
        LOCAL_CODES.put(key, new CodeEntry(code, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(CODE_TTL_SECONDS)));
        try {
            redisTemplate.delete(key);
            redisTemplate.opsForValue().set(key, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        sendSms(phone, code);
        log.info("SMS SEND SUCCESS - phone: {}, code: {}, prefix: {}", phone, code, prefix);
        return code;
    }

    private void sendSms(String phone, String code) {
        if (smsProperties != null && !smsProperties.isRealSendEnabled()) {
            if (environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                log.info("SMS SEND SKIPPED (real send disabled) - phone: {}, code: {}", phone, code);
            }
            return;
        }

        String token = smsSpugProperties == null || smsSpugProperties.getToken() == null ? "" : smsSpugProperties.getToken().trim();
        if (token.isEmpty()) {
            token = resolveTokenFromFallbackFiles();
        }
        ThrowUtils.throwIf(token.isEmpty(), ErrorCode.OPERATION_ERROR, "短信服务未配置：请在配置文件中配置 sms.spug.token");

        String base = smsSpugProperties == null || smsSpugProperties.getBaseUrl() == null ? "" : smsSpugProperties.getBaseUrl().trim();
        if (base.isEmpty()) {
            base = "https://push.spug.cc";
        }

        String url = base.endsWith("/") ? (base + "sms/" + token) : (base + "/sms/" + token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "name", smsSpugProperties == null || smsSpugProperties.getSenderName() == null ? "" : smsSpugProperties.getSenderName(),
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

    private String resolveTokenFromFallbackFiles() {
        String v = readYamlProp(new ClassPathResource("application-dev.yml"), "sms.spug.token");
        if (!v.isEmpty()) return v;
        v = readYamlProp(new ClassPathResource("application.yml"), "sms.spug.token");
        if (!v.isEmpty()) return v;
        v = readYamlProp(new FileSystemResource("./.private/tutor-appointment-service.yml"), "sms.spug.token");
        if (!v.isEmpty()) return v;
        v = readYamlProp(new FileSystemResource("../.private/tutor-appointment-service.yml"), "sms.spug.token");
        return v;
    }

    private String readYamlProp(org.springframework.core.io.Resource resource, String key) {
        try {
            if (resource == null || !resource.exists()) return "";
            YamlPropertiesFactoryBean bean = new YamlPropertiesFactoryBean();
            bean.setResources(resource);
            Properties props = bean.getObject();
            if (props == null) return "";
            String v = props.getProperty(key);
            return v == null ? "" : v.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public boolean verifyCode(String phone, String code,String prefix) {
        String key = prefix + phone;
        String storedCode = null;
        try {
            storedCode = (String) redisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
        }
        if (storedCode == null) {
            CodeEntry entry = LOCAL_CODES.get(key);
            if (entry != null && entry.expireAtMs > System.currentTimeMillis()) {
                storedCode = entry.code;
            }
        }
        ThrowUtils.throwIf(storedCode == null, ErrorCode.VERIFICATION_EXPIRED_ERROR);
        return code != null && code.equals(storedCode);
    }

    public String debugPeekCode(String phone, String prefix) {
        String key = prefix + phone;
        CodeEntry entry = LOCAL_CODES.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAtMs <= System.currentTimeMillis()) {
            LOCAL_CODES.remove(key);
            return null;
        }
        return entry.code;
    }
}
