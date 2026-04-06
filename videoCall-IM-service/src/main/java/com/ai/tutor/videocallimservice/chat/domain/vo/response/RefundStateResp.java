package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "聊天页退款按钮状态")
public class RefundStateResp {

    @Schema(description = "是否允许发起退款申请")
    private boolean canApply;

    @Schema(description = "禁用原因码（可用于前端区分样式）")
    private String disableReasonCode;

    @Schema(description = "鼠标悬浮提示文案")
    private String hoverText;
}

