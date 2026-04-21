package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollment {
    private Long id;
    private Long applicationId;
    private Long roomId;
    private Long proposalId;
    private Long teacherUid;
    private Long studentUid;
    private String teachingMode;
    private String courseName;
    private String classTime;
    private Integer frequencyPerWeek;
    private String lessonPrice;
    private String status;
    private LocalDateTime trialStartAt;
    private LocalDateTime trialEndAt;
    private LocalDateTime weeklyScheduleDeadlineAt;
    private LocalDateTime weeklyScheduleSubmittedAt;
    private LocalDateTime weeklyReminder12hSentAt;
    private LocalDateTime weeklyReminder6hSentAt;
    private LocalDateTime weeklyReminder1hSentAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
