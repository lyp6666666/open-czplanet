package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建/获取会话请求")
public class ChatRoomCreateReq {

    @NotNull
    @Schema(description = "对方用户id")
    private Long targetUid;
}

