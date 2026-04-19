package com.ai.tutor.liveclass.domain.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IssueJoinTokenRequest {
    @NotBlank
    private String clientType;
    private String deviceFingerprint;
    private String joinMode;
}
