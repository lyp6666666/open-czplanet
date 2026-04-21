package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "发起合作提案请求")
public class CreateCollaborationProposalReq {

    @NotNull
    @Schema(description = "房间id")
    private Long roomId;

    @NotBlank
    @Schema(description = "收费标准（每小时）")
    private String pricePerHour;

    @Schema(description = "试课开始时间（毫秒时间戳，北京时间展示）", example = "1771412400000")
    private Long trialStartAt;

    @Schema(description = "试课结束时间（毫秒时间戳，北京时间展示）", example = "1771419600000")
    private Long trialEndAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "客户端幂等键")
    private String clientRequestId;

    @Schema(description = "兼容旧版本：上课时间（自由文本）")
    private String classTime;

    @Schema(description = "兼容旧版本：上课频次（每周次数）")
    private Integer frequencyPerWeek;
}
