package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatTypingReq {
    @NotNull
    @Schema(description = "房间 id")
    private Long roomId;

    @NotNull
    @Schema(description = "是否正在输入")
    private Boolean typing;
}
