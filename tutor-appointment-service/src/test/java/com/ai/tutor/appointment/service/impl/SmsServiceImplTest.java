package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.SmsProperties;
import com.ai.tutor.appointment.integration.sms.AliyunSmsGateway;
import com.ai.tutor.appointment.integration.sms.SpugSmsGateway;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SmsServiceImplTest {

    @Test
    void localModeStoresAndVerifiesGeneratedCode() {
        SmsServiceImpl service = newService(false, "aliyun", null, null);

        String phone = "13800000001";
        String prefix = "sms:test:local:";
        String code = service.sendCode(phone, prefix);

        assertThat(code).matches("\\d{4}");
        assertThat(service.debugPeekCode(phone, prefix)).isEqualTo(code);
        assertThat(service.verifyCode(phone, code, prefix)).isTrue();
        assertThat(service.verifyCode(phone, "9999", prefix)).isFalse();
    }

    @Test
    void realSendModeStoresAliyunSessionAndDelegatesVerification() {
        AliyunSmsGateway gateway = mock(AliyunSmsGateway.class);
        when(gateway.sendVerifyCode(eq("13800000002"), any()))
                .thenReturn(new AliyunSmsGateway.SendResult("out-123", null));
        when(gateway.checkVerifyCode("13800000002", "out-123", "2468")).thenReturn(true);
        SpugSmsGateway spugGateway = mock(SpugSmsGateway.class);
        SmsServiceImpl service = newService(true, "aliyun", gateway, spugGateway);

        String code = service.sendCode("13800000002", "sms:test:aliyun:");

        assertThat(code).isEmpty();
        assertThat(service.debugPeekCode("13800000002", "sms:test:aliyun:")).isNull();
        assertThat(service.verifyCode("13800000002", "2468", "sms:test:aliyun:")).isTrue();
        verify(gateway).checkVerifyCode("13800000002", "out-123", "2468");
        verify(spugGateway, never()).sendVerifyCode(any(), any());
    }

    @Test
    void spugModeSendsGeneratedCodeAndVerifiesLocally() {
        AliyunSmsGateway aliyunGateway = mock(AliyunSmsGateway.class);
        SpugSmsGateway spugGateway = mock(SpugSmsGateway.class);
        SmsServiceImpl service = newService(true, "spug", aliyunGateway, spugGateway);

        String phone = "13800000003";
        String prefix = "sms:test:spug:";
        String code = service.sendCode(phone, prefix);

        assertThat(code).matches("\\d{4}");
        assertThat(service.debugPeekCode(phone, prefix)).isEqualTo(code);
        assertThat(service.verifyCode(phone, code, prefix)).isTrue();
        verify(spugGateway).sendVerifyCode(phone, code);
        verify(aliyunGateway, never()).sendVerifyCode(any(), any());
    }

    @SuppressWarnings("unchecked")
    private SmsServiceImpl newService(boolean realSendEnabled, String provider, AliyunSmsGateway aliyunGateway, SpugSmsGateway spugGateway) {
        SmsServiceImpl service = new SmsServiceImpl();
        SmsProperties properties = new SmsProperties();
        properties.setRealSendEnabled(realSendEnabled);
        properties.setProvider(provider);

        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReflectionTestUtils.setField(service, "smsProperties", properties);
        ReflectionTestUtils.setField(service, "aliyunSmsGateway", aliyunGateway == null ? mock(AliyunSmsGateway.class) : aliyunGateway);
        ReflectionTestUtils.setField(service, "spugSmsGateway", spugGateway == null ? mock(SpugSmsGateway.class) : spugGateway);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        return service;
    }
}
