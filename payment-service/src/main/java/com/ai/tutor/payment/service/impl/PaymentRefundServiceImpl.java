package com.ai.tutor.payment.service.impl;

import com.ai.tutor.payment.mapper.PaymentRefundMapper;
import com.ai.tutor.payment.model.entity.PaymentRefund;
import com.ai.tutor.payment.service.PaymentRefundService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 支付退款服务实现
 */
@Service
public class PaymentRefundServiceImpl extends ServiceImpl<PaymentRefundMapper, PaymentRefund> implements PaymentRefundService {

    @Override
    public PaymentRefund getByRequestId(Long requestId) {
        if (requestId == null) {
            return null;
        }
        return this.getOne(new QueryWrapper<PaymentRefund>().eq("request_id", requestId).last("limit 1"));
    }
}

