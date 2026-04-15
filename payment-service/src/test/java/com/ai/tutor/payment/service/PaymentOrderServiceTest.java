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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentOrderServiceTest {

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    private PaymentOrderServiceImpl paymentOrderService;

    private PaymentOrder pendingOrder;

    @BeforeEach
    void setUp() {
        paymentOrderService = new PaymentOrderServiceImpl();
        ReflectionTestUtils.setField(paymentOrderService, "baseMapper", paymentOrderMapper);
        ReflectionTestUtils.setField(paymentOrderService, "rocketMQTemplate", rocketMQTemplate);

        pendingOrder = new PaymentOrder();
        pendingOrder.setId(1L);
        pendingOrder.setOrderNo("ORDER123");
        pendingOrder.setStatus(PaymentStatus.PENDING.getCode());
        pendingOrder.setAmount(100L);
        pendingOrder.setUserId(1001L);
        pendingOrder.setContextId(2001L);
        pendingOrder.setContextType("BROKERAGE_ORDER");
    }

    @Test
    void testUpdateSuccess() {
        // Mock update
        when(paymentOrderMapper.update(any(), any())).thenReturn(1);
        // Mock getByOrderNo for event construction
        when(paymentOrderMapper.selectOne(any(), anyBoolean())).thenReturn(pendingOrder);

        boolean result = paymentOrderService.updateSuccess("ORDER123", "TRANS456", LocalDateTime.now());

        assertTrue(result);
        verify(paymentOrderMapper, atLeastOnce()).update(any(), any());
        verify(rocketMQTemplate).convertAndSend(eq("payment-success-topic"), any(Object.class));
    }

    @Test
    void createOrReusePending_amountChanged_shouldCloseOldAndCreateNew() {
        PaymentOrder oldPending = new PaymentOrder();
        oldPending.setId(10L);
        oldPending.setOrderNo("OLD_ORDER");
        oldPending.setStatus(PaymentStatus.PENDING.getCode());
        oldPending.setAmount(19900L);
        oldPending.setExpireTime(LocalDateTime.now().plusMinutes(5));

        when(paymentOrderMapper.selectOne(any(), anyBoolean())).thenReturn(oldPending);
        when(paymentOrderMapper.update(any(), any())).thenReturn(1);
        doAnswer(invocation -> {
            PaymentOrder inserted = invocation.getArgument(0);
            inserted.setId(11L);
            return 1;
        }).when(paymentOrderMapper).insert(any(PaymentOrder.class));

        PaymentOrder created = paymentOrderService.createOrReusePending(
                "BROKERAGE_ORDER",
                2001L,
                1001L,
                "WECHAT",
                1L,
                "信息费支付",
                "对接咨询费支付",
                "127.0.0.1"
        );

        assertTrue(created != null && created.getId() != null && !"OLD_ORDER".equals(created.getOrderNo()));
        verify(paymentOrderMapper).update(any(), any());
        verify(paymentOrderMapper).insert(any(PaymentOrder.class));
    }
}
