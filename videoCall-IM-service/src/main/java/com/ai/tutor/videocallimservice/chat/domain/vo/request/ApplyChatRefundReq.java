package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "聊天阶段申请退费请求")
public class ApplyChatRefundReq {

    @NotNull
    @Schema(description = "会话房间id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roomId;

    @Schema(description = "退费原因（可选）")
    private String reason;
}

