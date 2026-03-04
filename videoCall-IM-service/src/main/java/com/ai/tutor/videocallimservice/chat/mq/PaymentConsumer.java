package com.ai.tutor.videocallimservice.chat.mq;

import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.service.TutorApplicationService;
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
    private BrokerageOrderMapper brokerageOrderMapper;

    @Resource
    private TutorApplicationService tutorApplicationService;

    @Override
    public void onMessage(PaymentSuccessEvent event) {
        log.info("Received payment success event: {}", event);

        // 如果上下文类型是 BROKERAGE_ORDER，则更新 BrokerageOrder 并解锁应用
        if ("BROKERAGE_ORDER".equals(event.getContextType())) {
            Long orderId = event.getContextId();
            BrokerageOrder order = brokerageOrderMapper.selectById(orderId);
            if (order == null) {
                log.warn("BrokerageOrder {} not found", orderId);
                return;
            }

            // 更新 BrokerageOrder 状态
            // 注意：这里需要考虑并发或幂等性，但 PaymentSuccessEvent 应该已经由 PaymentOrderService 保证幂等（如果它只发送一次）
            // 或者在这里再次检查 BrokerageOrder 状态
            if (BrokerageOrderStatus.PENDING.name().equals(order.getStatus())) {
                // order.setStatus(BrokerageOrderStatus.PAID.name());
                // order.setPayTime(event.getSuccessTime()); // 如果有该字段
                brokerageOrderMapper.markPaid(order.getId(), event.getSuccessTime());
                log.info("BrokerageOrder {} status updated to PAID", orderId);

                // 触发应用解锁逻辑
                tutorApplicationService.onBrokerageOrderPaid(order.getApplicationId());
            } else {
                log.info("BrokerageOrder {} is already {}, skipping update", orderId, order.getStatus());
                // 如果已经是PAID，可能也需要确保应用解锁逻辑被触发（防止之前的解锁失败）
                if (BrokerageOrderStatus.PAID.name().equals(order.getStatus())) {
                    tutorApplicationService.onBrokerageOrderPaid(order.getApplicationId());
                }
            }
        }
    }
}
