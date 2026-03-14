package com.ai.tutor.payment.client;

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
}

