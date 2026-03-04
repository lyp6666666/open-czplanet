package com.ai.tutor.payment.service.impl;

import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.mapper.PaymentOrderMapper;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.service.PaymentOrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付订单服务实现类
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

    @jakarta.annotation.Resource
    private org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;

    @Override
    public PaymentOrder getByOrderNo(String orderNo) {
        return this.getOne(new QueryWrapper<PaymentOrder>().eq("order_no", orderNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSuccess(String orderNo, String transactionId, LocalDateTime successTime) {
        // 使用乐观锁或状态机保证幂等性
        // 只有 PENDING 状态才能更新为 SUCCESS
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", PaymentStatus.PENDING.getCode())
                .set("status", PaymentStatus.SUCCESS.getCode())
                .set("transaction_id", transactionId)
                .set("success_time", successTime != null ? successTime : LocalDateTime.now());
        
        boolean updated = this.update(updateWrapper);
        if (updated) {
            log.info("Payment order {} updated to SUCCESS, transactionId: {}", orderNo, transactionId);
            // 发送MQ消息
            try {
                PaymentOrder order = getByOrderNo(orderNo);
                if (order != null) {
                    com.ai.tutor.common.event.PaymentSuccessEvent event = new com.ai.tutor.common.event.PaymentSuccessEvent();
                    event.setOrderNo(orderNo);
                    event.setUserId(order.getUserId());
                    event.setAmount(order.getAmount());
                    event.setContextId(order.getContextId());
                    event.setContextType(order.getContextType());
                    event.setTransactionId(transactionId);
                    event.setSuccessTime(order.getSuccessTime());
                    rocketMQTemplate.convertAndSend("payment-success-topic", event);
                }
            } catch (Exception e) {
                log.error("Failed to send payment success event for order {}", orderNo, e);
            }
            return true;
        } else {
            // 如果更新失败，可能是状态已经不是 PENDING，查询一下状态
            PaymentOrder order = getByOrderNo(orderNo);
            if (order != null && PaymentStatus.SUCCESS.getCode().equals(order.getStatus())) {
                log.info("Payment order {} is already SUCCESS, idempotent check passed.", orderNo);
                return true;
            }
            log.warn("Failed to update payment order {} to SUCCESS. Current status might not be PENDING.", orderNo);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFailed(String orderNo, String reason) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", PaymentStatus.PENDING.getCode())
                .set("status", PaymentStatus.FAILED.getCode());
        
        // 可以在 extraParams 中记录失败原因，暂时忽略
        
        boolean updated = this.update(updateWrapper);
        if (updated) {
             log.info("Payment order {} updated to FAILED. Reason: {}", orderNo, reason);
        }
        return updated;
    }
}
