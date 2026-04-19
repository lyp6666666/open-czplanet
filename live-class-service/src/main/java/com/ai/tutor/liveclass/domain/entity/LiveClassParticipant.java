package com.ai.tutor.liveclass.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassParticipant {
    private Long id;
    private Long sessionId;
    private Long uid;
    private String role;
    private String identityType;
    private Integer joinCount;
    private LocalDateTime firstJoinAt;
    private LocalDateTime lastJoinAt;
    private LocalDateTime lastLeaveAt;
    private String onlineStatus;
    private Boolean cameraEnabled;
    private Boolean micEnabled;
    private String deviceInfoJson;
    private Integer networkScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
