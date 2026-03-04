package com.ai.tutor.payment.strategy.impl;

import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.strategy.PaymentStrategy;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.app.model.Amount;
import com.wechat.pay.java.service.payments.app.model.PrepayRequest;
import com.wechat.pay.java.service.payments.app.model.PrepayWithRequestPaymentResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信App支付策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPaymentStrategy implements PaymentStrategy {

    private final PaymentProperties paymentProperties;
    private AppServiceExtension service;

    @PostConstruct
    public void init() {
        PaymentProperties.Wechat config = paymentProperties.getWechat();
        if (StringUtils.hasText(config.getMchId()) && StringUtils.hasText(config.getPrivateKeyPath())) {
            try {
                // 使用自动更新平台证书的RSA配置
                Config rsaConfig = new RSAAutoCertificateConfig.Builder()
                        .merchantId(config.getMchId())
                        .privateKeyFromPath(config.getPrivateKeyPath())
                        .merchantSerialNumber(config.getCertificateSerialNo())
                        .apiV3Key(config.getApiV3Key())
                        .build();
                // 构建service
                service = new AppServiceExtension.Builder().config(rsaConfig).build();
            } catch (Exception e) {
                log.error("Failed to initialize Wechat Pay service", e);
            }
        } else {
            log.warn("Wechat configuration is missing, Wechat strategy will not work.");
        }
    }

    @Override
    public String getChannel() {
        return PaymentChannel.WECHAT.getCode();
    }

    @Override
    public Object generatePayParams(PaymentOrder order) {
        if (service == null) {
            throw new RuntimeException("微信支付配置未完成");
        }
        
        PaymentProperties.Wechat config = paymentProperties.getWechat();

        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(Math.toIntExact(order.getAmount()));
        amount.setCurrency("CNY");
        request.setAmount(amount);
        request.setAppid(config.getAppId());
        request.setMchid(config.getMchId());
        request.setDescription(order.getSubject());
        request.setNotifyUrl(config.getNotifyUrl());
        request.setOutTradeNo(order.getOrderNo());
        
        // 调用下单方法，得到应答
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
        return response;
    }
}
