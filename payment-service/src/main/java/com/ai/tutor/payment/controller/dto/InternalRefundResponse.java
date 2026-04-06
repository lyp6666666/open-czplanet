package com.ai.tutor.payment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "内部退款响应")
public class InternalRefundResponse {

    @Schema(description = "平台退款单号")
    private String refundNo;

    @Schema(description = "退款状态 PENDING/SUCCESS/FAILED")
    private String status;
}

