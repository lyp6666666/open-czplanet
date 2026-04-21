package com.ai.tutor.appointment.model.vo.email;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserEmailItemVO {
    private String emailMasked;
    private String verifyStatus;
    private String bounceStatus;
    private LocalDateTime verifiedAt;
}
