package com.ai.tutor.appointment.model.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CancelAppointmentRequest", description = "取消预约请求")
public class CancelAppointmentRequest {

    @Schema(description = "取消原因")
    private String remark;
}

