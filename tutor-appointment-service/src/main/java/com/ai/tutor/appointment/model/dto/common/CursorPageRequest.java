package com.ai.tutor.appointment.model.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通用游标分页请求：
 * - cursor：上一页最后一条记录的 id（首屏不传）
 * - pageSize：每页大小
 */
@Data
@Schema(name = "CursorPageRequest", description = "游标分页请求")
public class CursorPageRequest {

    @Schema(description = "页面大小", example = "10")
    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    @Schema(description = "游标（首屏不传，后续传上一页最后一条的 id）", example = "123")
    private Long cursor;
}

