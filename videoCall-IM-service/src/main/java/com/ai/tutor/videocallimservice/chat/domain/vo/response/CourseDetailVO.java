package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "长期课程详情")
public class CourseDetailVO {

    @Schema(description = "course_enrollment.id")
    private Long courseId;

    @Schema(description = "tutor_application.id")
    private Long applicationId;

    @Schema(description = "roomId")
    private Long roomId;

    @Schema(description = "教师 uid")
    private Long teacherUid;

    @Schema(description = "学生 uid")
    private Long studentUid;

    @Schema(description = "授课形式 ONLINE/OFFLINE")
    private String teachingMode;

    @Schema(description = "长期课程名称")
    private String courseName;

    @Schema(description = "每周固定时间描述")
    private String classTime;

    @Schema(description = "每周课次数")
    private Integer frequencyPerWeek;

    @Schema(description = "单节课课时费文案")
    private String lessonPrice;

    @Schema(description = "长期课程状态")
    private String status;

    @Schema(description = "试课开始时间")
    private LocalDateTime trialStartAt;

    @Schema(description = "试课结束时间")
    private LocalDateTime trialEndAt;
}
