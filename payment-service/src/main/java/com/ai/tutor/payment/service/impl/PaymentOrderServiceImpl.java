package com.ai.tutor.payment.service.impl;

import com.ai.tutor.payment.enums.PaymentStatus;
import com.ai.tutor.payment.mapper.PaymentOrderMapper;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.service.PaymentOrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 支付订单服务实现类
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

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
                    event.setChannel(order.getChannel());
                    event.setProvider(order.getProvider());
                    event.setProviderOrderNo(order.getProviderOrderNo());
                    if (rocketMQTemplate != null) {
                        rocketMQTemplate.convertAndSend("payment-success-topic", event);
                        markEventSent(orderNo);
                    } else {
                        log.warn("RocketMQTemplate 不存在，跳过支付成功事件投递. orderNo={}", orderNo);
                        markEventSendFailed(orderNo, "RocketMQTemplate missing");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send payment success event for order {}", orderNo, e);
                markEventSendFailed(orderNo, truncateReason(e.getMessage()));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createOrReusePending(String contextType, Long contextId, Long userId, String channel, Long amount, String subject, String body, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        PaymentOrder existing = this.getOne(new QueryWrapper<PaymentOrder>()
                .eq("context_type", contextType)
                .eq("context_id", contextId)
                .eq("user_id", userId)
                .eq("channel", channel)
                .eq("status", PaymentStatus.PENDING.getCode())
                .orderByDesc("create_time")
                .last("limit 1"));

        if (existing != null) {
            boolean expired = existing.getExpireTime() != null && existing.getExpireTime().isBefore(now);
            boolean amountChanged = !Objects.equals(existing.getAmount(), amount);
            if (expired || amountChanged) {
                UpdateWrapper<PaymentOrder> closeWrapper = new UpdateWrapper<>();
                closeWrapper.eq("id", existing.getId())
                        .eq("status", PaymentStatus.PENDING.getCode())
                        .set("status", PaymentStatus.CLOSED.getCode());
                this.update(closeWrapper);
                log.info("Close stale pending payment order. orderNo={}, expired={}, amountChanged={}, oldAmount={}, newAmount={}",
                        existing.getOrderNo(), expired, amountChanged, existing.getAmount(), amount);
            } else {
                return existing;
            }
        }

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setAmount(amount);
        order.setCurrency("CNY");
        order.setChannel(channel);
        order.setProvider("YUNGOUOS");
        order.setStatus(PaymentStatus.PENDING.getCode());
        order.setContextType(contextType);
        order.setContextId(contextId);
        order.setSubject(subject);
        order.setBody(body);
        order.setClientIp(clientIp);
        order.setNotifyCount(0);
        order.setNotifyVerified(0);
        order.setEventSent(0);
        order.setExpireTime(now.plus(5, ChronoUnit.MINUTES));
        this.save(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePayData(String orderNo, String payData, LocalDateTime expireTime) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", PaymentStatus.PENDING.getCode())
                .set("pay_data", payData)
                .set("expire_time", expireTime);
        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordNotifyReceipt(String orderNo, int notifyVerified) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .setSql("notify_count = notify_count + 1")
                .set("last_notify_time", LocalDateTime.now());
        if (notifyVerified == 1) {
            updateWrapper.set("notify_verified", 1);
        }
        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSuccessFromNotify(String orderNo, String transactionId, String providerOrderNo, LocalDateTime successTime, int notifyVerified) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", PaymentStatus.PENDING.getCode())
                .set("status", PaymentStatus.SUCCESS.getCode())
                .set("transaction_id", transactionId)
                .set("provider_order_no", providerOrderNo)
                .set("success_time", successTime != null ? successTime : LocalDateTime.now())
                .setSql("notify_count = notify_count + 1")
                .set("last_notify_time", LocalDateTime.now());
        if (notifyVerified == 1) {
            updateWrapper.set("notify_verified", 1);
        }

        boolean updated = this.update(updateWrapper);
        if (updated) {
            log.info("Payment order {} updated to SUCCESS by notify, transactionId: {}", orderNo, transactionId);
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
                    event.setChannel(order.getChannel());
                    event.setProvider(order.getProvider());
                    event.setProviderOrderNo(order.getProviderOrderNo());
                    if (rocketMQTemplate != null) {
                        rocketMQTemplate.convertAndSend("payment-success-topic", event);
                        markEventSent(orderNo);
                    } else {
                        log.warn("RocketMQTemplate 不存在，跳过支付成功事件投递. orderNo={}", orderNo);
                        markEventSendFailed(orderNo, "RocketMQTemplate missing");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send payment success event for order {}", orderNo, e);
                markEventSendFailed(orderNo, truncateReason(e.getMessage()));
            }
            return true;
        }

        PaymentOrder order = getByOrderNo(orderNo);
        if (order != null && PaymentStatus.SUCCESS.getCode().equals(order.getStatus())) {
            log.info("Payment order {} is already SUCCESS, idempotent check passed.", orderNo);
            recordNotifyReceipt(orderNo, notifyVerified);
            return true;
        }
        recordNotifyReceipt(orderNo, notifyVerified);
        log.warn("Failed to update payment order {} to SUCCESS from notify. Current status might not be PENDING.", orderNo);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSuccessFromProviderQuery(String orderNo, String transactionId, String providerOrderNo, LocalDateTime successTime) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .eq("status", PaymentStatus.PENDING.getCode())
                .set("status", PaymentStatus.SUCCESS.getCode())
                .set("transaction_id", transactionId)
                .set("provider_order_no", providerOrderNo)
                .set("success_time", successTime != null ? successTime : LocalDateTime.now());

        boolean updated = this.update(updateWrapper);
        if (updated) {
            log.info("Payment order {} updated to SUCCESS by provider query, transactionId: {}", orderNo, transactionId);
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
                    event.setChannel(order.getChannel());
                    event.setProvider(order.getProvider());
                    event.setProviderOrderNo(order.getProviderOrderNo());
                    if (rocketMQTemplate != null) {
                        rocketMQTemplate.convertAndSend("payment-success-topic", event);
                        markEventSent(orderNo);
                    } else {
                        log.warn("RocketMQTemplate 不存在，跳过支付成功事件投递. orderNo={}", orderNo);
                        markEventSendFailed(orderNo, "RocketMQTemplate missing");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send payment success event for order {}", orderNo, e);
                markEventSendFailed(orderNo, truncateReason(e.getMessage()));
            }
            return true;
        }

        PaymentOrder order = getByOrderNo(orderNo);
        if (order != null && PaymentStatus.SUCCESS.getCode().equals(order.getStatus())) {
            log.info("Payment order {} is already SUCCESS after provider query, idempotent check passed.", orderNo);
            return true;
        }
        log.warn("Failed to update payment order {} to SUCCESS from provider query. Current status might not be PENDING.", orderNo);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markEventSent(String orderNo) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .set("event_sent", 1)
                .set("event_sent_time", LocalDateTime.now())
                .set("event_send_fail_reason", null);
        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markEventSendFailed(String orderNo, String reason) {
        UpdateWrapper<PaymentOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .set("event_sent", 0)
                .set("event_send_fail_reason", truncateReason(reason));
        return this.update(updateWrapper);
    }

    @Override
    public PaymentOrder getLatestSuccessByContext(String contextType, Long contextId) {
        if (contextType == null || contextType.trim().isEmpty() || contextId == null) {
            return null;
        }
        return this.getOne(new QueryWrapper<PaymentOrder>()
                .eq("context_type", contextType.trim())
                .eq("context_id", contextId)
                .eq("status", PaymentStatus.SUCCESS.getCode())
                .orderByDesc("create_time")
                .last("limit 1"));
    }

    private static String truncateReason(String reason) {
        if (reason == null) return null;
        String r = reason.trim();
        if (r.length() <= 256) return r;
        return r.substring(0, 256);
    }
}
