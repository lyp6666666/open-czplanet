package com.ai.tutor.payment.service;

import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.mapper.PaymentOrderMapper;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.service.impl.PaymentOrderServiceImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentOrderServiceIdempotentTest {

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    private PaymentOrderServiceImpl paymentOrderService;

    @BeforeEach
    void setUp() {
        paymentOrderService = new PaymentOrderServiceImpl();
        ReflectionTestUtils.setField(paymentOrderService, "baseMapper", paymentOrderMapper);
        ReflectionTestUtils.setField(paymentOrderService, "rocketMQTemplate", rocketMQTemplate);
    }

    @Test
    void updateSuccessFromNotify_isIdempotent() {
        PaymentOrder pending = new PaymentOrder();
        pending.setOrderNo("O1");
        pending.setStatus(PaymentStatus.PENDING.getCode());
        pending.setUserId(1L);
        pending.setAmount(100L);
        pending.setContextType("BROKERAGE_ORDER");
        pending.setContextId(10L);
        pending.setChannel("WECHAT");
        pending.setProvider("YUNGOUOS");
        pending.setProviderOrderNo("Y1");
        pending.setSuccessTime(LocalDateTime.now());

        PaymentOrder success = new PaymentOrder();
        success.setOrderNo("O1");
        success.setStatus(PaymentStatus.SUCCESS.getCode());
        success.setUserId(1L);
        success.setAmount(100L);
        success.setContextType("BROKERAGE_ORDER");
        success.setContextId(10L);
        success.setChannel("WECHAT");
        success.setProvider("YUNGOUOS");
        success.setProviderOrderNo("Y1");
        success.setSuccessTime(LocalDateTime.now());

        when(paymentOrderMapper.update(any(), any())).thenReturn(1).thenReturn(0);
        when(paymentOrderMapper.selectOne(any(), anyBoolean())).thenReturn(pending).thenReturn(success);

        boolean r1 = paymentOrderService.updateSuccessFromNotify("O1", "T1", "Y1", LocalDateTime.now(), 1);
        boolean r2 = paymentOrderService.updateSuccessFromNotify("O1", "T1", "Y1", LocalDateTime.now(), 1);

        assertTrue(r1);
        assertTrue(r2);
        verify(rocketMQTemplate, times(1)).convertAndSend(eq("payment-success-topic"), any(Object.class));
    }
}

