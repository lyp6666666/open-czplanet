package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发起沟通请求")
public class ChatRoomStartReq {
    @NotNull
    @Schema(description = "对方用户id")
    private Long targetUid;

    @Size(max = 1024, message = "招呼语过长")
    @Schema(description = "首条招呼语（可选，仅在首次建立联系时发送）")
    private String greeting;
}
