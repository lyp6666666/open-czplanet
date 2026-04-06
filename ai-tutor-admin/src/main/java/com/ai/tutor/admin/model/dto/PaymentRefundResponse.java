package com.ai.tutor.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "调用 payment-service 发起退款响应（内部）")
public class PaymentRefundResponse {

    @Schema(description = "平台退款单号")
    private String refundNo;

    @Schema(description = "退款状态 PENDING/SUCCESS/FAILED")
    private String status;
}

