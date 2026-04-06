package com.ai.tutor.payment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "内部退款请求（用于管理端审核通过后发起原路退款）")
public class InternalRefundRequest {

    @Schema(description = "原支付单号（payment_order.order_no），与 contextType/contextId 二选一")
    private String paymentOrderNo;

    @Schema(description = "业务上下文类型（例如 BROKERAGE_ORDER），与 paymentOrderNo 二选一")
    private String contextType;

    @Schema(description = "业务上下文ID（例如 brokerage_order.id），与 paymentOrderNo 二选一")
    private Long contextId;

    @NotNull
    @Schema(description = "业务幂等键（refund_request.id），同一 requestId 只允许发起一次退款", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long requestId;

    @NotNull
    @Schema(description = "退款金额（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long refundAmountFen;

    @NotBlank
    @Schema(description = "退款原因（用于第三方退款备注）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}

