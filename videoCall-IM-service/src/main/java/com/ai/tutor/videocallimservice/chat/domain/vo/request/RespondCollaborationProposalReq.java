package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "响应合作提案请求")
public class RespondCollaborationProposalReq {

    @NotBlank
    @Schema(description = "操作：ACCEPT/REJECT")
    private String action;
}
