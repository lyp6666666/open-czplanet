package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrepareLiveSessionResp {
    private Long sessionId;
    private String status;
    private String courseTitle;
    private String peerDisplayName;
    private Boolean canJoin;
    private Boolean joinableNow;
    private String joinBlockedReason;
    private Long blockingPaymentOrderId;
    private Long blockingLessonId;
    private String defaultMediaPolicy;
    private Boolean deviceCheckRequired;
}
