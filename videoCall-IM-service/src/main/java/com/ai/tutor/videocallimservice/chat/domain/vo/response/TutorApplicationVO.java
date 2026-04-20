package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "申请详情/列表项")
public class TutorApplicationVO {
    private Long id;
    private Long senderUid;
    private Long receiverUid;
    private String senderRole;
    private String receiverRole;
    private String contextType;
    private Long contextId;
    private String teachingMode;
    private String content;
    private String status;
    private String chatAccessStatus;
    private String paymentPayerRole;
    private Long orderId;
    private Long roomId;
    private Boolean receiverRead;
    private LocalDateTime decidedAt;
    private LocalDateTime createTime;
}
