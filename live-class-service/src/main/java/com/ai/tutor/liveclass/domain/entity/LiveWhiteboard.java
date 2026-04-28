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
public class LiveWhiteboard {
    private Long id;
    private Long liveSessionId;
    private Long courseId;
    private Long scheduleEventId;
    private String sceneJson;
    private Long sceneVersion;
    private Long updatedByUid;
    private Boolean finalized;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
