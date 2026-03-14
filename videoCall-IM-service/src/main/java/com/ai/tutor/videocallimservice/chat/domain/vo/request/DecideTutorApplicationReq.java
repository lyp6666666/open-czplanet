package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "处理申请请求")
public class DecideTutorApplicationReq {

    @NotBlank
    @Schema(description = "操作：ACCEPT/REJECT")
    private String action;
}
