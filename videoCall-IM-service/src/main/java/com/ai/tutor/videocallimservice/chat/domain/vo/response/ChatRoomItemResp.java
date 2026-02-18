package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "会话列表项")
public class ChatRoomItemResp {

    @Schema(description = "会话id")
    private Long roomId;

    @Schema(description = "对方用户id")
    private Long otherUid;

    @Schema(description = "最后消息id")
    private Long lastMsgId;

    @Schema(description = "最后消息内容")
    private Object lastMsgBody;

    @Schema(description = "未读消息数")
    private Long unreadCount;

    @Schema(description = "最后活跃时间")
    private Date activeTime;
}
