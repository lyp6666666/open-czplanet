package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "试课结果提交请求")
public class SubmitTrialResultReq {

    @NotBlank
    @Schema(description = "试课结果：PASS/FAIL", requiredMode = Schema.RequiredMode.REQUIRED, example = "FAIL")
    private String result;

    @Schema(description = "试课说明/不通过原因")
    private String reason;

    @Schema(description = "试课不通过时的截图证据（线下可传）")
    private List<String> evidenceImageUrls;

    @Schema(description = "试课不通过时的微信聊天录屏 URL（线下可传）")
    private String evidenceVideoUrl;

    @Schema(description = "录屏时长（秒，线下要求 1-60）")
    private Integer evidenceVideoDurationSeconds;
}
