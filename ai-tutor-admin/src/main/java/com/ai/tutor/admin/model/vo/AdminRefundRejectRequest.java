package com.ai.tutor.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理端退款拒绝请求")
public class AdminRefundRejectRequest {

    @NotBlank
    @Schema(description = "拒绝原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}

