package com.ai.tutor.payment.strategy.impl;

import com.ai.tutor.payment.config.PaymentProperties;
import com.ai.tutor.payment.enums.PaymentChannel;
import com.ai.tutor.payment.model.entity.PaymentOrder;
import com.ai.tutor.payment.strategy.PaymentStrategy;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 支付宝App支付策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPaymentStrategy implements PaymentStrategy {

    private final PaymentProperties paymentProperties;
    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        PaymentProperties.Alipay config = paymentProperties.getAlipay();
        if (StringUtils.hasText(config.getAppId()) && StringUtils.hasText(config.getPrivateKey())) {
            this.alipayClient = new DefaultAlipayClient(
                    config.getGatewayUrl(),
                    config.getAppId(),
                    config.getPrivateKey(),
                    "json",
                    "UTF-8",
                    config.getAlipayPublicKey(),
                    "RSA2"
            );
        } else {
            log.warn("Alipay configuration is missing, Alipay strategy will not work.");
        }
    }

    @Override
    public String getChannel() {
        return PaymentChannel.ALIPAY.getCode();
    }

    @Override
    public Object generatePayParams(PaymentOrder order) {
        if (alipayClient == null) {
            throw new RuntimeException("支付宝配置未完成");
        }
        
        PaymentProperties.Alipay config = paymentProperties.getAlipay();
        
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setBody(order.getBody());
        model.setSubject(order.getSubject());
        model.setOutTradeNo(order.getOrderNo());
        model.setTimeoutExpress("30m");
        // 金额单位为元，精确到2位小数
        model.setTotalAmount(new BigDecimal(order.getAmount()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).toString());
        model.setProductCode("QUICK_MSECURITY_PAY");
        request.setBizModel(model);
        request.setNotifyUrl(config.getNotifyUrl());
        
        try {
            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
            if (response.isSuccess()) {
                return response.getBody();
            } else {
                throw new RuntimeException("支付宝加签失败: " + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("Alipay generate pay params failed", e);
            throw new RuntimeException("支付宝参数生成失败", e);
        }
    }
}
