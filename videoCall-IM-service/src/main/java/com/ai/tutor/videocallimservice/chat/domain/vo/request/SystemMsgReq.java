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
}

