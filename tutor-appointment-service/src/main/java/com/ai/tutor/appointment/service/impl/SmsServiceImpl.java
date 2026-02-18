package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final ConcurrentHashMap<String, CodeEntry> LOCAL_CODES = new ConcurrentHashMap<>();

    private static class CodeEntry {
        private final String code;
        private final long expireAtMs;

        private CodeEntry(String code, long expireAtMs) {
            this.code = code;
            this.expireAtMs = expireAtMs;
        }
    }

    public String sendCode(String phone,String prefix) {
        String code = String.format("%04d", new Random().nextInt(10000));
        // 构造Key
        String key = prefix + phone;
        // 存入Redis，设置过期时间 60 秒
        LOCAL_CODES.put(key, new CodeEntry(code, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60)));
        try {
            redisTemplate.opsForValue().set(key, code, 60, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        //todo 这里应该调用云服务进行短信发送
        System.out.println("【模拟发送短信】手机号：" + phone + " 验证码：" + code);
        return code;
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
