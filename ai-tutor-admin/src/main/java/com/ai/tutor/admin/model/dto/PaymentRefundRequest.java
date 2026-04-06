package com.ai.tutor.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "调用 payment-service 发起退款请求（内部）")
public class PaymentRefundRequest {

    @Schema(description = "原支付单号（payment_order.order_no），与 contextType/contextId 二选一")
    private String paymentOrderNo;

    @Schema(description = "业务上下文类型（例如 BROKERAGE_ORDER），与 paymentOrderNo 二选一")
    private String contextType;

    @Schema(description = "业务上下文ID（例如 brokerage_order.id），与 paymentOrderNo 二选一")
    private Long contextId;

    @Schema(description = "业务幂等键（refund_request.id）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long requestId;

    @Schema(description = "退款金额（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long refundAmountFen;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}

