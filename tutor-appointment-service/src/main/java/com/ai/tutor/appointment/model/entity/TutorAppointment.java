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
public class TutorAppointment {

    private Long id;

    private Long parentId;

    private Long tutorId;

    private Long parentJobPostingId;

    private Long tutorJobPostingId;

    private Long subjectId;

    private String classMode;

    private String city;

    private String address;

    private LocalDateTime startTime;

    private Integer durationMinutes;

    private Integer status;

    private Long createdBy;

    private LocalDateTime proposedStartTime;

    private Long proposedBy;

    private Long cancelBy;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
