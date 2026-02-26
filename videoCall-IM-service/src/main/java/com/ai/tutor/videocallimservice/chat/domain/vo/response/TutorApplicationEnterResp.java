package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "进入聊天返回（含gating状态）")
public class TutorApplicationEnterResp {
    private Boolean paymentRequired;
    private Boolean waitingForTeacherPayment;
    private Long orderId;
    private Long roomId;
}
