package com.ai.tutor.appointment.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CursorPageResponse", description = "游标分页返回")
public class CursorPageResponse<T> {

    @Schema(description = "下一页游标（下一次请求带上）")
    private Long nextCursor;

    @Schema(description = "是否最后一页")
    private Boolean isLast;

    @Schema(description = "数据列表")
    private List<T> list;
}

