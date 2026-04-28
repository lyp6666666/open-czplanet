package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.SmsProperties;
import com.ai.tutor.appointment.integration.sms.AliyunSmsGateway;
import com.ai.tutor.appointment.integration.sms.SpugSmsGateway;
import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private BizKpiMetrics bizKpiMetrics;

    @Resource
    private AliyunSmsGateway aliyunSmsGateway;

    @Resource
    private SpugSmsGateway spugSmsGateway;

    @Resource
    private Environment environment;

    @Resource
    private SmsProperties smsProperties;

    private static final ConcurrentHashMap<String, CodeEntry> LOCAL_CODES = new ConcurrentHashMap<>();
    private static final long CODE_TTL_SECONDS = 5 * 60L;
    private static final String ALIYUN_SESSION_PREFIX = "ALIYUN:";

    private static class CodeEntry {
        private final String code;
        private final long expireAtMs;

        private CodeEntry(String code, long expireAtMs) {
            this.code = code;
            this.expireAtMs = expireAtMs;
        }
    }

    public String sendCode(String phone, String prefix) {
        String key = prefix + phone;
        String resultCode;
        if (smsProperties != null && smsProperties.isRealSendEnabled()) {
            if (isSpugProvider()) {
                resultCode = String.format("%04d", new Random().nextInt(10000));
                putCodeSession(key, resultCode);
                spugSmsGateway.sendVerifyCode(phone, resultCode);
            } else {
                String outId = buildOutId(prefix);
                AliyunSmsGateway.SendResult sendResult = aliyunSmsGateway.sendVerifyCode(phone, outId);
                String session = ALIYUN_SESSION_PREFIX + (sendResult == null ? outId : sendResult.outId());
                putCodeSession(key, session);
                resultCode = sendResult == null ? null : sendResult.verifyCode();
            }
        } else {
            resultCode = String.format("%04d", new Random().nextInt(10000));
            putCodeSession(key, resultCode);
            if (environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                log.info("SMS SEND SKIPPED (real send disabled) - phone: {}", phone);
            }
        }

        if (bizKpiMetrics != null) {
            /*
             * Grafana 业务 KPI 指标打点（短信验证码发送次数）。
             * - metric: ai_tutor_biz_sms_code_send_total
             * - labels: 无
             * - PromQL（按天）：sum(increase(ai_tutor_biz_sms_code_send_total[1d]))
             */
            bizKpiMetrics.incSmsCodeSend();
        }
        log.info("SMS SEND SUCCESS - phone: {}, prefix: {}", phone, prefix);
        return resultCode == null ? "" : resultCode;
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
        if (storedCode.startsWith(ALIYUN_SESSION_PREFIX)) {
            String outId = storedCode.substring(ALIYUN_SESSION_PREFIX.length());
            return aliyunSmsGateway.checkVerifyCode(phone, outId, code);
        }
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
        if (entry.code != null && entry.code.startsWith(ALIYUN_SESSION_PREFIX)) {
            return null;
        }
        return entry.code;
    }

    private void putCodeSession(String key, String value) {
        LOCAL_CODES.put(key, new CodeEntry(value, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(CODE_TTL_SECONDS)));
        try {
            redisTemplate.delete(key);
            redisTemplate.opsForValue().set(key, value, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private String buildOutId(String prefix) {
        String normalizedPrefix = prefix == null ? "sms" : prefix.replaceAll("[^A-Za-z0-9_-]", "-");
        return normalizedPrefix + UUID.randomUUID();
    }

    private boolean isSpugProvider() {
        return smsProperties != null && "spug".equalsIgnoreCase(smsProperties.getProvider());
    }
}
