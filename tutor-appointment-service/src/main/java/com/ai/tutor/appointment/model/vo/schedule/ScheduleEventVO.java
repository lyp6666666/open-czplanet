package com.ai.tutor.appointment.model.vo.schedule;

import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 日历事件展示对象（课程日程）。
 */
@Data
@Builder
@Schema(description = "课程日程（用于日历渲染）")
public class ScheduleEventVO implements Serializable {

    @Schema(description = "事件/预约 id")
    private Long id;

    @Schema(description = "所属长期课程 id")
    private Long courseId;

    @Schema(description = "课程名称/标题")
    private String title;

    @Schema(description = "课节类型：TRIAL/NORMAL")
    private String lessonType;

    @Schema(description = "单节标准课价（分）")
    private Long lessonPriceFen;

    @Schema(description = "试课收费比例，默认 50 表示半节课")
    private Integer trialPricePercent;

    @Schema(description = "当前课节应付金额（分）")
    private Long payableAmountFen;

    @Schema(description = "课节支付状态：UNBILLED/PENDING/PAYING/PAID/CANCELED")
    private String paymentStatus;

    @Schema(description = "课节支付单 id")
    private Long lessonPaymentOrderId;

    @Schema(description = "平台服务费率，线上课默认为 10%")
    private Integer platformFeeRate;

    @Schema(description = "平台服务费金额（分）")
    private Long platformFeeAmountFen;

    @Schema(description = "教师预计到账金额（分）")
    private Long teacherIncomeAmountFen;

    @Schema(description = "备注")
    private String description;

    @Schema(description = "开始时间（毫秒时间戳）")
    private Long startAt;

    @Schema(description = "结束时间（毫秒时间戳）")
    private Long endAt;

    @Schema(description = "状态：PENDING/ACCEPTED/RESCHEDULE_PENDING/REJECTED/CANCELED/COMPLETED")
    private String status;

    @Schema(description = "发起方用户 id")
    private Long creatorUserId;

    @Schema(description = "授课对象用户信息")
    private UserSimpleVO participant;

    @Schema(description = "关联聊天会话 id")
    private Long chatRoomId;

    @Schema(description = "当前课节时长（分钟）")
    private Integer durationMinutes;

    @Schema(description = "待确认改期开始时间（毫秒时间戳）")
    private Long proposedStartAt;

    @Schema(description = "待确认改期结束时间（毫秒时间戳）")
    private Long proposedEndAt;

    @Schema(description = "调课发起人用户 id")
    private Long proposedBy;

    @Schema(description = "取消课节的操作者用户 id")
    private Long cancelBy;

    private static final long serialVersionUID = 1L;
}
