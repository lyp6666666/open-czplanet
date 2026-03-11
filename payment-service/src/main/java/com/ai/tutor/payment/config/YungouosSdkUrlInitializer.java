package com.ai.tutor.payment.config;

import com.yungouos.pay.config.AlipayApiConfig;
import com.yungouos.pay.config.WxPayApiConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * YunGouOS SDK URL 初始化
 *
 * <p>YunGouOS SDK 内部通过静态字段维护 API 地址。本项目为了支持环境隔离（dev/staging/prod）
 * 与未来可能的网关代理，允许通过配置覆盖 baseUrl。</p>
 */
@Component
@RequiredArgsConstructor
public class YungouosSdkUrlInitializer {

    private final PaymentProperties paymentProperties;

    @PostConstruct
    public void init() {
        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        if (config == null || !StringUtils.hasText(config.getBaseUrl())) {
            return;
        }

        String baseUrl = config.getBaseUrl().trim();
        if (!baseUrl.startsWith("http")) {
            return;
        }

        WxPayApiConfig.apiUrl = baseUrl;
        WxPayApiConfig.nativePayUrl = baseUrl + "/api/pay/wxpay/nativePay";

        AlipayApiConfig.apiUrl = baseUrl;
        AlipayApiConfig.nativePayUrl = baseUrl + "/api/pay/alipay/nativePay";
    }
}

