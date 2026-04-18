package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ChatPresenceResp {

    /**
     * 用户 uid
     */
    private Long uid;

    /**
     * 当前是否至少存在一个活跃 SSE 会话
     */
    private Boolean online;

    /**
     * 最近一次从在线切换到离线的时间
     */
    private Date lastOnlineAt;
}
