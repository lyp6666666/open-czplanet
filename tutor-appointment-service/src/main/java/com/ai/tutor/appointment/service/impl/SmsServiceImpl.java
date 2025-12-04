package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.enums.RedisKeyPrefix;
import com.ai.tutor.appointment.service.SmsService;
import com.ai.tutor.appointment.utils.ThrowUtils;
import com.ai.tutor.enums.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    public String sendCode(String phone,String prefix) {
        String code = String.format("%04d", new Random().nextInt(10000));
        // 构造Key
        String key = prefix + phone;
        // 存入Redis，设置过期时间 60 秒
        redisTemplate.opsForValue().set(key, code, 60, TimeUnit.SECONDS);
        //todo 这里应该调用云服务进行短信发送
        System.out.println("【模拟发送短信】手机号：" + phone + " 验证码：" + code);
        return code;
    }

    public boolean verifyCode(String phone, String code,String prefix) {
        String key = prefix + phone;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(storedCode == null, ErrorCode.VERIFICATION_EXPIRED_ERROR);
        return code != null && code.equals(storedCode);
    }
}
