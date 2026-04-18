package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatDeliveryAckReq {
    @NotNull
    @Schema(description = "房间 id")
    private Long roomId;

    @NotNull
    @Schema(description = "当前设备已收到的最新消息 id")
    private Long lastDeliveredMsgId;
}
