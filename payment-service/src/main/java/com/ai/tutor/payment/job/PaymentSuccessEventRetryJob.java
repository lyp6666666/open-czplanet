package com.ai.tutor.payment.job;

import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.service.PaymentOrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付成功事件补偿任务
 *
 * <p>目的：当“支付单已成功入库”但 MQ 投递失败时，保证最终事件可达，驱动业务域最终一致性。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessEventRetryJob {

    private final PaymentOrderService paymentOrderService;
    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    @Scheduled(fixedDelayString = "${payment.eventRetryDelayMs:60000}")
    public void run() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<PaymentOrder> list = paymentOrderService.list(new QueryWrapper<PaymentOrder>()
                .eq("status", PaymentStatus.SUCCESS.getCode())
                .eq("event_sent", 0)
                .ge("update_time", since)
                .orderByAsc("update_time")
                .last("limit 200"));

        if (list == null || list.isEmpty()) {
            return;
        }

        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            return;
        }

        for (PaymentOrder order : list) {
            if (order == null) {
                continue;
            }
            try {
                PaymentSuccessEvent event = new PaymentSuccessEvent();
                event.setOrderNo(order.getOrderNo());
                event.setUserId(order.getUserId());
                event.setAmount(order.getAmount());
                event.setContextId(order.getContextId());
                event.setContextType(order.getContextType());
                event.setTransactionId(order.getTransactionId());
                event.setSuccessTime(order.getSuccessTime());
                event.setChannel(order.getChannel());
                event.setProvider(order.getProvider());
                event.setProviderOrderNo(order.getProviderOrderNo());
                rocketMQTemplate.convertAndSend("payment-success-topic", event);
                paymentOrderService.markEventSent(order.getOrderNo());
            } catch (Exception e) {
                log.warn("补偿投递支付成功事件失败，orderNo={}, msg={}", order.getOrderNo(), e.getMessage());
                paymentOrderService.markEventSendFailed(order.getOrderNo(), e.getMessage());
            }
        }
    }
}
