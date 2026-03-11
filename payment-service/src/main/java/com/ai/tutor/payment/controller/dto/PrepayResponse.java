package com.ai.tutor.payment.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一下单响应（收银台展示所需字段）
 */
@Data
public class PrepayResponse {
    /**
     * 商户订单号（本系统支付单号）
     */
    private String orderNo;

    /**
     * 支付金额（分）
     */
    private Long amountFen;

    /**
     * 支付渠道：WECHAT / ALIPAY
     */
    private String channel;

    /**
     * 二维码图片地址（当使用 type=2 时）
     */
    private String qrCodeUrl;

    /**
     * 支付链接（当使用 type=1 时，由前端自行生成二维码）
     */
    private String codeUrl;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}

