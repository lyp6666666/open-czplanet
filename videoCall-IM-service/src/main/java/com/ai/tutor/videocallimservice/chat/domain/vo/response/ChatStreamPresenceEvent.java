package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ChatStreamPresenceEvent {

    /**
     * 状态发生变化的用户 uid
     */
    private Long uid;

    /**
     * true 表示在线，false 表示离线
     */
    private Boolean online;

    /**
     * 最近一次离线时间；在线事件通常为 null
     */
    private Date lastOnlineAt;
}
