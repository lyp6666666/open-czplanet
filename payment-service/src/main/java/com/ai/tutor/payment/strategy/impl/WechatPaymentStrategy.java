package com.ai.tutor.payment.strategy.impl;

import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.strategy.PaymentStrategy;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.app.model.Amount;
import com.wechat.pay.java.service.payments.app.model.PrepayRequest;
import com.wechat.pay.java.service.payments.app.model.PrepayWithRequestPaymentResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信App支付策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPaymentStrategy implements PaymentStrategy {

    private final PaymentProperties paymentProperties;
    private AppServiceExtension appService;
    private JsapiServiceExtension jsapiService;

    @PostConstruct
    public void init() {
        PaymentProperties.Wechat config = paymentProperties.getWechat();
        
        if (Boolean.TRUE.equals(config.getMockEnabled())) {
            log.info("Mock Wechat Pay Enabled");
            return;
        }

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
                appService = new AppServiceExtension.Builder().config(rsaConfig).build();
                jsapiService = new JsapiServiceExtension.Builder().config(rsaConfig).build();
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
        PaymentProperties.Wechat config = paymentProperties.getWechat();
        
        // Mock 模式
        if (Boolean.TRUE.equals(config.getMockEnabled())) {
            Map<String, Object> mockParams = new HashMap<>();
            mockParams.put("mock", true);
            mockParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            mockParams.put("nonceStr", "mock_nonce_str");
            mockParams.put("package", "prepay_id=mock_prepay_id");
            mockParams.put("signType", "RSA");
            mockParams.put("paySign", "mock_pay_sign");
            return mockParams;
        }

        if (appService == null || jsapiService == null) {
            throw new RuntimeException("微信支付配置未完成");
        }

        // 默认走 APP 支付，如果 Extra 包含 openid 则走 JSAPI
        String openid = order.getExtra("openid"); // 假设 PaymentOrder 有 getExtra 方法，或者需要修改 PaymentOrder

        if (StringUtils.hasText(openid)) {
            // JSAPI 支付 (小程序)
            com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest request = new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
            com.wechat.pay.java.service.payments.jsapi.model.Amount amount = new com.wechat.pay.java.service.payments.jsapi.model.Amount();
            amount.setTotal(Math.toIntExact(order.getAmount()));
            amount.setCurrency("CNY");
            request.setAmount(amount);
            request.setAppid(config.getMiniappId()); // 使用小程序 AppID
            request.setMchid(config.getMchId());
            request.setDescription(order.getSubject());
            request.setNotifyUrl(config.getNotifyUrl());
            request.setOutTradeNo(order.getOrderNo());
            Payer payer = new Payer();
            payer.setOpenid(openid);
            request.setPayer(payer);

            return jsapiService.prepayWithRequestPayment(request);
        } else {
            // APP 支付
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
            
            return appService.prepayWithRequestPayment(request);
        }
    }
}
