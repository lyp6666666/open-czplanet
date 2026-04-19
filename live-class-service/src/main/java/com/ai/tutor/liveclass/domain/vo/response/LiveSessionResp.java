package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveSessionResp {
    private Long sessionId;
    private Long courseId;
    private String status;
    private LocalDateTime joinOpenAt;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private Long teacherUid;
    private Long studentUid;
    private Long roomId;
    private String provider;
    private String providerRoomName;
    private Boolean canJoin;
    private Boolean joinableNow;
    private Boolean peerJoined;
    private Boolean peerOnline;
    private String recordPolicy;
    private String aiPolicy;
}
