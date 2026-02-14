package com.ai.tutor.appointment.model.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "RescheduleAppointmentRequest", description = "改期请求")
public class RescheduleAppointmentRequest {

    @NotNull
    @Schema(description = "新开始时间（ISO-8601）", example = "2026-02-15T20:00:00")
    private LocalDateTime proposedStartTime;

    @Schema(description = "新时长（分钟）", example = "60")
    private Integer durationMinutes;
}

