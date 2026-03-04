package com.ai.tutor.payment.enums;

import lombok.Getter;

@Getter
public enum PaymentChannel {
    ALIPAY("ALIPAY", "支付宝"),
    WECHAT("WECHAT", "微信支付");

    private final String code;
    private final String desc;

    PaymentChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
