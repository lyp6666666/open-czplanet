package com.ai.tutor.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundChatParticipantVO {
    private Long uid;
    private String role;
    private String name;
    private String avatar;
}
