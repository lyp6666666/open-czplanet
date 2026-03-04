package com.ai.tutor.payment.strategy;

import com.ai.tutor.payment.model.entity.PaymentOrder;

/**
 * 支付策略接口
 */
public interface PaymentStrategy {

    /**
     * 获取支付渠道标识
     * @return 支付渠道代码
     */
    String getChannel();

    /**
     * 生成支付参数
     * @param order 支付订单
     * @return 支付参数（字符串或JSON对象）
     */
    Object generatePayParams(PaymentOrder order);
}
