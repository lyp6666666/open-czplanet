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

    @Schema(description = "课程状态")
    private String status;

    @Schema(description = "试课开始时间（可空）")
    private LocalDateTime trialStartAt;

    @Schema(description = "试课结束时间（可空）")
    private LocalDateTime trialEndAt;

    @Schema(description = "信息费支付截止时间（WAIT_PAY 后 48 小时，可空）")
    private LocalDateTime payDeadlineAt;

    @Schema(description = "是否已因教师超时未支付信息费而归档")
    private Boolean payExpired;

    @Schema(description = "归档原因")
    private String archiveReason;

    @Schema(description = "最近一条退费申请")
    private RefundInfo latestRefund;

    @Schema(description = "最近一条合作/试课提案")
    private ProposalInfo latestProposal;

    @Data
    @Builder
    @Schema(description = "课程退费信息")
    public static class RefundInfo {
        private Long id;
        private String type;
        private String status;
        private String reason;
        private String adminNote;
        private Integer refundPercent;
        private Long refundAmountFen;
        private LocalDateTime createTime;
        private LocalDateTime decidedAt;
    }

    @Data
    @Builder
    @Schema(description = "课程合作/试课提案信息")
    public static class ProposalInfo {
        private Long id;
        private Long fromUid;
        private Long toUid;
        private String status;
        private String pricePerHour;
        private String classTime;
        private Integer frequencyPerWeek;
        private LocalDateTime trialStartAt;
        private LocalDateTime trialEndAt;
        private String remark;
        private LocalDateTime expireAt;
    }
}
