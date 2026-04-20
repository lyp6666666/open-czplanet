package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "试课不通过退款申请请求（线下试课审核通过后退 80% 信息费）")
public class ApplyTrialRefundReq {

    @NotNull
    @NotEmpty
    @Schema(description = "试课不通过说明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @NotEmpty
    @Schema(description = "证据图片 URL 列表（至少 1 张）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> evidenceImageUrls;

    @NotNull
    @Schema(description = "教师与家长微信聊天录屏 URL（需包含滚动并删除拉黑的完整操作）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String evidenceVideoUrl;

    @NotNull
    @Schema(description = "录屏时长（秒，最长 60 秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer evidenceVideoDurationSeconds;
}
