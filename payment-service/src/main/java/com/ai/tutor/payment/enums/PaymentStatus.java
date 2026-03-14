package com.ai.tutor.payment.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING("PENDING", "待支付"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "交易关闭");

    private final String code;
    private final String desc;

    PaymentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
