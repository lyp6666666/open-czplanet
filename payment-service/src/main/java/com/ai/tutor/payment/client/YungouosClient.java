package com.ai.tutor.payment.client;

import com.yungouos.pay.entity.PayOrder;

/**
 * YunGouOS SDK 调用封装
 *
 * <p>目的：将 SDK 的静态调用封装为可注入组件，便于单元测试/集成测试进行 Mock，
 * 同时避免业务代码与 SDK 参数细节强耦合。</p>
 */
public interface YungouosClient {

    /**
     * 微信扫码支付下单（NativePay）
     *
     * @return YunGouOS 返回的二维码图片地址或支付链接（取决于 type）
     */
    String wechatNativePay(String outTradeNo,
                           String totalFeeYuan,
                           String mchId,
                           String body,
                           String type,
                           String appId,
                           String attach,
                           String notifyUrl,
                           String returnUrl,
                           String appKey);

    /**
     * 支付宝扫码支付下单（NativePay）
     *
     * @return YunGouOS 返回的二维码图片地址或支付链接（取决于 type）
     */
    String alipayNativePay(String outTradeNo,
                           String totalFeeYuan,
                           String mchId,
                           String body,
                           String type,
                           String appId,
                           String attach,
                           String notifyUrl,
                           String appKey);

    /**
     * 根据商户订单号查询第三方订单状态。
     */
    PayOrder getOrderInfoByOutTradeNo(String outTradeNo,
                                      String mchId,
                                      String appKey);

    /**
     * 微信退款（原路退款）
     *
     * @param outTradeNo        商户订单号（payment_order.order_no）
     * @param mchId             渠道商户号
     * @param refundMoneyYuan   退款金额（单位：元，字符串）
     * @param outTradeRefundNo  商户退款单号（平台退款单号）
     * @param refundDesc        退款原因
     * @param notifyUrl         退款回调地址（可空）
     * @param appKey            YunGouOS 支付密钥
     */
    com.yungouos.pay.entity.RefundOrder wechatRefund(String outTradeNo,
                                                     String mchId,
                                                     String refundMoneyYuan,
                                                     String outTradeRefundNo,
                                                     String refundDesc,
                                                     String notifyUrl,
                                                     String appKey);

    /**
     * 支付宝退款（原路退款）
     */
    com.yungouos.pay.entity.RefundOrder alipayRefund(String outTradeNo,
                                                     String mchId,
                                                     String refundMoneyYuan,
                                                     String outTradeRefundNo,
                                                     String refundDesc,
                                                     String notifyUrl,
                                                     String appKey);
}
