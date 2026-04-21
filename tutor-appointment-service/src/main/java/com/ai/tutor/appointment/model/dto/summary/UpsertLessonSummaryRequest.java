package com.ai.tutor.appointment.model.dto.summary;

import lombok.Data;

@Data
public class UpsertLessonSummaryRequest {
    private Long lessonId;
    private String title;
    private String summaryBrief;
    private String summaryContent;
    private String homework;
}
