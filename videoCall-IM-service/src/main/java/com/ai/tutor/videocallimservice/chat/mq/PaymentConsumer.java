package com.ai.tutor.videocallimservice.chat.mq;

import com.ai.tutor.common.event.PaymentSuccessEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "payment-success-topic", consumerGroup = "tutor-application-payment-group")
public class PaymentConsumer implements RocketMQListener<PaymentSuccessEvent> {

    @Resource
    private com.ai.tutor.videocallimservice.chat.service.BrokerageOrderService brokerageOrderService;

    @Override
    public void onMessage(PaymentSuccessEvent event) {
        log.info("Received payment success event: {}", event);

        // 如果上下文类型是 BROKERAGE_ORDER，则更新 BrokerageOrder 并解锁应用
        if ("BROKERAGE_ORDER".equals(event.getContextType())) {
            brokerageOrderService.onPaymentSuccess(event.getContextId(), event.getSuccessTime(), event.getChannel());
        }
    }
}
