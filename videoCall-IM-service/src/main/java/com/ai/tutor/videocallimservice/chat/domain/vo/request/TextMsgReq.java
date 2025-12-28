package com.ai.tutor.videocallimservice.chat.domain.vo.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

/**
 * 文本消息入参
 */
public class TextMsgReq {

    @NotBlank(message = "内容不能为空")
    @Size(max = 1024, message = "消息内容过长")
    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "回复的消息id,如果没有别传就好")
    private Long replyMsgId;

}
