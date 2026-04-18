package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Data;

/**
 * 会话输入中事件。
 * 该事件只用于在线态展示，不参与历史补偿。
 */
@Data
public class ChatStreamTypingEvent {
    private Long roomId;
    private Long typingUid;
    private Boolean typing;
}
