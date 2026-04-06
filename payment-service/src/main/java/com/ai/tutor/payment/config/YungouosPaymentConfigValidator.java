package com.ai.tutor.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * YunGouOS 支付配置校验器
 *
 * 说明：
 * - 该校验器用于尽早发现配置缺失，避免在支付链路运行时才暴露问题
 * - 若需要在本地开发环境跳过，可通过 payment.enabled=false 禁用支付能力
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YungouosPaymentConfigValidator {

    private final PaymentProperties paymentProperties;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        if (Boolean.FALSE.equals(paymentProperties.getEnabled())) {
            log.warn("支付功能已禁用（payment.enabled=false），将跳过 YunGouOS 配置校验");
            return;
        }

        boolean prod = environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));

        PaymentProperties.Yungouos config = paymentProperties.getYungouos();
        if (config == null) {
            if (!prod) {
                log.warn("缺少支付配置：payment.yungouos（非生产环境将跳过校验）");
                return;
            }
            throw new IllegalStateException("缺少支付配置：payment.yungouos");
        }

        if (!StringUtils.hasText(config.getAppKey())) {
            if (!prod) {
                log.warn("缺少支付配置：payment.yungouos.appKey（非生产环境将跳过校验）");
                return;
            }
            throw new IllegalStateException("缺少支付配置：payment.yungouos.appKey");
        }
        if (!StringUtils.hasText(config.getAppId())) {
            if (!prod) {
                log.warn("缺少支付配置：payment.yungouos.appId（非生产环境将跳过校验）");
                return;
            }
            throw new IllegalStateException("缺少支付配置：payment.yungouos.appId");
        }
        if (!StringUtils.hasText(config.getWechatMchId()) && !StringUtils.hasText(config.getAlipayMchId())) {
            if (!prod) {
                log.warn("缺少支付配置：payment.yungouos.wechatMchId 或 payment.yungouos.alipayMchId（非生产环境将跳过校验）");
                return;
            }
            throw new IllegalStateException("缺少支付配置：payment.yungouos.wechatMchId 或 payment.yungouos.alipayMchId 至少配置一个");
        }
        if (!StringUtils.hasText(config.getNotifyUrl())) {
            if (!prod) {
                log.warn("缺少支付配置：payment.yungouos.notifyUrl（非生产环境将跳过校验）");
                return;
            }
            throw new IllegalStateException("缺少支付配置：payment.yungouos.notifyUrl");
        }
        String nativePayType = config.getNativePayType() == null ? null : config.getNativePayType().trim();
        if (!"1".equals(nativePayType) && !"2".equals(nativePayType)) {
            if (!prod) {
                log.warn("支付配置不合法：payment.yungouos.nativePayType 仅支持 1/2（非生产环境将跳过校验）");
                return;
            }
            throw new IllegalStateException("支付配置不合法：payment.yungouos.nativePayType 仅支持 1/2");
        }
    }
}
