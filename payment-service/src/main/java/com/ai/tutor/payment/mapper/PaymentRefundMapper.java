package com.ai.tutor.payment.mapper;

import com.ai.tutor.payment.model.entity.PaymentRefund;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付退款 Mapper
 */
@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefund> {
}

