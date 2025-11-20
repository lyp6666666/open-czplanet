package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResp {

    @Schema(description = "发送者信息")
    private UserInfo fromUser;
    @Schema(description = "消息详情")
    private Message message;

    public static class UserInfo {

    }

    public static class Message {

    }


}
