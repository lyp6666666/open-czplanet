package com.ai.tutor.admin.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long roomId;
    private Long fromUid;
    private Long toUid;
    private String content;
    private Integer isMasked;
    private Long replyMsgId;
    private Integer status;
    private Integer gapCount;
    private Integer type;
    private String extra;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
