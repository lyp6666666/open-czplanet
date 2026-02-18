package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ChatStreamMessageEvent {
    private Long msgId;
    private Long roomId;
    private Long fromUid;
    private Long toUid;
    private Date sendTime;
    private Object body;
}
