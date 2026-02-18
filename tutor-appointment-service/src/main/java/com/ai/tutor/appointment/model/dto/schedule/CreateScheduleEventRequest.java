package com.ai.tutor.appointment.model.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建课程日程请求（用于“课程安排”日历）。
 *
 * <p>该接口面向“约课”场景，因此以课程标题、授课对象与起止时间为核心字段；
 * 为兼容现有数据模型，科目可选（后端会兜底为一个可用叶子科目）。</p>
 */
@Data
@Schema(name = "CreateScheduleEventRequest", description = "创建课程日程（发起授课申请）")
public class CreateScheduleEventRequest {

    @NotBlank
    @Schema(description = "课程名称/标题", example = "初二数学｜一次函数强化")
    private String title;

    @NotNull
    @Schema(description = "授课对象用户 id", example = "10002")
    private Long participantUserId;

    @Schema(description = "科目 id（可选，不传则后端兜底为一个可用叶子科目）", example = "201")
    private Long subjectId;

    @NotNull
    @Schema(description = "开始时间（毫秒时间戳）", example = "1771412400000")
    private Long startAt;

    @NotNull
    @Schema(description = "结束时间（毫秒时间戳）", example = "1771416000000")
    private Long endAt;

    @Schema(description = "备注（可选）", example = "希望先讲例题再做练习")
    private String description;
}

