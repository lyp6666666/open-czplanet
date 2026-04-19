package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class EndLiveSessionRequest {
    private String reason;
    private Boolean confirm;
}
