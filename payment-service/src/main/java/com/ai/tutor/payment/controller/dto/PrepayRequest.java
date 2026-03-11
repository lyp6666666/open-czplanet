package com.ai.tutor.payment.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 统一下单请求
 */
@Data
public class PrepayRequest {
    /**
     * 业务上下文类型，例如：BROKERAGE_ORDER
     */
    @NotBlank(message = "contextType 不能为空")
    private String contextType;

    /**
     * 业务上下文ID，例如：brokerageOrderId
     */
    @NotNull(message = "contextId 不能为空")
    private Long contextId;

    /**
     * 支付渠道：WECHAT / ALIPAY
     */
    @NotBlank(message = "channel 不能为空")
    private String channel;
}

