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
public class LiveClassSession {
    private Long id;
    private Long courseId;
    private Long scheduleEventId;
    private Long roomId;
    private String provider;
    private String providerRoomName;
    private Long teacherUid;
    private Long studentUid;
    private String status;
    private LocalDateTime joinOpenAt;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private LocalDateTime hostJoinedAt;
    private LocalDateTime peerJoinedAt;
    private Long endedByUid;
    private String endReason;
    private String recordPolicy;
    private String aiPolicy;
    private String extraJson;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
