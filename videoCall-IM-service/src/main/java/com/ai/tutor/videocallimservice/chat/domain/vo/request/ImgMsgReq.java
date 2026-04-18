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
public class ImgMsgReq {

    @NotBlank(message = "图片地址不能为空")
    @Schema(description = "图片地址")
    private String url;

    @Schema(description = "对象存储 key")
    private String objectKey;

    @Schema(description = "文件类型")
    private String contentType;

    @NotNull(message = "图片大小不能为空")
    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "回复的消息id,如果没有别传就好")
    private Long replyMsgId;
}
