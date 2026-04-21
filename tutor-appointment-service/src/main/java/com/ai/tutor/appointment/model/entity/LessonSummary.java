package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSummary {
    private Long id;
    private Long lessonId;
    private Long courseId;
    private Long teacherUid;
    private Long studentUid;
    private String title;
    private String summaryStatus;
    private String summaryBrief;
    private String summaryContent;
    private String homework;
    private LocalDateTime readyAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
