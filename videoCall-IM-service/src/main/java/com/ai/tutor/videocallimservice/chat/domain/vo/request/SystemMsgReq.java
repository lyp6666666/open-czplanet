package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统消息请求体（用于业务系统向聊天投递结构化卡片/通知）。
 *
 * <p>V1 主要用于“授课申请（约课）”：</p>
 * <ul>
 *   <li>LESSON_REQUEST：授课申请卡片</li>
 *   <li>LESSON_STATUS：授课申请状态变更通知</li>
 * </ul>
 */
@Data
@Schema(description = "系统消息体（结构化）")
public class SystemMsgReq {

    @NotBlank
    @Schema(description = "业务类型：LESSON_REQUEST / LESSON_STATUS", example = "LESSON_REQUEST")
    private String bizType;

    @NotNull
    @Schema(description = "业务事件 id（如授课日程/预约 id）", example = "123")
    private Long eventId;

    @Schema(description = "课程名称/标题", example = "初二数学｜一次函数强化")
    private String title;

    @Schema(description = "开始时间（毫秒时间戳）", example = "1771412400000")
    private Long startAt;

    @Schema(description = "结束时间（毫秒时间戳）", example = "1771416000000")
    private Long endAt;

    @Schema(description = "事件状态：PENDING/ACCEPTED/REJECTED/CANCELED", example = "PENDING")
    private String status;

    @Schema(description = "发起方用户 id（LESSON_REQUEST 用）", example = "10001")
    private Long creatorUserId;

    @Schema(description = "操作者用户 id（LESSON_STATUS 用）", example = "10002")
    private Long actorUserId;

    @Schema(description = "收费标准（每小时）")
    private String pricePerHour;

    @Schema(description = "上课时间（自由文本）")
    private String classTime;

    @Schema(description = "上课频次（每周次数）")
    private Integer frequencyPerWeek;

    @Schema(description = "试课开始时间（毫秒时间戳）")
    private Long trialStartAt;

    @Schema(description = "试课结束时间（毫秒时间戳）")
    private Long trialEndAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "过期时间（毫秒时间戳）")
    private Long expireAt;

    @Schema(description = "合作提案id（部分系统消息使用）")
    private Long proposalId;

    @Schema(description = "中介费订单id（部分系统消息使用）")
    private Long orderId;

    @Schema(description = "金额（分）（部分系统消息使用）")
    private Long amountFen;

    @Schema(description = "文本内容（部分系统消息使用）")
    private String content;

    @Schema(description = "上下文类型（部分系统消息使用）")
    private String contextType;

    @Schema(description = "上下文id（部分系统消息使用）")
    private Long contextId;

    @Schema(description = "授课形式（线上/线下）")
    private String teachingMode;

    @Schema(description = "报告状态（课堂 AI 结果消息使用）")
    private String reportStatus;
}
