package com.ai.tutor.videocallimservice.chat.mq;

import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.videocallimservice.chat.service.BrokerageOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class PaymentConsumerTest {

    @Test
    void onMessage_brokerageOrder_callsService() {
        BrokerageOrderService brokerageOrderService = Mockito.mock(BrokerageOrderService.class);
        PaymentConsumer consumer = new PaymentConsumer();
        ReflectionTestUtils.setField(consumer, "brokerageOrderService", brokerageOrderService);

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setContextType("BROKERAGE_ORDER");
        event.setContextId(100L);
        event.setChannel("WECHAT");
        event.setSuccessTime(LocalDateTime.now());

        consumer.onMessage(event);

        verify(brokerageOrderService).onPaymentSuccess(eq(100L), eq(event.getSuccessTime()), eq("WECHAT"));
    }
}

