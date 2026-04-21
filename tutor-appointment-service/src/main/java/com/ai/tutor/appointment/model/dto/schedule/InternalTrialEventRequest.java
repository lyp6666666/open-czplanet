package com.ai.tutor.appointment.model.dto.schedule;

import lombok.Data;

@Data
public class InternalTrialEventRequest {
    private Long courseId;
    private Long roomId;
    private Long teacherUid;
    private Long studentUid;
    private Long createdBy;
    private String title;
    private String lessonPrice;
    private Long startAt;
    private Long endAt;
    private String remark;
    private String clientRequestId;
}
