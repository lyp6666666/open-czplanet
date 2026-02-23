package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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

    @NotBlank
    @Schema(description = "上课时间（自由文本）")
    private String classTime;

    @NotNull
    @Min(1)
    @Schema(description = "上课频次（每周次数）")
    private Integer frequencyPerWeek;
}
