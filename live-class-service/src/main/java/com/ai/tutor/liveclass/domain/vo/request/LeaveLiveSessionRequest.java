package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class LeaveLiveSessionRequest {
    private String leaveReason;
    private String connectionState;
    private Integer durationSeconds;
}
