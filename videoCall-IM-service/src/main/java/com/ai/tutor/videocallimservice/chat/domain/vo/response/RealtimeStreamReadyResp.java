package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RealtimeStreamReadyResp {

    /**
     * 当前连接绑定的客户端标识，前端可持久化后复用。
     */
    private String clientId;

    /**
     * 建连成功时服务端已知的最新事件 id。
     */
    private Long lastEventId;

    /**
     * 本次建连前补发的事件数，便于排查补偿是否生效。
     */
    private Integer replayedCount;
}
