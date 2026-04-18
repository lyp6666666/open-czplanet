package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Data;

/**
 * 会话送达事件。
 * 该事件表示“对方设备已经收到消息”，但不代表对方已经阅读。
 */
@Data
public class ChatStreamDeliveryEvent {
    private Long roomId;
    private Long deliverUid;
    private Long lastDeliveredMsgId;
}
