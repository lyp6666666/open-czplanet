package com.ai.tutor.videocallimservice.chat.domain.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建申请请求")
public class CreateTutorApplicationReq {

    @NotNull
    @Schema(description = "接收方用户id")
    private Long receiverUid;

    @NotBlank
    @Schema(description = "业务上下文类型：DEMAND/TUTOR/ORG_POSTING")
    private String contextType;

    @NotNull
    @Schema(description = "业务上下文id（demandId/tutorId）")
    private Long contextId;

    @NotBlank
    @Schema(description = "申请内容")
    private String content;

    @Schema(description = "授课形式：ONLINE/OFFLINE。学生主动找教师时必填；教师/机构申请需求时以后端需求为准")
    private String teachingMode;

    @Schema(description = "客户端幂等键（可选）")
    private String clientRequestId;
}
