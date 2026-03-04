package com.ai.tutor.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付成功事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
    private String orderNo;
    private Long userId;
    private Long amount;
    private Long contextId;
    private String contextType;
    private String transactionId;
    private LocalDateTime successTime;
}
