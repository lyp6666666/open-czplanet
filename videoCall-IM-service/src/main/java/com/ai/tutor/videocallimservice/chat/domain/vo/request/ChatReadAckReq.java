package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatReadAckReq {
    @NotNull
    @Schema(description = "房间id")
    private Long roomId;

    @NotNull
    @Schema(description = "最后已读消息id")
    private Long lastReadMsgId;
}
