package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IssueJoinTokenResp {
    private String provider;
    private String serverUrl;
    private String roomName;
    private String participantName;
    private String participantIdentity;
    private String accessToken;
    private LocalDateTime expireAt;
}
