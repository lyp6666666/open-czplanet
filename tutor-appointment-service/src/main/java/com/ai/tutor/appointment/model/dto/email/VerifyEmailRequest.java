package com.ai.tutor.appointment.model.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String emailType;

    @NotBlank
    private String code;

    private String scene;

    private String bindSource;
}
