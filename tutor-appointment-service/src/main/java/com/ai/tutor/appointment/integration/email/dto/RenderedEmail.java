package com.ai.tutor.appointment.integration.email.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RenderedEmail {
    private String subject;
    private String htmlBody;
}
