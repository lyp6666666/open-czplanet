package com.ai.tutor.appointment.model.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "CreateAppointmentRequest", description = "创建预约/邀约请求")
public class CreateAppointmentRequest {

    @NotNull
    @Schema(description = "对方用户id（家长邀约老师/老师邀约家长）")
    private Long targetUid;

    @Schema(description = "家长需求贴id（可选）")
    private Long parentJobPostingId;

    @Schema(description = "老师服务贴id（可选）")
    private Long tutorJobPostingId;

    @NotNull
    @Schema(description = "科目id")
    private Long subjectId;

    @NotNull
    @Schema(description = "开始时间（ISO-8601）", example = "2026-02-14T19:00:00")
    private LocalDateTime startTime;

    @Schema(description = "时长（分钟）", example = "60")
    private Integer durationMinutes = 60;

    @Schema(description = "授课方式：online/offline/both", example = "online")
    private String classMode;

    @Schema(description = "城市（线下时使用）")
    private String city;

    @Schema(description = "地址（线下时使用）")
    private String address;

    @Schema(description = "备注")
    private String remark;
}

