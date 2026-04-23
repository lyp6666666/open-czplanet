package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class JoinLiveSessionAckRequest {
    private String clientType;
    private String joinMode;
    private String connectionState;
    private Boolean cameraEnabled;
    private Boolean micEnabled;
    private String cameraDeviceId;
    private String micDeviceId;
}
