package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建直接支付订单请求")
public class CreateDirectBrokerageOrderReq {

    @NotNull
    @Min(1)
    @Schema(description = "金额（分）")
    private Long amountFen;

    @Schema(description = "订单描述")
    private String subject;

    @Schema(description = "关联预约ID")
    private Long appointmentId;
}
