package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "我的课程条目")
public class CourseItemVO {

    @Schema(description = "course_enrollment.id")
    private Long courseId;

    @Schema(description = "tutor_application.id")
    private Long applicationId;

    @Schema(description = "roomId（可空）")
    private Long roomId;

    @Schema(description = "教师 uid")
    private Long teacherUid;

    @Schema(description = "学生 uid")
    private Long studentUid;

    @Schema(description = "课程状态")
    private String status;

    @Schema(description = "试课结束时间（可空）")
    private LocalDateTime trialEndAt;
}

