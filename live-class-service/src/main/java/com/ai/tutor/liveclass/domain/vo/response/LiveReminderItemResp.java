package com.ai.tutor.liveclass.domain.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveReminderItemResp {
    private Long sessionId;
    private Long courseId;
    private String title;
    private String status;
    private Boolean joinableNow;
    private Boolean canJoin;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private LocalDateTime joinOpenAt;
    private String peerDisplayName;
}
