package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "会话列表请求（游标分页）")
public class ChatRoomPageReq {

    @Schema(description = "页面大小", minimum = "1", maximum = "100")
    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    @Schema(description = "游标（上一页最后一条 roomId）")
    private Long cursor;
}

