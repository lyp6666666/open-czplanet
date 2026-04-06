package com.ai.tutor.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理端退款通过请求")
public class AdminRefundDecisionRequest {

    @Schema(description = "审核备注（通过时可选，拒绝时必填）")
    private String note;
}
