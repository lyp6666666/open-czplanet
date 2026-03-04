package com.ai.tutor.payment.service;

import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDateTime;

/**
 * 支付订单服务接口
 */
public interface PaymentOrderService extends IService<PaymentOrder> {

    /**
     * 根据订单号查询订单
     * @param orderNo 商户订单号
     * @return 订单信息
     */
    PaymentOrder getByOrderNo(String orderNo);

    /**
     * 更新订单状态为成功
     * @param orderNo 商户订单号
     * @param transactionId 第三方交易号
     * @param successTime 支付成功时间
     * @return 是否更新成功
     */
    boolean updateSuccess(String orderNo, String transactionId, LocalDateTime successTime);

    /**
     * 更新订单状态为失败
     * @param orderNo 商户订单号
     * @param reason 失败原因
     * @return 是否更新成功
     */
    boolean updateFailed(String orderNo, String reason);
}
