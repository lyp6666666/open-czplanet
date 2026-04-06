package com.ai.tutor.payment.service;

import com.ai.tutor.payment.model.entity.PaymentRefund;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 支付退款服务
 */
public interface PaymentRefundService extends IService<PaymentRefund> {

    PaymentRefund getByRequestId(Long requestId);
}

