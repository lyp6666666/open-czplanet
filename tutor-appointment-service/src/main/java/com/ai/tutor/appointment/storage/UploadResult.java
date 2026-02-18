package com.ai.tutor.appointment.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 上传结果（前端可直接使用 url 渲染图片）。
 */
@Data
@Builder
@Schema(name = "UploadResult", description = "资源上传结果")
public class UploadResult {

    @Schema(description = "对象 Key（bucket 内路径）")
    private String objectKey;

    @Schema(description = "对外可访问 URL")
    private String url;

    @Schema(description = "Content-Type")
    private String contentType;

    @Schema(description = "文件大小（字节）")
    private Long size;
}

