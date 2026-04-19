package com.ai.tutor.liveclass.domain.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncCourseSessionRequest {
    @NotNull
    private Long courseId;
    private Long scheduleEventId;
    private Long roomId;
    @NotNull
    private Long teacherUid;
    @NotNull
    private Long studentUid;
    private String title;
    @NotNull
    private LocalDateTime scheduledStartAt;
    @NotNull
    private LocalDateTime scheduledEndAt;
    private String recordPolicy;
    private String aiPolicy;
}
