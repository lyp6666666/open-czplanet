package com.ai.tutor.videocallimservice.integration.dto;

import lombok.Data;

@Data
public class InternalUserEmailsVO {
    private Long userId;
    private Integer userType;
    private EmailValue primaryEmail;
    private EmailValue summaryEmail;

    @Data
    public static class EmailValue {
        private String email;
        private Boolean verified;
        private String bounceStatus;
    }
}
