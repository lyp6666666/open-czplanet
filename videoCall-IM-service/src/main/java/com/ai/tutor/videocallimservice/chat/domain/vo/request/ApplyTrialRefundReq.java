package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "试课不通过退款申请请求（退 60% 信息费）")
public class ApplyTrialRefundReq {

    @NotNull
    @NotEmpty
    @Schema(description = "试课不通过说明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @NotEmpty
    @Schema(description = "证据图片 URL 列表（至少 1 张）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> evidenceImageUrls;
}

