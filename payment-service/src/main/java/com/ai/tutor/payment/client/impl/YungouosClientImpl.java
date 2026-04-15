package com.ai.tutor.payment.client.impl;

import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.yungouos.pay.entity.PayOrder;
import com.yungouos.pay.entity.RefundOrder;
import com.yungouos.pay.alipay.AliPay;
import com.yungouos.pay.common.PayException;
import com.yungouos.pay.order.SystemOrder;
import com.yungouos.pay.wxpay.WxPay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * YunGouOS Client 默认实现
 */
@Component
@RequiredArgsConstructor
public class YungouosClientImpl implements YungouosClient {

    private final PaymentProperties paymentProperties;

    @Override
    public String wechatNativePay(String outTradeNo, String totalFeeYuan, String mchId, String body, String type, String appId, String attach, String notifyUrl, String returnUrl, String appKey) {
        PaymentProperties.Yungouos yungouos = paymentProperties.getYungouos();
        if (yungouos != null && yungouos.getBaseUrl() != null && yungouos.getBaseUrl().startsWith("mock://")) {
            return "https://example.com/mockpay/wechat/" + outTradeNo;
        }
        try {
            return WxPay.nativePay(
                    outTradeNo,
                    totalFeeYuan,
                    mchId,
                    body,
                    type,
                    appId,
                    attach,
                    notifyUrl,
                    returnUrl,
                    null,
                    null,
                    null,
                    null,
                    appKey
            );
        } catch (PayException e) {
            throw e;
        }
    }

    @Override
    public String alipayNativePay(String outTradeNo, String totalFeeYuan, String mchId, String body, String type, String appId, String attach, String notifyUrl, String appKey) {
        PaymentProperties.Yungouos yungouos = paymentProperties.getYungouos();
        if (yungouos != null && yungouos.getBaseUrl() != null && yungouos.getBaseUrl().startsWith("mock://")) {
            return "https://example.com/mockpay/alipay/" + outTradeNo;
        }
        try {
            return AliPay.nativePay(
                    outTradeNo,
                    totalFeeYuan,
                    mchId,
                    body,
                    type,
                    appId,
                    attach,
                    notifyUrl,
                    null,
                    null,
                    null,
                    null,
                    null,
                    appKey
            );
        } catch (PayException e) {
            throw e;
        }
    }

    @Override
    public PayOrder getOrderInfoByOutTradeNo(String outTradeNo, String mchId, String appKey) {
        PaymentProperties.Yungouos yungouos = paymentProperties.getYungouos();
        if (yungouos != null && yungouos.getBaseUrl() != null && yungouos.getBaseUrl().startsWith("mock://")) {
            return null;
        }
        try {
            return SystemOrder.getOrderInfoByOutTradeNo(outTradeNo, mchId, appKey);
        } catch (PayException e) {
            throw e;
        }
    }

    @Override
    public RefundOrder wechatRefund(String outTradeNo, String mchId, String refundMoneyYuan, String outTradeRefundNo, String refundDesc, String notifyUrl, String appKey) {
        PaymentProperties.Yungouos yungouos = paymentProperties.getYungouos();
        if (yungouos != null && yungouos.getBaseUrl() != null && yungouos.getBaseUrl().startsWith("mock://")) {
            RefundOrder order = new RefundOrder();
            order.setOutTradeNo(outTradeNo);
            order.setOutTradeRefundNo(outTradeRefundNo);
            order.setRefundNo("MOCK_REFUND_" + outTradeRefundNo);
            order.setRefundMoney(refundMoneyYuan);
            order.setRefundStatus(1);
            order.setRefundDesc(refundDesc);
            return order;
        }
        try {
            return WxPay.orderRefund(outTradeNo, mchId, refundMoneyYuan, outTradeRefundNo, refundDesc, notifyUrl, appKey);
        } catch (PayException e) {
            throw e;
        }
    }

    @Override
    public RefundOrder alipayRefund(String outTradeNo, String mchId, String refundMoneyYuan, String outTradeRefundNo, String refundDesc, String notifyUrl, String appKey) {
        PaymentProperties.Yungouos yungouos = paymentProperties.getYungouos();
        if (yungouos != null && yungouos.getBaseUrl() != null && yungouos.getBaseUrl().startsWith("mock://")) {
            RefundOrder order = new RefundOrder();
            order.setOutTradeNo(outTradeNo);
            order.setOutTradeRefundNo(outTradeRefundNo);
            order.setRefundNo("MOCK_REFUND_" + outTradeRefundNo);
            order.setRefundMoney(refundMoneyYuan);
            order.setRefundStatus(1);
            order.setRefundDesc(refundDesc);
            return order;
        }
        try {
            return AliPay.orderRefund(outTradeNo, mchId, refundMoneyYuan, outTradeRefundNo, refundDesc, notifyUrl, appKey);
        } catch (PayException e) {
            throw e;
        }
    }
}
