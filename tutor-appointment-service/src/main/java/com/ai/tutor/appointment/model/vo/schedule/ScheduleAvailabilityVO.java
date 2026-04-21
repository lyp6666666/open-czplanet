package com.ai.tutor.appointment.model.vo.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Schema(description = "双方日程可用性（日视图）")
public class ScheduleAvailabilityVO implements Serializable {

    @Schema(description = "查询日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "时区，当前固定 Asia/Shanghai")
    private String timezone;

    @Schema(description = "当前用户 id")
    private Long myUserId;

    @Schema(description = "对方用户 id")
    private Long otherUserId;

    @Schema(description = "当前用户当天占用块")
    private List<BusyBlockVO> myBusyBlocks;

    @Schema(description = "对方当天占用块")
    private List<BusyBlockVO> otherBusyBlocks;

    @Data
    @Builder
    @Schema(description = "日程占用块")
    public static class BusyBlockVO implements Serializable {
        @Schema(description = "日程/预约 id")
        private Long eventId;

        @Schema(description = "所属长期课程 id")
        private Long courseId;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "课节类型：TRIAL/NORMAL")
        private String lessonType;

        @Schema(description = "开始时间（毫秒时间戳）")
        private Long startAt;

        @Schema(description = "结束时间（毫秒时间戳）")
        private Long endAt;

        @Schema(description = "状态：PENDING/ACCEPTED/RESCHEDULE_PENDING/COMPLETED 等")
        private String status;
    }

    private static final long serialVersionUID = 1L;
}
