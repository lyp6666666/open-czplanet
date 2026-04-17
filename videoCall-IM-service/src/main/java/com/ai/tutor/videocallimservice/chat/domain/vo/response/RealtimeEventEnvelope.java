package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class RealtimeEventEnvelope {

    /**
     * 递增事件 id，用于前端断线重连后的短窗口补偿。
     */
    private Long eventId;

    /**
     * 统一事件类型，前端通过它路由到具体业务模块。
     */
    private String eventType;

    /**
     * 粗粒度业务域，便于前端做快速筛选。
     */
    private String bizType;

    private Long targetUid;

    private Long roomId;

    private Long msgId;

    private Date occurredAt;

    private String clientId;

    private Object payload;
}
