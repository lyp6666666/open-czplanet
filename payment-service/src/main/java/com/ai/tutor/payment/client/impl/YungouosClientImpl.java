package com.ai.tutor.payment.client.impl;

import com.ai.tutor.payment.client.YungouosClient;
import com.ai.tutor.payment.config.PaymentProperties;
import com.yungouos.pay.alipay.AliPay;
import com.yungouos.pay.common.PayException;
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
}
