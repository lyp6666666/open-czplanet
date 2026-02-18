package com.ai.tutor.appointment.model.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 响应授课申请请求。
 */
@Data
@Schema(name = "RespondScheduleEventRequest", description = "响应授课申请（接收/拒绝）")
public class RespondScheduleEventRequest {

    @NotBlank
    @Schema(description = "动作：ACCEPT 或 REJECT", example = "ACCEPT")
    private String action;
}

