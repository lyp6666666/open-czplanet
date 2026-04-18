package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageSearchReq extends CursorPageBaseReq {

    @NotNull
    @Schema(description = "会话id")
    private Long roomId;

    @NotBlank
    @Schema(description = "搜索关键词")
    private String keyword;
}
