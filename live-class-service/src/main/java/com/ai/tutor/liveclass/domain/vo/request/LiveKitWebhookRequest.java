package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class LiveKitWebhookRequest {
    private String eventId;
    private String eventType;
    private String roomName;
    private String participantIdentity;
    private String participantName;
    private Long uid;
    private Boolean cameraEnabled;
    private Boolean micEnabled;
    private Object payload;
}
