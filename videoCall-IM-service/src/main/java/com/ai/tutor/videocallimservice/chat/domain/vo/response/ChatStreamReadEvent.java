package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Data;

/**
 * 会话已读回执事件。
 */
@Data
public class ChatStreamReadEvent {
    /**
     * 会话 id。
     */
    private Long roomId;

    /**
     * 阅读方用户 id。
     */
    private Long readerUid;

    /**
     * 该用户已读到的最新消息 id。
     */
    private Long lastReadMsgId;
}
