package com.ai.tutor.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付模块配置属性
 *
 * @author ai-tutor
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    /**
     * 是否启用支付功能，默认为 true
     */
    private Boolean enabled = true;

    /**
     * 支付宝配置
     */
    private Alipay alipay = new Alipay();

    /**
     * 微信支付配置
     */
    private Wechat wechat = new Wechat();

    @Data
    public static class Alipay {
        /**
         * 支付宝应用ID
         * 获取方式：登录支付宝开放平台 (https://open.alipay.com) -> 控制台 -> 我的应用 -> 查看详情 -> APPID
         */
        private String appId;
        /**
         * 应用私钥
         * 获取方式：使用支付宝开发助手生成RSA2密钥对，将应用公钥配置到开放平台，保留私钥在此处。
         */
        private String privateKey;
        /**
         * 支付宝公钥
         * 获取方式：在开放平台配置应用公钥后，查看“支付宝公钥”（注意不是应用公钥）。
         */
        private String alipayPublicKey;
        /**
         * 异步通知地址
         * 说明：必须是公网可访问的HTTPS地址，例如 https://api.yourdomain.com/payment/notify/alipay
         */
        private String notifyUrl;
        /**
         * 支付宝网关地址
         * 正式环境：https://openapi.alipay.com/gateway.do
         * 沙箱环境：https://openapi.alipaydev.com/gateway.do
         */
        private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
    }

    @Data
    public static class Wechat {
        /**
         * 微信应用ID
         * 获取方式：微信开放平台 (https://open.weixin.qq.com) -> 管理中心 -> 应用详情 -> AppID
         */
        private String appId;
        /**
         * 商户号
         * 获取方式：微信支付商户平台 (https://pay.weixin.qq.com) -> 账户中心 -> 商户信息 -> 微信支付商户号
         */
        private String mchId;
        /**
         * APIv3密钥
         * 获取方式：微信支付商户平台 -> 账户中心 -> API安全 -> 设置APIv3密钥
         */
        private String apiV3Key;
        /**
         * 商户私钥路径
         * 获取方式：微信支付商户平台 -> 账户中心 -> API安全 -> 申请API证书 -> 下载并解压 -> apiclient_key.pem 的路径
         */
        private String privateKeyPath;
        /**
         * 商户证书序列号
         * 获取方式：微信支付商户平台 -> 账户中心 -> API安全 -> 管理证书 -> 查看序列号
         */
        private String certificateSerialNo;
        /**
         * 异步通知地址
         * 说明：必须是公网可访问的HTTPS地址，例如 https://api.yourdomain.com/payment/notify/wechat
         */
        private String notifyUrl;
    }
}
