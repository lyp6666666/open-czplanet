package com.ai.tutor.payment.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付单状态查询响应
 */
@Data
public class PaymentOrderStatusResponse {
    private String orderNo;
    private String status;
    private Long amountFen;
    private String channel;
    private LocalDateTime successTime;
    private LocalDateTime expireTime;
}

