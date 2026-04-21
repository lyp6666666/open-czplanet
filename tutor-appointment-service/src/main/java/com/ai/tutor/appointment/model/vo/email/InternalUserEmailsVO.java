package com.ai.tutor.appointment.model.vo.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalUserEmailsVO {
    private Long userId;
    private Integer userType;
    private EmailValue primaryEmail;
    private EmailValue summaryEmail;

    @Data
    @Builder
    public static class EmailValue {
        private String email;
        private Boolean verified;
        private String bounceStatus;
    }
}
