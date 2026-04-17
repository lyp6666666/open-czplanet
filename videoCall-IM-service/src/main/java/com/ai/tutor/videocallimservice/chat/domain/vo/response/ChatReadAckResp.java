package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "已读上报结果")
public class ChatReadAckResp {

    @Schema(description = "房间id")
    private Long roomId;

    @Schema(description = "服务端确认的最后已读消息id")
    private Long lastReadMsgId;
}
