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

    /**
     * 所属长期课程 id。
     * 中文说明：线上短期课节统一挂到长期课程下，避免后续约课/调课时丢失课程归属。
     */
    private Long courseId;

    private Long parentId;

    private Long tutorId;

    private Long parentJobPostingId;

    private Long tutorJobPostingId;

    /**
     * 课程名称/标题（用于日历与授课申请展示）。
     */
    private String title;

    /**
     * 课节类型：TRIAL/NORMAL。
     */
    private String lessonType;

    /**
     * 单节标准课价（分）。
     */
    private Long lessonPriceFen;

    /**
     * 试课按标准课价的折扣比例，默认 50 表示半节课费用。
     */
    private Integer trialPricePercent;

    /**
     * 当前课节应付金额（分）。
     */
    private Long payableAmountFen;

    private Long subjectId;

    private String classMode;

    private String city;

    private String address;

    private LocalDateTime startTime;

    private Integer durationMinutes;

    private Integer status;

    private Long createdBy;

    /**
     * 关联聊天会话 id（用于跳转到会话并展示授课申请卡片）。
     */
    private Long roomId;

    private LocalDateTime proposedStartTime;

    private Long proposedBy;

    private Long cancelBy;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
